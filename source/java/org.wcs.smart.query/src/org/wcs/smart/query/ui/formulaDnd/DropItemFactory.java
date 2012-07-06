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
package org.wcs.smart.query.ui.formulaDnd;

import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.DateGroupByOption;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.query.ui.formulaDnd.BracketDropItem.BracketType;
import org.wcs.smart.query.ui.queyfilter.QueryFilterContentProvider;
import org.wcs.smart.query.ui.queyfilter.QueryFilterSelection;
import org.wcs.smart.query.ui.queyfilter.QueryFilterSelection.FilterType;
import org.wcs.smart.query.ui.queyfilter.SummaryDmObject;

/**
 * 
 * Factory for creating drop items.
 * @author Emily
 * @since 1.0.0
 */
public class DropItemFactory {

	public static final DropItemFactory INSTANCE = new DropItemFactory();
	/**
	 * Creates a new factory
	 * 
	 * @param target drop target 
	 */
	private DropItemFactory(){
	}
	
	/**
	 * Creates a drop item for a category.
	 * 
	 * @param category 
	 * @return
	 */
	public DropItem createCategoryDropItem(Category category){
		CategoryDropItem item = new CategoryDropItem(category);
		return item;
	}
	
	/**
	 * Creates a drop item for a given categoryAttribute 
	 * @param attribute
	 * @return
	 */
	public DropItem createAttributeDropItem(CategoryAttribute attribute){
		DropItem item = null;
		if (attribute.getAttribute().getType() == AttributeType.NUMERIC || attribute.getAttribute().getType() == AttributeType.TEXT){
			item =  new AttributeDropItem(attribute);
		}else if (attribute.getAttribute().getType() == AttributeType.LIST){
			item =  new AttributeListDropItem(attribute);
		}else if (attribute.getAttribute().getType() == AttributeType.TREE){
			item = new AttributeTreeDropItem(attribute);
		}else if (attribute.getAttribute().getType() == AttributeType.BOOLEAN){
			item = new AttributeDropItem(attribute);
		}
		return item;
	}
	

	/**
	 * Creates a drop tiem for a given attribute
	 * @param attribute
	 * @return
	 */
	public DropItem createAttributeDropItem(Attribute attribute){
		DropItem item = null;
		if (attribute.getType() == AttributeType.NUMERIC || attribute.getType() == AttributeType.TEXT){
			item =  new AttributeDropItem(attribute);
		}else if (attribute.getType() == AttributeType.LIST){
			item =  new AttributeListDropItem(attribute);
		}else if (attribute.getType() == AttributeType.TREE){
			item = new AttributeTreeDropItem(attribute);
		}else if (attribute.getType() == AttributeType.BOOLEAN){
			item = new AttributeDropItem(attribute);
		}
		return item;
	}

	/**
	 * Creates a drop item for a boolean operator
	 * @return
	 */
	public DropItem createBooleanOpDropItem(){
		DropItem item = new BooleanOpDropItem();
		return item;
	}
	
	/**
	 * Creates a patrol drop item.
	 * @param option
	 * @return
	 */
	public DropItem createPatrolFilterDropItem(PatrolQueryOption option){
		DropItem item = null;
		if (option == PatrolQueryOption.ARMED){
			item = new BooleanPatrolDropItem( option);
		}else if (option == PatrolQueryOption.ID){
			item = new PatrolIdDropItem(option);
		}else if (option == PatrolQueryOption.MANDATE || 
				option == PatrolQueryOption.PATROL_TYPE ||
				option == PatrolQueryOption.PATROL_TRANSPORT_TYPE ||
				option == PatrolQueryOption.STATION ||
				option == PatrolQueryOption.EMPLOYEE ||
				option == PatrolQueryOption.LEADER ||
				option == PatrolQueryOption.TEAM||
				option == PatrolQueryOption.PILOT){
			item = new PatrolListDropItem( option);
		}
		return item;
			
			
	}
	
