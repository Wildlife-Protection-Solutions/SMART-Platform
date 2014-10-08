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
package org.wcs.smart.er.query.filter;

import org.hibernate.Session;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Filter for survey design objects.
 * 
 */
public class SurveyDesignFilter implements IFilter {

	
	/**
	 * Creates a survey design filter.
	 * 
	 * @return
	 */
	public static SurveyDesignFilter createStringFilter(String key){
		return new SurveyDesignFilter(key);
	}

	private String surveyDesignKey;

	
	/**
	 * Creates a new survey design filter
	 * 
	 * @param key survey design key
	 */
	public SurveyDesignFilter (String key){
		this.surveyDesignKey = key;
	}
	
	/**
	 * 
	 * @return the survey design key represented by the filter
	 */
	public String getKey(){
		return this.surveyDesignKey;
	}
	/**
	 * @see org.wcs.smart.patrol.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		return "surveydesign: " + surveyDesignKey;  //$NON-NLS-1$ 
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);		
	}
	
	/**
	 * Drop item not supported for this filter
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception{
		return null;
	}
}
