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

import org.wcs.smart.SmartUtils;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class AttributeFilter implements Filter {

	
	private String attributeKey;
	private AttributeType attributeType;
	private Operator op;
	private Object value1;
	private Object value2;
	
	public AttributeFilter(String attributeKey, AttributeType type){
		this.attributeKey = attributeKey;
		this.attributeType = type;
	}
	
	public AttributeFilter(String attributeKey, AttributeType type, Operator op, Object value){
		this(attributeKey, type);
		this.op = op;
		this.value1 = value;
	}
	
	/* for between operators */
	public AttributeFilter(String attributeKey, AttributeType type, Operator op, Object value, Object value2){
		this(attributeKey, type);
		this.op = op;
		this.value1 = value;
		this.value2 = value2;
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#asString()
	 */
	@Override
	public String asString() {
		if (attributeType == AttributeType.BOOLEAN){
			return attributeKey;
		}else if (attributeType == AttributeType.NUMERIC){
			return attributeKey + " " + op.asString() + " " + ((Double)value1).toString();
		}else if (attributeType == AttributeType.TEXT){
			return attributeKey + " " + op.asString() + " \"" + ((String)value1) + "\"";
		}else if (attributeType == AttributeType.TREE || attributeType == AttributeType.LIST){
			return attributeKey + " " + op.asString() + " " + ((String)value1);
		}
		return "";
	}
	
	public static AttributeFilter createBooleanFilter(String attributeKey){
		return new AttributeFilter(attributeKey, AttributeType.BOOLEAN);
	}
	
	public static AttributeFilter createValueFilter(String key, Operator op, Double value){
		return new AttributeFilter(key, AttributeType.NUMERIC, op, value);
	}
	
	public static AttributeFilter createStringFilter(String attributeKey, Operator op, String value){
		value = SmartUtils.stripQuotes(value);
		return new AttributeFilter(attributeKey, AttributeType.TEXT, op, value);
	}

	public static AttributeFilter createListItemFilter(String attributeKey, Operator op, String attributeItemKey){
		return new AttributeFilter(attributeKey, AttributeType.LIST, op, attributeItemKey);
	}
	public static AttributeFilter createTreeItemFilter(String attributeKey, Operator op, String attributeItemKey){
		return new AttributeFilter(attributeKey, AttributeType.TREE, op, attributeItemKey);
	}
	
	
	@Override
	public String asHql(HashMap<Class<?>, String> tableMapping, HashMap<String, Object> parameters) {
		String keyPart = attributeKey.split(":")[2];
		
		String attprefix = tableMapping.get(Attribute.class);
		if (attprefix == null){
			throw new IllegalStateException("Attribute prefix could not be determined.");
		}
		String attObprefix = tableMapping.get(WaypointObservationAttribute.class);
		if (attObprefix == null){
			throw new IllegalStateException("Waypoint Observation Attribute prefix could not be determined.");
		}

		if (attributeType == AttributeType.BOOLEAN){
			String param1 = keyPart;
			String key1 = "p" + String.valueOf(parameters.size());
			parameters.put(key1, param1);
			return "( " + attprefix + ".keyId = :" + key1 + " and " + attObprefix + ".numberValue > 0.5 )";			
		}else if (attributeType == AttributeType.NUMERIC){
			String param1 = keyPart;
			Double param2 = (Double)value1;
			String key1 = "p" + String.valueOf(parameters.size());
			String key2 = "p" + String.valueOf(parameters.size()+1);
			parameters.put(key1, param1);
			parameters.put(key2, param2);
			
			return "( " + attprefix + ".keyId = :" + key1 + " and " + attObprefix + ".numberValue " + op.asHql() + " :" + key2 + " )";
		}else if (attributeType == AttributeType.TEXT){
			String param1 = keyPart;
			String param2 = (String)value1;
			String key1 = "p" + String.valueOf(parameters.size());
			String key2 = "p" + String.valueOf(parameters.size()+1);
			String queryStr = "";
			if (op == Operator.STR_CONTAINS || op == Operator.STR_NOTCONTAINS){
				param2 = "%" + param2 + "%";	
				queryStr = "( " + attprefix + ".keyId = :" + key1 + " and " + attObprefix + ".stringValue " + op.asHql() + " :" + key2 + " )";	
			}else if (op == Operator.STR_EQUALS){
				queryStr = "( " + attprefix + ".keyId = :" + key1 + " and " + attObprefix + ".stringValue " + op.asHql() + " :" + key2 + " )";
			}
			parameters.put(key1, param1);
			parameters.put(key2, param2);
			return queryStr;
		}else if (attributeType == AttributeType.LIST ){
			String param1 = keyPart;
			String param2 = (String)value1;
			String key1 = "p" + String.valueOf(parameters.size());
			String key2 = "p" + String.valueOf(parameters.size()+1);
			parameters.put(key1, param1);
			parameters.put(key2, param2);
			
			String listPrefix = tableMapping.get(AttributeListItem.class);
			return "( " + attprefix + ".keyId = :" + key1 + " and " 
					+ listPrefix + ".keyId " + op.asHql() + " :" + key2 + " )";			
		}else if (attributeType == AttributeType.TREE){
			//TODO: - need to add hkey to dm_attribute_tree_table
			String param1 = keyPart;
			String param2 = (String)value1;
			String param3 = (String)value1 + ".%";
			String key1 = "p" + String.valueOf(parameters.size());
			String key2 = "p" + String.valueOf(parameters.size()+1);
			String key3 = "p" + String.valueOf(parameters.size()+2);
			parameters.put(key1, param1);
			parameters.put(key2, param2);
			parameters.put(key3, param3);
			
			String treePrefix = tableMapping.get(AttributeTreeNode.class);
			return "( " + attprefix + ".keyId = :" + key1 + " and " 
					+ "(" + treePrefix + ".keyid like  :" + key2 + " OR " + treePrefix + ".keyid like :" + key3 + ") )";
			//change to keyid to hkey; and add like or equals
		}
		return "";
	}
	
	
	
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		String keyPart = attributeKey.split(":")[2];
		
		String attprefix = tableMapping.get(Attribute.class);
		if (attprefix == null){
			throw new IllegalStateException("Attribute prefix could not be determined.");
		}
		String attObprefix = tableMapping.get(WaypointObservationAttribute.class);
		if (attObprefix == null){
			throw new IllegalStateException("Waypoint Observation Attribute prefix could not be determined.");
		}

		if (attributeType == AttributeType.BOOLEAN){
			return "( " + attprefix + ".keyId = '" + keyPart + "' and " + attObprefix + ".number_value > 0.5 )";			
		}else if (attributeType == AttributeType.NUMERIC){
			return "( " + attprefix + ".keyId = '" + keyPart + "' and " + attObprefix + ".number_value " + op.asSql() + " " + String.valueOf((Double)value1) + " )";
		}else if (attributeType == AttributeType.TEXT){
			String queryStr = "";
			if (op == Operator.STR_CONTAINS || op == Operator.STR_NOTCONTAINS){
				queryStr = "( " + attprefix + ".keyId = '" + keyPart + "' and " + attObprefix + ".string_value " + op.asSql() + " '" + (String)value1 + "' )";	
			}else if (op == Operator.STR_EQUALS){
				queryStr = "( " + attprefix + ".keyId = '" + keyPart + "' and " + attObprefix + ".string_value " + op.asSql() + " '" + (String)value1 + "' )";
			}
			return queryStr;
		}else if (attributeType == AttributeType.LIST ){
			String listPrefix = tableMapping.get(AttributeListItem.class);
			return "( " + attprefix + ".keyId = '" + keyPart + "' and " 
					+ listPrefix + ".keyId " + op.asSql() + " '" + (String)value1 + "' )";			
		}else if (attributeType == AttributeType.TREE){
			String treePrefix = tableMapping.get(AttributeTreeNode.class);
			return "( " + attprefix + ".keyId = '" + keyPart + "' and " 
					+  treePrefix + ".keyid like  '" + (String)value1 + "') ";
			//change to keyid to hkey; and add like or equals
		}
		return "";
		
		 
		
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeTreeItemFilter()
	 */
	@Override
	public boolean hasAttributeTreeItemFilter() {
		return (attributeType == AttributeType.TREE);
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeListItemFilter()
	 */
	@Override
	public boolean hasAttributeListItemFilter() {
		return (attributeType == AttributeType.LIST);
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return true;
	}
	
	
}

