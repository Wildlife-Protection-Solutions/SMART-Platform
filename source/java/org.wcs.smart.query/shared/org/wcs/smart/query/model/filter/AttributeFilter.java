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
package org.wcs.smart.query.model.filter;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.util.SharedUtils;

/**
 * Query filter for data model attributes.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeFilter implements IFilter {

	public static final String ANY_OPTION_KEY = "list.any"; //$NON-NLS-1$

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
		value = SharedUtils.stripQuotes(value);
		return new AttributeFilter(attributeIdentifier,  op, value);
	}

	/**
	 * Creates a new date attribute filter
	 * 
	 * Date filters are of the form: <DATE> BETWEEN <DATE1> AND <DATE2>
	 * 
	 * @param attributeIdentifier the attribute identifier in the form "attribute:s:<key>"
	 * @param date1 the first date
	 * @param date2 the second date
	 * @return
	 */
	public static AttributeFilter createDateFilter(String attributeIdentifier, String date1, String date2, Operator op){
		return new AttributeFilter(attributeIdentifier, op, date1, date2);
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
	
	
	private String fullIdentifier;
	private String attributeKey;
	private AttributeType attributeType;
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
		this.fullIdentifier = attributeIdentifier;
		
		String[] bits = this.fullIdentifier.split(":"); //$NON-NLS-1$
		
		this.attributeType = Attribute.decodeAttributeTypeKey(bits[1]);
		attributeKey = bits[2];
	}
	
	/**
	 * Creates a new attribute filter 
	 * @param attributeIdentifier the attribute key of the form attribute:type:attributeKey
	 * @param op the filter operator
	 * @param value the filter value
	 */
	protected AttributeFilter(String attributeIdentifier, Operator op, Object value){
		this(attributeIdentifier);
		this.op = op;
		this.value1 = value;
	}
	
	/* for between operators */
	protected AttributeFilter(String attributeIdentifier, Operator op, Object value, Object value2){
		this(attributeIdentifier, op,  value);
		this.value2 = value2;
	}
	
	public Operator getOperator(){
		return this.op;
	}
	/**
	 * @return the unique attribute key
	 */
	public String getAttributeKey(){
		return this.attributeKey;
	}
	/**
	 * @return the type of attribute represented by the filter
	 */
	public AttributeType getAttributeType(){
		return this.attributeType;
	}
	/**
	 * @return the attribute filter value; the type depends on the attribute type
	 */
	public Object getValue(){
		return this.value1;
	}
	/**
	 * @return the second attribute filter value; the type depends on the attribute type
	 */
	public Object getValue2(){
		return this.value2;
	}
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		if (attributeType == AttributeType.BOOLEAN){
			return fullIdentifier;
		}else if (attributeType == AttributeType.NUMERIC){
			return fullIdentifier + " " + op.asSmartValue() + " " + ((Double)value1).toString();  //$NON-NLS-1$  //$NON-NLS-2$
		}else if (attributeType == AttributeType.TEXT){
			return fullIdentifier + " " + op.asSmartValue() + " \"" + ((String)value1) + "\"";  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}else if (attributeType == AttributeType.TREE || attributeType == AttributeType.LIST){
			return fullIdentifier + " " + op.asSmartValue() + " " + ((String)value1);  //$NON-NLS-1$  //$NON-NLS-2$  
		}else if (attributeType == AttributeType.DATE){
			return fullIdentifier + " " + op.asSmartValue() + " " + (String)value1 + " " + Operator.AND.asSmartValue() + " " + ((String)value2); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$ 
		}
		return ""; //$NON-NLS-1$
	}

	public void accept(IFilterVisitor visitor){
		visitor.visit(this);
	}
}

