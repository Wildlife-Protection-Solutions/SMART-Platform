package org.wcs.smart.i2.query;

import java.text.DateFormat;
import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;
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
			//TODO: move out of shared
			return RecordLabelProvider.getRecordStatusLabel(IntelRecord.Status.valueOf(i.getRecordStatus().toUpperCase()));
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
