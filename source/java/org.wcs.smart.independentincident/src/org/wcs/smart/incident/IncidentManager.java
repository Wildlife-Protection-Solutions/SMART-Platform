/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.incident;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.user.UserLevelManager;

/**
 * Manager for processing different incident sources and providing ui components
 * @author Emily
 *
 */
public class IncidentManager {
	
	private static final String INCIDENT_PROVIDER_EXT_ID = "org.wcs.smart.independentincident.provider"; //$NON-NLS-1$
	
	private static IncidentManager instance = null;
	private static HashMap<String, IIncidentProvider> incidentProviders = null;
	
	private IncidentManager(){
		
	}
	
	public static IncidentManager getInstance(){
		if (instance == null){
			instance = new IncidentManager();
		}
		return instance;
	}
	/**
	 * 
	 * @param waypoint the waypoint to be edited
	 * @param ops current observation options {@link ObservationHibernateManager#getPatrolOptions(org.wcs.smart.ca.ConservationArea, Session)}
	 * @return null if the patrol can be edited, otherwise a string
	 * that described reason why can't be edited.
	 */
	public String canEdit(Waypoint waypoint, ObservationOptions ops){
		if (!UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN, UserLevelManager.MANAGER, UserLevelManager.DATA_ENTRY, UserLevelManager.ANALYST)){
			return Messages.IncidentManager_Insufficientprivleges;
		}
		if (ops.getEditTime() == null || ops.getEditTime() < 0){
			return null;
		}else if (UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN, UserLevelManager.MANAGER)){
			return null;
		}else if (UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.DATA_ENTRY, UserLevelManager.ANALYST)){
			Date d = new Date();
			d.setTime( d.getTime() - (long)ops.getEditTime() * 24 * 60 * 60 * 1000 );
			if (waypoint.getDateTime().after(d)){
				return null;
			}else{
				return MessageFormat.format(Messages.IncidentManager_IncidentTooOldToEdit, new Object[]{ops.getEditTime() }) ;
			
			}
		}else{
			return null;
		}
	}
	
	public synchronized Collection<IIncidentProvider> getIncidentProviders(){
		if (incidentProviders == null) {
			loadIncidentProviders();
		}
		return incidentProviders.values();
	}
	
	public synchronized IIncidentProvider getIncidentProvider(String sourceKey) {
		if (incidentProviders == null) loadIncidentProviders();
		return incidentProviders.get(sourceKey);
	}
	/*
	 * Load source extension points
	 */
	private void loadIncidentProviders() {
		incidentProviders = new HashMap<>();
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(INCIDENT_PROVIDER_EXT_ID);
		for (IConfigurationElement element : elements){
			try{
				IIncidentProvider source = (IIncidentProvider) element.createExecutableExtension("class"); //$NON-NLS-1$
				incidentProviders.put(source.getWaypointSourceKey(), source);
			}catch (Exception ex){
				IncidentPlugIn.log("Error loading all incident providers", ex); //$NON-NLS-1$
			}
		}
	}	
	
	/**
	 * Computes the next incident ID
	 * 
	 * @param session
	 * @return
	 */
	public Integer getNextIncidentId(Session session) {
		Set<String> incidentsources = getIncidentProviders().stream()
				.map(e->e.getWaypointSourceKey()).collect(Collectors.toSet());
		
		Query<?> q = session.createQuery("SELECT max(id) + 1 FROM Waypoint WHERE sourceId IN (:source) AND conservationArea = :ca"); //$NON-NLS-1$
		q.setParameterList("source", incidentsources); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		List<?> maxIs = q.list();
		if (maxIs.size() > 0 && maxIs.get(0) != null ) return (Integer) maxIs.get(0);
		return 1;
	}
}
