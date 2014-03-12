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
package org.wcs.smart.observation.query.ui.definition;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.observation.query.ObservationQueryPlugIn;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.model.ObservationGriddedQuery;
import org.wcs.smart.observation.query.model.ObservationSummaryQuery;
import org.wcs.smart.observation.query.model.types.ObservationGridQueryType;
import org.wcs.smart.observation.query.model.types.ObservationSummaryQueryType;
import org.wcs.smart.observation.query.ui.itempanel.GeneralContentProvider;
import org.wcs.smart.observation.query.ui.itempanel.GriddedItemPanel;
import org.wcs.smart.observation.query.ui.itempanel.SummaryFilterPanel;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.ui.itempanel.SummaryDataModelContentProvider;
import org.wcs.smart.query.common.ui.itempanel.SummaryDmObject;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.definition.BasicGridDefinitionPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.AttributeListValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeTreeValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeValueDropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.CategoryValueDropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
/**
 * Drop item factory for observation queries
 * @author Emily
 *
 */
public class ObservationDropItemFactory extends BasicDropItemFactory implements IDropItemFactory {

	public static ObservationDropItemFactory INSTANCE = new ObservationDropItemFactory();
	
	protected ObservationDropItemFactory(){
		
	}
	
	
	/**
	 * Generates a drop item for the given source object from
	 * the given query item panel.
	 * @param source source object
	 * @param queryItemPanelId object source location
	 */
	@Override
	public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
		
		DropItem[] items = super.generateDropItem(source, queryItemPanelId);
		if (items != null){
			return items;
		}
		
