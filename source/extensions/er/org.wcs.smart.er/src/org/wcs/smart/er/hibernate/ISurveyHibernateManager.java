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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyEditorInput;

public interface ISurveyHibernateManager {

	/**
	 * Gets all sampling units for a conservation area and survey.
	 * This includes all fixed sampling units and reconnaissance
	 * sampling units (represented as tracks).
	 *  
	 * <p>
	 * If in CCAA mode, will return all sampling units in all
	 * conservation ares whose survey keys match.
	 * </p>
	 *  
	 * @return all sampling units for the given conservation area
	 */
	public List<Object> getSamplingUnits(SurveyDesign survey, Session s);
	

	/**
	 * Returns all surveys design that match the given filter.  If the filter
	 * is not provided all surveys are returned.
	 * 
	 * @param s
	 * @param filter filter or null if not filter should be applied
	 * @return
	 */
	public List<SurveyDesignEditorInput> getSurveyDesignEditorInputs(Session s, SurveyDesignFilter filter);
	
	/**
	 * Returns all surveys that match the given filter.  If the filter
	 * is not provided all surveys are returned.
	 * 
	 * @param s
	 * @param filter filter or null if not filter should be applied
	 * @return
	 */
	public List<SurveyEditorInput> getSurveys(Session s, SurveyFilter filter);
	
	
	/**
	 * Returns all surveys associated with an active survey design
	 * for the current conservation area.
	 * 
	 * @param s
	 * @return
	 */
	public List<Survey> getActiveSurveys(Session s);
	
	/**
	 * Returns all surveys associated with an active survey design
	 * for the current conservation area and the given survey design
	 * 
	 * @param s
	 * @return
	 */
	public List<Survey> getActiveSurveys(SurveyDesign sd, Session s);
	
	/**
	 * Returns a set of sampling unit types applicable
	 * for the given survey design 
	 */
	public Set<SamplingUnit.SamplingUnitType> getSamplingUnitTypes(SurveyDesign sd, Session s);
}
