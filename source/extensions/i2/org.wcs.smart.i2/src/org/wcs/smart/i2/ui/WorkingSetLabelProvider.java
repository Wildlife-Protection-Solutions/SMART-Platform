package org.wcs.smart.i2.ui;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.model.IntelWorkingSetItem;

public class WorkingSetLabelProvider extends LabelProvider {

	public static WorkingSetLabelProvider INSTANCE = new WorkingSetLabelProvider();
	
	public String getText(Object element){
		if (element instanceof IntelWorkingSet){
			return ((IntelWorkingSet) element).getName();
		}else if (element instanceof IntelWorkingSetCategory){
			return ((IntelWorkingSetCategory) element).getGuiName();
		}else if (element instanceof IntelWorkingSetItem){
			return ((IntelWorkingSetItem) element).getLabel();
		}
		return super.getText(element);
	}
	
	public Image getImage(Object element){
		if (element instanceof IntelWorkingSetCategory){
			return ((IntelWorkingSetCategory) element).getImage();
		}else if (element instanceof IntelWorkingSetItem){
			return ((IntelWorkingSetItem) element).getImageDescriptor().createImage();
			
		}
		return super.getImage(element);
	}
}