		if (source instanceof Operator) {
			items = createOtherDropItem((Operator)source);
		} else if (source instanceof IDateGroupBy) {
			items = new DropItem[]{createDateGroupByDropItem(
							(IDateGroupBy) source)};
		
		} else if (source instanceof SummaryDmObject) {
			items = new DropItem[]{createSummaryDmDropItem((SummaryDmObject)source)};
			
		}else if (source instanceof AreaType){
			if (queryItemPanelId.equals(SummaryFilterPanel.ID)){
				items = new DropItem[]{createAreaGroupByDropItem((AreaType)source)};
			}
		}else if (source instanceof Area){
			if (queryItemPanelId.equals(SummaryFilterPanel.ID)){
				items = new DropItem[]{createAreaGroupByDropItem((Area)source)};
			}
		}else if (source == SummaryDataModelContentProvider.DataModelItem.CATEGORIES_VALUE){
			if (queryItemPanelId.equals(SummaryFilterPanel.ID) ||
					queryItemPanelId.equals(GriddedItemPanel.ID)){
				items = new DropItem[]{createCategoryValueDropItem(null)};
			}
		}else if (source instanceof GeneralContentProvider.GeneralItems){
			if (queryItemPanelId.equals(SummaryFilterPanel.ID)){
				items = new DropItem[]{createWaypointSourceGroupByDropItem()};
			}else{
				items = new DropItem[]{createWaypointSourceFilterDropItem((GeneralContentProvider.GeneralItems)source)};
			}
		}
		return items;
		
	}

	public DropItem createWaypointSourceGroupByDropItem(){
		return new WaypointSourceGroupByDropItem();
	}
	public DropItem createWaypointSourceFilterDropItem(GeneralContentProvider.GeneralItems source){
		if (source == GeneralContentProvider.GeneralItems.WAYPOINT_SOURCE){
			return new WaypointSourceFilterDropItem();
		}
		throw new IllegalStateException(MessageFormat.format(Messages.ObservationDropItemFactory_QueryItemNotSupported, new Object[]{source.guiName}));
	}
	
	/**
	 * Creates anew attribute value drop item
	 * @param att
	 * @return
	 */
	public DropItem createAttributeValueDropItem(Attribute att){
		return new AttributeValueDropItem(att);
	}
	
	/**
	 * Creates a new category attribute value drop item
	 * @param catatt
	 * @return
	 */
	public DropItem createAttributeValueDropItem(CategoryAttribute catatt){
		return new AttributeValueDropItem(catatt);
	}
	
	/**
	 * Creates a new attribute list drop item
	 * @param item
	 * @return
	 */
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item){
		return new AttributeListValueDropItem(item);
	}
	
	/**
	 * Creates a new attribute list item associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item, Category cat){
		return new AttributeListValueDropItem(item,cat);
	}
	
	/**
	 * Creates a new attribute tree node drop item
	 * @param item
	 * @return
	 */
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item ){
		return new AttributeTreeValueDropItem(item);
	}
	
	/**
	 * Creates a new attribute tree node associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item, Category cat){
		return new AttributeTreeValueDropItem(item,cat);
	}
	
	/**
	 * Creates a category value drop item
	 * @param cat
	 * @return
	 */
	public DropItem createCategoryValueDropItem(Category cat){
		if (cat == null){
			return new CategoryValueDropItem();
		}
		return new CategoryValueDropItem(cat);
	}
	
	
	/**
	 * Creates one of the other query drop items
	 * @param other
	 * @return an array of drop items of the associated type
	 */
	private DropItem[] createOtherDropItem(Operator other){
		if (other == Operator.BRACKETS){
			return createBracketIems();
		}else if (other == Operator.NOT){
			return new DropItem[]{ createNotDropItem() };
		}
		return null;
	}
	
	/*
	 * Creates a drop item from a SummaryDmObject
	 */
	private DropItem createSummaryDmDropItem(SummaryDmObject object) {
		if (object.isValue()) {
			if (object.getObject() instanceof Attribute) {
				if (((Attribute)object.getObject()).getType() == AttributeType.NUMERIC){
					return createAttributeValueDropItem(
						(Attribute) object.getObject());
				}
				return null;
			} else if (object.getObject() instanceof CategoryAttribute) {
				if (((CategoryAttribute)object.getObject()).getAttribute().getType() == AttributeType.NUMERIC){
					return createAttributeValueDropItem((CategoryAttribute) object.getObject());
				}
				return null;
			} else if (object.getObject() instanceof Category) {
				return createCategoryValueDropItem(
						(Category) object.getObject());
			} else if (object.getObject() instanceof AttributeListItem){
				if (object.getObject2() != null){
					return createAttributeListItemValueDropItem((AttributeListItem)object.getObject(),(Category)object.getObject2());
				}else{
					return createAttributeListItemValueDropItem((AttributeListItem)object.getObject());
				}
			} else if (object.getObject() instanceof AttributeTreeNode){
				if (object.getObject2() != null){
					return createAttributeTreeNodeValueDropItem((AttributeTreeNode)object.getObject(), (Category)object.getObject2());
				}else{
					return createAttributeTreeNodeValueDropItem((AttributeTreeNode)object.getObject());
				}
			}
		} else {
			// category
			if (object.getObject() instanceof Category) {
				return createCategoryGroupByDropItem((Category) object.getObject());
			} else if (object.getObject() instanceof Attribute) {
				if (((Attribute)object.getObject()).getType() == AttributeType.LIST ){
					return createAttributeListGroupByDropItem((Attribute) object.getObject());
				}
			} else if (object.getObject() instanceof CategoryAttribute) {
				return createAttributeGroupByDropItem((CategoryAttribute) object.getObject());
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

	
	
	
	
	/**
	 * Generates drop items for the given query proxy.
	 * 
	 * @see org.wcs.smart.query.ui.model.impl.BasicDropItemFactory#generateDropItems(org.wcs.smart.query.model.QueryProxy, org.hibernate.Session)
	 */
	@Override
	public void generateDropItems(QueryProxy proxy, Session session) {
		if (proxy.getQuery() instanceof SimpleQuery){
			
			IFilter queryFilter = ((SimpleQuery)proxy.getQuery()).getFilter().getFilter();
			proxy.setDropItems(BasicFilterDefintionPanel.ID, asDropItems(queryFilter, session));
					
		}else if (proxy.getQuery().getType().getKey().equals(ObservationSummaryQueryType.KEY)){
			ObservationSummaryQuery q = (ObservationSummaryQuery) proxy.getQuery();
			SumQueryDefinition def = q.getQueryDefinition();
			
			proxy.setDropItems(BasicFilterDefintionPanel.ID, def == null || def.getValueFilter() == null || def.getValueFilter().getFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); 
			
			proxy.setDropItems(ObservationSummaryGroupByValuePanel.ID + "." + ObservationSummaryGroupByValuePanel.ListTargetType.COLUMN.name(), //$NON-NLS-1$
					def == null || def.getColumnGroupByPart() == null ? null : def.getColumnGroupByPart().getDropItems(session));
			proxy.setDropItems(ObservationSummaryGroupByValuePanel.ID + "." + ObservationSummaryGroupByValuePanel.ListTargetType.ROW.name(), //$NON-NLS-1$
					def == null || def.getRowGroupByPart() == null ? null : def.getRowGroupByPart().getDropItems(session));
			proxy.setDropItems(ObservationSummaryGroupByValuePanel.ID + "." + ObservationSummaryGroupByValuePanel.ListTargetType.VALUE.name(), //$NON-NLS-1$
					def == null || def.getValuePart() == null ? null : def.getValuePart().getDropItems(session));
			
		}else if(proxy.getQuery().getType().getKey().equalsIgnoreCase(ObservationGridQueryType.KEY)){
			ObservationGriddedQuery q = (ObservationGriddedQuery) proxy.getQuery();
			GridQueryDefinition def = q.getQueryDefinition();
			
			proxy.setDropItems(BasicFilterDefintionPanel.ID, def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session));
			
			DropItem valueItem = null;
			try{
				valueItem = def.getValuePart().asDropItem(session);
			}catch(Exception ex){
				ObservationQueryPlugIn.log(ex.getMessage(), ex);
				valueItem = new ErrorDropItem(ex.getMessage());
			}
			proxy.setDropItems(BasicGridDefinitionPanel.ID + BasicGridDefinitionPanel.VALUE_PANEL_SUFFIX,
					Collections.singletonList(valueItem));
					
			
		}
	}
	
	/*
	 * Converts a filter to a set of drop items
	 */
	private List<DropItem> asDropItems(IFilter filter, Session session){
		List<DropItem> items = new ArrayList<DropItem>();
		try{
			DropItem[] filterItems = filter.getDropItems(session);
			for(DropItem i : filterItems){
				items.add(i);
			}
			
		}catch (Exception ex){
			items.add(new ErrorDropItem(MessageFormat.format(Messages.SimpleQuery_DropItemParseError, new Object[]{ex.getMessage()})));
		}
		return items;
	}
}