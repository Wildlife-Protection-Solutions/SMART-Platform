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

import org.wcs.smart.query.model.filter.IFilter;

/**
 * A definition panel for filtering survey track queries.  Excludes the incident/observation
 * query option.  Includes the ability to
 * change the survey design.
 * 
 * @author Emily
 * 
 */
public class TrackFilterDefinitionPanel extends FilterDefintionPanel {

	public static final String ID = "org.wcs.smart.query.er.survey.definition.filter.track"; //$NON-NLS-1$
	
	public TrackFilterDefinitionPanel(){
		super(true, false);
	}
	
	@Override
	public String getId(){
		return ID;
	}
	
	@Override
	public void setFilterType(IFilter.FilterType filterType){
		
	}
}
