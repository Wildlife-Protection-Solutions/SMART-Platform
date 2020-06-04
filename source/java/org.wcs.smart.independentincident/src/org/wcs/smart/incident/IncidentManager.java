package org.wcs.smart.incident;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.user.UserLevelManager;

public class IncidentManager {
	
	private static final String INCIDENT_PROVIDER_EXT_ID = "org.wcs.smart.independentincident.provider";
	
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
}
