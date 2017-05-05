package org.wcs.smart.entity.query.model.columns;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.query.model.EntityQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

public class EtAttributeQueryColumn extends AttributeQueryColumn {

	public EtAttributeQueryColumn(String name, String attributeId, AttributeType type){
		super(name, attributeId, type);
	}
	
	/**
	 * Creates a new column with the given column type.
	 * @param name
	 * @param key the query column full key of the form "attribute:<ATTRIBUTEID>"
	 * @param type
	 */
	public EtAttributeQueryColumn(String name, String key, ColumnType type){
		super(name, key, type);
	}


	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#getValue(org.wcs.smart.patrol.query.model.PatrolQueryResultItem)
	 */
	@Override
	public Object getValue(IResultItem queryResultItem) {
		if (queryResultItem instanceof EntityQueryResultItem) {
			EntityQueryResultItem item = (EntityQueryResultItem) queryResultItem;
			Object x = item.getAttributeValue(attributeKey);
			if (x != null && getType() == QueryColumn.ColumnType.BOOLEAN){
				return Boolean.valueOf((Double)x >= 0.5);
			}
			return x;
		}
		return ""; //$NON-NLS-1$
	}


	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		QueryColumn newColumn = new EtAttributeQueryColumn(getName(), getKey(), getType());
		return newColumn;
	}


}