	/**
	 * Creates one of the other query drop items
	 * @param other
	 * @return an array of drop items of the associated type
	 */
	public DropItem[] createOtherDropItem(QueryFilterContentProvider.OtherItems other){
		if (other == QueryFilterContentProvider.OtherItems.BRACKETS){
			return new DropItem[]{new BracketDropItem(BracketType.OPEN),
			 new BracketDropItem(BracketType.CLOSE)};
		}else if (other == QueryFilterContentProvider.OtherItems.NOT){
			return new DropItem[]{new NotDropItem()};
		}
		return null;
	}
	
	/**
	 * Creates one of the other query drop items
	 * @param other
	 * @return an array of drop items of the associated type
	 */
	public DropItem createOtherSingleBracketDropItem(BracketType type){
			return  new BracketDropItem(type);
	}
	
	/**
	 * Creates a new patrol value drop item
	 * @param item
	 * @return
	 */
	public DropItem createPatrolValueDropItem(PatrolValueOption item){
		return new PatrolValueDropItem(item);
	}
	
	/**
	 * Creates a new patrol group by drop item
	 * @param item
	 * @return
	 */
	public DropItem createPatrolGroupByDropItem(PatrolQueryOption item){
		return new PatrolGroupByDropItem(item);
	}
	
	/**
	 * Creates a new date group by drop item
	 * @param item
	 * @return
	 */
	public DropItem createDateGroupByDropItem(DateGroupByOption item){
		return new DateGroupByDropItem(item);
	}
	
	/**
	 * Creates a new value drop item that represents an datamodel
	 * attribute.
	 * 
	 * @param attribute
	 * @return
	 */
	public DropItem createAttributeValueDropItem(Attribute attribute){
		return new AttributeValueDropItem(attribute);
	}

	/**
	 * Creates a new value drop item that represents an datamodel
	 * attribute and associated category.
	 * 
	 * @param catAtt
	 * @return
	 */
	public DropItem createAttributeValueDropItem(CategoryAttribute catAtt){
		return new AttributeValueDropItem(catAtt);
	}
	
	/**
	 * Creates a new category value drop item that represents
	 * the total number of observations of the given
	 * category.
	 * 
	 * @param category
	 * @return
	 */
	public DropItem createCategoryValueDropItem(Category category){
		return new CategoryValueDropItem(category);
	}	
	
	/**
	 * Creates a new category group by drop item.  The category
	 * provided should be the parent category.
	 * 
	 * @param category parent group by category
	 * @return
	 */
	public DropItem createCategoryGroupByDropItem(Category category){
		return new CategoryGroupByDropItem(category);
	}
	
	public DropItem createCategoryGroupByDropItem(int treeLevel){
		return new CategoryGroupByDropItem(treeLevel);
	}
	
	/**
	 * Creates a new category group by drop item.  The category
	 * provided should be the parent category.
	 * 
	 * @param category parent group by category
	 * @return
	 */
	public DropItem createAttributeGroupByDropItem(Attribute attribute){
		if (attribute.getType() == AttributeType.LIST){
			return new AttributeListGroupByDropItem(attribute);
		}
		return null;
	}
	
	
	/**
	 * Creates a new category group by drop item.  The category
	 * provided should be the parent category.
	 * 
	 * @param category parent group by category
	 * @return
	 */
	public DropItem createAttributeGroupByDropItem(CategoryAttribute catAttribute){
		if (catAttribute.getAttribute().getType() == AttributeType.LIST){
			return new AttributeListGroupByDropItem(catAttribute);
		}
		return null;
	}

	/**
	 * Creates a new drop item for a tree attribute node with no
	 * associated category.
	 * 
	 * @param attribute the attribute
	 * @param level the level in the tree of the items in the group by
	 * @return
	 */
	public DropItem createAttributeTreeNodeGroupByDropItem(Attribute attribute, int level){
		return new AttributeTreeGroupByDropItem(attribute, level);
	}
	

	/**
	 * Creates a new drop item for a tree attribute node with the 
	 * associated category.
	 * 
	 * @param attribute the attribute
	 * @param cateogry associated category
	 * @param level the level in the tree of the items in the group by
	 * @return
	 */
	public DropItem createAttributeTreeNodeGroupByDropItem(Attribute attribute, int level, Category category){
		return new AttributeTreeGroupByDropItem(attribute, level, category);
	}

	
	/**
	 * Creates a new drop item for a tree attribute node with no
	 * associated category.
	 * 
	 * @param node
	 * @return
	 */
	public DropItem createAttributeTreeNodeGroupByDropItem(AttributeTreeNode node){
		return new AttributeTreeGroupByDropItem(node);
	}
	
