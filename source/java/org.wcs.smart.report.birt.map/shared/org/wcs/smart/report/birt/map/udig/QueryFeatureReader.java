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
package org.wcs.smart.report.birt.map.udig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.IResultIterator;
import org.eclipse.birt.data.engine.api.IResultMetaData;
import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.report.birt.map.MapLayerInfo;

/**
 * Feature reader for BIRT SMART Query Results
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	
	private IResultIterator itr;
	private IResultMetaData md;
	private MapLayerInfo info;
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	public QueryFeatureReader(IQueryResults queryResults,
			SimpleFeatureType ftype,
			MapLayerInfo info) {
		
		this.info = info;
		this.ftype = ftype;
		try{
			this.itr = queryResults.getPreparedQuery().execute(null).getResultIterator();
			this.md = itr.getResultMetaData();	
		}catch (Exception ex){
			Logger.getLogger(QueryFeatureReader.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
		}
		
	}
	

	/**
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		try {
			if (itr != null){
				itr.close();
				itr = null;
			}
		} catch (BirtException e) {
			throw new IOException(e);
		}
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
		try {
			if (itr == null) return false;
			return itr.next();
		} catch (BirtException e) {
			throw new IOException(e);
		}
	}

	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		try{
			List<Object> data = new ArrayList<Object>();
			data.add(itr.getValue(info.getGeometryColumn()));
			data.add(String.valueOf(System.nanoTime()));
			//create a feature
			for (int k = 1; k <= md.getColumnCount(); k ++){
				
				if (md.getColumnLabel(k).equals(info.getGeometryColumn())){
					continue;
				}
				data.add(itr.getValue(md.getColumnLabel(k)));
			}
			return SimpleFeatureBuilder.build(ftype, data, (String)data.get(1));
		}catch(Exception ex){
			throw new IOException(ex);
		}
	}
	
}
