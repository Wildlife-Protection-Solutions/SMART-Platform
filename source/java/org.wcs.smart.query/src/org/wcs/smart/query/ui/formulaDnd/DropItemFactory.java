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

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.parser.internal.PatrolFilter;
import org.wcs.smart.query.parser.internal.PatrolFilter.PatrolFilterOption;
import org.wcs.smart.query.ui.formulaDnd.BracketDropItem.BracketType;
import org.wcs.smart.query.ui.queyfilter.QueryFilterContentProvider;

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
	public DropItem createPatrolDropItem(PatrolFilter.PatrolFilterOption option){
		DropItem item = null;
		if (option == PatrolFilterOption.ARMED){
			item = new BooleanPatrolDropItem( option);
		}else if (option == PatrolFilterOption.ID){
			item = new PatrolIdDropItem(option);
		}else if (option == PatrolFilterOption.MANDATE || 
				option == PatrolFilterOption.PATROLTYPE ||
				option == PatrolFilterOption.TRANSPORT ||
				option == PatrolFilterOption.STATION ||
				option == PatrolFilterOption.EMPLOYEE ||
				option == PatrolFilterOption.LEADER ||
				option == PatrolFilterOption.TEAM||
				option == PatrolFilterOption.PILOT){
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
	
	

	
}
