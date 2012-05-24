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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.query.xml.model.UuidItemType;

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
		String bits[] = key.split(":");
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
			sb.append("category:");
			sb.append(categoryHkey);
			sb.append(":");
		}
		sb.append("attribute:");
		sb.append(attributeType.typeKey);
		sb.append(":");
		sb.append(attributeKey);
		if (attributeType == AttributeType.TREE){
			sb.append(":");
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
		sb.append(":");
		if (filterHkeys != null){
			for (int i =0; i < filterHkeys.length; i ++){
				sb.append(filterHkeys[i]);
				if (i < filterHkeys.length-1){
					sb.append(":");
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
		
		Attribute att = QueryHibernateManager.getAttribute(session, attributeKey);
		
		List<ListItem> items = new ArrayList<ListItem>();
		if (att.getType() == AttributeType.LIST){
			if (filterHkeys != null) {
				for (AttributeListItem it : att.getAttributeList()) {
					for (int i = 0; i < filterHkeys.length; i++) {
						if (filterHkeys[i].equals(it.getKeyId())) {
							items.add(new ListItem(null, it.getName(), it
									.getKeyId()));
							break;
						}
					}
				}
			}else{
				for (AttributeListItem it : att.getAttributeList()) {
					if (it.getIsActive()){
						items.add(new ListItem(null, it.getName(), it.getKeyId()));
					}
				}
			}
		}else if (att.getType() == AttributeType.TREE){
			//do something different
			if (filterHkeys == null){
				//get all attribute nodes with given hkey length
				for(AttributeTreeNode child : QueryHibernateManager.getAttributeTreeNodes(session, att.getUuid(), treeLevel)){
					//TODO: full name
					if (child.getIsActive()){
						items.add(new ListItem(null, child.getName(), child.getHkey()));
					}
				}
			}else{
				HashSet<String> keys = new HashSet<String>();
				for (int i = 0; i < filterHkeys.length; i ++){
					keys.add(filterHkeys[i]);
				}
				for(AttributeTreeNode child : QueryHibernateManager.getAttributeTreeNodes(session, att.getUuid(), treeLevel)){
					//TODO: full name
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
	public DropItem asDropItem(Session session) {
		
		Attribute attribute = QueryHibernateManager.getAttribute(session, attributeKey);
		
		DropItem it = null;
		if (categoryHkey != null){
			Category category = QueryHibernateManager.getCategory(session, categoryHkey);
			it = DropItemFactory.INSTANCE.createAttributeGroupByDropItem(new CategoryAttribute(category, attribute));
		}else{
			it = DropItemFactory.INSTANCE.createAttributeGroupByDropItem(attribute);
		}

		if (attributeType == AttributeType.LIST){
			if (filterHkeys != null){
				ArrayList<ListItem> items = new ArrayList<ListItem>();
				for (int i = 0; i < filterHkeys.length ; i++){
					for (AttributeListItem ali : attribute.getAttributeList()){
						if (ali.getKeyId().equals(filterHkeys[i])){
							items.add(new ListItem(null, ali.getName(), ali.getKeyId()));
							break;
						}
					}
				}
				it.initializeData(items);
			}
			
		}else if (attributeType == AttributeType.TREE){
			if (filterHkeys != null){
				HashSet<String> keys = new HashSet<String>();
				for (int i = 0; i < filterHkeys.length; i ++){
					keys.add(filterHkeys[i]);
				}
				ArrayList<ListItem> items = new ArrayList<ListItem>();
				for(AttributeTreeNode child : QueryHibernateManager.getAttributeTreeNodes(session, attribute.getUuid(), treeLevel)){
					//TODO: full name
					if (keys.contains(child.getHkey())){
						items.add(new ListItem(null, child.getName(), child.getHkey()));	
					}
				}
				it.initializeData(items);
			}
		}
		return it;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#isCategory()
	 */
	public boolean isCategory(){
		return true;
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#validateAndImport(org.hibernate.Session)
	 */
	public List<String> validateAndImport(String langCode, HashMap<String, UuidItemType> uuidLookup, Session session) throws Exception{
		//ensure category key exists
		if (categoryHkey != null){
			QueryHibernateManager.validateCategory(categoryHkey, session);
		}
		QueryHibernateManager.validateAttribute(attributeKey, session);
		if (attributeType == AttributeType.TREE){
			for (int i = 0; i < filterHkeys.length; i ++){
				QueryHibernateManager.validateAttributeTreeNode(filterHkeys[i], session);
			}
		}else{
			for (int i = 0; i < filterHkeys.length; i ++){
				QueryHibernateManager.validateAttributeListItem(filterHkeys[i], session);
			}
		}
		return null;
	}
}