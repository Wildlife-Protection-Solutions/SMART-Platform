package org.wcs.smart.cybertracker.model;

import org.wcs.smart.dataentry.model.ConfigurableModel;

public interface IIncidentCtPackage {

	public boolean getHasIncident() ;
	public void setHasIncident(boolean hasIncident);
	public ConfigurableModel getIncidentModel() ;
	public void setIncidentModel(ConfigurableModel incidentmodel) ;
	
}
