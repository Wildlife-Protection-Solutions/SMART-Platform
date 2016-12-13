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
package org.wcs.smart.query.model.summary;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Represents an attribute group by option
 * of a summary query.
 * 
 * Valid for tree and list attribute types.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AttributeGroupByViewer extends AbstractGroupByViewer<AttributeGroupBy> {
	
	public AttributeGroupByViewer(AttributeGroupBy gb) {
		super(gb);
	}

	@Override
	public List<ListItem> getItems(Session session) {
		String[]  filterHkeys = groupBy.getFilterKeys();
		//get children categories
		Attribute att = QueryDataModelManager.getInstance().getAttribute(session, groupBy.getAttributeKey());
		if (att == null){
			throw new RuntimeException(MessageFormat.format(Messages.AttributeGroupBy_AttributeNotFound, new Object[]{groupBy.getAttributeKey()}));
		}
		List<ListItem> items = new ArrayList<ListItem>();
		if (att.getType() == AttributeType.LIST){
			if (filterHkeys != null) {
				for (AttributeListItem it : QueryDataModelManager.getInstance().getAttributeListItems(att, session, true)) {
					for (String key : filterHkeys){
						if (key.equals(it.getKeyId())){
							items.add(new ListItem(null, it.getName(), it.getKeyId()));
							break;
						}
					}
				}
			}else{				
				for (AttributeListItem it : QueryDataModelManager.getInstance().getActiveAttributeListItems(att, session)) {
					items.add(new ListItem(null, it.getName(), it.getKeyId()));
				}
			}
		}else if (att.getType() == AttributeType.TREE){
			if (filterHkeys == null){
				//get all attribute nodes with given hkey length
				for(AttributeTreeNode child : QueryDataModelManager.getInstance().getAttributeTreeNodes(session, att, groupBy.getTreeLevel(), true)){
					items.add(new ListItem(null, child.getName(), child.getHkey()));
				}
			}else{
				HashSet<String> keys = new HashSet<String>();
				for (int i = 0; i < filterHkeys.length; i ++){
					keys.add(filterHkeys[i]);
				}
				for(AttributeTreeNode child : QueryDataModelManager.getInstance().getAttributeTreeNodes(session, att, groupBy.getTreeLevel(), false)){
					if (keys.contains(child.getHkey())){
						items.add(new ListItem(null, child.getName(), child.getHkey()));	
					}
				}
			}
		}
		return items;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception{
		String attributeKey = groupBy.getAttributeKey();
		String categoryHkey = groupBy.getCategoryHkey();
		Attribute.AttributeType attributeType = groupBy.getAttributeType();
		
		try {
			Attribute attribute = QueryDataModelManager.getInstance().getAttribute(session, attributeKey);
			if (attribute == null) {
				throw new Exception(MessageFormat.format(Messages.AttributeGroupBy_AttributeNotFoundError,new Object[] { attributeKey }));
			}
			DropItem it = null;
			if (categoryHkey != null) {
				Category category = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
				if (category == null) {
					throw new Exception(MessageFormat.format(Messages.AttributeGroupBy_CategoryNotFoundError,new Object[] { categoryHkey }));
				}
				category.getFullCategoryName();
				if (attributeType == AttributeType.LIST) {
					it = BasicDropItemFactory.INSTANCE.createAttributeListGroupByDropItem(new CategoryAttribute(category, attribute));
				} else if (attributeType == AttributeType.TREE ){
					it = BasicDropItemFactory.INSTANCE.createAttributeTreeNodeGroupByDropItem(attribute,groupBy.getTreeLevel(), category);
				}
			} else {
				if (attributeType == AttributeType.LIST) {
					it = BasicDropItemFactory.INSTANCE.createAttributeListGroupByDropItem(attribute);
				} else if (attributeType == AttributeType.TREE ) {
					it = BasicDropItemFactory.INSTANCE.createAttributeTreeNodeGroupByDropItem(attribute,groupBy.getTreeLevel());
				}
			}
			String[] filterHkeys = groupBy.getFilterKeys();
			if (attributeType == AttributeType.LIST) {
				if (filterHkeys != null) {
					ArrayList<ListItem> items = new ArrayList<ListItem>();
					for (int i = 0; i < filterHkeys.length; i++) {
						AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session,attribute.getKeyId(), filterHkeys[i]);
						if (ali != null) {
							items.add(new ListItem(null, ali.getName(), ali.getKeyId()));
						}
					}
					it.initializeData(items);
				}

			} else if (attributeType == AttributeType.TREE) {
				if (filterHkeys != null) {
					HashSet<String> keys = new HashSet<String>();
					for (int i = 0; i < filterHkeys.length; i++) {
						keys.add(filterHkeys[i]);
					}
					ArrayList<ListItem> items = new ArrayList<ListItem>();
					for (String hkey : keys) {
						AttributeTreeNode item = QueryDataModelManager.getInstance().getAttributeTreeNode(session,attribute.getKeyId(), hkey);
						if (item != null) {
							items.add(new ListItem(null, item.getName(), item.getHkey()));
						}
					}

					it.initializeData(items);
				}
			}
			return it;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
	}
	
}