/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.model.map;

import java.util.Locale;

import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

/**
 * Factory for generating features and schemas for QAErrorItems
 * @author Emily
 *
 */
public class FeatureFactory {
	
	public static final String QA_ERROR_PNT_TYPE_NAME ="QaErrorPoints"; //$NON-NLS-1$
	public static final String QA_ERROR_LINE_TYPE_NAME ="QaErrorLines"; //$NON-NLS-1$
	
	public static Filter qaItemFilter(QaError e){
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		return ff.equals(ff.property("fid"), ff.literal(UuidUtils.uuidToString(e.getUuid()))); //$NON-NLS-1$
	}
	
	public static Filter newStatusFilter(){
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		return ff.equals(ff.property("status"), ff.literal(QaError.Status.NEW.name())); //$NON-NLS-1$
	}
	
	public static SimpleFeatureType createPointQaErrorFeatureType() throws SchemaException{
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:Point:srid=4326,"); //$NON-NLS-1$
		sb.append("fid:String,"); //$NON-NLS-1$
		sb.append("status_string:String,"); //$NON-NLS-1$
		sb.append("status:String,"); //$NON-NLS-1$
		sb.append("error_id:String,"); //$NON-NLS-1$
		sb.append("error_description:String,"); //$NON-NLS-1$
		sb.append("fix:String,"); //$NON-NLS-1$
		sb.append("qa_routine:String,"); //$NON-NLS-1$
		sb.append("data_provider:String,"); //$NON-NLS-1$
		sb.append("src_obj:String"); //$NON-NLS-1$
		
		SimpleFeatureType type =  DataUtilities.createType(QA_ERROR_PNT_TYPE_NAME, sb.toString());
		return type;
	}
	
	public static SimpleFeatureType createLineQaErrorFeatureType() throws SchemaException{
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:MultiLineString:srid=4326,"); //$NON-NLS-1$
		sb.append("fid:String,"); //$NON-NLS-1$
		sb.append("status_string:String,"); //$NON-NLS-1$
		sb.append("status:String,"); //$NON-NLS-1$
		sb.append("error_id:String,"); //$NON-NLS-1$
		sb.append("error_description:String,"); //$NON-NLS-1$
		sb.append("fix:String,"); //$NON-NLS-1$
		sb.append("qa_routine:String,"); //$NON-NLS-1$
		sb.append("data_provider:String,"); //$NON-NLS-1$
		sb.append("src_obj:String"); //$NON-NLS-1$
		
		SimpleFeatureType type =  DataUtilities.createType(QA_ERROR_LINE_TYPE_NAME, sb.toString());
		return type;
	}
	
	public static SimpleFeature createQaFeature(SimpleFeatureType type, QaError error, Locale l){
		if (error.getGeometryObject() == null) return null;
		Geometry g = error.getGeometryObject();
		if (error.getGeometryObject() instanceof LineString){
			g = new MultiLineString(new LineString[]{(LineString)error.getGeometryObject()}, GeometryFactoryProvider.getFactory());
		}
		Object[] data = new Object[10];
		int i = 0;
		data[i++] = g;
		data[i++] = error.getUuid() == null ? String.valueOf(System.nanoTime()) : UuidUtils.uuidToString(error.getUuid());
		data[i++] = error.getStatus().getGuiName(l);
		data[i++] = error.getStatus().name();
		data[i++] = error.getErrorId();
		data[i++] = error.getErrorDescription();
		data[i++] = error.getFixMessage();
		data[i++] = error.getQaRoutine().getName();
		data[i++] = error.getDataProvider().getName(l);
		data[i++] = UuidUtils.uuidToString(error.getSourceId());
		
		return SimpleFeatureBuilder.build(type, data, (String) data[1]);
	}
}
