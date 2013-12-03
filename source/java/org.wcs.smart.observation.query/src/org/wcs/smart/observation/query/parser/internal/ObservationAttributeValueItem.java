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
package org.wcs.smart.observation.query.parser.internal;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.ui.definition.ObservationDropItemFactory;
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
public class ObservationAttributeValueItem extends AttributeValueItem {

	/**
	 * Creates a new attribute value item of the form
	 * < ATTRIBUTE_VALUE_KEY : "attribute:n:" < AGG > ":" < ATTRIBUTE_KEY >
	 * 
	 * @param key
	 * @return
	 */
	public static ObservationAttributeValueItem createAttributeItem(String key){
		return new ObservationAttributeValueItem(key, false);
	}
	
	/**
	 * Creates a new attribute value item with category filter of the
	 * form:
	 * |    < SUM_CAT_ATT_VALUE_KEY : "category:" < DM_KEY > ":attribute:n:" < AGG > ":" < DM_KEY > >
	 * 
	 * @param key
	 * @return
	 */
	public static ObservationAttributeValueItem createCategoryAttributeItem(String key){
		return new ObservationAttributeValueItem(key, true);
	}
	
	public ObservationAttributeValueItem(String key, boolean includeCategory){
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
					di = ObservationDropItemFactory.INSTANCE.createAttributeValueDropItem(att);
				}else{
					di = ObservationDropItemFactory.INSTANCE.createAttributeValueDropItem(new CategoryAttribute(cat, att));
				}
			}else if (attributeType == AttributeType.LIST){
				AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, itemKey);
				if (ali == null){
					throw new Exception(MessageFormat.format(Messages.AttributeValueItem_ListItemNotFound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = ObservationDropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali);
				}else{
					di = ObservationDropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali,cat);
				}
			
			}else if (attributeType == AttributeType.TREE){
				AttributeTreeNode atn = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, itemKey);
				if (atn == null){
					throw new Exception(MessageFormat.format(Messages.AttributeValueItem_TreeNodeNotFound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = ObservationDropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn);
				}else{
					di = ObservationDropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn,cat);
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
