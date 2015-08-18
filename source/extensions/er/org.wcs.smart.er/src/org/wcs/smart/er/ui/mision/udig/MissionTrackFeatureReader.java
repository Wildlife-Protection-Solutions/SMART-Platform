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
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.util.UuidUtils;

/**
 * Feature reader for mission tracks.
 * 
 * @author Emily
 *
 */
public class MissionTrackFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType featureType;
	private Iterator<MissionTrack> iterator;
	
	public MissionTrackFeatureReader(Mission mission, SimpleFeatureType type){
		List<MissionTrack> me = new ArrayList<MissionTrack>();
		for (MissionDay md : mission.getMissionDays()){
			me.addAll(md.getTracks());
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
	
	private SimpleFeature createFeature(MissionTrack point){
		String fid = UuidUtils.uuidToString(point.getUuid());
		Object[] data = new Object[7];
		data[0] = fid;
		data[1] = point.getId();
		data[2] = point.getMissionDay().getDate();
		data[3] = point.getSamplingUnit() == null ? "" : point.getSamplingUnit().getId(); //$NON-NLS-1$
		data[4] = point.getMissionDay().getMission().getId();
		try{
			data[5] = point.getGeometryLengthKm();
			data[6] = point.getLineString();
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		}
		return SimpleFeatureBuilder.build(featureType, data, fid);
	}
}
