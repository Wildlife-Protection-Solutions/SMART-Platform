package org.wcs.smart.i2.ui;

import org.eclipse.jface.viewers.LabelProvider;
import org.wcs.smart.i2.model.IntelAttributeListItem;

public class AttributeListItemLabelProvider extends LabelProvider {
	
	public static AttributeListItemLabelProvider INSTANCE = new AttributeListItemLabelProvider();
	
	public String getText(Object element){
		if (element instanceof IntelAttributeListItem){
			return ((IntelAttributeListItem) element).getName() + " [" + ((IntelAttributeListItem)element).getKeyId() + "]";
		}
		return super.getText(element);
	}
	
}
