package org.wcs.smart.cybertracker.model;

import org.wcs.smart.dataentry.model.ConfigurableModel;

public interface IIncidentCtPackage {

	/**
	 * If package has incident option
	 * @return
	 */
	public boolean getHasIncident() ;
	/**
	 * Set has incident option
	 * 
	 * @param hasIncident
	 */
	public void setHasIncident(boolean hasIncident);
	/**
	 * 
	 * @return incident model
	 */
	public ConfigurableModel getIncidentModel() ;
	/**
	 * sets the incident model
	 * @param incidentmodel
	 */
	public void setIncidentModel(ConfigurableModel incidentmodel) ;
	
}
