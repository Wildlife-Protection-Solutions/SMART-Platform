/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query.observation.filter;

import java.util.Date;

import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.util.SharedUtils;

/**
 * Intelligence attribute filter.
 * 
 * @author Emily
 *
 */
public class IntelAttributeFilter implements IQueryFilter {

		
	//boolean
	public static IntelAttributeFilter create(String key){
		IntelAttributeFilter filter = createCore(key);
		return filter;
	}
		
	//numeric
	public static IntelAttributeFilter create(String key, Operator operator, Double value){
		IntelAttributeFilter filter = createCore(key);
		filter.operator = operator;
		filter.numberValue = value;
		return filter;
	}
		
	//text
	public static IntelAttributeFilter create(String key, Operator operator, String value){
		IntelAttributeFilter filter = createCore(key);
		filter.operator = operator;
		filter.stringValue = SharedUtils.stripQuotes(value);
		return filter;
	}
	
	//list and tree
	public static IntelAttributeFilter create(String key, String keyId){
		IntelAttributeFilter filter = createCore(key);
		filter.keyValue = keyId;
		return filter;
	}
		
	//dates
	public static IntelAttributeFilter create(String key, Operator operator, Date date1, Date date2){
		IntelAttributeFilter filter = createCore(key);
		filter.operator = operator;
		filter.dateValues = new Date[]{date1, date2};
		return filter;
	}
		
	private static IntelAttributeFilter createCore(String key){
		String bits[] = key.split(":");
		IntelAttribute.AttributeType type = parseType(bits[1]);
		String attributeKey = bits[2];
		String entityTypeKey = null;
		if (bits.length > 3){
			entityTypeKey = bits[3];
			if (entityTypeKey.trim().isEmpty()) entityTypeKey = null;
		}
		return new IntelAttributeFilter(type,attributeKey,entityTypeKey);
	}
	
	private static IntelAttribute.AttributeType parseType(String attributeType){
		for (IntelAttribute.AttributeType t : IntelAttribute.AttributeType.values()){
			if (t.key.equalsIgnoreCase(attributeType)){
				return t;
			}
		}
		throw new IllegalStateException(attributeType + " is not a valid attribute type identifier");
	}


	
	private IntelAttribute.AttributeType attributeType = null;
	private String attributeKey = null;
	private String entityTypeKey = null;
	
	private Operator operator = null;
	private Double numberValue = null;
	private String stringValue = null;
	private String keyValue = null;
	private Date[] dateValues = null;
	

	public IntelAttributeFilter(IntelAttribute.AttributeType type, String attributeKey, String entityTypeKey){
		this.entityTypeKey = entityTypeKey;
		this.attributeKey = attributeKey;
		this.attributeType = type;
	}
	
	public IntelAttributeFilter(IntelAttribute.AttributeType type, String attributeKey, String entityTypeKey, Operator operator, Double numberValue){
		this(type, attributeKey, entityTypeKey);
		this.operator = operator;
		this.numberValue = numberValue;
	}
	
	public IntelAttributeFilter(IntelAttribute.AttributeType type, String attributeKey, String entityTypeKey, Operator operator, String stringValue){
		this(type, attributeKey, entityTypeKey);
		this.operator = operator;
		this.stringValue = stringValue;
	}

	public IntelAttributeFilter(IntelAttribute.AttributeType type, String attributeKey, String entityTypeKey, String keyId){
		this(type, attributeKey, entityTypeKey);
		this.keyValue = keyId;
	}
	
	public IntelAttributeFilter(IntelAttribute.AttributeType type, String attributeKey, String entityTypeKey, Operator operator, Date[] dates){
		this(type, attributeKey, entityTypeKey);
		this.operator = operator;
		this.dateValues = dates;
	}
	
	public IntelAttribute.AttributeType getAttributeType(){
		return this.attributeType;
	}
	
	public String getAttributeKey(){
		return this.attributeKey;
	}
	
	public String getEntityTypeKey(){
		return this.entityTypeKey;
	}
	
	public Operator getOperator(){
		return this.operator;
	}
	public String getStringValue(){
		return this.stringValue;
	}
	public Double getNumberValue(){
		return this.numberValue;
	}
	public String getKeyValue(){
		return this.keyValue;
	}
	public Date[] getDateValues(){
		return this.dateValues;
	}
}
