package org.wcs.smart.i2.query.observation.filter;

import java.util.Date;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.query.Operator;

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
		filter.stringValue = value;
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
		String entityTypeKey = "";
		if (bits.length > 3){
			entityTypeKey = bits[3];
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
