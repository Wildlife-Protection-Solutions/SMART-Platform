package org.wcs.smart.i2.query.observation.filter;

import java.util.Date;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.i2.query.Operator;

public class DataModelFilter implements IQueryFilter {

	//category
	public static DataModelFilter createCategory(String key){
		String[] bits = key.split(":");
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
	public static DataModelFilter create(String key, Operator operator, String value){
		DataModelFilter filter = createCore(key);
		filter.operator = operator;
		filter.stringValue = value;
		return filter;
	}
	
	//list and tree
	public static DataModelFilter create(String key, String keyId){
		DataModelFilter filter = createCore(key);
		filter.keyValue = keyId;
		return filter;
	}
	
	//dates
	public static DataModelFilter create(String key, Operator operator, Date date1, Date date2){
		DataModelFilter filter = createCore(key);
		filter.operator = operator;
		filter.dateValues = new Date[]{date1, date2};
		return filter;
	}
	
	private static DataModelFilter createCore(String key){
		String bits[] = key.split(":");
		Attribute.AttributeType type = parseType(bits[1]);
		String categoryKey = bits[2];
		String attributeKey = bits[3];
		return new DataModelFilter(type,attributeKey,categoryKey);
	}
	
	private static Attribute.AttributeType parseType(String attributeType){
		for (Attribute.AttributeType t : Attribute.AttributeType.values()){
			if (t.typeKey.equalsIgnoreCase(attributeType)){
				return t;
			}
		}
		throw new IllegalStateException(attributeType + " is not a valid attribute type identifier");
	}
	
	private Attribute.AttributeType attributeType = null;
	private String attributeKey = null;
	private String categoryKey = null;
	
	private Operator operator = null;
	private Double numberValue = null;
	private String stringValue = null;
	private String keyValue = null;
	private Date[] dateValues = null;
	
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
	
	public DataModelFilter(Attribute.AttributeType type, String attributeKey, String categoryKey, Operator operator, Date[] dates){
		this(type, attributeKey, categoryKey);
		this.operator = operator;
		this.dateValues = dates;
	}
	
	public Attribute.AttributeType getAttributeType(){
		return this.attributeType;
	}
	
	public String getAttributeKay(){
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
	public Date[] getDateValues(){
		return this.dateValues;
	}
}
