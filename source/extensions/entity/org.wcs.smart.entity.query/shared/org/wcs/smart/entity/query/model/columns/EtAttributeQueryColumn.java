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
		QueryColumn newColumn = new EtAttributeQueryColumn(getName(), getAttributeId(), getAttributeType());
		newColumn.setEdit(canEdit());
		return newColumn;
	}


}
