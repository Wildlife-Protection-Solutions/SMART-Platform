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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.SurveyWaypoint;

/**
 * Feature reader for mission observation points.
 * 
 * @author Emily
 * @author elitvin
 *
 */
public class MissionFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType featureType;
	private Iterator<SurveyWaypoint> iterator;
	private String typename;
	
	public MissionFeatureReader(Mission mission, SimpleFeatureType type, String typename){
		List<SurveyWaypoint> me = new ArrayList<SurveyWaypoint>();
		for (MissionDay md : mission.getMissionDays()){
			me.addAll(md.getWaypoints());
		}
		iterator = me.iterator();
		this.featureType = type;
		this.typename = typename;
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
		if (typename.equals(MissionDataSource.MISSIONRAWWAYPOINT_TYPE)){
			return SurveyFeatureFactory.createWaypointPrjFeature(featureType, point);
		}else if (typename.equals(MissionDataSource.MISSIONWAYPOINT_TYPE)){ 	
			return SurveyFeatureFactory.createWaypointFeature(featureType, point);
		}
		return null;
	}
}
