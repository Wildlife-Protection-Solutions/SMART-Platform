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
package org.wcs.smart.incident;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.UuidUtils;

/**
 * Utilities for generating feature types and features for incidents
 * 
 * @author Emily
 *
 */
public class IncidentFeatureFactory {
	
	private static final String SMART_POINT_SPEC = "fid:String,id:Integer,the_geom:Point:srid=4326"; //$NON-NLS-1$
	public  static final String SMART_POINT_TYPE_NAME = "smart.independentincident"; //$NON-NLS-1$
	
	private static final String SMART_POINT_PRJ_SPEC = "fid:String,id:Integer,rawx:Double,rawy:Double,x:Double,y:Double,the_geom:LineString:srid=4326"; //$NON-NLS-1$
	public  static final String SMART_POINT_PRJ_TYPE_NAME = "smart.independentincidentprojected"; //$NON-NLS-1$
	
	
	/**
	 * Creates a simple feature type that only include the id and the geometry.
	 * @return
	 * @throws SchemaException
	 */
	public static SimpleFeatureType createSimpleIncidentSchema(String typeName) throws SchemaException{
		if (typeName.equals(SMART_POINT_TYPE_NAME)) {
			return DataUtilities.createType(SMART_POINT_TYPE_NAME, SMART_POINT_SPEC);
		}else if (typeName.equals(SMART_POINT_PRJ_TYPE_NAME)) {
			return DataUtilities.createType(SMART_POINT_PRJ_TYPE_NAME, SMART_POINT_PRJ_SPEC);
		}
		return null;
	}
	
	/**
	 * Creates a feature from an incident.  Assumes the ftype is generated
	 * from createSimpleIncidentSchema()
	 * 
	 * @param ftype
	 * @return
	 */
	public static SimpleFeature createSimpleIncidentFeature(SimpleFeatureType ftype, Waypoint incident) {
		if (ftype.getName().getLocalPart().equals("independentincident")) { //$NON-NLS-1$
			Object data[] = new Object[3];
			String name = ftype.getName() + "." + UuidUtils.uuidToString(incident.getUuid()); //$NON-NLS-1$
			int i = 0;
			data[i++] = name;
			data[i++] = incident.getId();
			data[i++] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(incident.getX(), incident.getY()));
			SimpleFeature f = SimpleFeatureBuilder.build(ftype, data, name);
			return f;
		}else if (ftype.getName().getLocalPart().equals("independentincidentprojected")) { //$NON-NLS-1$
			Object data[] = new Object[7];
			String name = ftype.getName() + "." + UuidUtils.uuidToString(incident.getUuid()); //$NON-NLS-1$
			int i = 0;
			data[i++] = name;
			data[i++] = incident.getId();
			data[i++] = incident.getRawX();
			data[i++] = incident.getRawY();
			data[i++] = incident.getX();
			data[i++] = incident.getY();
			data[i++] = GeometryFactoryProvider.getFactory().createLineString(new Coordinate[] {new Coordinate(incident.getRawX(), incident.getRawY()), new Coordinate(incident.getX(), incident.getY())});
			SimpleFeature f = SimpleFeatureBuilder.build(ftype, data, name);
			return f;
		}
		return null;
	}
	
}
