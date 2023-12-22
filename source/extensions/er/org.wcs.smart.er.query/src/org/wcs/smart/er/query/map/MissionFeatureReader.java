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
package org.wcs.smart.er.query.map;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Session;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.engine.ISurveyQueryMissionResult;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.util.UuidUtils;

/**
 * Feature reading for mission tracks associated with observation
 * query.
 * 
 * @author Emily
 *
 */
public class MissionFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private Iterator<?> fIterator;
	
	private Session session = null;
	
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	public MissionFeatureReader(SimpleQuery query,
			SimpleFeatureType ftype) {
		
		this.ftype = ftype;
		this.fIterator = null;
		
		
		try {
			Object cachedResults = query.getCachedResults();
			fIterator = ((ISurveyQueryMissionResult) cachedResults).getMissionUuids().iterator();
			
		} catch (Exception e) {
			QueryPlugIn.log(e.getMessage(), e);
		}
	}
	

	/**
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		if (fIterator != null && fIterator instanceof Closeable){
			((Closeable) fIterator).close();
		}
		if (session != null){
			session.close();
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
		if (fIterator == null){
			return false;
		}
		return fIterator.hasNext();
	}

	private synchronized Session getSession(){
		if (session == null){
			session = HibernateManager.openSession();
		}
		return session;
	}
	
	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		byte[] next = (byte[]) this.fIterator.next();
		Mission mission = (Mission) getSession().get(Mission.class, UuidUtils.byteToUUID(next));
		SimpleFeature f = createObservationFeature(mission, ftype);
		return f;
		
	}

	public static SimpleFeature createObservationFeature(Mission mission, SimpleFeatureType  ftype){
		//"fid:String,id:String,start:Date,end:Date,comment:String,geom:LineString:srid=4326"
		
		Object[] data = new Object[6];
		List<LineString> geoms = new ArrayList<LineString>();
		for (MissionDay md : mission.getMissionDays()){
			for (MissionTrack mt : md.getTracks()){
				try{
					geoms.add(mt.getLineString());
				}catch (Exception ex){
					ERQueryPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		
		if (geoms.size() > 0){
			Geometry g = GeometryFactoryProvider.getFactory().createMultiLineString(geoms.toArray(new LineString[geoms.size()]));
			data[0] = g;
		}else{
			data[0] = null;
		}
		
		data[1] = mission.getId() + "." + UuidUtils.uuidToString(mission.getUuid()); //$NON-NLS-1$
		data[2] = mission.getId();
		data[3] = mission.getStartDate();
		data[4] = mission.getEndDate();
		data[5] = mission.getComment();
		
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
}
