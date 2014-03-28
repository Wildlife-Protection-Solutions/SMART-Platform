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
package org.wcs.smart.patrol.query.parser.internal.summary;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.summary.AttributeValueItem;
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
public class PatrolAttributeValueItem extends AttributeValueItem {
	
	/**
	 * Creates a new attribute value item of the form
	 * < ATTRIBUTE_VALUE_KEY : "attribute:n:" < AGG > ":" < ATTRIBUTE_KEY >
	 * 
	 * @param key
	 * @return
	 */
	public static PatrolAttributeValueItem createAttributeItem(String key){
		return new PatrolAttributeValueItem(key, false);
	}
	
	/**
	 * Creates a new attribute value item with category filter of the
	 * form:
	 * |    < SUM_CAT_ATT_VALUE_KEY : "category:" < DM_KEY > ":attribute:n:" < AGG > ":" < DM_KEY > >
	 * 
	 * @param key
	 * @return
	 */
	public static PatrolAttributeValueItem createCategoryAttributeItem(String key){
		return new PatrolAttributeValueItem(key, true);
	}

	
	/**
	 * Creates a new value item from the given key.
	 * @param key key
	 * @param includeCategory if the key includes a category
	 */
	public PatrolAttributeValueItem(String key, boolean includeCategory){
		super(key, includeCategory);
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception{		
		try{
			Attribute att = QueryDataModelManager.getInstance().getAttribute(session,attributeKey);
			if (att == null){
				throw new Exception(MessageFormat.format(Messages.PatrolAttributeValueItem_AttributeNotFound, new Object[]{attributeKey}));
			}
			DropItem di = null;
			Category cat = null;
			if (categoryKey != null){
				cat = QueryDataModelManager.getInstance().getCategory(session, categoryKey);
				if (cat == null){
					throw new Exception(MessageFormat.format(Messages.PatrolAttributeValueItem_CategoryNotFound, new Object[]{categoryKey}));
				}
				cat.getFullCategoryName();			
			}
			if (attributeType == AttributeType.NUMERIC){
				if (cat == null){
					di = PatrolDropItemFactory.INSTANCE.createAttributeValueDropItem(att);
				}else{
					di = PatrolDropItemFactory.INSTANCE.createAttributeValueDropItem(new CategoryAttribute(cat, att));
				}
			}else if (attributeType == AttributeType.LIST){
				AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, itemKey);
				if (ali == null){
					throw new Exception(MessageFormat.format(Messages.PatrolAttributeValueItem_ListItemNotFound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = PatrolDropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali);
				}else{
					di = PatrolDropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali,cat);
				}
			
			}else if (attributeType == AttributeType.TREE){
				AttributeTreeNode atn = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, itemKey);
				if (atn == null){
					throw new Exception(MessageFormat.format(Messages.PatrolAttributeValueItem_TreeNodeNotFound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = PatrolDropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn);
				}else{
					di = PatrolDropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn,cat);
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
	
	
}
