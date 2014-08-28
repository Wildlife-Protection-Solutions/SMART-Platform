package org.wcs.smart.er.hibernate;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.SmartDB;

public class SurveyDesignFilter {
	
	private SurveyDesign.State[] states = null;
	
	/**
	 * Creates a new filter with default values.
	 */
	public SurveyDesignFilter(){
		setDefaults();
	}
	
	/**
	 * 
	 * @return patrol type filters
	 */
	public SurveyDesign.State[] getSurveyStateFilters(){
		return this.states;
	}
	
	/**
	 * Resets all values to the default
	 */
	public void setDefaults(){
		this.states = new SurveyDesign.State[]{SurveyDesign.State.ACTIVE};
	}
		
	/**
	 * Sets the survey states to fill.  Set to null to 
	 * include all states.
	 * 
	 * @param types list of patrol types
	 */
	public void setSurveyStates(SurveyDesign.State[] states){
		this.states = states;
	}
	
	/**
	 * Builds a query that returns the following survey design
	 * fields: {uuid, name, state} for any survey design
	 * in the current conservation area.
	 * 
	 * @param s
	 * @return
	 */
	public Query buildQuery(Session s){ 
		StringBuilder str = new StringBuilder();
		
		str.append("SELECT s.uuid, lbl.value, s.state "); //$NON-NLS-1$
		str.append("FROM SurveyDesign s, Label lbl "); //$NON-NLS-1$
		str.append("WHERE s.conservationArea = :ca " ); //$NON-NLS-1$
		str.append("AND  lbl.id.element.uuid = s.uuid AND lbl.id.language = :language "); //$NON-NLS-1$

		boolean and = true;
		if (states != null && states.length > 0){
			if (and ){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			str.append(" s.state IN (:states) "); //$NON-NLS-1$
		}
		str.append(" ) ");
		str.append("ORDER BY lbl.value asc"); //$NON-NLS-1$
		
		Query query = s.createQuery(str.toString())
				.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
				.setParameter("language", SmartDB.getCurrentLanguage()); //$NON-NLS-1$

		
		if (states != null && states.length > 0){
			query.setParameterList("states", this.states); //$NON-NLS-1$
		}
		return query;
	}
}
