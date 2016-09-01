package org.wcs.smart.i2.ui;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelAttribute;

public class IntelAttributeLabelProvider extends LabelProvider {

	public static IntelAttributeLabelProvider INSTANCE = new IntelAttributeLabelProvider();
	
	public String getText(Object element){
		if (element instanceof IntelAttribute){
			return ((IntelAttribute) element).getName() + " [" + ((IntelAttribute)element).getKeyId() + "]";
		}
		return super.getText(element);
	}
	
	public Image getImage(Object element){
		if (element instanceof IntelAttribute){
			IntelAttribute a = (IntelAttribute)element;
			return a.getType().getImage();
		}
		return super.getImage(element);
	}
}