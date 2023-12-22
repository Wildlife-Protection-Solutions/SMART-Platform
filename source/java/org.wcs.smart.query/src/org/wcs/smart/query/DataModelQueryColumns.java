/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.query;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.AttributeQueryColumn.GeometryProperty;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.GeometryAttributeQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * 
 * @author Emily
 * @since 8.0.0
 */
public class DataModelQueryColumns {

	
	public static List<QueryColumn> generateDataModelQueryColumns(DataModel dataModel, boolean supportsEditing) {
		
		List<QueryColumn> cols = new ArrayList<>();
		
		// add data model category columns
		int numCategory = QueryDataModelManager.getInstance().getActiveDepth();
		for (int i = 0; i < numCategory; i++) {
			QueryColumn toAdd = new CategoryQueryColumn(MessageFormat.format(Messages.DataModelQueryColumns_CategoryColumnHeader, i), i);
			toAdd.setEdit(supportsEditing);
			cols.add(toAdd);
		}
			
		//sort attributes alphabetically
		List<Attribute> atts = new ArrayList<Attribute>();
		atts.addAll( dataModel.getAttributes() );
		Collections.sort(atts, new Comparator<Attribute>(){
			@Override
			public int compare(Attribute o1, Attribute o2) {
				return Collator.getInstance().compare(o1.getName(),o2.getName());
			}});
			
		for (Attribute att : atts) {
			String name = att.getName();
			QueryColumn toAdd;
			if (att.getType().isGeometry()) {
				toAdd = new GeometryAttributeQueryColumn(name, att.getKeyId(), att.getType(), att.getRegex());
			}else {
				toAdd = new AttributeQueryColumn(name, att.getKeyId(), att.getType(), att.getRegex());	
			}
			
			toAdd.setEdit(supportsEditing);
			cols.add(toAdd);
			
			if (att.getType().isGeometry()) {
				cols.add(new AttributeQueryColumn(GeometryProperty.SOURCE.getColumnName(name, Locale.getDefault()), att.getKeyId(), GeometryProperty.SOURCE, Attribute.AttributeType.TEXT));	
				cols.add(new AttributeQueryColumn(GeometryProperty.PERIMETER.getColumnName(name, Locale.getDefault()), att.getKeyId(), GeometryProperty.PERIMETER, Attribute.AttributeType.NUMERIC));
				if (att.getType() == Attribute.AttributeType.POLYGON) {
					cols.add(new AttributeQueryColumn(GeometryProperty.AREA.getColumnName(name, Locale.getDefault()), att.getKeyId(), GeometryProperty.AREA, Attribute.AttributeType.NUMERIC));
				}
			}
		}
		
		return cols;
	}
}
