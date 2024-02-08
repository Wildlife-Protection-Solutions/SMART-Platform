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
package org.wcs.smart.query.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.query.common.model.GeometrySummaryQueryResult;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.UuidUtils;

/**
 * Feature reader for the results of an
 * asset summary query.  The assumption is
 * the first column is the station/location id
 * the result of the columns contain the results data.
 *  
 * 
 * @author Emily
 *
 */
public class SummaryQueryFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private List<QueryColumn> columns;
	int currentIndex = -1;
	
	private GeometrySummaryQueryResult results;
	
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	public SummaryQueryFeatureReader(SummaryQuery query, SimpleFeatureType ftype, List<QueryColumn> columns) {
		this.ftype = ftype;
		this.columns = new ArrayList<>(columns);
		for (Iterator<QueryColumn> iterator = this.columns.iterator(); iterator.hasNext();) {
			QueryColumn queryColumn = iterator.next();
			if (queryColumn.isDefaultGeometryColumn()) iterator.remove();
		}
	
		this.results = (GeometrySummaryQueryResult)query.getCachedResults();
		currentIndex = 0;
	}
	

	/**
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
	}

	/**
	 * @see org.geotools.data.FeatureReader#getFeatureType()
	 */
	@Override
	public SimpleFeatureType getFeatureType() {
		return ftype;
	}

	/**
	 * @see org.geotools.data.FeatureReader#hasNext()
	 */
	@Override
	public boolean hasNext() throws IOException {
		return currentIndex < results.getNumDataRows();
	}

	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		int thisIndex = currentIndex;
		currentIndex ++;
		
		String id = results.getRowHeaderValues()[thisIndex][0].getFullName();
		UUID uuid = UuidUtils.stringToUuid(results.getRowHeaderValues()[thisIndex][0].getIdentifier());
		
		List<Object> data = new ArrayList<Object>();
		data.add(GeometryFactoryProvider.getFactory().createPoint(results.getCoordinate(uuid)));
		data.add(id.trim().toLowerCase() + "." + System.nanoTime()); //$NON-NLS-1$
		data.add(id);
		
		for (int i = 0; i < columns.size() - 1; i ++) {
			data.add(results.getData()[thisIndex][i]);
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data.get(1));
		
	}
	
}
