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
package org.wcs.smart.entity.map;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.entity.query.EntitySightingQuery;
import org.wcs.smart.entity.query.SightingPagedResults;
import org.wcs.smart.entity.query.SightingResultItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
/**
 * Entity sightings query feature reader.
 * 
 * @author Emily
 *
 */
public class EntityQueryDataSourceFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private Iterator<IResultItem> fIterator;
	private EntitySightingQuery  query;
	
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	public EntityQueryDataSourceFeatureReader(EntitySightingQuery query,
			SimpleFeatureType ftype) {
		
		this.ftype = ftype;
		this.fIterator = null;
		this.query = query;
		
		try {
			Object cachedResults = query.getCachedResults(new NullProgressMonitor());
			if (cachedResults != null){
				fIterator = ((SightingPagedResults)cachedResults).iterator(IPagedQueryResultSet.MAP_PAGE_SIZE);
			}
		} catch (Exception e) {
			QueryPlugIn.log(e.getMessage(), e);
		}
		
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
		if (fIterator == null){
			return false;
		}
		return fIterator.hasNext();
	}

	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {
		
		SightingResultItem next = (SightingResultItem) this.fIterator.next();
		SimpleFeature f = createSightingResult(next, query.getQueryColumns(), ftype);
		return f;
	}
	
	
	/**
	 * Converts a sighting result item to a feature.
	 * The feature type must have been generated 
	 * from the same set of query table columns.
	 * 
	 * @param it the query result item 
	 * @param columns the columns that make up the feature type
	 * @param ftype the feature type 
	 * @return created feature 
	 */
	public static SimpleFeature createSightingResult(SightingResultItem it, List<QueryColumn> columns, SimpleFeatureType  ftype){
		
		GeometryFactory gf = new GeometryFactory();
		Object[] data = new Object[columns.size() + 2];
		data[0] = gf.createPoint(new Coordinate(it.getWaypointX(), it.getWaypointY()));
		data[1] = it.getEntityId() + "." + SmartUtils.encodeHex(it.getWaypointUuid()); //$NON-NLS-1$ 
		for (int i = 0; i < columns.size(); i ++){
			data[i+2] = QueryColumn.getValue(it, columns.get(i), ftype.getDescriptor(i + 1));
		}
		return new EntityFeature( SimpleFeatureBuilder.build(ftype, data, (String)data[1]));
		
	}
	

}
