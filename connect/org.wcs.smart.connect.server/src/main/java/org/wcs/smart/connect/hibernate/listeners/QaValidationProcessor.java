/*
 * Copyright (C) 2017 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.connect.hibernate.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.qa.RoutineExtensionManager;
import org.wcs.smart.qa.SingleItemDataProvider;
import org.wcs.smart.qa.ValidationEngine;
import org.wcs.smart.qa.er.ErWaypointDataProvider;
import org.wcs.smart.qa.incident.IncidentDataProvider;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.patrol.routine.PatrolTrackDataProvider;
import org.wcs.smart.qa.patrol.routine.PatrolWaypointDataProvider;
import org.wcs.smart.qa.patrol.routine.TrackLocationData;
import org.wcs.smart.qa.routine.ValidationTask;
import org.wcs.smart.qa.routine.WaypointLocationData;

/**
 * Auto validation job/manager
 * 
 * @author Emily
 *
 */
public class QaValidationProcessor implements Runnable{

	private SessionFactory sessionFactory = null;
	private List<Object[]> tasks = Collections.synchronizedList(new ArrayList<>());
	
	private Thread currentRunnable = null;

	public QaValidationProcessor(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Register a new data provider for validations.  
	 * Data providers must provide data when passed null date filters 
	 */
	public void addTask(Object data, Locale l){
		tasks.add(new Object[] {data, l});
		start();
	}
	
	private synchronized void start() {
		if (currentRunnable != null && currentRunnable.isAlive()) return;
		currentRunnable = new Thread(this);
		currentRunnable.start();
	}
	
	private void processItem(Object item, Locale l) {
		ConservationArea ca = null;
		SingleItemDataProvider provider = null;
		
		if (item instanceof PatrolWaypoint pw){
			ca = pw.getWaypoint().getConservationArea();
			WaypointLocationData data = new WaypointLocationData(pw.getWaypoint());
			provider = new SingleItemDataProvider(
					RoutineExtensionManager.INSTANCE.findDataProvider(PatrolWaypointDataProvider.ID), data);
		
		}else if (item instanceof Track t){
			ca = t.getPatrolLegDay().getPatrolLeg().getPatrol().getConservationArea();
			TrackLocationData data = new TrackLocationData(t);
			provider = new SingleItemDataProvider(
					RoutineExtensionManager.INSTANCE.findDataProvider(PatrolTrackDataProvider.ID), data);
		}
		if (item instanceof SurveyWaypoint sw){
			ca = sw.getWaypoint().getConservationArea();
			WaypointLocationData data = new WaypointLocationData(sw.getWaypoint());
			provider = new SingleItemDataProvider(
					RoutineExtensionManager.INSTANCE.findDataProvider(ErWaypointDataProvider.ID), data);			
		}else if (item instanceof MissionTrack mt){
			ca = mt.getMissionDay().getMission().getSurvey().getSurveyDesign().getConservationArea();
			org.wcs.smart.qa.er.TrackLocationData data = new org.wcs.smart.qa.er.TrackLocationData(mt);
			provider = new SingleItemDataProvider(
					RoutineExtensionManager.INSTANCE.findDataProvider(ErWaypointDataProvider.ID), data);
		}else if (item instanceof Waypoint wp) {
			ca = wp.getConservationArea();
			WaypointLocationData data = new WaypointLocationData(wp);
			
			if (wp.getSourceId().equals(IndepedentIncidentSource.KEY)){
				provider = new SingleItemDataProvider(
						RoutineExtensionManager.INSTANCE.findDataProvider(IncidentDataProvider.ID), data);
			}

		}
		
		if (ca != null && provider != null) {
			try(Session session = sessionFactory.openSession()){
				List<QaRoutine> routines = QueryFactory.buildQuery(session, QaRoutine.class, 
						new Object[] {"conservationArea", ca}, //$NON-NLS-1$
						new Object[] {"autoCheck", true}).getResultList(); //$NON-NLS-1$
				
				ValidationEngine engine = new ValidationEngine(l);
				
				for (QaRoutine routine : routines) {
					ValidationTask task = new ValidationTask(routine, provider, null, null, ca, l);
					engine.addValidationTask(task);
				}
			
				Collection<QaError> errors = engine.validate(session, new NullProgressMonitor());
				session.beginTransaction();
				for (QaError error : errors){
					session.persist(error);
				}
				session.getTransaction().commit();
			}catch(Exception ex){
				Logger.getLogger(QaValidationProcessor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
			}
		}
	}

	@Override
	public void run() {
		
		while(!tasks.isEmpty()) {
			Object[] data = tasks.remove(0);
			processItem(data[0], (Locale) data[1]);
		}
		
	}
	
	
}
