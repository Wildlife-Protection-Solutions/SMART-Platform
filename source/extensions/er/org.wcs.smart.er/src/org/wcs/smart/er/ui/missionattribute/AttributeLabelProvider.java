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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;

/**
 * Mission attribute label provider.  Provides images and labels
 * for mission attributes and mission attribute list items.
 * 
 * @author Emily
 *
 */
public class AttributeLabelProvider extends LabelProvider {

	private Language language;
	
	public AttributeLabelProvider(){
		
	}
	
	public AttributeLabelProvider(Language lang){
		this.language = lang;
	}

	public void setLanguage(Language l){
		this.language = l;
	}
	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns <code>null</code>.
	 * Subclasses may override.
	 */
	public Image getImage(Object element) {
		if (element instanceof MissionAttribute){
			MissionAttribute ma = (MissionAttribute)element;
			return DataModel.getAttributeImage(ma.getType());
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
			if (language == null){
				return ((MissionAttribute)element).getName();
			}else{
				String value = ((MissionAttribute)element).findNameNull(language);
				if (value == null){
					value = ((MissionAttribute)element).findNameNull(language) ;
				}
				return value;
			}
		}else if (element instanceof MissionAttributeListItem){
			if (language == null){
				return ((MissionAttributeListItem)element).getName();
			}else{
				String value = ((MissionAttributeListItem)element).findNameNull(language);
				if (value == null){
					value = ((MissionAttributeListItem)element).getName();
				}
				return value + " [" + ((MissionAttributeListItem)element).getKeyId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return super.getText(element);
	}
}
