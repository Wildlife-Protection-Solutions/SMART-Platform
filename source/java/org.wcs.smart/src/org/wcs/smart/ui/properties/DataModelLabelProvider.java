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
package org.wcs.smart.ui.properties;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Label provided for data model tree
 * @author Emily
 * @since 1.0.0
 */
public class DataModelLabelProvider extends LabelProvider implements IColorProvider {

	
	public static final Color BLACK = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	public static final Color GRAY = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);

	private Language currentLang = null;
	
	/**
	 * Creates new data model provided.  In this case
	 * the current system language is used.
	 * 
	 */
	public DataModelLabelProvider(){
	}
	
	/**
	 * Creates new data model provided
	 * @param lang the working language
	 */
	public DataModelLabelProvider(Language lang){
		this.currentLang = lang;
	}
	/**
	 * Update the language
	 * @param lang new language
	 */
	public void setLanguage(Language lang){
		this.currentLang = lang;
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof DataModelContentProvider.RootNode){
			return Messages.DataModelLabelProvider_RootNode_Label;
		}
		if (element instanceof CategoryAttribute){
			element = ((CategoryAttribute)element).getAttribute();
		}
		
		if (element instanceof DmObject){
			DmObject obj = (DmObject)element;
			if (currentLang != null && !currentLang.equals(SmartDB.getCurrentLanguage())){
				String x = obj.findNameNull(currentLang);
				if (x==null){
					x = obj.getName();
					if (x == null){
						x = obj.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
					}
				}
				return x;
			}else{
				return obj.getName();
			}
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof Category) {
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON);
		}else if (element instanceof CategoryAttribute){
			CategoryAttribute ca = (CategoryAttribute)element;
			return DataModel.getAttributeImage(ca.getAttribute().getType());
		}else if (element instanceof Attribute){
			return DataModel.getAttributeImage(((Attribute)element).getType());
		}else if (element instanceof DataModelContentProvider.RootNode){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON);
		}
		return null;
	}
	
	@Override
	public Color getForeground(Object element) {
		boolean active = true;
		if (element instanceof Category){
			active = ((Category) element).getIsActive();
		}else if (element instanceof CategoryAttribute){
			active = ((CategoryAttribute)element).getIsActive();
		}
		if (active){
			return BLACK;
		}else{
			return GRAY;
		}
	}
	
	@Override
	public Color getBackground(Object element) {
		return null;
	}

}
