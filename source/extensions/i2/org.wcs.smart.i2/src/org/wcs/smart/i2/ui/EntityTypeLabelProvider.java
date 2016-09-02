package org.wcs.smart.i2.ui;

import java.awt.image.BufferedImage;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntityType;

public class EntityTypeLabelProvider extends LabelProvider {

	public static EntityTypeLabelProvider INSTANCE = new EntityTypeLabelProvider();
	
	@Override
	public String getText(Object element){
		if (element instanceof IntelEntityType){
			return ((IntelEntityType) element).getName();
		}
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element){
		if (element instanceof IntelEntityType){
			try{
				BufferedImage image = ((IntelEntityType) element).getIconAsImage();
				if (image != null){
					return AWTSWTImageUtils.convertToSWTImage(image);
				}
			}catch (Exception ex){
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
			return null;
		}
		return super.getImage(element);
	}
}
