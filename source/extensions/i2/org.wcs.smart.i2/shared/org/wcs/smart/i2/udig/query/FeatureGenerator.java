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
package org.wcs.smart.i2.udig.query;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.wcs.smart.i2.query.IGeometryResultItem;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryColumn.Type;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;

/**
 * Tools for generating features and feature schemas for intelligence queries.
 * 
 * @author Emily
 *
 */
public class FeatureGenerator {

	/**
	 * Converts a query result item to a feature
	 * 
	 * @param ftype the target feature type
	 * @param item the query result item
	 * @param columns the query columns in the query results
	 * @return
	 */
	public static SimpleFeature toFeature(SimpleFeatureType ftype, IResultItem item, List<IQueryColumn> columns){
		List<Object> data = new ArrayList<Object>();
		data.add(((IGeometryResultItem)item).getGeometry());
		
		if (item instanceof IntelObservationResultItem){
			IntelObservationResultItem ii = (IntelObservationResultItem)item;
			
			StringBuilder sb = new StringBuilder();
			sb.append(ii.getLocationId());
			sb.append(".");
			sb.append(DateFormat.getInstance().format(ii.getLocationDate()));
			data.add(sb.toString()); 
		}else{
			data.add(System.nanoTime());
		}
		
		int i = 0;
		for (IQueryColumn c : columns){
			if (c.getDataType() != Type.GEOMETRY){
				if (c.isVisible()){
					data.add(convertValue(item, c, ftype.getDescriptor(i++)));
				}
			}
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data.get(1));
	}
	
	/**
	 * Converts results item value to appropriate value for feature type
	 * @param item
	 * @param column
	 * @param descriptor
	 * @return
	 */
	private static Object convertValue(IResultItem item, IQueryColumn column, AttributeDescriptor descriptor){
		Object x =  column.getValue(item);
		
		if (x instanceof Boolean){
			if ((Boolean)x){
				x = 1;
			}else{
				x = 0;
			}
		}
		if (column.getDataType() == IQueryColumn.Type.TIME &&
				descriptor.getType().getBinding().equals(String.class)){
				//this is a datetime object which needs to be converted to a string
				x = DateFormat.getTimeInstance().format((Date)x);
		}
		return x;
	}
	
	/**
	 * Generates a feature schema for the given set of columns.  A single geometry
	 * column is assumed and only visible columns are included in the feature schema.
	 * 
	 * @param geometryType
	 * @param typeName
	 * @param columns
	 * @return
	 * @throws SchemaException
	 */
	public static SimpleFeatureType generateFeatureType(String geometryType, Name typeName, List<IQueryColumn> columns) throws SchemaException{
		
		Set<String> usedNames = new HashSet<>();
		usedNames.add("name");//not a valid column name
		usedNames.add("the_geom");
		usedNames.add("fid");
		
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:" + geometryType + ":srid=4326,");
		sb.append("fid:String,");
		for (IQueryColumn c : columns){
			if (c.getDataType() != Type.GEOMETRY && c.isVisible()){
				String name = generateFieldName(c.getColumnName(), usedNames);
				usedNames.add(name.toLowerCase());
				sb.append(name);
				sb.append(":");
				sb.append(c.getDataType().getFeatureType());
				sb.append(",");
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		return DataUtilities.createType(typeName.getNamespaceURI(), typeName.getLocalPart(), sb.toString());
	}
	
	
	/**
	 * Generates the name for a simple feature type field 
	 * @param name column name
	 * @param usedNames list of names already used for the simple feautre type
	 * @return
	 */
	private static String generateFieldName(String name, Set<String> usedNames){
		
		boolean forShape = false;
		name = name.replaceAll(" ", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		name = name.replaceAll("[^\\p{L}\\p{Nd}_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (forShape && name.length() > 10){
			name = name.substring(0, 10);
		}
		
		String tempname = name;
		int cnt = 1;
		while(usedNames.contains(tempname.toUpperCase())){
			String postfix = "_" + cnt; //$NON-NLS-1$
			tempname = name + postfix; 
			if ( forShape && tempname.length() > 10){
				tempname = name.substring(0, 10-postfix.length()) + postfix;
			}
			cnt++;
		}
		return tempname;
	}
}
