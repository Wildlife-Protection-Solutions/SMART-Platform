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
package org.wcs.smart.er.map.samplingunit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Data source for sampling units.  This will find
 * all sampling units associated with a given survey design. 
 * It contains two layers, one for plots and one for transects.
 * 
 * 
 * @author Emily
 *
 */
public class SamplingUnitDataSource extends ContentDataStore{

	private SurveyDesign sd;
	
	
	/**
	 * Creates a new data source for the given survey design.
	 * @param sd
	 */
	public SamplingUnitDataSource(SurveyDesign sd){
		this.sd = sd;
	}
	
	/**
	 * 
	 * @return the design associated with the survey
	 */
	public SurveyDesign getDesign(){
		return this.sd;
	}
	/**
	 * Updates the survey design; should be called whenever a modification
	 * is made.
	 * 
	 * @param sd new survey design
	 */
	public void update(SurveyDesign sd){
		this.sd = sd;

	}
	

	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> names = new ArrayList<>();
		names.add(new NameImpl(GeometryType.PLOT.name()));
		names.add(new NameImpl(GeometryType.TRANSECT.name()));
		return names;
	}

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		return new SamplingUnitFeatureSource(entry);
	}
	

}
