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

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyEditorInput;

/**
 * To be implemeneted when ccaa analysis implemented for ccaa survey;
 * 
 * @author Emily
 *
 */
public class CcaaSurveyHibernateManager implements ISurveyHibernateManager{

	@Override
	public List<Object> getSamplingUnits(SurveyDesign survey, Session s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SurveyDesignEditorInput> getSurveyDesignEditorInputs(Session s,
			SurveyDesignFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Survey> getActiveSurveys(Session s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Survey> getActiveSurveys(SurveyDesign sd, Session s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SurveyEditorInput> getSurveys(Session s, SurveyFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SamplingUnitType> getSamplingUnitTypes(SurveyDesign sd, Session s) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
