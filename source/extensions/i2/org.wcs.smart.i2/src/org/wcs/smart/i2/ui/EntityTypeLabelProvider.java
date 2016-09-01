package org.wcs.smart.i2.ui;

import org.eclipse.jface.viewers.LabelProvider;
import org.wcs.smart.i2.model.IntelEntityType;

public class EntityTypeLabelProvider extends LabelProvider {

	public static EntityTypeLabelProvider INSTANCE = new EntityTypeLabelProvider();
	
	public String getText(Object element){
		if (element instanceof IntelEntityType){
			return ((IntelEntityType) element).getName() + " [" + ((IntelEntityType)element).getKeyId() + "]";
		}
		return super.getText(element);
	}
}
