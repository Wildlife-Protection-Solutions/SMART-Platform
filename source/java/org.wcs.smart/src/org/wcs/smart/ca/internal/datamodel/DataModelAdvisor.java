package org.wcs.smart.ca.internal.datamodel;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.IDataModelAdvisor;

public class DataModelAdvisor implements IDataModelAdvisor {

	@Override
	public String canDelete(Category category) {
		if (category.hasAttributes()){
			return "Category cannot be removed until all attributes are also removed.";
		}else if (category.getChildren().size() > 0){
			return "Category cannot be removed until all children are also removed.";
		}
		return null;
	}

	@Override
	public String canDelete(Attribute attribute) {
		return null;
	}

	@Override
	public String canDelete(CategoryAttribute categoryAttribute) {
		return null;
	}

	@Override
	public String canDelete(AttributeListItem item) {
		return null;
	}

	@Override
	public String canDelete(AttributeTreeNode node) {
		return null;
	}

}
