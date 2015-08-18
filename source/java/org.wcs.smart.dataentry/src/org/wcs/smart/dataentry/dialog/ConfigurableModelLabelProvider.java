/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.dataentry.dialog;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.dataentry.DataentryPlugIn;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Label provider for {@link NamedItem} objects.
 * 
 * @author elitvin
 */
public class ConfigurableModelLabelProvider extends LabelProvider implements IColorProvider {

	private Language currentLanguage;
	
	public ConfigurableModelLabelProvider(){
		currentLanguage = null;
	}
	
	public void setLanguage(Language currentLanguage){
		this.currentLanguage = currentLanguage;
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof NamedItem) {
			NamedItem i = (NamedItem)element;
			if (currentLanguage == null){
				return i.getName();
			}
			String l = i.findNameNull(currentLanguage);
			if (l == null){
				l = i.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			}
			
			return l;
		}
		if (element instanceof CmRootNode) {
			return getText(((CmRootNode)element).model);
		}
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof CmNode) {
			CmNode node = (CmNode) element;
			return node.isGroup() ? DataentryPlugIn.getDefault().getImageRegistry().get(DataentryPlugIn.GROUP_ICON) : SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON);
		} else if (element instanceof CmAttribute){
			return DataModel.getAttributeImage((((CmAttribute)element).getAttribute()).getType());
		} else if (element instanceof CmRootNode){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON);
		}
		return null;
	}


	@Override
	public Color getForeground(Object element) {
		if (element instanceof CmAttribute){
			if (!((CmAttribute)element).isVisible()){
				return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
			}
		}
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		return null;
	}
	
}
