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
package org.wcs.smart.er.query.ui.panels.definition;

import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.ui.editor.SurveyQueryEventManager;
import org.wcs.smart.er.query.ui.panels.ISurveyPanel;

/**
 * Query survey design change listener for 
 * survey definition panels.  Refreshes the panel
 * when the survey design is changes. 
 * 
 * @author Emily
 *
 */
public class DefinitionListener implements SurveyQueryEventManager.QuerySurveyDesignChangeListener  {

	private ISurveyPanel panel;
	
	/**
	 * @param panel Survey panel to refresh
	 */
	public DefinitionListener(ISurveyPanel panel){
		this.panel = panel;
	}
	
	@Override
	public void surveyDesignChange(ISurveyQuery query, SurveyDesign newDesign) {
		if (panel.getQuery() == null || panel.getQuery().equals(query)){
			panel.refreshPanel(newDesign);
		}
	}

}
