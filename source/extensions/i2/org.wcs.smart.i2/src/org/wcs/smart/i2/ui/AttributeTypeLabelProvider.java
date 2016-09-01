package org.wcs.smart.i2.ui;

import java.util.Locale;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelAttribute;

public class AttributeTypeLabelProvider extends LabelProvider {
	
	public static AttributeTypeLabelProvider INSTANCE = new AttributeTypeLabelProvider();
	
	public String getText(Object element){
		if (element instanceof IntelAttribute.IAttributeType){
			return ((IntelAttribute.IAttributeType) element).getGuiName(Locale.getDefault());
		}
		return super.getText(element);
	}
	
	public Image getImage(Object element){
		if (element instanceof IntelAttribute.IAttributeType){
			IntelAttribute.IAttributeType a = (IntelAttribute.IAttributeType)element;
			return a.getImage();
		}
		return super.getImage(element);
	}
}
