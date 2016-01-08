package org.wcs.smart.incident;

import java.text.MessageFormat;
import java.util.Date;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;

public class IncidentManager {
	
	
	private static IncidentManager instance = null;
	
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
		if (ops.getEditTime() == null || ops.getEditTime() < 0){
			return null;
		}else if (SmartDB.getCurrentEmployee().getSmartUserLevel() == Employee.SmartUserLevel.DATA_ENTRY || 
				SmartDB.getCurrentEmployee().getSmartUserLevel() == Employee.SmartUserLevel.ANALYST ){
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
}
