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
package org.wcs.smart.i2.ui.views.query;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.ui.AttributeLabelProvider;

/**
 * Label provider for FilterTreeItems
 * @author Emily
 *
 */
public class FilterTreeLabelProvider extends LabelProvider {

	private AttributeLabelProvider attributeInstance = new AttributeLabelProvider();
	private Map<Object, Image> toDispose = new HashMap<Object, Image>();

	@Override
	public String getText(Object element){
		if (element instanceof FilterTreeItem) return ((FilterTreeItem) element).getName();
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element){
		Image img = toDispose.get(element);
		if (img != null) return img;
		if (element instanceof AttributeTreeFilterItem){
			return attributeInstance.getImage(((AttributeTreeFilterItem) element).getType());
		}
		if (element instanceof EntityTypeTreeFilterItem){
			if (((EntityTypeTreeFilterItem) element).getImage() != null){
				img = ((EntityTypeTreeFilterItem) element).getImage().createImage();
				if (img != null){
					toDispose.put(element, img);
					return img;
				}
			}
			return null;
		}
		if (element instanceof AttributeHeaderFilterItem){
			if (((AttributeHeaderFilterItem) element).isGroup()){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON);
			}
			return AttributeType.NUMERIC.getImage();
		}
		
		if (element instanceof AreaTreeFilterItem){
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_AREA);
		}
		if (element instanceof DataModelTreeFilterItem){
			if (((DataModelTreeFilterItem) element).getType() == null){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON); 
			}else{
				return DataModel.getAttributeImage(((DataModelTreeFilterItem) element).getType());
			}
			
		}
		if (element instanceof BasicTreeFilterItem){
			if (((BasicTreeFilterItem) element).getImage() != null){
				img = ((BasicTreeFilterItem) element).getImage().createImage();
				if (img != null){
					toDispose.put(element, img);
					return img;
				}
			}
		}
		return null;
	}
	
	@Override
	public void dispose(){
		attributeInstance.dispose();
		toDispose.values().forEach(e->e.dispose());
	}
}
