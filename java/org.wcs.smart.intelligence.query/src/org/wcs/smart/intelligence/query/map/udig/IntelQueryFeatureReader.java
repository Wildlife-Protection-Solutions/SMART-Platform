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
package org.wcs.smart.intelligence.query.map.udig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordResultItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * A feature reading for intelligence points associated with intelligence
 * record query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class IntelQueryFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {
	
	private SimpleFeatureType ftype;
	private IQueryResultSetIterator<? extends IResultItem> fIterator;
	private List<QueryColumn> cols = null;
	
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	public IntelQueryFeatureReader(IntelligenceRecordQuery query,
			SimpleFeatureType ftype) {
		cols = query.getQueryColumns(Locale.getDefault(), null);
		this.ftype = ftype;
		this.fIterator = null;
		
		if (query instanceof IPagedQuery){
			try {
				IPagedQueryResultSet cachedResults = (IPagedQueryResultSet) query.getCachedResults();
				if (cachedResults != null){
					fIterator = cachedResults.iterator(IPagedQueryResultSet.MAP_PAGE_SIZE);
				}
			} catch (Exception e) {
				QueryPlugIn.log(e.getMessage(), e);
			}	
		}	
		
	}
	
	
	/**
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		fIterator.close();
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
		return fIterator.hasNext();
	}

	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {
		IResultItem it = fIterator.next();
		return toSimpleFeature(cols, ftype, (IntelligenceRecordResultItem)it);
		
		
	}
	
	/**
	 * Creates a simple feature from an intelligence point
	 * @param ftype feature type
	 * @param currentIntel intelligence record
	 * @param ip intelligence point
	 * @return 
	 */
	public static SimpleFeature toSimpleFeature(List<QueryColumn> columns, 
			SimpleFeatureType ftype, IntelligenceRecordResultItem currentIntel){		
		List<Object>data = new ArrayList<Object>();
		data.add(currentIntel.asGeometry(IntelligenceRecordResultItem.GEOMCOLUMN_KEY));
		data.add(UuidUtils.uuidToString(currentIntel.getUuid()));
		int i = 0;
		for (QueryColumn c : columns){
			if (c.isVisible()){
				data.add(QueryColumnUtils.getValue(currentIntel, c, ftype.getDescriptor(i++)));
			}
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data.get(1));
	}
}
