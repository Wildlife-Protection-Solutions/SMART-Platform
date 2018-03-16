package org.wcs.smart.event.ui.filter;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.event.filter.Operator;
import org.wcs.smart.event.ui.filter.BracketDropItem.BracketType;

public enum DropItemFactory {
	INSTANCE;
	
	public static final AttributeListItem ANY_ATTRIBUTE = new AttributeListItem() {
		@Override
		public String getKeyId() {
			return "any";
		}
		
		@Override
		public String getName() {
			return "Any";
		}
	};
	
	public DropItem[] createDropItem(Object element) {
		if (element instanceof Category) {
			return new DropItem[] {new CategoryDropItem((Category)element) };
		}
		if (element instanceof Attribute) {
			Attribute a = (Attribute)element;
			if (a.getType() == Attribute.AttributeType.LIST) {
				return new DropItem[] {new AttributeListDropItem((Attribute)element) };
			}else if (a.getType() == Attribute.AttributeType.TREE) {
				return new DropItem[] {new AttributeTreeDropItem((Attribute)element) };
			}else {
				return new DropItem[] {new AttributeDropItem((Attribute)element) };	
			}
			
		}
		if (element instanceof CategoryAttribute) {
			CategoryAttribute a = (CategoryAttribute)element;
			if (a.getAttribute().getType() == Attribute.AttributeType.LIST) {
				return new DropItem[] {new AttributeListDropItem((CategoryAttribute)element) };
			}else if (a.getAttribute().getType() == Attribute.AttributeType.TREE) {
				return new DropItem[] {new AttributeTreeDropItem((CategoryAttribute)element) };
			}else {
				return new DropItem[] {new AttributeDropItem((CategoryAttribute)element) };	
			}
		}
		if (element instanceof Operator) {
			Operator op = (Operator)element;
			if (op == Operator.BRACKETS) {
				return new DropItem[] {new BracketDropItem(BracketType.OPEN), new BracketDropItem(BracketType.CLOSE)}; 
			}
			if (op == Operator.NOT) {
				return new DropItem[] { new NotDropItem() };
			}
		}
		
		
		return null;
	}

	public DropItem createAndOrOpterator() {
		return new BooleanOpDropItem();
	}
}
