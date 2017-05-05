package org.wcs.smart.entity.query.parser.internal;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.util.SharedUtils;

public class EntityAttributeFilter implements IFilter {
	
	
	/**
	 * Creates a new boolean attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:b:<key>"
	 * @return
	 */
	public static EntityAttributeFilter createBooleanFilter(String attributeIdentifier){
		return new EntityAttributeFilter(attributeIdentifier);
	}
	
	/**
	 * Creates a new value attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:n:<key>"
	 * @param op the operator
	 * @param Double value the filter value
	 * @return
	 */
	public static EntityAttributeFilter createValueFilter(String attributeIdentifier, Operator op, Double value){
		return new EntityAttributeFilter(attributeIdentifier, op, value);
	}
	/**
	 * Creates a new text attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:s:<key>"
	 * @param op the string operator
	 * @param value the filter value
	 * @return
	 */
	public static EntityAttributeFilter createStringFilter(String attributeIdentifier, Operator op, String value){
		value = SharedUtils.stripQuotes(value);
		return new EntityAttributeFilter(attributeIdentifier,  op, value);
	}

	/**
	 * Creates a new date attribute filter
	 * 
	 * Date filters are of the form: <DATE> BETWEEN <DATE1> AND <DATE2>
	 * 
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:s:<key>"
	 * @param date1 the first date
	 * @param date2 the second date
	 * @return
	 */
	public static EntityAttributeFilter createDateFilter(String attributeIdentifier, String date1, String date2, Operator op){
		return new EntityAttributeFilter(attributeIdentifier, op, date1, date2);
	}
	
	/**
	 * Creates a new list item attribute filter 
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:l:<key>"
	 * @param op the list operator
	 * @param attributeItemKey the list item key
	 * @return
	 */
	public static EntityAttributeFilter createListItemFilter(String attributeIdentifier, Operator op, String attributeItemKey){
		return new EntityAttributeFilter(attributeIdentifier,  op, attributeItemKey);
	}
	
	/**
	 * Creates a new list item attribute filter 
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:t:<hkey>"
	 * @param op the list operator
	 * @param attributeItemKey the tree item hkey
	 * @return
	 */
	public static EntityAttributeFilter createTreeItemFilter(String attributeIdentifier, Operator op, String attributeItemKey){
		return new EntityAttributeFilter(attributeIdentifier,  op, attributeItemKey);
	}
	
	private String entityKey;
	private String entityAttributeKey;
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
	private EntityAttributeFilter(String attributeIdentifier){
		String[] bits = attributeIdentifier.split(":"); //$NON-NLS-1$
		
		this.entityKey = bits[1];
		this.attributeType = Attribute.decodeAttributeTypeKey(bits[3]);
		entityAttributeKey = bits[4];
	}
	
	/**
	 * Creates a new attribute filter 
	 * @param attributeIdentifier the attribute key of the form attribute:type:attributeKey
	 * @param op the filter operator
	 * @param value the filter value
	 */
	private EntityAttributeFilter(String attributeIdentifier, Operator op, Object value){
		this(attributeIdentifier);
		this.op = op;
		this.value1 = value;
	}
	
	/* for between operators */
	private EntityAttributeFilter(String attributeIdentifier, Operator op, Object value, Object value2){
		this(attributeIdentifier, op,  value);
		this.value2 = value2;
	}
	
	public Operator getOperator(){
		return this.op;
	}
	
	
	/**
	 * @return the unique entity attribute key
	 */
	public String getEntityAttributeKey(){
		return this.entityAttributeKey;
	}
	
	/**
	 * 
	 * @return the unique entity key
	 */
	public String getEntityKey(){
		return this.entityKey;
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
		String key = "entity:" + entityKey + ":attribute:" + attributeType.typeKey + ":" + entityAttributeKey; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		if (attributeType == AttributeType.BOOLEAN){
			return key ;
		}else if (attributeType == AttributeType.NUMERIC){
			return key + " " + op.asSmartValue() + " " + ((Double)value1).toString();  //$NON-NLS-1$  //$NON-NLS-2$
		}else if (attributeType == AttributeType.TEXT){
			return key + " " + op.asSmartValue() + " \"" + ((String)value1) + "\"";  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}else if (attributeType == AttributeType.TREE || attributeType == AttributeType.LIST){
			return key + " " + op.asSmartValue() + " " + ((String)value1);  //$NON-NLS-1$  //$NON-NLS-2$  
		}else if (attributeType == AttributeType.DATE){
			return key + " " + op.asSmartValue() + " " + (String)value1 + " " + Operator.AND.asSmartValue() + " " + ((String)value2); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$ 
		}
		return ""; //$NON-NLS-1$
	}	
	
	
	
	/**
	 * 
	 */
	@Override
	public void accept(IFilterVisitor visitor){
		visitor.visit(this);
	}
}
