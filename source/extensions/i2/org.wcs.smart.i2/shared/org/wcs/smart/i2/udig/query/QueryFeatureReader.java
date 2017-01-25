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
package org.wcs.smart.i2.udig.query;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.query.IGeometryResultItem;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.PagedResultSetIterator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Feature reader for query result set
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private IPagedQueryResultSet results;
	
	private PagedResultSetIterator iterator;
	
	private IResultItem currentItem;
	private boolean isPoint;
	private boolean isPolygon;
	
	private Session session = null;
	
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	public QueryFeatureReader(IPagedQueryResultSet results, SimpleFeatureType ftype) {
		
		session = HibernateManager.openSession();
		try{
			this.iterator = new PagedResultSetIterator(results, session);
		}catch (Throwable t){
			session.close();
			throw t;
		}
		this.ftype = ftype;
		this.results = results;
		isPoint = ftype.getName().equals(QueryDataSource.POINT_TYPE);
		isPolygon = ftype.getName().equals(QueryDataSource.POLYGON_TYPE);
		
		moveNext();
	}
	

	/**
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		session.close();
		currentItem = null;
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
		return currentItem != null;
	}

	private void moveNext(){
		currentItem = null;
		while(iterator.hasNext()){
			IResultItem nextItem = iterator.next();
			if (matchesType(nextItem)){
				currentItem = nextItem;
				return;
			}
		}
		
	}
	
	
	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		//convert currentItem to Feature;
		SimpleFeature feature = FeatureGenerator.toFeature(ftype, currentItem, results.getQueryColumns());
		moveNext();
		return feature;
	}
	
	private boolean matchesType(IResultItem item){
		if (!(item instanceof IGeometryResultItem)) return false;
		
		Geometry g = ((IGeometryResultItem)item).getGeometry();
		if (isPoint && (g instanceof Point || g instanceof MultiPoint)) return true;
		if (isPolygon && (g instanceof Polygon || g instanceof MultiPolygon)) return true;
		return false;
	}
	

}
