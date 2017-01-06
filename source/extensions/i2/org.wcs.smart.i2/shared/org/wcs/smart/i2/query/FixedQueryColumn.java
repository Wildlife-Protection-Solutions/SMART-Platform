package org.wcs.smart.i2.query;

import java.text.DateFormat;
import java.util.Locale;

import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.query.engine.IntelRecordResultItem;
import org.wcs.smart.i2.ui.RecordLabelProvider;

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
	public FixedQueryColumn(Column column){
		super(column.name(), column.key);
		this.column = column;
	}


	@Override
	public String getValue(IResultItem item, Locale l) {
		if (!(item instanceof  IntelRecordResultItem)) return null;
		IntelRecordResultItem i = (IntelRecordResultItem) item;
		switch(column){
		case LOC_COMMENT:
			return i.getLocationComment();
		case LOC_DATE:
			return DateFormat.getDateInstance().format(i.getLocationDate());
			
		case LOC_GEOMTRY:
			return "TODO:";
			
		case LOC_ID:
			return i.getLocationId();
			
		case LOC_TIME:
			return DateFormat.getTimeInstance().format(i.getLocationDate());
		case RECORD_STATUS:
			//TODO: move out of shared
			return RecordLabelProvider.getRecordStatusLabel(IntelRecord.Status.valueOf(i.getRecordStatus().toUpperCase()));
		case RECORD_TITLE:
			return i.getRecordTitle();
		}
		return "";
	}
}
