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
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.BracketDropItem.BracketType;

public class BasicDropItemFactory implements IDropItemFactory{

	public static BasicDropItemFactory INSTANCE = new BasicDropItemFactory();
	
	protected BasicDropItemFactory(){
		
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
	
	public DropItem createCategoryDropItem(Category c){
		return new CategoryDropItem(c);
	}
	
	public DropItem createAttributeDropItem(CategoryAttribute ca){
		if (ca.getAttribute().getType() == AttributeType.BOOLEAN || 
				ca.getAttribute().getType() == AttributeType.NUMERIC ||
				ca.getAttribute().getType() == AttributeType.TEXT ){
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
						attribute.getType() == AttributeType.TEXT ){
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
	 * Does nothing; needs to be overwritten
	 */
	@Override
	public void generateDropItems(QueryProxy q, Session session) {
	}
	
	
}
