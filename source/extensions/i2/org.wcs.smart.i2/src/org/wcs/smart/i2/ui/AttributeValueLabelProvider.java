package org.wcs.smart.i2.ui;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelObservationAttribute;

public class AttributeValueLabelProvider extends LabelProvider {
	
	public static AttributeValueLabelProvider INSTANCE = new AttributeValueLabelProvider();
	
	public String getText(Object element){
		Object value = null;
		if (element instanceof IntelEntityAttributeValue){
			value = ((IntelEntityAttributeValue) element).getAttributeValue();
		}else if (element instanceof IntelEntityRelationshipAttributeValue){
			value = ((IntelEntityRelationshipAttributeValue) element).getAttributeValue();
		}else if (element instanceof IntelObservationAttribute){
			value = ((IntelEntityRelationshipAttributeValue) element).getAttributeValue();
		}else{
			return super.getText(element);
		}
		if (value == null){
			return "";
		}
		if (value instanceof String){
			return (String) value;
		}else if (value instanceof Number){
			return ((Number)value).toString();
		}else if (value instanceof Date){
			return DateFormat.getInstance().format((Date)value);
		}else if (value instanceof NamedItem){
			return ((NamedItem) value).getName();
		}else{
			return value.toString();
		}
	}
	
	
	public Image getImage(Object element){
		//TODO:
		return super.getImage(element);
	}
}
