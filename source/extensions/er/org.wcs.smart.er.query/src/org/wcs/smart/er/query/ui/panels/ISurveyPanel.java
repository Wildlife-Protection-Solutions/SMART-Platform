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
package org.wcs.smart.er.query.ui.panels;

import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.query.model.Query;

/**
 * An interface for a survey query panel.  Can be used for
 * both definition and item panels.
 * 
 * Provides the ability to get a survey design, update
 * the panel based on a new design and get the current query.
 * 
 * @author Emily
 *
 */
public interface ISurveyPanel {
	
	/**
	 * 
	 * @return the current survey design or null if not applicable
	 */
	public SurveyDesign getSurveyDesign();
	
//	/**
//	 * Updates the panel based on the new survey design
//	 * key.
//	 * 
//	 * @param currentDesign
//	 * @param qType
//	 */
//	public void refreshPanel(String surveyDesignKey);
	/**
	 * The same as refreshPanel(String surveyDesignKey) except
	 * it uses the surveydesign object provided instead of the key.
	 * @param surveyDesign
	 */
	public void refreshPanel(SurveyDesign surveyDesign);
	/**
	 * 
	 * @return the current query or null if not applicable
	 */
	public Query getQuery();
	
}
