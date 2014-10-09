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

import java.text.Collator;
import java.util.Comparator;

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

	public static enum FixedColumns{TYPE, ID, LENGTH, STATE};
	
	private String key;
	private AttributeType type;
	private Comparator<Object> comparator = null;
	
	/**
	 * Creates a new label provider for a fixed column.
	 * Key = One of the fixed column values. For attributes
	 * use the constructor (String, AttributeType)
	 * 
	 * @param key
	 */
	public SamplingUnitColumnLabelProvider(String key){
		this.key = key;
	}
	
	/**
	 * Creates a new label provider for an attribute column.
	 * @param key
	 * @param type
	 */
	public SamplingUnitColumnLabelProvider(String key, AttributeType type){
		this.key = key;
		this.type = type;
	}
	
	/**
	 * Compares the two objects; 
	 * 
	 * @param o1
	 * @param o2
	 * @return
	 */
	public int compare(Object o1, Object o2){
		Object v1 = getSortObject(o1);
		Object v2 = getSortObject(o2);
		if (v1 == null && v2 == null) return 0;
		if (v1 == null && v2 != null) return -1;
		if (v1 != null && v2 == null) return 1;
		
		if (comparator == null){
			findComparator();
		}
		return comparator.compare(v1, v2);
	}
	
	private void findComparator(){
		if ( (type == null &&
				(key.equals(FixedColumns.TYPE.name()) ||
				key.equals(FixedColumns.ID.name()) ||
				key.equals(FixedColumns.STATE.name()))) || 
			(type == AttributeType.TEXT))  {
			//string
				comparator = new Comparator<Object>() {
					@Override
					public int compare(Object o1, Object o2) {
						return Collator.getInstance().compare(o1, o2);
					}
				};
		}else if ( (type == null &&
				(key.equals(FixedColumns.LENGTH.name()))) || 
				(type == AttributeType.NUMERIC))  { 
			
				comparator = new Comparator<Object>() {
					@Override
					public int compare(Object o1, Object o2) {
						return ((Double)o1).compareTo((Double)o2);
					}
				};
			
		}else{
			comparator = new Comparator<Object>() {
				@Override
				public int compare(Object o1, Object o2) {
					return Collator.getInstance().compare(o1.toString(), o2.toString());
				}
			};
		}
	}
	
	private Object getSortObject(Object element){
		if (element instanceof SamplingUnit){
			SamplingUnit su = (SamplingUnit) element;
			
			if (type == null){
				if (key.equals(FixedColumns.TYPE.name())){
					return su.getType().getGuiName();
				}else if(key.equals(FixedColumns.ID.name())){
					return su.getId();
				}else if (key.equals(FixedColumns.LENGTH.name())){
					if (su.getGeometryLengthKm() == null){
						return null; 
					}else{
						return su.getGeometryLengthKm();
					}
				}else if (key.equals(FixedColumns.STATE.name())){
					return su.getState().getGuiName();
				}
			}else{
				//search attributes
				for (SamplingUnitAttributeValue sua : su.getAttributes()){
					if (sua.getSamplingUnitAttribute().getKeyId().equals(key)){
						return sua.getValueAsString();
					}
				}
				return null;
			}
			
		}
		return null;
	}
	
	public String getText(Object element){
		if (element instanceof SamplingUnit){
			SamplingUnit su = (SamplingUnit) element;
			
			if (key.equals(FixedColumns.TYPE.name())){
				return su.getType().getGuiName();
			}else if(key.equals(FixedColumns.ID.name())){
				return su.getId();
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
						return sua.getValueAsString();
					}
				}
				return ""; //$NON-NLS-1$
				
			}
			
		}
		
		return super.getText(element);
	}
}
