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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.util.SharedUtils;

/**
 * A record attribute filter.
 * 
 * @author Emily
 *
 */
public class RecordAttributeFilter implements IQueryFilter, IColumnIdentifierProvider {

	//boolean
	public static RecordAttributeFilter create(String key){
		RecordAttributeFilter filter = createCore(key);
		return filter;
	}
		
	//numeric
	public static RecordAttributeFilter create(String key, Operator operator, Double value){
		RecordAttributeFilter filter = createCore(key);
		filter.operator = operator;
		filter.numberValue = value;
		return filter;
	}
		
	//text
	public static RecordAttributeFilter create(String key, Operator operator, String value){
		RecordAttributeFilter filter = createCore(key);
		filter.operator = operator;
		filter.stringValue = SharedUtils.stripQuotes(value);
		return filter;
	}
	
	//list and tree and employee
	public static RecordAttributeFilter create(String key, String keyId){
		RecordAttributeFilter filter = createCore(key);
		filter.keyValue = keyId;
		return filter;
	}
		
	//dates
	public static RecordAttributeFilter create(String key, Operator operator, LocalDate date1, LocalDate date2){
		RecordAttributeFilter filter = createCore(key);
		filter.operator = operator;
		filter.dateValues = new LocalDate[]{date1, date2};
		return filter;
	}
		
	private static RecordAttributeFilter createCore(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		IntelAttribute.AttributeType type = parseType(bits[1]);
		String recordsourceattributekey = bits[2];
		String recordsourcekey = bits[3];
		return new RecordAttributeFilter(type,recordsourceattributekey,recordsourcekey);
	}
	
	private static IntelAttribute.AttributeType parseType(String attributeType){
		if (attributeType.equalsIgnoreCase("entity")) return null; //$NON-NLS-1$
		try {
			return IntelAttribute.AttributeType.parse(attributeType);
		}catch (RuntimeException re) {
			throw new IllegalStateException(attributeType + " is not a valid attribute type identifier"); //$NON-NLS-1$
		}
	}


	
	private IntelAttribute.AttributeType attributeType = null; //attribute type or null if it's an entity attribute
	private String attributeKey = null; //IntelRecordSourceAttribute key
	private String recordsourceKey = null; //IntelRecordSource key
	
	private Operator operator = null;
	private Double numberValue = null;
	private String stringValue = null;
	private String keyValue = null;
	private LocalDate[] dateValues = null;
	
	public RecordAttributeFilter(IntelAttribute.AttributeType type, String attributeentitykey, String recordsourceKey){
		this.attributeType = type;
		this.recordsourceKey = recordsourceKey;
		this.attributeKey = attributeentitykey;
		this.attributeType = type;		
	}

	
	public String getRecordSourceKey() {
		return this.recordsourceKey;
	}
	
	/**
	 * Combines the various filter fields to generate a unique 
	 * string identifier for the filter
	 * 
	 * @return
	 */
	@Override
	public String getUniqueColumnIdentifier(){
		StringBuilder sb = new StringBuilder();
		sb.append("ra_"); //$NON-NLS-1$
		sb.append(attributeKey);
		sb.append("_"); //$NON-NLS-1$
		if (attributeType != null) {
			switch(attributeType){
			case BOOLEAN:
				break;
			case DATE:
				sb.append(operator.name());
				sb.append("_"); //$NON-NLS-1$
				sb.append(dateValues[0].toString());
				sb.append("_"); //$NON-NLS-1$
				sb.append(dateValues[1].toString());
				break;
			case EMPLOYEE:
				sb.append(keyValue);
				break;
			case LIST:
				sb.append(keyValue);
				break;
			case NUMERIC:
				sb.append(operator.name());
				sb.append("_"); //$NON-NLS-1$
				sb.append(numberValue);
				break;
			case TEXT:
				sb.append(operator.name());
				sb.append("_"); //$NON-NLS-1$
				sb.append(stringValue);
				break;
			case POSITION:
				throw new UnsupportedOperationException("position attributes not supported in queries"); //$NON-NLS-1$
			}
		}else {
			sb.append(keyValue);
		}
		return sb.toString();
	}
	
	/**
	 * Will be null if attribute is an entity type attribute
	 * @return
	 */
	public IntelAttribute.AttributeType getAttributeType(){
		return this.attributeType;
	}
	
	public String getAttributeKey(){
		return this.attributeKey;
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
	public LocalDate[] getDateValues(){
		return this.dateValues;
	}
	
	
	@Override
	public String asString() {
		// | < RATTRIBUTE_KEY_ENTITY: "r_attribute:entity:"< DM_KEY >":"(< DM_KEY >)?>
		StringBuilder sb = new StringBuilder();
		sb.append("r_attribute:"); //$NON-NLS-1$
		if (attributeType != null) {
			sb.append(attributeType.key);
			sb.append(":"); //$NON-NLS-1$
		} else {
			sb.append("entity:"); //$NON-NLS-1$
		}
		sb.append(attributeKey);
		sb.append(":"); //$NON-NLS-1$
		sb.append(recordsourceKey == null ? "" : recordsourceKey); //$NON-NLS-1$

		if (attributeType == null) {
			sb.append(" "); //$NON-NLS-1$
			sb.append(Operator.EQUALS.getKey());
			sb.append(" "); //$NON-NLS-1$
			sb.append(keyValue);
		} else if (attributeType == AttributeType.BOOLEAN) {
			// nothing
		} else if (attributeType == AttributeType.NUMERIC) {
			sb.append(" "); //$NON-NLS-1$
			sb.append(operator.getKey());
			sb.append(" "); //$NON-NLS-1$
			sb.append(numberValue);
		} else if (attributeType == AttributeType.TEXT) {
			sb.append(" "); //$NON-NLS-1$
			sb.append(operator.getKey());
			sb.append(" "); //$NON-NLS-1$
			sb.append("\"" + stringValue + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (attributeType == AttributeType.LIST || attributeType == AttributeType.EMPLOYEE) {
			sb.append(" "); //$NON-NLS-1$
			sb.append(Operator.EQUALS.getKey());
			sb.append(" "); //$NON-NLS-1$
			sb.append(keyValue);

		} else if (attributeType == AttributeType.DATE) {
			sb.append(" "); //$NON-NLS-1$
			sb.append(operator.getKey());
			sb.append(" "); //$NON-NLS-1$
			sb.append(DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR).format(dateValues[0]));
			sb.append(" "); //$NON-NLS-1$
			sb.append(Operator.AND.getKey());
			sb.append(" "); //$NON-NLS-1$
			sb.append(DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR).format(dateValues[1]));
		}
		return sb.toString();
	}
}
