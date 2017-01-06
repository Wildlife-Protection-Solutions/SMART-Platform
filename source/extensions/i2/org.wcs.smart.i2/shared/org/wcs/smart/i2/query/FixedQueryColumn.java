package org.wcs.smart.i2.query;

import java.util.Locale;

public class FixedQueryColumn extends AbstractQueryColumn{
	
	public enum Column{
		RECORD_TITLE("record:title"),
		RECORD_STATUS("record:status"),
		RECORD_DATE_CREATED("record:created"),
		RECORD_DATE_MODIFIED("record:modified"),
		RECORD_CREATED_BY("record:createdby"),
		RECORD_MODIFIED_BY("record:modifiedby"),
		
		OBS_ID("obs:id"),
		OBS_DATE("obs:date"),
		OBS_TIME("obs:time"),
		OBS_COMMENT("obs:comment"),
		OBS_GEOMTRY("obs:geom");
		
		String key;
		Column(String key){
			this.key = key;
		}
	}

	public FixedQueryColumn(Column column){
		super(column.name(), column.key);
	}


	@Override
	public String getValue(IResultItem item, Locale l) {
		// TODO Auto-generated method stub
		return null;
	}
}
