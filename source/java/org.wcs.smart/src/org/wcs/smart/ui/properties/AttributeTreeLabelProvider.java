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
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider.RootNode;

/**
  * Label provided for attribute tree
  * @author Emily
  *
  */
public  class AttributeTreeLabelProvider extends LabelProvider implements IColorProvider {

	private static final Color BLACK = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	private static final Color GRAY = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);

	private Language currentLang = null;
		
	/**
	 * Creates new data model provided
	 * @param lang the working language; if null default is used
	 */
	public AttributeTreeLabelProvider(Language lang){
		this.currentLang = lang;
	}
		
	public AttributeTreeLabelProvider(){
		this(null);
	}
		
	/**
	 * Update the display language
	 * 
	 * @param lang the new display language
	 */
	public void setLanguage(Language lang){
		this.currentLang = lang;
	}
		
	/**
	 * 
	 * @return the current display language
	 */
	public Language getLanguage(){
		return this.currentLang;
	}
		
	@Override
	public String getText(Object element) {
		if (element instanceof RootNode){
			return Messages.AttributeTreeLabelProvider_RootNode_Label;
		}
		
		if (element instanceof DmObject){
			DmObject obj = (DmObject)element;
			if (currentLang != null){
				String l = obj.findNameNull(currentLang);
				if (l == null){
					l = obj.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
				}
				return l + "  [" + obj.getKeyId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				return obj.getName() + "  [" + obj.getKeyId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$;
			}
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}
		
	@Override
	public Color getForeground(Object element) {
		boolean active = true;
		if (element instanceof AttributeTreeNode){
			active = ((AttributeTreeNode)element).getIsActive();
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