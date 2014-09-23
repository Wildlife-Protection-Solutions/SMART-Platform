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
package org.wcs.smart.query.ui.model.impl;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.BracketDropItem.BracketType;

public class BasicDropItemFactory implements IDropItemFactory{

	public static BasicDropItemFactory INSTANCE = new BasicDropItemFactory();
	
	protected BasicDropItemFactory(){
		
	}
	
	public DropItem createConservationAreaGroupByDropItem(){
		return new ConservationAreaGroupByDropItem();
	}
	
	
	public static DropItem createBooleanOpDropItem(){
		BooleanOpDropItem op = new BooleanOpDropItem();
		return op;
	}
	
	public static DropItem[] createBracketIems(){
		BracketDropItem open = new BracketDropItem(BracketType.OPEN);
		BracketDropItem close = new BracketDropItem(BracketType.CLOSE);
		
		return new DropItem[]{open, close};
	}
	
	public static DropItem createErrorDropItem(String errorMessage){
		return new ErrorDropItem(errorMessage);
	}
	public static DropItem createNotDropItem(){
		return new NotDropItem();
	}
	
	public DropItem createObserverDropItem(){
		return new ObserverDropItem();
	}
	
	public DropItem createObserverGroupByDropItem(){
		return new ObserverGroupByDropItem();
	}
	
	public DropItem createCategoryDropItem(Category c){
		return new CategoryDropItem(c);
	}
	
	public DropItem createAttributeDropItem(CategoryAttribute ca){
		if (ca.getAttribute().getType() == AttributeType.BOOLEAN || 
				ca.getAttribute().getType() == AttributeType.NUMERIC ||
				ca.getAttribute().getType() == AttributeType.TEXT ||
				ca.getAttribute().getType() == AttributeType.DATE ){
			return new AttributeDropItem(ca);
		}else if (ca.getAttribute().getType() == AttributeType.LIST ){
			return new AttributeListDropItem(ca);
		}else if (ca.getAttribute().getType() == AttributeType.TREE ){
			return new AttributeTreeDropItem(ca);
		}
		return null;
	}
	
	public DropItem createAttributeDropItem(Attribute attribute){
		if (attribute.getType() == AttributeType.BOOLEAN || 
				attribute.getType() == AttributeType.NUMERIC ||
				attribute.getType() == AttributeType.TEXT ||
				attribute.getType() == AttributeType.DATE ){
			return new AttributeDropItem(attribute);
		}else if (attribute.getType() == AttributeType.LIST ){
			return new AttributeListDropItem(attribute);
		}else if (attribute.getType() == AttributeType.TREE ){
			return new AttributeTreeDropItem(attribute);
		}
		return null;
	}
	
	public DropItem createAreaDropItem(Area source, AreaFilter.AreaFilterGeometryType type){
		return new AreaDropItem(source, type);
	}
	
	public DropItem createAreaGroupByDropItem(AreaType areaType){
		return new AreaGroupByItem(areaType);
	}
	public DropItem createAreaGroupByDropItem(Area area){
		return new AreaGroupByItem(area);
	}
	
	public DropItem createAttributeGroupByDropItem(CategoryAttribute cat){
		return new AttributeListGroupByDropItem(cat);
	}
	public DropItem createAttributeTreeNodeGroupByDropItem(Attribute att, int treeLevel, Category cat){
		return new AttributeTreeGroupByDropItem(att,treeLevel,cat);
	}
	
	public DropItem createAttributeTreeNodeGroupByDropItem(AttributeTreeNode node){
		return new AttributeTreeGroupByDropItem(node);
	}
	
	public DropItem createAttributeTreeNodeGroupByDropItem(AttributeTreeNode node, Category category){
		return new AttributeTreeGroupByDropItem(node, category);
	}
	
	public DropItem createAttributeListGroupByDropItem(Attribute attribute){
		return new AttributeListGroupByDropItem(attribute);
	}
	public DropItem createAttributeTreeNodeGroupByDropItem(Attribute att, int treeLevel){
		return new AttributeTreeGroupByDropItem(att,treeLevel);
	}

	
	public DropItem createCategoryGroupByDropItem(int treeLevel){
		return new CategoryGroupByDropItem(treeLevel);
	}
	
	public DropItem createCategoryGroupByDropItem(Category category){
		return new CategoryGroupByDropItem(category);
	}
	
	public DropItem createDateGroupByDropItem(IDateGroupBy op){
		return new DateGroupByDropItem(op);
	}

	/**
	 * @return null if cannot create a drop item based on the source
	 * object
	 */
	@Override
	public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
		
		DropItem[] items = null; 
		if (source instanceof Category) {
			items = new DropItem[]{ createCategoryDropItem((Category)source)};
			
		} else if (source instanceof CategoryAttribute) {
			items = new DropItem[]{ createAttributeDropItem((CategoryAttribute)source)};
		
		} else if (source instanceof Attribute) {
			items = new DropItem[]{ createAttributeDropItem((Attribute)source)};
		
		} else if (source instanceof IDateGroupBy) {
			items = new DropItem[]{createDateGroupByDropItem(
					(IDateGroupBy) source)};
		}

		
		return items;
	}
	
	
	/**
	 * Creates anew attribute value drop item
	 * @param att
	 * @return
	 */
	public DropItem createAttributeValueDropItem(Attribute att){
		return new AttributeValueDropItem(false, att);
	}
	
	/**
	 * Creates a new category attribute value drop item
	 * @param catatt
	 * @return
	 */
	public DropItem createAttributeValueDropItem(CategoryAttribute catatt){
		return new AttributeValueDropItem(false, catatt);
	}
	
	/**
	 * Creates a new attribute list drop item
	 * @param item
	 * @return
	 */
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item){
		return new AttributeListValueDropItem(false, item);
	}
	
	/**
	 * Creates a new attribute list item associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item, Category cat){
		return new AttributeListValueDropItem(false, item,cat);
	}
	
	/**
	 * Creates a new attribute tree node drop item
	 * @param item
	 * @return
	 */
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item ){
		return new AttributeTreeValueDropItem(false, item);
	}
	
	/**
	 * Creates a new attribute tree node associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item, Category cat){
		return new AttributeTreeValueDropItem(false, item,cat);
	}
	
	/**
	 * Creates a category value drop item
	 * @param cat
	 * @return
	 */
	public DropItem createCategoryValueDropItem(Category cat){
		if (cat == null){
			return new CategoryValueDropItem(false);
		}
		return new CategoryValueDropItem(false, cat);
	}
	
	
	/**
	 * Creates one of the other query drop items
	 * @param other
	 * @return an array of drop items of the associated type
	 */
	public DropItem[] createOtherDropItem(Operator other){
		if (other == Operator.BRACKETS){
			return createBracketIems();
		}else if (other == Operator.NOT){
			return new DropItem[]{ createNotDropItem() };
		}
		return null;
	}
	

	/**
	 * Does nothing; needs to be overwritten
	 */
	@Override
	public void generateDropItems(QueryProxy q, Session session) {
	}
	
	
}
