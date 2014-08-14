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
package org.wcs.smart.er.query.model;

import org.wcs.smart.er.model.SurveyDesign;

/**
 * Interface for survey queries.
 * 
 * @author Emily
 *
 */
public interface ISurveyQuery {

	/**
	 * Sets the survey design key
	 * @param key
	 */
	public void setSurveyDesign(String key);
	
	/**
	 * 
	 * @return the survey design key
	 */
	public String getSurveyDesign();
	
	/**
	 * Sets the survey design object;  this should
	 * call setSurveyDeisgn(String key) to ensure the
	 * key is updated correctly.
	 * 
	 * @param design
	 */
	public void setSurveyDesign(SurveyDesign design);
	
	/**
	 * Gets the survey design object.
	 * 
	 * @return
	 */
	public SurveyDesign getSurveyDesignAsObject(); 
}
