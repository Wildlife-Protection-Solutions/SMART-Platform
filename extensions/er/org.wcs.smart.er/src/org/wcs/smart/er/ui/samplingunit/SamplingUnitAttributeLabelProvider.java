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
package org.wcs.smart.er.ui.samplingunit;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.er.model.SamplingUnitAttribute;

/**
 * Sampling unit attribute label provider.  Provides images and labels
 * for sampling unit attributes.
 * 
 * @author Emily
 *
 */
public class SamplingUnitAttributeLabelProvider extends LabelProvider {

	private Language language;
	
	public SamplingUnitAttributeLabelProvider(){
		
	}
	
	public SamplingUnitAttributeLabelProvider(Language lang){
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
		if (element instanceof SamplingUnitAttribute){
			SamplingUnitAttribute ma = (SamplingUnitAttribute)element;
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
		if (element instanceof SamplingUnitAttribute){
			if (language == null){
				return ((SamplingUnitAttribute)element).getName();
			}else{
				String value = ((SamplingUnitAttribute)element).findNameNull(language);
				if (value == null){
					value = ((SamplingUnitAttribute)element).findNameNull(language) ;
				}
				return value;
			}
		}
		return super.getText(element);
	}
}
