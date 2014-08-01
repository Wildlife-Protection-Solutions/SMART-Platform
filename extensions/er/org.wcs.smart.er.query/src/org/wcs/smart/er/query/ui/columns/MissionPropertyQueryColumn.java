package org.wcs.smart.er.query.ui.columns;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

public class MissionPropertyQueryColumn extends QueryColumn {
	
	public MissionPropertyQueryColumn(MissionAttribute mp){
		super(mp.getName(), "missionatt:" + mp.getKeyId(), null);
		if (mp.getType() == AttributeType.NUMERIC){
			super.setType(ColumnType.NUMBER);
		}else if (mp.getType() == AttributeType.TEXT){
			super.setType(ColumnType.STRING);
		}else if (mp.getType() == AttributeType.LIST){
			super.setType(ColumnType.STRING);
		}
	}
	
	protected MissionPropertyQueryColumn(String name, String key, ColumnType type){
		super(name, key, type);
	}
	
	@Override
	public Object getValue(IResultItem item) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryColumn clone() {
		return new MissionPropertyQueryColumn(getName(), getKey(), getType());
	}

}
