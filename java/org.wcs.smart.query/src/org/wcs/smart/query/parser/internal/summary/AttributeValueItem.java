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
package org.wcs.smart.query.parser.internal.summary;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.filter.FilterValidator;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.query.ui.formulaDnd.ErrorDropItem;

/**
 * Creates a new value item that represents
 * a numeric attribute or a 
 * numeric attribute and it's associated category.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AttributeValueItem implements IValueItem {

	/**
	 * Creates a new attribute value item of the form
	 * < ATTRIBUTE_VALUE_KEY : "attribute:n:" < AGG > ":" < ATTRIBUTE_KEY >
	 * 
	 * @param key
	 * @return
	 */
	public static AttributeValueItem createAttributeItem(String key){
		return new AttributeValueItem(key, false);
	}
	
	/**
	 * Creates a new attribute value item with cateogry filter of the
	 * form:
	 * |    < SUM_CAT_ATT_VALUE_KEY : "category:" < DM_KEY > ":attribute:n:" < AGG > ":" < DM_KEY > >
	 * 
	 * @param key
	 * @return
	 */
	public static AttributeValueItem createCategoryAttributeItem(String key){
		return new AttributeValueItem(key, true);
	}
	
	private String key;
	private String categoryKey = null;
	private String attributeKey = null;;
	private String aggregationKey = null;
	private Aggregation aggregation = null;
	
	private String itemKey = null;
	private IValueItem.ValueType valueType;
	private AttributeType attributeType;
	
	/**
	 * Creates a new value item from the given key.
	 * @param key key
	 * @param includeCategory if the key includes a category
	 */
	public AttributeValueItem(String key, boolean includeCategory){
		this.key = key;
		String[] bits = key.split(":"); //$NON-NLS-1$
		
		String attTypeKey = null;
		if (includeCategory){
			attTypeKey = bits[3];
		}else{
			attTypeKey = bits[1];
		}
		this.attributeType = Attribute.decodeAttributeTypeKey(attTypeKey);
		if(attributeType != Attribute.AttributeType.NUMERIC && 
		   attributeType != Attribute.AttributeType.LIST &&
		   attributeType != Attribute.AttributeType.TREE){ 
			throw new IllegalStateException(Messages.AttributeValueItem_InvalidKey);
		}
		if (attributeType == AttributeType.NUMERIC){
			//numeric are of the format
			//< SUM_ATTRIBUTE_VALUE_KEY : "attribute:n:" < AGG > ":" < DM_KEY > 
			//< SUM_CAT_ATT_VALUE_KEY : "category:" < DM_KEY > ":" < SUM_ATTRIBUTE_VALUE_KEY >
			if (includeCategory){
				this.categoryKey = bits[1];
				this.attributeKey = bits[5];
				this.aggregationKey = bits[4];
			}else{
				this.attributeKey = bits[3];
				this.aggregationKey = bits[2];
			}
		}else if (attributeType == AttributeType.LIST || 
				attributeType == AttributeType.TREE ){
				//< SUM_ATTRIBUTE_VALUE_LISTTREE_KEY : "attribute:" ("t" | "l") ":sum:" ("obs" | "wp") ":" < DM_KEY > >
				//< SUM_CAT_ATT_VALUE_LISTTREE_KEY : "category:" < DM_KEY > ":" < SUM_ATTRIBUTE_VALUE_LISTTREE_KEY >
				String valueTypeKey = ""; //$NON-NLS-1$
				if (includeCategory){
					this.categoryKey = bits[1];
					this.attributeKey = bits[6];
					this.aggregationKey = bits[4];
					valueTypeKey = bits[5];
					
				}else{
					this.attributeKey = bits[4];
					this.aggregationKey = bits[2];
					valueTypeKey = bits[3];
				}
				int index = attributeKey.indexOf('.');
				if (index <= 0){
					throw new IllegalStateException(Messages.AttributeValueItem_TreeListItemParseError);	
				}
				String temp = attributeKey;
				attributeKey = temp.substring(0, index);
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
		return this.key;
	}

	/**
	 * @return the attribute key that makes up the value item
	 */
	public String getAttributeKey(){
		return this.attributeKey;
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
	 * @return the category key that makes up the item or
	 * null if no category for this item
	 * 
	 */
	public String getCategoryKey(){
		return this.categoryKey;
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
		Attribute att = QueryDataModelManager.getInstance().getAttribute(session,attributeKey);
		if (att == null){
			return ""; //$NON-NLS-1$
		}
		String itemName = null;
		if (att.getType() == AttributeType.LIST){
			AttributeListItem it = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, itemKey);
			itemName = it.getName();
		}else if (att.getType() == AttributeType.TREE){
			AttributeTreeNode it = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, itemKey);
			itemName = it.getName();
		}
		StringBuilder name = new StringBuilder();
		if (valueType != null){
			name.append(valueType.guiLabel);
		}else if (getAggregation() != null){
			name.append(getAggregation().getGuiName());
		}
		name.append(" "); //$NON-NLS-1$
		if (itemName != null){
			name.append(itemName);
		}else{
			name.append(att.getName());
		}
		
		if (categoryKey != null){
			Category cat = QueryDataModelManager.getInstance().getCategory(session, categoryKey);
			if (cat != null){
				name.append( " (" + cat.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				name.append(" (not found) "); //$NON-NLS-1$
			}
		}
		return name.toString();
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getFullName(org.hibernate.Session)
	 */
	public String getFullName(Session session){


		Attribute att = QueryDataModelManager.getInstance().getAttribute(session,attributeKey);
		if (att == null){
			return ""; //$NON-NLS-1$
		}
		String itemName = null;
		if (att.getType() == AttributeType.LIST){
			AttributeListItem it = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, itemKey);
			itemName = it.getName();
		}else if (att.getType() == AttributeType.TREE){
			AttributeTreeNode it = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, itemKey);
			itemName = it.getName();
		}
		StringBuilder name = new StringBuilder();
		if (valueType != null){
			name.append(valueType.guiLabel);
		}else if (getAggregation() != null){
			name.append(getAggregation().getGuiName());
		}
		name.append(" "); //$NON-NLS-1$
		if (itemName != null){
			name.append(itemName);
			name.append(" [" + att.getName() + "] "); //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			name.append(att.getName());
		}
		
		if (categoryKey != null){
			Category cat = QueryDataModelManager.getInstance().getCategory(session, categoryKey);
			if (cat != null){
				name.append( " (" + cat.getFullCategoryName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				name.append(" (not found) "); //$NON-NLS-1$
			}
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
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception{
		try{
			Attribute att = QueryDataModelManager.getInstance().getAttribute(session,attributeKey);
			if (att == null){
				throw new Exception(MessageFormat.format(Messages.AttributeValueItem_AttributeNotFoundError, new Object[]{attributeKey}));
			}
			DropItem di = null;
			Category cat = null;
			if (categoryKey != null){
				cat = QueryDataModelManager.getInstance().getCategory(session, categoryKey);
				if (cat == null){
					throw new Exception(MessageFormat.format(Messages.AttributeValueItem_CategoryNotFoundError, new Object[]{categoryKey}));
				}
				cat.getFullCategoryName();			
			}
			if (attributeType == AttributeType.NUMERIC){
				if (cat == null){
					di = DropItemFactory.INSTANCE.createAttributeValueDropItem(att);
				}else{
					di = DropItemFactory.INSTANCE.createAttributeValueDropItem(new CategoryAttribute(cat, att));
				}
			}else if (attributeType == AttributeType.LIST){
				AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, itemKey);
				if (ali == null){
					throw new Exception(MessageFormat.format(Messages.AttributeValueItem_ListItemNotFound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = DropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali);
				}else{
					di = DropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali,cat);
				}
			
			}else if (attributeType == AttributeType.TREE){
				AttributeTreeNode atn = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, itemKey);
				if (atn == null){
					throw new Exception(MessageFormat.format(Messages.AttributeValueItem_TreeNodeNotFound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = DropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn);
				}else{
					di = DropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn,cat);
				}
			}
			if (di != null){
				di.initializeData(new Object[]{getDropItemInitializeData(), null});
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
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#hasCategory()
	 */
	public boolean hasCategory(){
		return categoryKey != null;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#hasAttribute()
	 */
	public boolean hasAttribute(){
		return true;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#validateDatabase(org.hibernate.Session)
	 */
	public void validateDatabase(Session session) throws Exception{
		if (categoryKey != null){
			//ensure category key exists
			FilterValidator.validateCategory(categoryKey, session);
		}else if (attributeKey != null){
			//ensure attribute key exists
			FilterValidator.validateAttribute(attributeKey, session);
		}else if(aggregationKey != null){
			boolean found = false;
			for (Aggregation agg : DataModel.getAggregations()){
				if (agg.getName().equalsIgnoreCase(aggregationKey)){
					found = true;
					break;
				}
			}
			if (!found){
				throw new Exception(MessageFormat.format(Messages.AttributeValueItem_AggNoSupported, new Object[]{ aggregationKey }));
			}
		}
	}
}
