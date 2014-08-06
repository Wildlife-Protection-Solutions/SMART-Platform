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
package org.wcs.smart.er.query.filter;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;

public class MissionPropertyFilter implements IFilter {

	
	/**
	 * Creates a survey id filter.
	 * 
	 * @return
	 */
	public static MissionPropertyFilter createFilter(String key, Operator op, Object value){
		String[] bits = key.split(":"); //$NON-NLS-1$
		String keyString = bits[3];
		String strtype = bits[2];
		
		Attribute.AttributeType type = Attribute.decodeAttributeTypeKey(strtype);
		MissionPropertyFilter filter = new MissionPropertyFilter(keyString, type, op, value);
		return filter;
	}
	
	private Operator op;
	private String missionAttributeKey;
	private Attribute.AttributeType type;
	private Object value;
	
	public MissionPropertyFilter(String attributeKey, Attribute.AttributeType type, Operator op, Object value){
		this.missionAttributeKey = attributeKey;
		this.op = op;
		this.value = value;
		this.type = type;
	}
	
	/**
	 * 
	 * @return the mission attribute key
	 */
	public String getAttributeKey(){
		return this.missionAttributeKey;
	}
	
	/**
	 * mission attribute type
	 * @return
	 */
	public Attribute.AttributeType getAttributeType(){
		return this.type;
	}
	
	/**
	 * filter value
	 * @return
	 */
	public Object getValue(){
		return this.value;
	}
	
	/**
	 * filter operator
	 * @return
	 */
	public Operator getOperator(){
		return this.op;
	}
	
	@Override
	public String asString() {
		String key = "s:missionproperty:" + type.typeKey  + ":" + missionAttributeKey + " " + op.asSmartValue() ;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		 if (type == AttributeType.NUMERIC){
			return key + " " + ((Double)value).toString();  //$NON-NLS-1$  
		}else if (type == AttributeType.TEXT){
			return key + " \"" + ((String)value) + "\"";  //$NON-NLS-1$  //$NON-NLS-2$  
		}else if (type == AttributeType.LIST){
			return key + " " + ((String)value); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		return null;
	}

}
