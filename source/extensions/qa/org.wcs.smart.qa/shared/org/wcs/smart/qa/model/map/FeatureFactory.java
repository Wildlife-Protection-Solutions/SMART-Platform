package org.wcs.smart.qa.model.map;

import java.util.Locale;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class FeatureFactory {
	
	public static final String QA_ERROR_TYPE_NAME ="org.wcs.smart.qa.error";
	
	public static SimpleFeatureType createPointQaErrorFeatureType() throws SchemaException{
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:Point:srid=4326,");
		sb.append("fid:String,");
		sb.append("status:String,");
		sb.append("error_id:String,");
		sb.append("error_description:String,");
		sb.append("qa_routine:String,");
		sb.append("data_provider:String,");
		sb.append("src_obj:String");
		
		SimpleFeatureType type =  DataUtilities.createType(QA_ERROR_TYPE_NAME, sb.toString());
		return type;
	}
	
	public static SimpleFeatureType createLineQaErrorFeatureType() throws SchemaException{
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:MultiLineString:srid=4326,");
		sb.append("fid:String,");
		sb.append("status:String,");
		sb.append("error_id:String,");
		sb.append("error_description:String,");
		sb.append("qa_routine:String,");
		sb.append("data_provider:String,");
		sb.append("src_obj:String");
		
		SimpleFeatureType type =  DataUtilities.createType(QA_ERROR_TYPE_NAME, sb.toString());
		return type;
	}
	
	public static SimpleFeature createQaFeature(SimpleFeatureType type, QaError error, Locale l){
		if (error.getGeometryObject() == null) return null;
		Geometry g = error.getGeometryObject();
		if (error.getGeometryObject() instanceof LineString){
			g = new MultiLineString(new LineString[]{(LineString)error.getGeometryObject()}, GeometryFactoryProvider.getFactory());
		}
		Object[] data = new Object[8];
		data[0] = g;
		data[1] = error.getUuid() == null ? String.valueOf(System.nanoTime()) : UuidUtils.uuidToString(error.getUuid());
		data[2] = error.getStatus().name();
		data[3] = error.getErrorId();
		data[4] = error.getErrorDescription();
		data[5] = error.getQaRoutine().getName();
		data[6] = error.getDataProvider().getName(l);
		data[7] = UuidUtils.uuidToString(error.getSourceId());
		
		return SimpleFeatureBuilder.build(type, data, (String) data[1]);
	}
}
