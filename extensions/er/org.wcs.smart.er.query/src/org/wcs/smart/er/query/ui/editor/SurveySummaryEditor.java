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
package org.wcs.smart.er.query.ui.editor;

import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.SurveyQueryFactory;
import org.wcs.smart.er.query.model.SurveySummaryQueryType;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.ui.SummaryEditor;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;

/**
 * Editor for displaying query results. The editor includes two pages a tabular
 * results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveySummaryEditor extends SummaryEditor{

	public static final String ID = "org.wcs.smart.er.query.ui.SummaryEditor"; //$NON-NLS-1$

	private ISurveyEventListener surveyDesignListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			if (o instanceof SurveyDesign){
				String key = ((ISurveyQuery)getQuery()).getSurveyDesign();
				if (key != null && key.equals(((SurveyDesign) o).getKeyId())){
					reparseQuery();		
				}
			}
				
		}
	};
	
	/**
	 * Creates a new editor
	 */
	public SurveySummaryEditor(){
		super();
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DESIGN_MODIFIED, surveyDesignListener);
	}
	
	/**
	 * Disposes editor
	 */
	@Override
	public void dispose(){
		super.dispose();
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DESIGN_MODIFIED, surveyDesignListener);
	}
	
	public SummaryQuery createNewQuery(){
		return SurveyQueryFactory.createSummaryQuery();
	}

	@Override
	protected IDateFieldFilter[] getValidDateFilters() {
		return SurveySummaryQueryType.validDateFields();
	}
	
}
