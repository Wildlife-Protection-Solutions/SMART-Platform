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
import org.wcs.smart.query.model.filter.IGroupByVisitor;
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
public class AttributeGroupBy implements IGroupBy {

	
	/**
	 * Creates a new category group by of the form:
	 *  | 	<  ATTRIBUTE_GROUP_BY :
			( "attribute:t:" < DM_KEY > ":" ( < DM_KEY > ":")* ) |
			( "attribute:l:" < DM_KEY > ":" < LEVEL > ":" ( < DM_KEY > ":")* ) >
			
	 *  <p>The first DM_KEY is the parent class attribute key.  The
	 *  remaining keys/hkeys must be direct children of the parent
	 *  hkey and are used to filter the results</p>
	 * 
	 * @param key
	 * @return
	 */
	public final static AttributeGroupBy createAttributeGroupBy(String key){
		return new AttributeGroupBy(key, false);
	}
	
	/**
	 * Creates a new category group by of the form:
	 *  | 	<  CATEGORY_ATTRIBUTE_GROUP_BY :
			( "category:<DM KEY>:attribute:t:" < DM_KEY > ":" ( < DM_KEY > ":")* ) |
			( "category:<DM KEY>:attribute:l:" < DM_KEY > ":" ( < DM_KEY > ":")* ) >
			
	 *  <p>The first DM_KEY is the parent class attribute key.  The
	 *  remaining keys/hkeys must be direct children of the parent
	 *  hkey and are used to filter the results</p>
	 * 
	 * @param key
	 * @return
	 */
	public final static AttributeGroupBy createCategoryAttributeGroupBy(String key){
		return new AttributeGroupBy(key, true);
	}
	
	private String categoryHkey = null;
	private String attributeKey = null;
	private String[] filterHkeys = null;
	private AttributeType attributeType = null;
	private Integer treeLevel = null;
	
	/**
	 * @param key
	 */
	protected AttributeGroupBy(String key, boolean includeCategory){
		String bits[] = key.split(":"); //$NON-NLS-1$
		int attIndex = 0;
		if (includeCategory){
			attIndex += 2;
			this.categoryHkey = bits[1];
		}
		
		this.attributeKey = bits[attIndex + 2];
		this.attributeType = Attribute.decodeAttributeTypeKey(bits[attIndex+1]);
		if (attributeType == AttributeType.TREE){
			treeLevel = Integer.parseInt( bits[attIndex+3] );
			attIndex ++;
		}
		
		if (bits.length - (attIndex+3) > 0){
			filterHkeys = new String[bits.length - (attIndex+3)];
			for (int i = attIndex + 3; i < bits.length; i ++){
				filterHkeys[i-(attIndex+3)] = bits[i];
			}
		}
	}
	
	/**
	 * @return the category hkey of associate category or null
	 * if no category specified with this attribute
	 */
	public String getCategoryHkey(){
		return this.categoryHkey;
	}
	
	/**
	 * @return the key of the associated attribute
	 * 
	 */
	public String getAttributeKey(){
		return this.attributeKey;
	}
	
	/**
	 * @return the type of the attribute group by
	 */
	public AttributeType getAttributeType(){
		return this.attributeType;
	}

	/**
	 * 
	 * @return the keys of the items to include in the group
	 * by; may be null if all items are included
	 */
	public String[] getFilterKeys(){
		return this.filterHkeys;
	}
	
	/**
	 * @return the level of the attribute tree or null
	 * if does not represent an attribute or type tree
	 */
	public Integer getTreeLevel(){
		return this.treeLevel;
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		StringBuilder sb = new StringBuilder();
		if (categoryHkey != null){
			sb.append("category:"); //$NON-NLS-1$
			sb.append(categoryHkey);
			sb.append(":"); //$NON-NLS-1$
		}
		sb.append("attribute:"); //$NON-NLS-1$
		sb.append(attributeType.typeKey);
		sb.append(":"); //$NON-NLS-1$
		sb.append(attributeKey);
		if (attributeType == AttributeType.TREE){
			sb.append(":"); //$NON-NLS-1$
			sb.append(treeLevel);
		}
		return sb.toString();
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asString()
	 */
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		sb.append(":"); //$NON-NLS-1$
		if (filterHkeys != null){
			for (int i =0; i < filterHkeys.length; i ++){
				sb.append(filterHkeys[i]);
				if (i < filterHkeys.length-1){
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}


	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getType()
	 */
	public GroupByType getType(){
		return GroupByType.KEY;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		//get children categories
		Attribute att = QueryDataModelManager.getInstance().getAttribute(session, attributeKey);
		if (att == null){
			throw new RuntimeException(MessageFormat.format(Messages.AttributeGroupBy_AttributeNotFound, new Object[]{attributeKey}));
		}
		List<ListItem> items = new ArrayList<ListItem>();
		if (att.getType() == AttributeType.LIST){
			if (filterHkeys != null) {
				for (AttributeListItem it : QueryDataModelManager.getInstance().getAttributeListItems(att, session)) {
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
				for(AttributeTreeNode child : QueryDataModelManager.getInstance().getAttributeTreeNodes(session, att, treeLevel, true)){
					items.add(new ListItem(null, child.getName(), child.getHkey()));
				}
			}else{
				HashSet<String> keys = new HashSet<String>();
				for (int i = 0; i < filterHkeys.length; i ++){
					keys.add(filterHkeys[i]);
				}
				for(AttributeTreeNode child : QueryDataModelManager.getInstance().getAttributeTreeNodes(session, att, treeLevel, true)){
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
					it = BasicDropItemFactory.INSTANCE.createAttributeTreeNodeGroupByDropItem(attribute,getTreeLevel(), category);
				}
			} else {
				if (attributeType == AttributeType.LIST) {
					it = BasicDropItemFactory.INSTANCE.createAttributeListGroupByDropItem(attribute);
				} else if (attributeType == AttributeType.TREE ) {
					it = BasicDropItemFactory.INSTANCE.createAttributeTreeNodeGroupByDropItem(attribute,getTreeLevel());
				}
			}

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
	public void visit(IGroupByVisitor visitor){
		visitor.visit(this);
	}
	
}