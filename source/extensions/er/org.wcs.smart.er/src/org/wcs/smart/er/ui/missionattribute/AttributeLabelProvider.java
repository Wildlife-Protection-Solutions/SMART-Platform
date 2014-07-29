package org.wcs.smart.er.ui.missionattribute;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;

public class AttributeLabelProvider extends LabelProvider {
	public static AttributeLabelProvider instance = null;
	
	
	public static AttributeLabelProvider getInstance(){
		synchronized(AttributeLabelProvider.class) {
			if (instance == null){
				instance = new AttributeLabelProvider();
			}
		}
		return instance;
	}
	
	private AttributeLabelProvider(){}
	
	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns <code>null</code>.
	 * Subclasses may override.
	 */
	public Image getImage(Object element) {
		if (element instanceof MissionAttribute){
			MissionAttribute ma = (MissionAttribute)element;
			return ma.getType().getImage();
		}
		return super.getImage(element);
	}

	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns the element's
	 * <code>toString</code> string. Subclasses may override.
	 */
	public String getText(Object element) {
		if (element instanceof MissionAttribute){
			return ((MissionAttribute)element).getName();
		}else if (element instanceof MissionAttributeListItem){
			return ((MissionAttributeListItem)element).getName();
		}
		return super.getText(element);
	}
}
