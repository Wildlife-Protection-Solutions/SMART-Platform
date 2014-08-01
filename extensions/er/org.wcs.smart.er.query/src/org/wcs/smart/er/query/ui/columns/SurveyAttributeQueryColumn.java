package org.wcs.smart.er.query.ui.columns;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

public class SurveyAttributeQueryColumn extends AttributeQueryColumn {

	public SurveyAttributeQueryColumn(String name, String attributeId, AttributeType type) {
		super(name, attributeId, type);
	}

	/**
	 * Creates a new column with the given column type.
	 * @param name
	 * @param key the query column full key of the form "attribute:<ATTRIBUTEID>"
	 * @param type
	 */
	public SurveyAttributeQueryColumn(String name, String key, ColumnType type){
		super(name, key, type);
		this.attributeKey = key.split(":")[1]; //$NON-NLS-1$
	}
	
	@Override
	public Object getValue(IResultItem item) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryColumn clone() {
		return new SurveyAttributeQueryColumn(getName(), getKey(), getType());
	}

}
