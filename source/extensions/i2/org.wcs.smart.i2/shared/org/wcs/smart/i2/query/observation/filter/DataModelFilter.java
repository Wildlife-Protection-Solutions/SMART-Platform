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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.util.SharedUtils;

/**
 * Data model item filter.
 * 
 * @author Emily
 *
 */
public class DataModelFilter implements IQueryFilter, IColumnIdentifierProvider  {

	public static final String MLIST_SEPERATOR = ","; //$NON-NLS-1$

	//category
	public static DataModelFilter createCategory(String key){
		String[] bits = key.split(":"); //$NON-NLS-1$
		return new DataModelFilter(bits[1]);
	}
	
	//boolean
	public static DataModelFilter create(String key){
		DataModelFilter filter = createCore(key);
		return filter;
	}
	
	//numeric
	public static DataModelFilter create(String key, Operator operator, Double value){
		DataModelFilter filter = createCore(key);
		filter.operator = operator;
		filter.numberValue = value;
		return filter;
	}
	
	//text
	/**
	 * 
	 * @param key
	 * @param operator
	 * @param value must be wrapped in quotes
	 * @return
	 */
	public static DataModelFilter create(String key, Operator operator, String value){
		DataModelFilter filter = createCore(key);
		filter.operator = operator;
		if (filter.attributeType == Attribute.AttributeType.MLIST) {
			filter.keyValues = Arrays.asList( value.split(MLIST_SEPERATOR) );
		}else {
			filter.stringValue = SharedUtils.stripQuotes(value);
		}
		return filter;
	}
	
	//list and tree
	public static DataModelFilter create(String key, String keyId){
		DataModelFilter filter = createCore(key);
		filter.keyValue = keyId;
		return filter;
	}
	
	//dates
	public static DataModelFilter create(String key, Operator operator, LocalDate date1, LocalDate date2){
		DataModelFilter filter = createCore(key);
		filter.operator = operator;
		filter.dateValues = new LocalDate[]{date1, date2};
		return filter;
	}
	
	private static DataModelFilter createCore(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		Attribute.AttributeType type = parseType(bits[1]);
		String categoryKey = bits[2];
		if (categoryKey.trim().isEmpty()) categoryKey = null;
		String attributeKey = bits[3];
		return new DataModelFilter(type,attributeKey,categoryKey);
	}
	
	private static Attribute.AttributeType parseType(String attributeType){
		for (Attribute.AttributeType t : Attribute.AttributeType.values()){
			if (t.typeKey.equalsIgnoreCase(attributeType)){
				return t;
			}
		}
		throw new IllegalStateException(attributeType + " is not a valid attribute type identifier"); //$NON-NLS-1$
	}
	
	private Attribute.AttributeType attributeType = null;
	private String attributeKey = null;
	private String categoryKey = null;
	
	private Operator operator = null;
	private Double numberValue = null;
	private String stringValue = null;
	private String keyValue = null;
	private List<String> keyValues = null;
	private LocalDate[] dateValues = null;
	
	public DataModelFilter(String categoryKey){
		this.categoryKey = categoryKey;
	}
	
	public DataModelFilter(Attribute.AttributeType type, String attributeKey, String categoryKey){
		this.categoryKey = categoryKey;
		this.attributeKey = attributeKey;
		this.attributeType = type;
	}
	
	public DataModelFilter(Attribute.AttributeType type, String attributeKey, String categoryKey, Operator operator, Double numberValue){
		this(type, attributeKey, categoryKey);
		this.operator = operator;
		this.numberValue = numberValue;
	}
	
	public DataModelFilter(Attribute.AttributeType type, String attributeKey, String categoryKey, Operator operator, String stringValue){
		this(type, attributeKey, categoryKey);
		this.operator = operator;
		this.stringValue = stringValue;
	}

	public DataModelFilter(Attribute.AttributeType type, String attributeKey, String categoryKey, String keyId){
		this(type, attributeKey, categoryKey);
		this.keyValue = keyId;
	}
	
	public DataModelFilter(Attribute.AttributeType type, String attributeKey, String categoryKey, List<String> keyIds){
		this(type, attributeKey, categoryKey);
		this.keyValues = keyIds;
	}
	
	public DataModelFilter(Attribute.AttributeType type, String attributeKey, String categoryKey, Operator operator, LocalDate[] dates){
		this(type, attributeKey, categoryKey);
		this.operator = operator;
		this.dateValues = dates;
	}
	
	public Attribute.AttributeType getAttributeType(){
		return this.attributeType;
	}
	
	public String getAttributeKey(){
		return this.attributeKey;
	}
	
	public String getCategoryKey(){
		return this.categoryKey;
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
	public Collection<String> getKeyValues(){
		return this.keyValues;
	}
	public LocalDate[] getDateValues(){
		return this.dateValues;
	}

	@Override
	public String getUniqueColumnIdentifier() {
		StringBuilder sb = new StringBuilder();
		sb.append("dm_"); //$NON-NLS-1$
		sb.append(categoryKey != null ? categoryKey : ""); //$NON-NLS-1$
		sb.append("_"); //$NON-NLS-1$
		if (attributeKey != null) {
			sb.append(attributeKey);
			sb.append("_"); //$NON-NLS-1$
			
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
				case MLIST:
					for (String x : keyValues) {
						sb.append(x);
					}
				case LIST:
				case TREE:
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
			}
		}
		return sb.toString();
	}
	
	public void convertToMultiList(List<String> keyValues) {
		this.operator = Operator.OR;
		this.keyValues = keyValues;
		this.keyValue = null;
		this.attributeType = Attribute.AttributeType.MLIST;
	}
	
	@Override
	public String asString() {
		//  | < ATTRIBUTE_KEY_MLIST: "dm_attribute:m:"(< DM_KEY >)?":"< DM_KEY >>

		StringBuilder sb = new StringBuilder();
		
		if (attributeKey == null) {
			sb.append("dm_category:");
			sb.append(categoryKey);
			return sb.toString();
		}
		
		sb.append("dm_attribute:"); //$NON-NLS-1$
		sb.append(attributeType.typeKey);
		sb.append(":"); //$NON-NLS-1$
		sb.append(categoryKey == null ? "" : categoryKey); //$NON-NLS-1$
		sb.append(":"); //$NON-NLS-1$
		sb.append(attributeKey);
		
		if (attributeType == Attribute.AttributeType.BOOLEAN) {
			// nothing
		} else if (attributeType == Attribute.AttributeType.NUMERIC) {
			sb.append(" "); //$NON-NLS-1$
			sb.append(operator.getKey());
			sb.append(" "); //$NON-NLS-1$
			sb.append(numberValue);
		} else if (attributeType == Attribute.AttributeType.TEXT) {
			sb.append(" "); //$NON-NLS-1$
			sb.append(operator.getKey());
			sb.append(" "); //$NON-NLS-1$
			sb.append("\"" + stringValue + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (attributeType == Attribute.AttributeType.LIST || 
				attributeType == Attribute.AttributeType.TREE) {
			sb.append(" "); //$NON-NLS-1$
			sb.append(Operator.EQUALS.getKey());
			sb.append(" "); //$NON-NLS-1$
			sb.append(keyValue);
		} else if (attributeType == Attribute.AttributeType.MLIST) {
			sb.append(" "); //$NON-NLS-1$
			sb.append(operator.getKey());
			sb.append(" "); //$NON-NLS-1$
			for (String x : keyValues) {
				sb.append(x);
				sb.append(MLIST_SEPERATOR);
			}
			sb.deleteCharAt(sb.length() - 1);

		} else if (attributeType == Attribute.AttributeType.DATE) {
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
