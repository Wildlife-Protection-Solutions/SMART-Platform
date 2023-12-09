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
package org.wcs.smart.observation.query.map.geotools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.observation.udig.WaypointSimpleFeature;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;

/**
 * Feature reader for waypoint/observation query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class GeometryAttributeQueryFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private IQueryResultSetIterator<? extends IResultItem> fIterator;
	private Iterator<AttributeQueryColumn> cIterator;
	private WaypointQueryResultItem current;
	private AttributeQueryColumn currentColumn;
	private List<QueryColumn> columns;
		
	private List<AttributeQueryColumn> geometryColumns;
	
	
	private List<QueryColumn> nonAttributeColumns;
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	public GeometryAttributeQueryFeatureReader(Query query, SimpleFeatureType ftype, List<QueryColumn> columns) {
		
		this.ftype = ftype;
		this.fIterator = null;
		this.columns = columns;
		
		this.geometryColumns = new ArrayList<>();
		this.nonAttributeColumns = new ArrayList<>();
		for (QueryColumn qc : columns) {
			if (qc instanceof AttributeQueryColumn ac) {
				if (ac.getAttributeType() == Attribute.AttributeType.LINE && ftype.getTypeName().equals(QueryDataSource.LINESTRING_GEOM_ATTRIBUTE_TYPE)) {
					this.geometryColumns.add(ac);
				}else if (ac.getAttributeType() == Attribute.AttributeType.POLYGON && ftype.getTypeName().equals(QueryDataSource.POLYGON_GEOM_ATTRIBUTE_TYPE)) {
					this.geometryColumns.add(ac);
				}
			}else {
				this.nonAttributeColumns.add(qc);
			}
		}
		
		if (query instanceof IPagedQuery){
			try {
				IQueryResult cachedResults = query.getCachedResults();
				if (cachedResults != null){
					fIterator = ((IPagedQueryResultSet<?>)cachedResults).iterator(IPagedQueryResultSet.MAP_PAGE_SIZE);
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
		if (fIterator != null) fIterator.close();
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
		
		//get next feature
		while(this.fIterator.hasNext()) {
			current = (WaypointQueryResultItem) this.fIterator.next();
			cIterator = this.geometryColumns.iterator();
			
			while (cIterator.hasNext()) {
				currentColumn = cIterator.next();
				
				if (currentColumn.getValue(current) != null) {
					return true;
				}
			}
		}
		
		return false;
	}

	private SimpleFeature createFeature() {
		Object geometryValue = currentColumn.getValue(current);
		
		//create a feature from all non-attribute columns
		
		QueryColumn srcColumn = null;
		QueryColumn areaColumn = null;
		QueryColumn perimeterColumn = null;
		for (QueryColumn c : columns) {
			if (c instanceof AttributeQueryColumn aqc && aqc.getGeometryProperty() != null) {
				switch(aqc.getGeometryProperty()) {
				case AREA:
					areaColumn = c;
					break;
				case PERIMETER:
					perimeterColumn = c;
					break;
				case SOURCE:
					srcColumn = c;
					break;
				default:
					break;
				
				}
			}
		}
		
		List<Object> data = new ArrayList<Object>();
		data.add(geometryValue);
		data.add(current.getWaypointId() + "." + System.nanoTime()); //$NON-NLS-1$
		data.add(currentColumn.getAttributeId());
		data.add(currentColumn.getAttributeId());
		data.add(srcColumn.getValue(current));
		
		if (areaColumn != null) data.add(areaColumn.getValue(current));
		data.add(perimeterColumn.getValue(current));
		
		int i = data.size()-1;
		for (QueryColumn c : this.nonAttributeColumns){
			if (c.isVisible()){
				data.add(QueryColumnUtils.getValue(current, c, ftype.getDescriptor(i++), Locale.getDefault()));
			}
		}
		return new WaypointSimpleFeature(SimpleFeatureBuilder.build(ftype, data, (String)data.get(1)), current.getWaypointUuid());
		
		
	}
	
	
	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		return createFeature();		
	}
	

}
