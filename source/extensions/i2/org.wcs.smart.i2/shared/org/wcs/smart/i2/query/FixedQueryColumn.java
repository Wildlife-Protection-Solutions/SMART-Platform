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
import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;

/**
 * Collection of fixed query columns.
 * 
 * @author Emily
 *
 */
public class FixedQueryColumn extends AbstractQueryColumn{
	
	public enum Column{
		RECORD_TITLE("record:title"),
		RECORD_STATUS("record:status"),
//		RECORD_DATE_CREATED("record:created"),
//		RECORD_DATE_MODIFIED("record:modified"),
//		RECORD_CREATED_BY("record:createdby"),
//		RECORD_MODIFIED_BY("record:modifiedby"),
		LOC_ID("loc:id"),
		LOC_DATE("loc:date"),
		LOC_TIME("loc:time"),
		LOC_COMMENT("loc:comment"),
		LOC_GEOMTRY("loc:geom");
		
		String key;
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
	public String getValue(IResultItem item, Locale l) {
		if (!(item instanceof  IntelObservationResultItem)) return null;
		IntelObservationResultItem i = (IntelObservationResultItem) item;
		switch(column){
		case LOC_COMMENT:
			return i.getLocationComment();
		case LOC_DATE:
			return DateFormat.getDateInstance().format(i.getLocationDate());
		case LOC_GEOMTRY:
			if (i.getGeometryError() != null) return "Parse Error";
			if (i.getGeometry() == null) return "";
			return i.getGeometry().toText();
		case LOC_ID:
			return i.getLocationId();
			
		case LOC_TIME:
			return DateFormat.getTimeInstance().format(i.getLocationDate());
		case RECORD_STATUS:
			return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(IntelRecord.Status.valueOf(i.getRecordStatus().toUpperCase()), l);
		case RECORD_TITLE:
			return i.getRecordTitle();
		}
		return "";
	}
	
	@Override
	public boolean canSort(){
		if (column == Column.LOC_GEOMTRY) return false;
		return true;
	}
}
