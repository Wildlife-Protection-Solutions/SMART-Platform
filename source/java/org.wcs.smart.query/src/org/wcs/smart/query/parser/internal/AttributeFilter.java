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
package org.wcs.smart.query.parser.internal;

import java.util.HashMap;
import java.util.HashSet;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.util.SmartUtils;

/**
 * Query filter for data model attributes.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeFilter implements Filter {
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
		
		String[] bits = this.fullIdentifier.split(":");
		if (bits[1].equals("b")){
			this.attributeType = AttributeType.BOOLEAN;
		}else if (bits[1].equals("n")){ 
			this.attributeType = AttributeType.NUMERIC;
		}else if (bits[1].equals("t")){
			this.attributeType = AttributeType.TREE;
		}else if (bits[1].equals("s")){
			this.attributeType = AttributeType.TEXT;
		}else if (bits[1].equals("l")){
			this.attributeType = AttributeType.LIST;
		}
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
	 * @see org.wcs.smart.query.parser.internal.Filter#asString()
	 */
	@Override
	public String asString() {
		if (attributeType == AttributeType.BOOLEAN){
			return fullIdentifier;
		}else if (attributeType == AttributeType.NUMERIC){
			return fullIdentifier + " " + op.asString() + " " + ((Double)value1).toString();
		}else if (attributeType == AttributeType.TEXT){
			return fullIdentifier + " " + op.asString() + " \"" + ((String)value1) + "\"";
		}else if (attributeType == AttributeType.TREE || attributeType == AttributeType.LIST){
			return fullIdentifier + " " + op.asString() + " " + ((String)value1);
		}
		return "";
	}
	
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		
		String attprefix = tableMapping.get(Attribute.class);
		if (attprefix == null){
			throw new IllegalStateException("Attribute prefix could not be determined.");
		}
		String attObprefix = tableMapping.get(WaypointObservationAttribute.class);
		if (attObprefix == null){
			throw new IllegalStateException("Waypoint Observation Attribute prefix could not be determined.");
		}

		if (attributeType == AttributeType.BOOLEAN){
			return " (qa." + attributeKey + " > 0.5 ) ";			
		}else if (attributeType == AttributeType.NUMERIC){
			return " (qa." + attributeKey + " " + op.asSql() + " " + String.valueOf((Double)value1) + ") ";
		}else if (attributeType == AttributeType.TEXT){
			String queryStr = "";
			//TODO: look into escape % & _ as these are wild card characters
			// SELECT a FROM tabA WHERE a LIKE '%=_' ESCAPE '='  (must specify escape character)
			String val = (String)value1;
			val = val.replaceAll("'", "''");
			
			if (op == Operator.STR_CONTAINS || op == Operator.STR_NOTCONTAINS){
				queryStr = "( qa." + attributeKey + " " + op.asSql() + " '%" + val + "%' )";	
			}else if (op == Operator.STR_EQUALS){
				queryStr = "( qa." + attributeKey + " " + op.asSql() + " '" + val + "' )";
			}
			return queryStr;
		}else if (attributeType == AttributeType.LIST ){
			return "( qa."+ attributeKey  + " " + op.asSql() + " '" + (String)value1 + "' )";
		}else if (attributeType == AttributeType.TREE){
			return "( qa." + attributeKey + " " + op.asSql() + " '" + (String)value1 + "' )";
		}
		return "";
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return true;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
		attributes.add(new AttributeInfo(attributeKey, attributeType));
	}
}

