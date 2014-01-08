package org.wcs.smart.entity.ui.typelist.editor.sightings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.typelist.editor.sightings.QueryColumn.ColumnType;
import org.wcs.smart.entity.ui.typelist.editor.sightings.SightingTableColumns.FixedColumns;

public class SightingTable {

	
	public SightingTable(Composite composite){
		
	}
	
	private List<QueryColumn> getQueryColumns(EntityType type){
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		
		//fixed columns for waypoint and fixed entity attributes
		for (FixedColumns fixed : SightingTableColumns.FixedColumns.values()){
			QueryColumn column = new QueryColumn(fixed.getGuiName(),fixed.getKey(),fixed.getType());
			cols.add(column);
		}
		
		//entity attributes
		for (EntityAttribute ea : type.getAttributes()){
			String name = ea.getName();
			String key = "entity:" + ea.getDmAttribute().getKeyId();
			ColumnType cType = ColumnType.STRING;
			if (ea.getDmAttribute().getType() == AttributeType.LIST ||
					ea.getDmAttribute().getType() == AttributeType.TREE ||
					ea.getDmAttribute().getType() == AttributeType.TEXT ){
				cType = ColumnType.STRING;
			}else if (ea.getDmAttribute().getType() == AttributeType.DATE){
				cType = ColumnType.DATE;
			}else if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN){
				cType = ColumnType.BOOLEAN;
			}else if (ea.getDmAttribute().getType() == AttributeType.NUMERIC){
				cType = ColumnType.NUMBER;
			}
			
			QueryColumn column = new QueryColumn(name, key, cType);
			cols.add(column);
		}
	
		return cols;
	}
}
