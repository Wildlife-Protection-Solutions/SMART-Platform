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
package org.wcs.smart.query.model;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.opengis.feature.type.AttributeDescriptor;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn.ColumnType;


/**
 * A column available for output 
 * by a observation query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class QueryColumnUtils implements Cloneable{

	/**
	 * Gets the value from the result item, converts if to correct type if
	 * required and returns the value.
	 * 
	 * @param item query result item to get results from 
	 * @param column query column being processed
	 * @param descriptor output feature type column descriptor 
	 * @return
	 */
	public static Object getValue(IResultItem item, QueryColumn column, AttributeDescriptor descriptor){
		Object x =  column.getValue(item);
		
		if (x instanceof Boolean){
			if ((Boolean)x){
				x = 0;
			}else{
				x = 1;
			}
		}
		if (column.getType() == QueryColumn.ColumnType.TIME &&
				descriptor.getType().getBinding().equals(String.class)){
				//this is a datetime object which needs to be converted to a string
				x = DateFormat.getTimeInstance().format((Date)x);
		}
		return x;
	}

	/**
	 * Creates a feature definition string from the list of columns.
	 * 
	 * @param columns the columns to include in the query type
	 * @param supportsTime if Time data type is supported in output type or not.
	 * @return feature type definition string prefixed with "," 
	 */
	public static String createFeatureDefinitionString(List<QueryColumn> columns, boolean supportsTime, boolean forShape){
		StringBuilder sb = new StringBuilder();
		HashSet<String> names = new HashSet<String>();
		for (int i = 0; i < columns.size(); i++){
			if (!columns.get(i).isVisible()) continue;	//skip non visible columns
			sb.append(","); //$NON-NLS-1$
			String name = columns.get(i).getName();
			name = name.replaceAll(" ", "_"); //$NON-NLS-1$ //$NON-NLS-2$
			name = name.replaceAll("[^\\p{L}\\p{Nd}_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (forShape && name.length() > 10){
				name = name.substring(0, 10);
			}
			
			String tempname = name;
			int cnt = 1;
			while(names.contains(tempname)){
				String postfix = "_" + cnt; //$NON-NLS-1$
				tempname = name + postfix; 
				if ( forShape && tempname.length() > 10){
					tempname = name.substring(0, 10-postfix.length()) + postfix;
				}
				cnt++;
			}
			//Name is not a valid attribute name
			if (tempname.equalsIgnoreCase("Name")){ //$NON-NLS-1$
				tempname = tempname +"_"; //$NON-NLS-1$
			}
			names.add(tempname);
			sb.append(tempname);
			sb.append(":"); //$NON-NLS-1$
			if (columns.get(i).getType() == ColumnType.TIME && !supportsTime){
				//time is not supported so convert to string
				sb.append(ColumnType.TIME_STR.geotoolsType);
			}else{
				sb.append(columns.get(i).getType().geotoolsType);
			}
		}
		return sb.toString();
	}
	
}
