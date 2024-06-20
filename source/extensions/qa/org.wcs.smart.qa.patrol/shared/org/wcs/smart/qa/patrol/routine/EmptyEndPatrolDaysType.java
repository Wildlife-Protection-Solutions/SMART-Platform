/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.qa.patrol.routine;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.qa.model.IQaRoutineType;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.patrol.ILabelProvider;
import org.wcs.smart.qa.patrol.ILabelProvider.Key;
import org.wcs.smart.qa.routine.ValidationTask;

/**
 * A QA routine that finds patrol with days at the end with
 * not observation data (can have track data). 
 * 
 * Ticket: #3592
 * 
 * 
 * @author Emily
 *
 */
public class EmptyEndPatrolDaysType implements IQaRoutineType {

	public static final String ID = "org.wcs.smart.qa.patrol.emptyenddays";  //$NON-NLS-1$
	
	public EmptyEndPatrolDaysType() {
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName(Locale l) {
		return ILabelProvider.getLabel(Key.EmptyEndPatrolDaysType_Name, l);
	}

	@Override
	public String getDescription(Locale l) {
		return ILabelProvider.getLabel(Key.EmptyEndPatrolDaysType_Desc, l);
	}
	
	@Override
	public String getParameterSummary(QaRoutine routine, Locale l, Session session){
		return ""; //$NON-NLS-1$
	}
	
	@Override
	public Collection<QaError> validateData(ValidationTask task, Session session, IProgressMonitor monitor) throws Exception{
		monitor.beginTask(task.getRoutine().getName(), 103);
		monitor.subTask(ILabelProvider.getLabel(Key.LoadingString, task.getLocale()));
		
		Collection<?> data = task.getDataProvider().getData(session, task.getConservationArea(), task.getStartDate(), task.getEndDate());
		monitor.worked(1);

		List<QaError> errors = new ArrayList<>();
		int cnt = 0;
		int lastsize = 0;
		for (Object x : data){
			if (x instanceof PatrolLocationData){
				QaError error = validatePatrol((PatrolLocationData)x, task, session);
				if (error != null) errors.add(error);
			}
			cnt++;
			int work = (int)Math.round( (cnt / (double)data.size()) * 100 );
			monitor.worked(work - lastsize);
			lastsize = work;
		}
		monitor.done();
		
		return errors;
	}
	
	private QaError validatePatrol(PatrolLocationData patrold, ValidationTask task, Session session){
		Patrol p = session.get(Patrol.class, patrold.getPatrol().getUuid());
		
		List<PatrolLegDay> days = new ArrayList<PatrolLegDay>();
		for (PatrolLeg pl : p.getLegs()) {
			days.addAll(pl.getPatrolLegDays());
		}
		
		//sort newest first
		Collections.sort(days, (a,b)->-1*a.getDate().compareTo(b.getDate()));
		
		LocalDate lastDate = null;
		int obscount = 0;
		int numdayswithnoobs = 0;
		for (PatrolLegDay pld : days) {
			if (lastDate == null || pld.getDate().equals(lastDate)) {
				obscount += pld.getWaypoints().size();			
			}else {
				if (obscount == 0) {
					numdayswithnoobs ++;
					obscount += pld.getWaypoints().size();			

				}else {
					//found a day with at least one observation
					break;
				}
			}
			lastDate = pld.getDate();
		}
		if (obscount == 0) {
			numdayswithnoobs ++;
		}
		
		if (numdayswithnoobs > 0) {
			List<LineString> tracks = new ArrayList<>();
			for (PatrolLeg pl : p.getLegs()) {
				for (PatrolLegDay pld : pl.getPatrolLegDays()) {
					if (pld.getTrack() == null) continue;
					try {
						tracks.addAll(pld.getTrack().getLineStrings());
					}catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			Geometry geometry = GeometryFactoryProvider.getFactory().createMultiLineString(tracks.toArray(new LineString[tracks.size()]));
			
			return createError(task, session, patrold, geometry, 
					MessageFormat.format(ILabelProvider.getLabel(Key.EmptyEndPatrolDaysType_EmptyDays, task.getLocale()), numdayswithnoobs, p.getId()));
		}
		
		return null;
	}
	
	
	
	private QaError createError(ValidationTask task, Session session, Object data, Geometry geometry, String message){
		if (message == null) return null;
		QaError error = new QaError();
		error.setConservationArea(task.getConservationArea());
		error.setDataProviderId(task.getDataProvider().getId());
		error.setErrorDescription(message);
		error.setErrorId(task.getDataProvider().getFeatureId(session, data, task.getLocale()));
		error.setGeometryObject(geometry);
		error.setQaRoutine(task.getRoutine());
		error.setSourceId(task.getDataProvider().getFeatureSource(session, data));
		error.setStatus(QaError.Status.NEW);
		error.setValidateDate(LocalDateTime.now());
		return error;
		
	}


}
