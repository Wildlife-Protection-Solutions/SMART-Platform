/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.er.hibernate;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Filter for filtering survey designs.  Filter options include: 
 * state filter.
 * 
 * @author Emily
 *
 */
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
		
		str.append("SELECT s.uuid, s.name, s.state, s.keyId "); //$NON-NLS-1$
		str.append("FROM SurveyDesign s "); //$NON-NLS-1$
		str.append("WHERE s.conservationArea = :ca " ); //$NON-NLS-1$

		boolean and = true;
		if (states != null && states.length > 0){
			if (and ){
				str.append(" AND ("); //$NON-NLS-1$
				and = false;
			}
			str.append(" s.state IN (:states) "); //$NON-NLS-1$
		}
		str.append(" ) "); //$NON-NLS-1$
		str.append("ORDER BY s.name asc"); //$NON-NLS-1$
		
		Query query = s.createQuery(str.toString())
				.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$

		
		if (states != null && states.length > 0){
			query.setParameterList("states", this.states); //$NON-NLS-1$
		}
		return query;
	}
}
