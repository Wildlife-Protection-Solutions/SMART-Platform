/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.patrol.internal.ui.properties;

import java.util.HashMap;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyIconItem;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Patrol attribute label provider.  Provides images and labels
 * for patrol attributes and attribute list items.
 * 
 * @author Emily
 *
 */
public class AttributeLabelProvider extends LabelProvider implements IColorProvider{

	public enum IconSetOption{
		ALL,
		DEFAULT
	}
	private Language language;
	private HashMap<Object, Image> images = new HashMap<>();
	
	private int iconSize = 16;
	private IconSetOption option = IconSetOption.DEFAULT; 
	
	public AttributeLabelProvider(){
		
	}
	
	public AttributeLabelProvider(int iconSize, IconSetOption op){
		this.iconSize = iconSize;
		this.option = op;
	}
	

	public AttributeLabelProvider(Language lang){
		this.language = lang;
	}

	public void setLanguage(Language l){
		this.language = l;
	}
	
	public void clearImageCache() {
		for (Image img : images.values()) img.dispose();
		images.clear();
	}
	@Override
	public void dispose() {
		super.dispose();
		clearImageCache();
	}
	
	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns <code>null</code>.
	 * Subclasses may override.
	 */
	public Image getImage(Object element) {
		
		if (element instanceof NamedKeyIconItem){
			NamedKeyIconItem ma = (NamedKeyIconItem)element;
			if (ma.getIcon() == null) return null;
			if (images.containsKey(ma)) return images.get(ma);
		
			//find icon for default icon set
			if (option == IconSetOption.DEFAULT) {
				Image img = SmartUtils.getImage(ma.getIcon(), iconSize);
				images.put(ma, img);			
				return img;
			}else if (option == IconSetOption.ALL) {
				Image img = SmartUtils.generateImage(ma.getIcon(), iconSize);
				images.put(element, img);			
				return img;
			}
		}
		
		return super.getImage(element);
	}

	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns the element's
	 * <code>toString</code> string. Subclasses may override.
	 */
	public String getText(Object element) {
		if (element instanceof PatrolAttribute){
			if (language == null){
				return ((PatrolAttribute)element).getName();
			}else{
				String value = ((PatrolAttribute)element).findNameNull(language);
				if (value == null){
					value = ((PatrolAttribute)element).findNameNull(language) ;
				}
				return value;
			}
		}else if (element instanceof PatrolAttributeListItem){
			if (language == null){
				return ((PatrolAttributeListItem)element).getName();
			}else{
				String value = ((PatrolAttributeListItem)element).findNameNull(language);
				if (value == null){
					value = ((PatrolAttributeListItem)element).getName();
				}
				return value + " [" + ((PatrolAttributeListItem)element).getKeyId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return super.getText(element);
	}

	@Override
	public Color getForeground(Object element) {
		if (!(element instanceof PatrolAttribute)) return null;
		if (!((PatrolAttribute)element).getIsActive()) {
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
		}
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		return null;
	}
}
