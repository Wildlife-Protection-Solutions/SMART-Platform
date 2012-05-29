package org.wcs.smart.patrol;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.IDataModelAdvisor;

public class PatrolDataModelAdvisor implements IDataModelAdvisor{

	@Override
	public String canDelete(Category category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String canDelete(Attribute attribute) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String canDelete(CategoryAttribute categoryAttribute) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String canDelete(AttributeListItem item) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String canDelete(AttributeTreeNode node) {
		// TODO Auto-generated method stub
		return null;
	}

}
