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
package org.wcs.smart.er.ui.surveydesign.editor;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;

/**
 * Label provider for a given sampling unit table column.
 * 
 * @author Emily
 *
 */
public class SamplingUnitColumnLabelProvider extends ColumnLabelProvider {

	public static enum FixedColumns{TYPE, ID, BUFFER, LENGTH, STATE};
	
	private String key;
	
	/**
	 * The column key.  One of the fixed column values or the
	 * sampling unit attribute key for sampling unit attributes.
	 * 
	 * @param key
	 */
	public SamplingUnitColumnLabelProvider(String key){
		this.key = key;
	}
	
	public String getText(Object element){
		if (element instanceof SamplingUnit){
			SamplingUnit su = (SamplingUnit) element;
			
			if (key.equals(FixedColumns.TYPE.name())){
				return su.getType().getGuiName();
			}else if(key.equals(FixedColumns.ID.name())){
				return su.getId();
			}else if (key.equals(FixedColumns.BUFFER.name())){
				if (su.getBuffer() == null){
					return ""; //$NON-NLS-1$
				}else{
					return su.getBuffer().toString();
				}
			}else if (key.equals(FixedColumns.LENGTH.name())){
				if (su.getGeometryLengthKm() == null){
					return ""; //$NON-NLS-1$
				}else{
					return su.getGeometryLengthKm().toString();
				}
			}else if (key.equals(FixedColumns.STATE.name())){
				return su.getState().getGuiName();
			}else{
				//search attributes
				for (SamplingUnitAttributeValue sua : su.getAttributes()){
					if (sua.getSamplingUnitAttribute().getKeyId().equals(key)){
						if (sua.getSamplingUnitAttribute().getType() == AttributeType.TEXT){
							if (sua.getStringValue() == null){
								return ""; //$NON-NLS-1$
							}else{
								return sua.getStringValue();
							}
						}else if (sua.getSamplingUnitAttribute().getType() == AttributeType.NUMERIC){
							if (sua.getDoubleValue() == null){
								return ""; //$NON-NLS-1$
							}else{
								return sua.getDoubleValue().toString();
							}
						}
					}
				}
				return ""; //$NON-NLS-1$
				
			}
			
		}
		
		return super.getText(element);
	}
}
