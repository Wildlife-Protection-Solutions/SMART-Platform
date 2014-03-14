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
package org.wcs.smart.entity.query.parser.internal;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.query.EntityQueryPlugIn;
import org.wcs.smart.entity.query.ui.definition.EntityDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Creates a new value item that represents
 * a numeric attribute or a 
 * numeric attribute and it's associated category.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class EntityAttributeValueItem implements IValueItem {
	
	/**
	 * Creates a new entity attribute value item of the form
	 * 
	 * @param key
	 * @return
	 */
	public static EntityAttributeValueItem createEntityAttributeItem(String key){
		return new EntityAttributeValueItem(key);
	}
	
	protected String entityAttributeKey = null;;
	protected String entityKey = null;
	protected String aggregationKey = null;
	private Aggregation aggregation = null;
	
	protected String itemKey = null;
	protected IValueItem.ValueType valueType;
	protected AttributeType attributeType;
	
	private EntityAttribute entityAttribute = null;
	
	
	/**
	 * Creates a new value item from the given key.
	 * @param key key
	 * @param includeCategory if the key includes a category
	 */
	//< SUM_ATTRIBUTE_VALUE_KEY : "entity:<key>:attribute:n:" < AGG > ":" < DM_KEY >
	//< SUM_ATTRIBUTE_VALUE_LISTTREE_KEY : "entity:<key>:attribute:" ("t" | "l") ":sum:" ("obs" | "wp") ":" < DM_KEY > >
	public EntityAttributeValueItem(String key){
		String[] bits = key.split(":"); //$NON-NLS-1$

		String attTypeKey = bits[3];
		this.attributeType = Attribute.decodeAttributeTypeKey(attTypeKey);
		
		if(attributeType != Attribute.AttributeType.NUMERIC && 
		   attributeType != Attribute.AttributeType.LIST &&
		   attributeType != Attribute.AttributeType.TREE){ 
			throw new IllegalStateException("Cannot create attribute value items from non-numeric attributes");
		}
		this.entityKey = bits[1];
		if (attributeType == AttributeType.NUMERIC){
			//numeric are of the format
			this.entityAttributeKey = bits[5];
			this.aggregationKey = bits[4];
		}else if (attributeType == AttributeType.LIST || 
				attributeType == AttributeType.TREE ){
				//< SUM_ATTRIBUTE_VALUE_LISTTREE_KEY : "attribute:" ("t" | "l") ":sum:" ("obs" | "wp") ":" < DM_KEY > >
				//< SUM_CAT_ATT_VALUE_LISTTREE_KEY : "category:" < DM_KEY > ":" < SUM_ATTRIBUTE_VALUE_LISTTREE_KEY >
				String valueTypeKey = ""; //$NON-NLS-1$
				this.entityAttributeKey = bits[6];
				this.aggregationKey = bits[4];
				valueTypeKey = bits[5];

				int index = entityAttributeKey.indexOf('.');
				if (index <= 0){
					throw new IllegalStateException("Could not parse attribute list or tree information from attribute value item.");	
				}
				String temp = entityAttributeKey;
				entityAttributeKey = temp.substring(0, index);
				itemKey = temp.substring(index + 1);
				
				this.valueType = ValueType.OBSERVATION;
				for (ValueType vt : ValueType.values()){
					if (vt.key.equals(valueTypeKey)){
						this.valueType = vt;
						break;
					}
				}
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asString()
	 */
	public String asString(){
		StringBuilder sb = new StringBuilder();
		sb.append("entity:");
		sb.append(entityKey);
		sb.append(":attribute:");
		sb.append(attributeType.typeKey);
		if (attributeType == attributeType.NUMERIC){
			sb.append(":");
			sb.append(aggregationKey);
			sb.append(":");
			sb.append(entityAttributeKey);
		}else{
			sb.append(":");
			sb.append(aggregationKey);
			sb.append(":");
			sb.append(valueType.key);
			sb.append(":");
			sb.append(entityAttributeKey);
			sb.append(".");
			sb.append(itemKey);
		}
		
		return sb.toString();
	}

	/**
	 * @return the attribute key that makes up the value item
	 */
	public String getEntityAttributeKey(){
		return this.entityAttributeKey;
	}
	
	public ValueType getValueType(){
		return this.valueType;
	}
	
	/**
	 * 
	 * @return the attribute type
	 */
	public AttributeType getAttributeType(){
		return this.attributeType;
	}
	/**
	 * @return the entity type key
	 * 
	 */
	public String getEntityKey(){
		return this.entityKey;
	}
	
	/**
	 * 
	 * @return attribute item key for list and tree
	 * attributes.
	 */
	public String getItemKey(){
		return this.itemKey;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getName(org.hibernate.Session)
	 */
	public String getName(Session session){
		//get children categories
		EntityAttribute ea = null;
		try {
			ea = getEntityAttribute(session);
		} catch (Exception ex) {
			EntityQueryPlugIn.displayLog(MessageFormat.format(
					"Entity attribute {0} could not be found for key {1}.",
					new Object[] { entityKey, entityAttributeKey }), ex);
			return ""; //$NON-NLS-1$
		}
				
		Attribute att = QueryDataModelManager.getInstance().getAttribute(session,ea.getDmAttribute());
		if (att == null){
			return ""; //$NON-NLS-1$
		}
		String itemName = null;
		if (att.getType() == AttributeType.LIST){
			AttributeListItem it = QueryDataModelManager.getInstance().getAttributeListItem(session, ea.getDmAttribute().getKeyId(), itemKey);
			itemName = it.getName();
		}else if (att.getType() == AttributeType.TREE){
			AttributeTreeNode it = QueryDataModelManager.getInstance().getAttributeTreeNode(session, ea.getDmAttribute().getKeyId(), itemKey);
			itemName = it.getName();
		}
		StringBuilder name = new StringBuilder();
		if (valueType != null){
			name.append(valueType.guiLabel);
		}else if (getAggregation() != null){
			name.append(getAggregation().getGuiName());
		}
		name.append(" "); //$NON-NLS-1$
		name.append(ea.getEntityType().getName());
		name.append(" - "); //$NON-NLS-1$
		if (itemName != null){
			name.append(itemName);
		}else{
			name.append(att.getName());
		}
		return name.toString();
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getFullName(org.hibernate.Session)
	 */
	public String getFullName(Session session){
		EntityAttribute ea = null;
		try {
			ea = getEntityAttribute(session);
		} catch (Exception ex) {
			EntityQueryPlugIn.displayLog(MessageFormat.format(
					"Entity attribute {0} could not be found for key {1}.",
					new Object[] { entityKey, entityAttributeKey }), ex);
			return ""; //$NON-NLS-1$
		}

		Attribute att = QueryDataModelManager.getInstance().getAttribute(session,ea.getDmAttribute());
		if (att == null){
			return ""; //$NON-NLS-1$
		}
		String itemName = null;
		if (att.getType() == AttributeType.LIST){
			AttributeListItem it = QueryDataModelManager.getInstance().getAttributeListItem(session, ea.getDmAttribute().getKeyId(), itemKey);
			itemName = it.getName();
		}else if (att.getType() == AttributeType.TREE){
			AttributeTreeNode it = QueryDataModelManager.getInstance().getAttributeTreeNode(session, ea.getDmAttribute().getKeyId(), itemKey);
			itemName = it.getName();
		}
		StringBuilder name = new StringBuilder();
		if (valueType != null){
			name.append(valueType.guiLabel);
		}else if (getAggregation() != null){
			name.append(getAggregation().getGuiName());
		}
		name.append(" "); //$NON-NLS-1$
		name.append(ea.getEntityType().getName());
		name.append(" - "); //$NON-NLS-1$
		if (itemName != null){
			name.append(itemName);
			name.append(" [" + att.getName() + "] "); //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			name.append(att.getName());
		}
		return name.toString();
		
	}
	
	/**
	 * @return attribute aggregation 
	 */
	public Aggregation getAggregation() {
		if (aggregation == null) {
			List<Aggregation> aggs = DataModel.getAggregations();
			for (Aggregation agg : aggs) {
				if (agg.getName().equals(aggregationKey)) {
					aggregation = agg;
					break;
				}
			}
		}
		return this.aggregation;
	}
	
	/**
	 * The key associated with the aggregation
	 * @return
	 */
	public String getAggregationKey(){
		return this.aggregationKey;
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	public DropItem asDropItem(Session session){
		try{
			EntityAttribute ea = getEntityAttribute(session);
			
			Attribute att = QueryDataModelManager.getInstance().getAttribute(session,ea.getDmAttribute());
			if (att == null){
				throw new Exception(MessageFormat.format("Attribute {0} not found.", new Object[]{ea.getDmAttribute().getKeyId()}));
			}
			DropItem di = null;
			if (attributeType == AttributeType.NUMERIC){
				di = EntityDropItemFactory.INSTANCE.createAttributeValueDropItem(att);

			}else if (attributeType == AttributeType.LIST){
				AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, ea.getDmAttribute().getKeyId(), itemKey);
				if (ali == null){
					throw new Exception(MessageFormat.format("The attribute {0} does not contain a list item with the key {1}.", new Object[]{ea.getDmAttribute().getKeyId(), itemKey}));		
				}
				di = EntityDropItemFactory.INSTANCE.createEntityAttributeListItemValueDropItem(ea, ali);
			}else if (attributeType == AttributeType.TREE){
				AttributeTreeNode atn = QueryDataModelManager.getInstance().getAttributeTreeNode(session, ea.getDmAttribute().getKeyId(), itemKey);
				if (atn == null){
					throw new Exception(MessageFormat.format("The attribute {0} does not contain a tree node with the key {1}.", new Object[]{ea.getDmAttribute().getKeyId(), itemKey}));		
				}
				di = EntityDropItemFactory.INSTANCE.createEntityAttributeTreeNodeValueDropItem(ea, atn);
			}
			if (di != null){
				di.initializeData(getDropItemInitializeData());
			}
			return di;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getInitializeData()
	 */
	public Object getDropItemInitializeData(){
		if (attributeType == AttributeType.NUMERIC){
			return getAggregation();
		}else{
			return getValueType().key;
		}
	}
	
	public void accept(IValueVisitor visitor){
		visitor.visit(this);
	}

	/**
	 * Loads the attribute from the database
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public EntityAttribute getEntityAttribute(Session session) throws Exception{
		if (entityAttribute != null){
			return entityAttribute;
		}
		Query q = session.createQuery("From EntityAttribute where entityType.conservationArea.uuid = :ca and entityType.keyId = :entitykey and keyId = :key"); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid()); //$NON-NLS-1$
		q.setParameter("key", entityAttributeKey); //$NON-NLS-1$
		q.setParameter("entitykey", entityKey); //$NON-NLS-1$
		q.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<EntityAttribute> results = q.list();
		if (results.size() != 1 ){
			throw new Exception(MessageFormat.format("The entity attribute {0} is not a valid attribute for the entity type {1}.", new Object[]{entityAttributeKey, entityKey}));
		}else{
			entityAttribute = results.get(0);
			return entityAttribute;
		}
	}
}
