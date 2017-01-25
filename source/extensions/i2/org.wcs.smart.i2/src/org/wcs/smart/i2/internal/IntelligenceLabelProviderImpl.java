/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.internal;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.query.FixedQueryColumn;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.export.CsvQueryExporter;
import org.wcs.smart.i2.query.export.ShpQueryExporter;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Desktop label provider for intelligence module.
 * 
 * @author Emily
 *
 */
public class IntelligenceLabelProviderImpl implements
		IIntelligenceLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof AttributeType){
			AttributeType type = (AttributeType) item;
			
			if (type == AttributeType.BOOLEAN){
				return "BOOLEAN";
			}else if (type == AttributeType.DATE){
				return "DATE";
			}else if (type == AttributeType.LIST){
				return "LIST";
			}else if (type == AttributeType.NUMERIC){
				return "NUMERIC";
			}else if (type == AttributeType.TEXT){
				return "TEXT";
			}
		}
		
		if (item == IntelWorkingSetCategory.ENTITY) return "Entities";
		if (item == IntelWorkingSetCategory.RECORD) return "Records";
		if (item == IntelWorkingSetCategory.QUERIES) return "Queries";
		
		if (item == FixedQueryColumn.Column.LOC_COMMENT) return "Comment";
		if (item == FixedQueryColumn.Column.LOC_DATE) return "Date";
		if (item == FixedQueryColumn.Column.LOC_TIME) return "Time";
		if (item == FixedQueryColumn.Column.LOC_GEOMTRY) return "Geometry";
		if (item == FixedQueryColumn.Column.LOC_ID) return "ID";
		if (item == FixedQueryColumn.Column.RECORD_STATUS) return "Record Status";
		if (item == FixedQueryColumn.Column.RECORD_TITLE) return "Record Title";
						
		if (item == IntelRecord.Status.NEW) return "Unprocessed";
		if (item == IntelRecord.Status.PROCESSING) return "In Progress";
		if (item == IntelRecord.Status.COMPLETE) return "Complete";
		
		if (item instanceof Operator){
			switch((Operator)item){
				case AND: return "And";
				case BETWEEN: return "Between";
				case BRACKETS: return "( )";
				case BRACKET_CLOSE: return ")";
				case BRACKET_OPEN: return "(";
				case EQUALS: return "=";
				case GREATERTHAN: return ">";
				case GREATERTHANEQUALS: return ">=";
				case LESSTHAN: return "<";
				case LESSTHANEQUALS: return "<=";
				case NOT: return "Not";
				case NOTEQUALS: return "!=";
				case NOT_BETWEEN: return "Not Between";
				case OR: return "Or";
				case STR_CONTAINS: return "Contains";
				case STR_EQUALS: return "Equals";
				case STR_NOTCONTAINS: return "Not Contains";
			}
		}
		
		if (item instanceof CsvQueryExporter) return "Comma Separated Values";
		if (item instanceof ShpQueryExporter) return "Shapefile";
		if (item == IntelQueryColumnProvider.ANY_ITEM) return "<Any>";
		if (item == Boolean.TRUE) return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
		if (item == Boolean.FALSE) return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
		return null;
		
	}
	
	public Image getImage(Object item){

		if (item == IntelWorkingSetCategory.RECORD){
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD);
		}else if (item == IntelWorkingSetCategory.ENTITY){
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY);
		}
		return null;
	}

}
