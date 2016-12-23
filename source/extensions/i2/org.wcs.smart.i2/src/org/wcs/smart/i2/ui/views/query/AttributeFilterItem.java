package org.wcs.smart.i2.ui.views.query;

import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;

public class AttributeFilterItem extends BasicFilterItem {

	private IntelEntityTypeAttribute etattribute;
	private IntelAttribute attribute;
	
	public AttributeFilterItem(IntelEntityTypeAttribute attribute) {
		super(attribute.getAttribute().getName());
		this.etattribute = attribute;
		this.attribute = attribute.getAttribute();
	}

	public AttributeFilterItem(IntelAttribute attribute) {
		super(attribute.getName());
		this.attribute = attribute;
	}
	
	public IntelAttribute getAttribute(){
		return attribute;
	}
	
	
	
	@Override
	public DropItem[] asDropItem() {
		
		
		//category and attribute
		StringBuilder partKey =new StringBuilder();
		partKey.append("entity_attribute:");
		partKey.append(attribute.getType().name());
		partKey.append(":");
		
		if (etattribute != null){
			partKey.append(etattribute.getEntityType().getKeyId());
			partKey.append(":");
		}else{
			partKey.append(":");
		}
		partKey.append(attribute.getKeyId());
		
		switch(attribute.getType()){
		case BOOLEAN:
			return new DropItem[]{new TextDropItem(getName(), partKey.toString())};
		case DATE:
			break;
		case LIST:
			break;
		case NUMERIC:
			return new DropItem[]{new TextBoxDropItem(getName(), partKey.toString(), TextBoxDropItem.InputType.NUMERIC)};
		case TEXT:
			return new DropItem[]{new TextBoxDropItem(getName(), partKey.toString(), TextBoxDropItem.InputType.TEXT)};
		default:
			break;
			
		}
		return null;
	}
}
