package org.wcs.smart.er.query.ui.columns;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

public class SamplingUnitAttributeQueryColumn  extends QueryColumn {
	
	private static final String KEY_PREFIX = "suatt"; //$NON-NLS-1$
	
	/**
	 * Creates a new query column based on the mission attribute.
	 * 
	 * @param mp
	 */
	public SamplingUnitAttributeQueryColumn(SamplingUnitAttribute sua){
		super(sua.getName(), KEY_PREFIX + ":" + sua.getKeyId(), null); //$NON-NLS-1$
		
		if (sua.getType() == AttributeType.NUMERIC){
			super.setType(ColumnType.NUMBER);
		}else if (sua.getType() == AttributeType.TEXT){
			super.setType(ColumnType.STRING);
		}
	}
	
	/**
	 * Creates a new query column with the given attribute.
	 * 
	 * @param name column name
	 * @param key column key
	 * @param type column type
	 */
	protected SamplingUnitAttributeQueryColumn(String name, String key, ColumnType type){
		super(name, key, type);
	}
	
	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof SurveyQueryResultItem){
			SurveyQueryResultItem i = (SurveyQueryResultItem) item;			
			String attributeKey = getKey().split(":")[1]; //$NON-NLS-1$
			return i.getSamplingUnitAttributeValue(attributeKey);
		}
		return null;
	}

	@Override
	public QueryColumn clone() {
		return new SamplingUnitAttributeQueryColumn(getName(), getKey(), getType());
	}

}
