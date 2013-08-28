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
package org.wcs.smart.query.parser.internal.filter;

import java.util.List;

import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.filter.Operator;
import org.wcs.smart.upgrade.v112.SmartUtils;

/**
 * Query filter for data model attributes.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeFilter implements IFilter {
	/**
	 * Creates a new boolean attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "attribute:b:<key>"
	 * @return
	 */
	public static AttributeFilter createBooleanFilter(String attributeIdentifier){
		return new AttributeFilter(attributeIdentifier);
	}
	
	/**
	 * Creates a new value attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "attribute:n:<key>"
	 * @param op the operator
	 * @param Double value the filter value
	 * @return
	 */
	public static AttributeFilter createValueFilter(String attributeIdentifier, Operator op, Double value){
		return new AttributeFilter(attributeIdentifier, op, value);
	}
	/**
	 * Creates a new text attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "attribute:s:<key>"
	 * @param op the string operator
	 * @param value the filter value
	 * @return
	 */
	public static AttributeFilter createStringFilter(String attributeIdentifier, Operator op, String value){
		value = SmartUtils.stripQuotes(value);
		return new AttributeFilter(attributeIdentifier,  op, value);
	}

	/**
	 * Creates a new list item attribute filter 
	 * @param attributeIdentifier the attribute identifier in the form "attribute:l:<key>"
	 * @param op the list operator
	 * @param attributeItemKey the list item key
	 * @return
	 */
	public static AttributeFilter createListItemFilter(String attributeIdentifier, Operator op, String attributeItemKey){
		return new AttributeFilter(attributeIdentifier,  op, attributeItemKey);
	}
	
	/**
	 * Creates a new list item attribute filter 
	 * @param attributeIdentifier the attribute identifier in the form "attribute:t:<hkey>"
	 * @param op the list operator
	 * @param attributeItemKey the tree item hkey
	 * @return
	 */
	public static AttributeFilter createTreeItemFilter(String attributeIdentifier, Operator op, String attributeItemKey){
		return new AttributeFilter(attributeIdentifier,  op, attributeItemKey);
	}
	
	
	private String attributeKey;
	private String attributeType;
	private Operator op;
	private Object value1;
	private Object value2;
	
	
	/**
	 * Creates a new attribute filter with a given key and type.
	 * 
	 * @param attributeIdentifier
	 * @param type
	 */
	private AttributeFilter(String attributeIdentifier){
		String[] bits = attributeIdentifier.split(":"); //$NON-NLS-1$
		attributeType = bits[1];
		attributeKey = bits[2];
	}

	
	/**
	 * Creates a new attribute filter 
	 * @param attributeIdentifier the attribute key of the form attribute:type:attributeKey
	 * @param op the filter operator
	 * @param value the filter value
	 */
	private AttributeFilter(String attributeIdentifier, Operator op, Object value){
		this(attributeIdentifier);
		this.op = op;
		this.value1 = value;
	}
	
	/* for between operators */
	private AttributeFilter(String attributeIdentifier, Operator op, Object value, Object value2){
		this(attributeIdentifier, op,  value);
		this.value2 = value2;
	}
	
	/**
	 * @return the unique attribute key
	 */
	public String getAttributeKey(){
		return this.attributeKey;
	}

	public void setAttributeKey(String attributeKey){
		this.attributeKey = attributeKey;
	}
	
	public String getAttributeType(){
		return this.attributeType;
	}
	
	/**
	 * @return the attribute filter value; the type depends on the attribute type
	 */
	public Object getValue(){
		return this.value1;
	}
	
	public void setValue(Object newValue){
		this.value1 = newValue;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		String fullIdentifier = "attribute:" + attributeType + ":" +  attributeKey;
		if (attributeType.equals("b")){
			return fullIdentifier;
		}else if (attributeType.equals("n")){
			return fullIdentifier + " " + op.asSmartValue() + " " + ((Double)value1).toString();  //$NON-NLS-1$  //$NON-NLS-2$
		}else if (attributeType.equals("s")){
			return fullIdentifier + " " + op.asSmartValue() + " \"" + ((String)value1) + "\"";  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}else if (attributeType.equals("t") || attributeType.equals("l")){
			return fullIdentifier + " " + op.asSmartValue() + " " + ((String)value1);  //$NON-NLS-1$  //$NON-NLS-2$  
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}
	
}

