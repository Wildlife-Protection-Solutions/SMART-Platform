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

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ca.IconCache;
import org.wcs.smart.ca.IconItem;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.patrol.model.PatrolAttribute;

/**
 * Patrol attribute label provider.  Provides images and labels
 * for patrol attributes and attribute list items.
 * 
 * @author Emily
 *
 */
public class AttributeLabelProvider extends LabelProvider implements IColorProvider{

	private Language language;
	private IconCache iconCache;
	
	public AttributeLabelProvider(){
	}
	
	public AttributeLabelProvider(IconManager.Size iconSize){
		this(iconSize, IconCache.IconSetOption.DEFAULT);
	}
	
	public AttributeLabelProvider(IconManager.Size iconSize, IconCache.IconSetOption op){
		this.iconCache = new IconCache(null, iconSize);
		iconCache.setIconSetOption(op);
	}

	public AttributeLabelProvider(Language lang){
		this(IconManager.Size.ICON, IconCache.IconSetOption.DEFAULT);
		this.language = lang;
	}

	public void setLanguage(Language l){
		this.language = l;
	}
	
	public void clearImageCache() {
		iconCache.clearCache();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		iconCache.dispose();
	}
	
	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns <code>null</code>.
	 * Subclasses may override.
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof IconItem) return iconCache.getImage((IconItem) element);
		return null;
	}

	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns the element's
	 * <code>toString</code> string. Subclasses may override.
	 */
	@Override
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
		}else if (element instanceof NamedKeyItem){
			if (language == null){
				return ((NamedKeyItem)element).getName();
			}else{
				String value = ((NamedKeyItem)element).findNameNull(language);
				if (value == null){
					value = ((NamedKeyItem)element).getName();
				}
				return value + " [" + ((NamedKeyItem)element).getKeyId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
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
