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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn.FixedColumns;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordResultItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * A feature reading for intelligence points associated with intelligence
 * record query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class IntelQueryFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {
	private static GeometryFactory gf = new GeometryFactory();
	
	private SimpleFeatureType ftype;
	private Iterator<? extends IResultItem> fIterator;
	
	private IntelligenceRecordResultItem currentIntel = null;
	private Iterator<IntelligencePoint> subInterator;
	private SimpleFeature next;
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	public IntelQueryFeatureReader(IntelligenceRecordQuery query,
			SimpleFeatureType ftype) {
		
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
		
		next = getNext();
	}

	private SimpleFeature getNext(){
		while (subInterator == null || !subInterator.hasNext()){
			if (!fIterator.hasNext()){
				return null;
			}
			currentIntel = (IntelligenceRecordResultItem) fIterator.next();	
			Session s = HibernateManager.openSession();
			try{
				Intelligence i = (Intelligence) s.load(Intelligence.class, currentIntel.getUuid());
				List<IntelligencePoint> pnts = new ArrayList<IntelligencePoint>(i.getPoints());
				subInterator = pnts.iterator();
			}finally{
				s.close();
			}
		}
	
		IntelligencePoint ip = subInterator.next();
		return toSimpleFeature(ftype, currentIntel, ip);
	}
	
	/**
	 * Creates a simple feature from an intelligence point
	 * @param ftype feature type
	 * @param currentIntel intelligence record
	 * @param ip intelligence point
	 * @return 
	 */
	public static SimpleFeature toSimpleFeature(SimpleFeatureType ftype, IntelligenceRecordResultItem currentIntel, IntelligencePoint ip){
		int size = FixedQueryColumn.FixedColumns.values().length+2;
		if (!SmartDB.isMultipleAnalysis()){
			size = size - 2;
		}
		Object[] data = new Object[size];
		int i = 0;
		data[i++] = gf.createPoint(new Coordinate(ip.getX(), ip.getY()));
		data[i++] = UuidUtils.uuidToString(ip.getUuid());
		for (FixedQueryColumn.FixedColumns c : FixedQueryColumn.FixedColumns.values()){
			boolean add = true;
			if (c == FixedColumns.CA_ID || c == FixedColumns.CA_NAME){
				if (!SmartDB.isMultipleAnalysis()){
					add = false;
				}
			}
			if (add){
				switch(c){
				case CA_ID:
					data[i++] = currentIntel.getConservationAreaId();
					break;
				case CA_NAME:
					data[i++] = currentIntel.getConservationAreaName();
					break;
				case INTEL_DATE_FROM:
					data[i++] = currentIntel.getFromDate();
					break;
				case INTEL_DATE_RECIEVED:
					data[i++] = currentIntel.getReceivedDate();
					break;
				case INTEL_DATE_TO:
					data[i++] = currentIntel.getToDate();
					break;
				case INTEL_DESCRIPTION:
					data[i++] = currentIntel.getDescription();
					break;
				case INTEL_INFORMANT_ID:
					data[i++] = currentIntel.getInformantId();
					break;
				case INTEL_NAME:
					data[i++] = currentIntel.getName();
					break;
				case INTEL_PATROL_SOURCE:
					data[i++] = currentIntel.getPatrolId();
					break;
				case INTEL_SOURCE:
					data[i++] = currentIntel.getSource();
					break;
				default:
					break;
					
				}
			}
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
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
		return next != null;
	}

	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {
		SimpleFeature current = next;
		next = getNext();
		return current;
		
	}
}
