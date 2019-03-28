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
package org.wcs.smart.i2.query;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.query.engine.EntityRecordQueryResultItem;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;

import org.locationtech.jts.geom.Geometry;

/**
 * Collection of fixed query columns.
 * 
 * @author Emily
 *
 */
public class FixedQueryColumn extends AbstractQueryColumn{
	
	public enum Column{
		RECORD_TITLE("record:title"), //$NON-NLS-1$
		RECORD_STATUS("record:status"), //$NON-NLS-1$
		RECORD_SOURCE("record:source"), //$NON-NLS-1$
		LOC_ID("loc:id"), //$NON-NLS-1$
		LOC_DATE("loc:date"), //$NON-NLS-1$
		LOC_TIME("loc:time"), //$NON-NLS-1$
		LOC_COMMENT("loc:comment"), //$NON-NLS-1$
		LOC_GEOMTRY("loc:geom"), //$NON-NLS-1$
		
		ENTITY_ID("entity:id"),  //$NON-NLS-1$
		ENTITY_TYPE("entity:type"), //$NON-NLS-1$
		
		CA_ID ("ca:id"),  //$NON-NLS-1$
		CA_NAME("ca:name");  //$NON-NLS-1$
		
		public String key;
		Column(String key){
			this.key = key;
		}
	}

	private Column column;
	
	public FixedQueryColumn(Column column, Locale l){
		super(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(column, l), column.key);
		this.column = column;
	}
	
	public Column getColumn(){
		return this.column;
	}


	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof EntityRecordQueryResultItem) {
			if (column == Column.ENTITY_ID) {
				return ((EntityRecordQueryResultItem)item).getEnityId();
			}else if (column == Column.ENTITY_TYPE) {
				return ((EntityRecordQueryResultItem)item).getEnityTypeName();
			}else if (column == Column.CA_ID) {
				return ((EntityRecordQueryResultItem)item).getConservationAreaId();
			}else if (column == Column.CA_NAME) {
				return ((EntityRecordQueryResultItem)item).getConservationAreaName();
			}
			return null;
		}
		
		if (!(item instanceof  IntelObservationResultItem)) return null;
		IntelObservationResultItem i = (IntelObservationResultItem) item;
		switch(column){
		case LOC_COMMENT:
			return i.getLocationComment();
		case LOC_DATE:
			return i.getLocationDate();
		case LOC_GEOMTRY:
			if (i.getGeometryError() != null) return "Parse Error"; //$NON-NLS-1$
			if (i.getGeometry() == null) return ""; //$NON-NLS-1$
			return i.getGeometry();
		case LOC_ID:
			return i.getLocationId();
		case LOC_TIME:
			return i.getLocationDate();
		case RECORD_STATUS:
			return IntelRecord.Status.valueOf(i.getRecordStatus().toUpperCase(Locale.ROOT));
		case RECORD_SOURCE:
			return i.getRecordSource();
		case RECORD_TITLE:
			return i.getRecordTitle();
		case CA_ID:
			return i.getConservationAreaId();
		case CA_NAME:
			return i.getConservationAreaName();
		default:
		}
		return null;
	}
	
	@Override
	public String getValue(IResultItem item, Locale l){
		Object toFormat = getValue(item);
		if (toFormat == null) return ""; //$NON-NLS-1$
		switch(column){
			case LOC_DATE:
				return DateFormat.getDateInstance(DateFormat.DEFAULT, l).format((Date)toFormat);
			case LOC_GEOMTRY:
				if (toFormat instanceof Geometry) {
					return ((Geometry)toFormat).toText();
				}
				return toFormat.toString();
			case LOC_TIME:
				return DateFormat.getTimeInstance(DateFormat.DEFAULT, l).format((Date)toFormat);
			case RECORD_STATUS:
				return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(toFormat, l);
			case LOC_COMMENT:
			case LOC_ID:
			case RECORD_TITLE:
			case ENTITY_ID:
			case ENTITY_TYPE:
			case CA_NAME:
			case CA_ID:
				return (String)toFormat;
			case RECORD_SOURCE:
				return ((IntelRecordSource)toFormat).getName();
		}
		return toFormat.toString();
	}
	
	@Override
	public boolean canSort(){
		if (column == Column.LOC_GEOMTRY) return false;
		return true;
	}
	
	@Override
	public Type getDataType() {
		switch(column){
			case LOC_DATE:
				return Type.DATE;
			case LOC_GEOMTRY:
				return Type.GEOMETRY;
			case LOC_TIME:
				return Type.TIME;
			case LOC_COMMENT:
			case LOC_ID:
			case RECORD_STATUS:
			case RECORD_TITLE:
			case RECORD_SOURCE:
			case ENTITY_ID:
			case ENTITY_TYPE:
				return Type.STRING;
		default:
			break;
		}
		return Type.STRING;
	}
}
