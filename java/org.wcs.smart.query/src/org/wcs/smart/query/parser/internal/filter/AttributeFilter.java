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
package org.wcs.smart.query.parser.internal.filter;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.filter.Operator;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.util.SmartUtils;

/**
 * Query filter for data model attributes.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeFilter implements IFilter {
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
		
		String[] bits = this.fullIdentifier.split(":"); //$NON-NLS-1$
		if (bits[1].equals("b")){ //$NON-NLS-1$
			this.attributeType = AttributeType.BOOLEAN;
		}else if (bits[1].equals("n")){  //$NON-NLS-1$
			this.attributeType = AttributeType.NUMERIC;
		}else if (bits[1].equals("t")){ //$NON-NLS-1$
			this.attributeType = AttributeType.TREE;
		}else if (bits[1].equals("s")){ //$NON-NLS-1$
			this.attributeType = AttributeType.TEXT;
		}else if (bits[1].equals("l")){ //$NON-NLS-1$
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
	
	public Operator getOperator(){
		return this.op;
	}
	/**
	 * @return the unique attribute key
	 */
	public String getAttributeKey(){
		return this.attributeKey;
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
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		if (attributeType == AttributeType.BOOLEAN){
			return fullIdentifier;
		}else if (attributeType == AttributeType.NUMERIC){
			return fullIdentifier + " " + op.asSmartValue() + " " + ((Double)value1).toString();  //$NON-NLS-1$  //$NON-NLS-2$
		}else if (attributeType == AttributeType.TEXT){
			return fullIdentifier + " " + op.asSmartValue() + " \"" + ((String)value1) + "\"";  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}else if (attributeType == AttributeType.TREE || attributeType == AttributeType.LIST){
			return fullIdentifier + " " + op.asSmartValue() + " " + ((String)value1);  //$NON-NLS-1$  //$NON-NLS-2$  
		}
		return ""; //$NON-NLS-1$
	}
	
	public String asSql(HashMap<Class<?>, String> tableMapping, HashMap<IFilter, String> colMapping){
		String col = colMapping.get(this);
		if (col == null){
			return asSql(tableMapping);
		}else{
			return " waypointTable." + col + " ";
		}
	}
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		
		String attprefix = tableMapping.get(Attribute.class);
		if (attprefix == null){
			throw new IllegalStateException(Messages.AttributeFilter_InvalidAttributePrefix);
		}
		String attObprefix = tableMapping.get(WaypointObservationAttribute.class);
		if (attObprefix == null){
			throw new IllegalStateException(Messages.AttributeFilter_InvalidWaypointObservationPrefix);
		}

		if (attributeType == AttributeType.BOOLEAN){
			return " (qa." + attributeKey + " > 0.5 ) ";			//$NON-NLS-1$ //$NON-NLS-2$
		}else if (attributeType == AttributeType.NUMERIC){
			return " (qa." + attributeKey + " " + op.asSql() + " " + String.valueOf((Double)value1) + ") "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}else if (attributeType == AttributeType.TEXT){
			String queryStr = ""; //$NON-NLS-1$
			//TODO: look into escape % & _ as these are wild card characters
			// SELECT a FROM tabA WHERE a LIKE '%=_' ESCAPE '='  (must specify escape character)
			String val = (String)value1;
			val = val.replaceAll("'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (op == Operator.STR_CONTAINS || op == Operator.STR_NOTCONTAINS){
				queryStr = "( LOWER(qa." + attributeKey + ") " + op.asSql() + " '%" + val.toLowerCase() + "%' )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$	
			}else if (op == Operator.STR_EQUALS){
				queryStr = "( LOWER(qa." + attributeKey + ") " + op.asSql() + " '" + val.toLowerCase() + "' )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			return queryStr;
		}else if (attributeType == AttributeType.LIST ){
			return "( qa."+ attributeKey  + " " + op.asSql() + " '" + (String)value1 + "' )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
		}else if (attributeType == AttributeType.TREE){
//			return "( " + prefix + ".hkey >= '" + keyPart + "' and " + prefix + ".hkey < '" + keyPart.substring(0,  keyPart.length() -1) + "/') ";
			return "( qa." + attributeKey + " >= '" + (String)value1 + "' and qa." + attributeKey + "<'" + ((String)value1).substring(0,  ((String)value1).length() -1) + "/')";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return true;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
		attributes.add(new AttributeInfo(attributeKey, attributeType));
	}

	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getDropItems(org.hibernate.Session)
	 */
	public DropItem[] getDropItems(Session session) throws Exception{
		Attribute att = getAttribute(session);
		DropItem it = DropItemFactory.INSTANCE.createAttributeDropItem(att);
		initDropItem(it, session);
		return new DropItem[]{it};
	}
	
	/**
	 * Initializes the drop item with the 
	 * values from this filter
	 * 
	 * 
	 * @param it the drop item to initialize
	 * @param session database session
	 */
	public void initDropItem(DropItem it,  Session session){
		if (attributeType == AttributeType.TEXT || attributeType == AttributeType.NUMERIC){
			it.initializeData(new String[]{op.getGuiValue(), String.valueOf(value1)});
			
		}else if (attributeType == AttributeType.LIST){
			AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, (String)value1);
			if (ali == null){
				throw new IllegalStateException(MessageFormat.format(Messages.AttributeFilter_ListItemNotFound, new Object[]{(String)value1, attributeKey}));
			}
			ListItem li = new ListItem(ali.getUuid(), ali.getName(), ali.getKeyId());
			it.initializeData(li);
		}else if (attributeType == AttributeType.TREE){
			AttributeTreeNode ali = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, (String)value1);
			if (ali == null){
				throw new IllegalStateException(MessageFormat.format(Messages.AttributeFilter_TreeNodeNotFound, new Object[]{(String)value1, attributeKey}));
			}
			it.initializeData(ali);
		}
		
	}
	/**
	 * Loads the attribute from the database
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public Attribute getAttribute(Session session) throws Exception{
		Attribute att = QueryDataModelManager.getInstance().getAttribute(session, attributeKey);
		if (att == null){
			throw new Exception(MessageFormat.format(Messages.AttributeFilter_AttributeNotFound, new Object[]{attributeKey}));
		}
		return att;
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}
	
}

