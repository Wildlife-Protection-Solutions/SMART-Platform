/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.parser.internal.filter;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.IFilterVisitor;
import org.wcs.smart.filter.Operator;

/**
 * A patrol filter for custom patrol attributes. 
 * Of the form "patrol:attribute:<typekey>:<key> <operator> <value1> <value2>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolAttributeFilter implements IFilter {
	
	private String key;
	private String attributeKey;
	private Attribute.AttributeType type;
	
	private Operator op;
	private Object value1;
	private Object value2;
	
	public static PatrolAttributeFilter createFilter(String key, Operator op, Object value){
		return new PatrolAttributeFilter(key, op, value, null);
	}
	
	public static PatrolAttributeFilter createFilter(String key, Operator op, Object value1, Object value2){
		return new PatrolAttributeFilter(key, op, value1, value2);
	}
	
	public static PatrolAttributeFilter createFilter(String key){
		return new PatrolAttributeFilter(key);
	}
	
	
	/**
	 * Creates a new patrol attribute filter
	 * @param patrolKey patrol key
	 * @param op operator
	 * @param value filter value
	 */
	public PatrolAttributeFilter (String key, Operator op, Object value1, Object value2){
		this(key);
		this.op = op;
		this.value1 = value1;
		this.value2 = value2;
	}
	
	
	/**
	 * Creates a new patrol filter
	 * @param patrolKey patrol filter key
	 */
	private PatrolAttributeFilter (String key){
		this.key = key;
		
		String[] bits = key.split(":"); //$NON-NLS-1$
		this.attributeKey = bits[3];
		
		String typeKey = bits[2];
		this.type = Attribute.decodeAttributeTypeKey(typeKey);
	}
	
	public String getAttributeKey() {
		return this.attributeKey;
	}
	
	public Attribute.AttributeType getAttributeType(){
		return this.type;
	}
	
	/**
	 * @see org.wcs.smart.patrol.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		if (value1 == null){
			return key;
		}else if (value1 != null && value2 == null) {
			return key + " " + op.asSmartValue() + " " + value1;  //$NON-NLS-1$  //$NON-NLS-2$
		}else if (value1 != null && value2 != null) {
			return key + " " + op.asSmartValue() + " " + value1 + " " + value2;  //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$
		}
		return key;
	}
	
	/**
	 * <p>Quotes have been removed</p>
	 * @return the filter value as a string
	 */
	public Object getValue1(){
		return value1;
	}
	
	public Object getValue2(){
		return value2;
	}
	
	public Operator getOperator(){
		return this.op;
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);		
	}
	
}
