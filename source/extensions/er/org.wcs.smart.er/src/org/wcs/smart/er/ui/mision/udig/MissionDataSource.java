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
import java.util.HashMap;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.internal.Messages;

/**
 * Data source for mission observations and tracks.
 * 
 * @author Emily
 * @author elitvin
 *
 */
public class MissionDataSource extends AbstractDataStore{

	public static final String MISSIONWAYPOINT_TYPE = "MissionPoint"; //$NON-NLS-1$
	public static final String MISSIONTRACK_TYPE = "MissionTrack"; //$NON-NLS-1$
	
	private HashMap<String, SimpleFeatureType> schemas = new HashMap<String, SimpleFeatureType>();
	
	private MissionService service;
	
	public MissionDataSource(MissionService service){
		this.service = service;
	}

	@Override
	public void dispose(){
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.store.ContentDataStore#createTypeNames()
	 */
	@Override
	public String[] getTypeNames()  {
		return new String[]{MISSIONWAYPOINT_TYPE, MISSIONTRACK_TYPE};
	}
	/* (non-Javadoc)
	 * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String)
	 */
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
		if (typeName.equals(MISSIONWAYPOINT_TYPE)) {
			return new MissionFeatureReader(this.service.getMissionRecord(), getSchema(typeName));
		}else if (typeName.equals(MISSIONTRACK_TYPE)){
			return new MissionTrackFeatureReader(this.service.getMissionRecord(), getSchema(typeName));
		}
		return null;
	}

	
	/**
	 * @see org.geotools.data.AbstractDataStore#getSchema(java.lang.String)
	 */
	@Override
	public SimpleFeatureType getSchema(String typeName) throws IOException {
		SimpleFeatureType type = schemas.get(typeName);
		if (type == null){
			try {
				if (typeName.equals(MISSIONWAYPOINT_TYPE)) {
					type = createPointSchema();
				}else if (typeName.equals(MISSIONTRACK_TYPE)){
					type = createTrackSchema();
				}
			}catch(SchemaException ex){
				throw new IOException(Messages.MissionDataSource_SchemaNotSupported + ex.getLocalizedMessage(), ex);
			}
			schemas.put(typeName, type);
		}
		return type;
	}

	private SimpleFeatureType createPointSchema() throws SchemaException{
		String spec = "fid:String,id:Integer,date:Date,observation:String,comment:String,geom:Point:srid=4326"; //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType("smart." + MISSIONWAYPOINT_TYPE, spec); //$NON-NLS-1$
		return type;
	}
	
	private SimpleFeatureType createTrackSchema() throws SchemaException{
		String spec = "fid:String,id:String,date:Date,sampling_unit_id:String,mission_id:String,distance:Double,geom:LineString:srid=4326"; //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType("smart." + MISSIONTRACK_TYPE, spec); //$NON-NLS-1$
		return type;
	}
}
