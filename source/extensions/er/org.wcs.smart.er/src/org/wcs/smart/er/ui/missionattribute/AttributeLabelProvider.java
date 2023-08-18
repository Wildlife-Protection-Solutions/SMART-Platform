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
package org.wcs.smart.er.ui.missionattribute;

import org.wcs.smart.ca.IconCache;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.Language;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.ui.NamedIconItemLabelProvider;

/**
 * Mission attribute label provider.  Provides images and labels
 * for mission attributes and mission attribute list items.
 * 
 * @author Emily
 *
 */
public class AttributeLabelProvider extends NamedIconItemLabelProvider {

	public AttributeLabelProvider(IconManager.Size iconSize){
		super(iconSize);
	}
	
	public AttributeLabelProvider(IconManager.Size iconSize, IconCache.IconSetOption iconType){
		super(iconSize, iconType);
	}
	
	public AttributeLabelProvider(){
		super();
	}
	
	public AttributeLabelProvider(Language lang){
		super();
		setLanguage(lang);
	}
	
	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns the element's
	 * <code>toString</code> string. Subclasses may override.
	 */
	public String getText(Object element) {
		if (element instanceof MissionAttributeListItem && currentLanguage != null){
			//if we have a language we are adding key
			String value = ((MissionAttributeListItem)element).findNameNull(currentLanguage);
			if (value == null){
				value = ((MissionAttributeListItem)element).getName();
			}
			return value + " [" + ((MissionAttributeListItem)element).getKeyId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.getText(element);
	}
}
