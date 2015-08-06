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
package org.wcs.smart.er.ui.mision.udig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Feature reader for mission observation points.
 * 
 * @author Emily
 * @author elitvin
 *
 */
public class MissionFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType featureType;
	private GeometryFactory gf = new GeometryFactory();
	private Iterator<SurveyWaypoint> iterator;
	
	public MissionFeatureReader(Mission mission, SimpleFeatureType type){
		List<SurveyWaypoint> me = new ArrayList<SurveyWaypoint>();
		for (MissionDay md : mission.getMissionDays()){
			me.addAll(md.getWaypoints());
		}
		iterator = me.iterator();
		this.featureType = type;
	}
	
	@Override
	public SimpleFeatureType getFeatureType() {
		return this.featureType;
	}

	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {
		SimpleFeature feature = createFeature(iterator.next());
		return feature;
	}

	@Override
	public boolean hasNext() throws IOException {
		return iterator.hasNext();
	}

	@Override
	public void close() throws IOException {
	}
	
	//String spec = 
	//"fid:String,id:Integer,date:Date,sampling_unit_id:String,observation:String,comment:String,geom:Point:srid=4326"; //$NON-NLS-1$
	private SimpleFeature createFeature(SurveyWaypoint point){
		String fid = point.getWaypoint().getId() + "." + UuidUtils.uuidToString(point.getWaypoint().getUuid()); //$NON-NLS-1$
		
		Object[] data = new Object[7];
		data[0] = fid;
		data[1] = point.getWaypoint().getId();
		data[2] = point.getWaypoint().getDateTime();
		if (point.getSamplingUnit() != null){
			data[3] = point.getSamplingUnit().getId();
		}else{
			data[3] = ""; //$NON-NLS-1$
		}
		data[4] = point.getWaypoint().getObservationsAsString();
		data[5] = point.getWaypoint().getComment();
		data[6] = gf.createPoint(new Coordinate(point.getWaypoint().getX(),point.getWaypoint().getY()));		
		
		return new SurveyFeature(SimpleFeatureBuilder.build(featureType, data, fid));
	}
}