	/**
	 * Creates a new drop item for a tree attribute node associated
	 * with the specified category
	 * 
	 * @param node tree attribute node
	 * @param category category
	 * @return
	 */
	public DropItem createAttributeTreeNodeGroupByDropItem(AttributeTreeNode node, Category category){
		return new AttributeTreeGroupByDropItem(node, category);
	}
	
	/**
	 * Creates a new drop item for an area item
	 * @param area
	 * @return
	 */
	public DropItem createAreaDropItem(Area area){
		return new AreaDropItem(area);
	}
	
	
	/**
	 * Creates a drop item or collection of drop items for the
	 * given object based on the type of the object.
	 * 
	 * @param object The object to create drop item for
	 * @param fType the type of drop item to create (filter, value etc)
	 * @return null or a array of drop items created
	 */
	public DropItem[] createDropItem(Object object, FilterType fType){
		if (object instanceof Category) {
			return new DropItem[]{ createCategoryDropItem((Category) object) };
		} else if (object instanceof CategoryAttribute) {
			return new DropItem[]{ createAttributeDropItem( (CategoryAttribute) object)};
		} else if (object instanceof Attribute) {
			return new DropItem[]{ createAttributeDropItem((Attribute) object)};
			
		} else if (object instanceof QueryFilterContentProvider.OtherItems) {
			return createOtherDropItem(
							(QueryFilterContentProvider.OtherItems) object);
		} else if (object instanceof PatrolValueOption) {
			return new DropItem[]{createPatrolValueDropItem(
							(PatrolValueOption) object)};

		} else if (object instanceof PatrolQueryOption) {
			if (fType == QueryFilterSelection.FilterType.FILTER) {
				return new DropItem[]{createPatrolFilterDropItem(
								(PatrolQueryOption) object)};
			} else if (fType == QueryFilterSelection.FilterType.SUMMARY) {
				return new DropItem[]{createPatrolGroupByDropItem(
								(PatrolQueryOption) object)};
			}
		} else if (object instanceof DateGroupByOption) {
			return new DropItem[]{createDateGroupByDropItem(
							(DateGroupByOption) object)};
		
		} else if (object instanceof SummaryDmObject) {
			return new DropItem[]{createSummaryDmDropItem((SummaryDmObject)object)};
			
		}else if (object instanceof Area){
			return new DropItem[]{ createAreaDropItem((Area)object) };

		}
		return null;
	}
	
	/*
	 * Creates a drop item from a SummaryDmObject
	 */
	private DropItem createSummaryDmDropItem(SummaryDmObject object) {
		if (object.isValue()) {
			if (object.getObject() instanceof Attribute) {
				return createAttributeValueDropItem(
						(Attribute) object.getObject());
			} else if (object.getObject() instanceof CategoryAttribute) {
				return createAttributeValueDropItem(
						(CategoryAttribute) object.getObject());
			} else if (object.getObject() instanceof Category) {
				return createCategoryValueDropItem(
						(Category) object.getObject());
			}
		} else {
			// category
			if (object.getObject() instanceof Category) {
				return createCategoryGroupByDropItem(
						(Category) object.getObject());
			} else if (object.getObject() instanceof Attribute) {
				return createAttributeGroupByDropItem(
						(Attribute) object.getObject());
			} else if (object.getObject() instanceof CategoryAttribute) {
				return createAttributeGroupByDropItem(
						(CategoryAttribute) object.getObject());
			} else if (object.getObject() instanceof AttributeTreeNode) {
				if (object.getObject2() != null) {
					return createAttributeTreeNodeGroupByDropItem(
									(AttributeTreeNode) object.getObject(),
									(Category) object.getObject2());
				} else {
					return createAttributeTreeNodeGroupByDropItem(
									(AttributeTreeNode) object.getObject());
				}
			}
		}
		return null;
	}
}
