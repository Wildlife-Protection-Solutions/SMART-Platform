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

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.observation.udig.ObservationAttributeFeatureFactory;

/**
 * Data source for mission observations and tracks.
 * 
 * @author Emily
 * @author elitvin
 *
 */
public class MissionFeatureSource extends ContentFeatureSource{

	public MissionFeatureSource(ContentEntry entry) {
		super(entry, Query.ALL);
	}
	
	private MissionDataSource getSource() {
		return (MissionDataSource)entry.getDataStore();
	}

	public boolean getDefaultVisibility() {
		if (entry.getTypeName().equals(MissionDataSource.MISSIONTRACK_TYPE)) return true;
		if (entry.getTypeName().equals(MissionDataSource.MISSIONWAYPOINT_TYPE)) return true;
		if (entry.getTypeName().equals(MissionDataSource.OBS_ATTRIBUTE_LINESTRING)) return true;
		if (entry.getTypeName().equals(MissionDataSource.OBS_ATTRIBUTE_POLYGON)) return true;
		return false;
	}
	
	public String getLayerName() {
		if (entry.getTypeName().equals(MissionDataSource.MISSIONTRACK_TYPE)) return Messages.MissionFeatureSource_TrackLayerName;
		if (entry.getTypeName().equals(MissionDataSource.MISSIONWAYPOINT_TYPE)) return Messages.MissionFeatureSource_WaypointLayerName;
		if (entry.getTypeName().equals(MissionDataSource.MISSIONRAWWAYPOINT_TYPE)) return Messages.MissionFeatureSource_RawWaypointLayerName;
		if (entry.getTypeName().equals(MissionDataSource.OBS_ATTRIBUTE_LINESTRING)) return "LineString Attributes";
		if (entry.getTypeName().equals(MissionDataSource.OBS_ATTRIBUTE_POLYGON)) return "Polygon Attributes";
		return entry.getTypeName();
	}
	
	@Override
	protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
		return null;
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		return -1;
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		if (entry.getTypeName().equals(MissionDataSource.MISSIONWAYPOINT_TYPE)) {
			return new MissionFeatureReader(getSource().getMission(), getSchema(), entry.getTypeName());
		}else if (entry.getTypeName().equals(MissionDataSource.MISSIONTRACK_TYPE)){
			return new MissionTrackFeatureReader(getSource().getMission(), getSchema());
		}else if (entry.getTypeName().equals(MissionDataSource.MISSIONRAWWAYPOINT_TYPE)){
			return new MissionFeatureReader(getSource().getMission(), getSchema(), entry.getTypeName());
		}else if (entry.getTypeName().equals(MissionDataSource.OBS_ATTRIBUTE_LINESTRING)){
			return new MissionFeatureReader(getSource().getMission(), getSchema(), entry.getTypeName());
		}else if (entry.getTypeName().equals(MissionDataSource.OBS_ATTRIBUTE_POLYGON)){
			return new MissionFeatureReader(getSource().getMission(), getSchema(), entry.getTypeName());
		}
		
		return null;
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try {
			if (entry.getTypeName().equals(MissionDataSource.MISSIONWAYPOINT_TYPE)) {
				return SurveyFeatureFactory.createWaypointSchema();
			}else if (entry.getTypeName().equals(MissionDataSource.MISSIONTRACK_TYPE)){
				return  SurveyFeatureFactory.createTrackSchema();
			}else if (entry.getTypeName().equals(MissionDataSource.MISSIONRAWWAYPOINT_TYPE)) {
				return SurveyFeatureFactory.createWaypointPrjSchema();
			}else if (entry.getTypeName().equals(MissionDataSource.OBS_ATTRIBUTE_LINESTRING)) {
				return ObservationAttributeFeatureFactory.createObservationLineStringSchema(MissionDataSource.OBS_ATTRIBUTE_LINESTRING);
			}else if (entry.getTypeName().equals(MissionDataSource.OBS_ATTRIBUTE_POLYGON)) {
				return ObservationAttributeFeatureFactory.createObservationPolygonSchema(MissionDataSource.OBS_ATTRIBUTE_POLYGON);
			}
		}catch(SchemaException ex){
			throw new IOException(Messages.MissionDataSource_SchemaNotSupported + ex.getLocalizedMessage(), ex);
		}
		return null;
	}

	
}
