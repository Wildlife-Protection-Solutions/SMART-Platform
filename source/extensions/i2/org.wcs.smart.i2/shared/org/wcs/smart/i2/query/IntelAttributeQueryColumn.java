package org.wcs.smart.i2.query;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.query.engine.EntityRecordQueryResultItem;

public class IntelAttributeQueryColumn extends AbstractQueryColumn {

	private IntelAttribute attribute;
	
	public IntelAttributeQueryColumn(IntelAttribute attribute) {
		super(attribute.getName(), "entityattribute:" + attribute.getKeyId());
		this.attribute = attribute;
	}
	
	public IntelAttribute getAttribute() {
		return this.attribute;
	}
	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof EntityRecordQueryResultItem) {
			Object value = ((EntityRecordQueryResultItem) item).getAttributeValue(attribute.getKeyId());
			return value;
			
		}
		return null;
	}

	@Override
	public String getValue(IResultItem item, Locale l) {
		Object toFormat = getValue(item);
		if (toFormat == null) return ""; //$NON-NLS-1$
		
		switch(attribute.getType()) {
			case BOOLEAN:
				if ((Double)toFormat > 0.5) return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, l);
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l);
			case DATE:
				return DateFormat.getDateInstance().format( (Date)toFormat );
			case LIST:
				return ((IntelAttributeListItem)toFormat).getName();
			case NUMERIC:
				return toFormat.toString();
			case TEXT:
				return toFormat.toString();
			case EMPLOYEE:
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel((Employee) toFormat, l);
			case POSITION:
				return toFormat.toString();
		}
		return "";
	}

	@Override
	public Type getDataType() {
		switch(attribute.getType()) {
		case BOOLEAN:
			return Type.BOOLEAN;
		case DATE:
			return Type.DATE;
		case NUMERIC:
			return Type.NUMERIC;
		case EMPLOYEE:
		case LIST:
		case POSITION:
		case TEXT:
			return Type.STRING;
		}
		return null;
	}

}
