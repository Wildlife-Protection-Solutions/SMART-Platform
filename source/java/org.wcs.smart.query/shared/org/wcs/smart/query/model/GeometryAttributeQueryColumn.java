package org.wcs.smart.query.model;

import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;

public class GeometryAttributeQueryColumn extends AttributeQueryColumn implements IGeometryColumn {

	public GeometryAttributeQueryColumn(String name, String attributeId, AttributeType type, String formatString) {
		super(name, attributeId, type, formatString);
	}
	
	public GeometryAttributeQueryColumn(String name, String attributeId, GeometryProperty prop, AttributeType type){
		super(name, attributeId, prop, type);
	}
	

	@Override
	public Type getGeometryType() {
		if (getAttributeType() == Attribute.AttributeType.POLYGON) return Type.MULTIPOLYGON;
		if (getAttributeType() == Attribute.AttributeType.LINE) return Type.MULTILINESTRING;
		throw new IllegalStateException();
	}

	/**
	 * @see org.wcs.smart.asset.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() { 
		QueryColumn newColumn = null;
		if (this.geomProperty == null) {
			newColumn = new GeometryAttributeQueryColumn(getName(), getAttributeId(), getAttributeType(), getFormatString());
		}else {
			newColumn = new GeometryAttributeQueryColumn(getName(), getAttributeId(), getGeometryProperty(), getAttributeType());
		}
		newColumn.setEdit(canEdit());
		newColumn.setCanSort(canSort());
		return newColumn;
	}
}
