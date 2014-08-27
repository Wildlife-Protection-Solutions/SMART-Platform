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
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Feature reader for missionligence points.
 * @author Emily
 * @author elitvin
 *
 */
public class MissionFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private Mission mission;
	private SimpleFeatureType featureType;
	private int cnt;
	private GeometryFactory gf = new GeometryFactory();
	
	public MissionFeatureReader(Mission mission, SimpleFeatureType type){
		this.mission = mission;
		this.featureType = type;
	}
	
	@Override
	public SimpleFeatureType getFeatureType() {
		return this.featureType;
	}

	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {
		SimpleFeature feature = createFeature(mission.getWaypoints().get(cnt));
		cnt++;
		return feature;
	}

	@Override
	public boolean hasNext() throws IOException {
		return cnt < mission.getWaypoints().size();
	}

	@Override
	public void close() throws IOException {
	}
	
	private SimpleFeature createFeature(SurveyWaypoint point){
		String fid = SmartUtils.encodeHex(point.getWaypoint().getUuid());
		Point pnt = gf.createPoint(new Coordinate(point.getWaypoint().getX(),point.getWaypoint().getY()));
		return SimpleFeatureBuilder.build(featureType, new Object[]{fid,pnt},fid);
	}
}
