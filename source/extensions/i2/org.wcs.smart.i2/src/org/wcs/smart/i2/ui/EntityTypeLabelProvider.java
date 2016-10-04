/*
 * Copyright (C) 2016 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.i2.ui;

import java.awt.image.BufferedImage;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Label provider for entity types.
 * 
 * @author Emily
 *
 */
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
	
	public ImageDescriptor createImageDescriptor(IntelEntityType type){
		return new ImageDescriptor() {
			
			@Override
			public ImageData getImageData() {
				try{
					BufferedImage image = type.getIconAsImage();
					if (image != null){
						return AWTSWTImageUtils.convertToSWTImage(image).getImageData();
					}
				}catch (Exception ex){
					
				}
				return null;
			}
		};
	}
}
