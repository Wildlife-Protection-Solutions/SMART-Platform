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
package org.wcs.smart.i2.ui.editors.query;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Generates a label provider for query columns
 * 
 * @author Emily
 *
 */
public class ColumnLabelProviderGenerator {

	public static ColumnLabelProvider createLabelProvider(IQueryColumn column){
		switch (column.getDataType()){
		case BOOLEAN:
			return createBooleanLabelProvider(column);
		case DATE:
			return createDateLabelProvider(column);
		case GEOMETRY:
			return createGeometryLabelProvider(column);
		case NUMERIC:
			return createNumericLabelProvider(column);
		case STRING:
			return createStringLabelProvider(column);
		case TIME:
			return createTimeLabelProvider(column);
		}	
		return new ColumnLabelProvider();
	}
	
	private static ColumnLabelProvider createBooleanLabelProvider(final IQueryColumn column){
		return new ColumnLabelProvider(){
			public String getText(Object element){
				if (element == null) return "";
				if (element instanceof IResultItem){
					Object value = column.getValue((IResultItem)element);	
					if (value == null) return "";
					if (value instanceof Boolean){
						if ((Boolean)value) return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, Locale.getDefault());
						return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, Locale.getDefault());
					}
					return "Not a valid boolean value";
				}
				return super.getText(element);
			}
		};
	}
	
	private static ColumnLabelProvider createStringLabelProvider(final IQueryColumn column){
		return new ColumnLabelProvider(){
			public String getText(Object element){
				if (element == null) return "";
				if (element instanceof IResultItem){
					Object value = column.getValue((IResultItem)element);
					if (value == null) return "";
					if (value instanceof String){
						return (String)value;
					}
					if (value instanceof IntelRecord.Status){
						return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel((IntelRecord.Status)value, Locale.getDefault()); 
					}
					return "Not a valid string value";
				}
				return super.getText(element);
			}
		};
	}
	
	private static ColumnLabelProvider createNumericLabelProvider(final IQueryColumn column){
		return new ColumnLabelProvider(){
			public String getText(Object element){
				if (element == null) return "";
				if (element instanceof IResultItem){
					Object value = column.getValue((IResultItem)element);	
					if (value == null) return "";
					if (value instanceof Number){
						return value.toString();
					}
					return "Not a valid numeric value";
				}
				return super.getText(element);
			}
		};
	}
	
	private static ColumnLabelProvider createDateLabelProvider(final IQueryColumn column){
		return new ColumnLabelProvider(){
			public String getText(Object element){
				if (element == null) return "";
				if (element instanceof IResultItem){
					Object value = column.getValue((IResultItem)element);	
					if (value == null) return "";
					if (value instanceof Date){
						return DateFormat.getDateInstance().format((Date)value);
					}
					return "Not a valid date value";
				}
				return super.getText(element);
			}
		};
	}
	
	private static ColumnLabelProvider createTimeLabelProvider(final IQueryColumn column){
		return new ColumnLabelProvider(){
			public String getText(Object element){
				if (element == null) return "";
				if (element instanceof IResultItem){
					Object value = column.getValue((IResultItem)element);	
					if (value == null) return "";
					if (value instanceof Date){
						return DateFormat.getTimeInstance().format((Date)value);
					}
					return "Not a valid time value";
				}
				return super.getText(element);
			}
		};
	}
	
	private static ColumnLabelProvider createGeometryLabelProvider(final IQueryColumn column){
		return new ColumnLabelProvider(){
			public String getText(Object element){
				if (element == null) return "";
				if (element instanceof IResultItem){
					Object value = column.getValue((IResultItem)element);	
					if (value == null) return "";
					if (value instanceof Geometry){
						return ((Geometry)value).toText();
					}
					return "Not a valid geometry value";
				}
				return super.getText(element);
			}
		};
	}

}
