package org.wcs.smart.i2.ui;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;

public class AttributeLabelProvider extends LabelProvider {

	public static AttributeLabelProvider INSTANCE = new AttributeLabelProvider();
	
	public String getText(Object element){
		if (element instanceof IntelAttribute){
			return ((IntelAttribute) element).getName();
		}else if (element instanceof IntelEntityTypeAttribute){
			return ((IntelEntityTypeAttribute)element).getAttribute().getName();
		}
		return super.getText(element);
	}
	
	public Image getImage(Object element){
		if (element instanceof IntelAttribute){
			IntelAttribute a = (IntelAttribute)element;
			return a.getType().getImage();
		}else if (element instanceof IntelEntityTypeAttribute){
			return ((IntelEntityTypeAttribute)element).getAttribute().getType().getImage();
		}
		return super.getImage(element);
	}
}