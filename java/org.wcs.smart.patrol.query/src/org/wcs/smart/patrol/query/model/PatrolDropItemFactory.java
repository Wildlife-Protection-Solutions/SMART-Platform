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
package org.wcs.smart.patrol.query.model;

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
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.types.PatrolGridQueryType;
import org.wcs.smart.patrol.query.model.types.PatrolSummaryQueryType;
import org.wcs.smart.patrol.query.parser.IExtensionOption;
import org.wcs.smart.patrol.query.parser.IPatrolQueryOption;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.patrol.query.ui.definition.PatrolGriddedQueryDefinitionPanel;
import org.wcs.smart.patrol.query.ui.definition.PatrolSummaryGroupByValuePanel;
import org.wcs.smart.patrol.query.ui.definition.SimpleValueRateFilterPanel;
import org.wcs.smart.patrol.query.ui.definition.dropItems.AbstractValueDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.AttributeListValueDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.AttributeTreeValueDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.AttributeValueDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.BooleanPatrolDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.CategoryValueDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolGroupByDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolIdDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolListDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolValueDropItem;
import org.wcs.smart.patrol.query.ui.itempanel.GriddedFilterPanel;
import org.wcs.smart.patrol.query.ui.itempanel.SummaryFilterPanel;
import org.wcs.smart.query.QueryPlugIn;
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
import org.wcs.smart.query.ui.definition.ValueRateFilterDeifnitionPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Drop item factory for patrol queries.
 * @author Emily
 *
 */
public class PatrolDropItemFactory extends BasicDropItemFactory implements IDropItemFactory {

	public static PatrolDropItemFactory INSTANCE = new PatrolDropItemFactory();
	
	protected PatrolDropItemFactory(){
		
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
		
		} else if (source instanceof PatrolValueOption) {
			items = new DropItem[]{createPatrolValueDropItem(
							(PatrolValueOption) source)};

		} else if (source instanceof IPatrolQueryOption) {
			if (queryItemPanelId == SummaryFilterPanel.ID){
				items = new DropItem[]{createPatrolGroupByDropItem(
						(IPatrolQueryOption) source)};
			}else{
				items = new DropItem[]{createPatrolFilterDropItem(
						(IPatrolQueryOption) source)};
			}
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
					queryItemPanelId.equals(GriddedFilterPanel.ID)){
				items = new DropItem[]{createCategoryValueDropItem(null)};
			}
		}else if (source instanceof IExtensionOption){
			items = new DropItem[]{((IExtensionOption) source).asDropItem()};
		}
		return items;
		
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
	public DropItem createPatrolGroupByDropItem(IPatrolQueryOption item){
		return new PatrolGroupByDropItem(item);
	}
	
	/**
	 * Creates a patrol drop item.
	 * @param option
	 * @return
	 */
	public DropItem createPatrolFilterDropItem(IPatrolQueryOption option){
		DropItem item = null;
		if (option == PatrolQueryOption.ARMED){
			item = new BooleanPatrolDropItem(option);
		}else if (option == PatrolQueryOption.ID){
			item = new PatrolIdDropItem(option);
		}else if (option == PatrolQueryOption.MANDATE ||
				option == PatrolQueryOption.MANDATE_KEY || 
				option == PatrolQueryOption.PATROL_TYPE ||
				option == PatrolQueryOption.PATROL_TRANSPORT_TYPE ||
				option == PatrolQueryOption.PATROL_TRANSPORT_TYPE_KEY ||
				option == PatrolQueryOption.STATION ||
				option == PatrolQueryOption.EMPLOYEE ||
				option == PatrolQueryOption.LEADER ||
				option == PatrolQueryOption.TEAM ||
				option == PatrolQueryOption.TEAM_KEY ||
				option == PatrolQueryOption.PILOT){
			item = new PatrolListDropItem(option);
		} else {
			switch (option.getType()) {
			case BOOLEAN:
				item = new BooleanPatrolDropItem(option);
				break;
			case STRING:
				item = new PatrolIdDropItem(option);
				break;
			case UUID:
				item = new PatrolListDropItem(option);
				break;
			default:
				break;
			}
		}
		return item;		
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
	 * Creates anew attribute value drop item
	 * @param att
	 * @return
	 */
	@Override
	public DropItem createAttributeValueDropItem(Attribute att){
		return new AttributeValueDropItem(att);
	}
	
	/**
	 * Creates a new category attribute value drop item
	 * @param catatt
	 * @return
	 */
	@Override
	public DropItem createAttributeValueDropItem(CategoryAttribute catatt){
		return new AttributeValueDropItem(catatt);
	}
	
	/**
	 * Creates a new attribute list drop item
	 * @param item
	 * @return
	 */
	@Override
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item){
		return new AttributeListValueDropItem(item);
	}
	
	/**
	 * Creates a new attribute list item associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	@Override
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item, Category cat){
		return new AttributeListValueDropItem(item,cat);
	}
	
	/**
	 * Creates a new attribute tree node drop item
	 * @param item
	 * @return
	 */
	@Override
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item ){
		return new AttributeTreeValueDropItem(item);
	}
	
	/**
	 * Creates a new attribute tree node associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	@Override
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item, Category cat){
		return new AttributeTreeValueDropItem(item,cat);
	}
	
	/**
	 * Creates a category value drop item
	 * @param cat
	 * @return
	 */
	@Override
	public DropItem createCategoryValueDropItem(Category cat){
		if (cat == null){
			return new CategoryValueDropItem();
		}
		return new CategoryValueDropItem(cat);
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
					
		}else if (proxy.getQuery().getType().getClass().equals(PatrolSummaryQueryType.class)){
			PatrolSummaryQuery q = (PatrolSummaryQuery) proxy.getQuery();
			SumQueryDefinition def = q.getQueryDefinition();
			
			//value filter panel
			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.RATE, //$NON-NLS-1$
					def == null || def.getRateFilter() == null ? null : asDropItems(def.getRateFilter().getFilter(), session));
			//rate filter panel
			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.VALUE, //$NON-NLS-1$
					def == null || def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); 
			//column group by
			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.COLUMN.name(), //$NON-NLS-1$
					def == null || def.getColumnGroupByPart() == null ? null : def.getColumnGroupByPart().getDropItems(session));
			//row group by
			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.ROW.name(), //$NON-NLS-1$
					def == null || def.getRowGroupByPart() == null ? null : def.getRowGroupByPart().getDropItems(session));

			//values
			List<DropItem> items = null;
			if (def != null && def.getValuePart() != null){
				items = def.getValuePart().getDropItems(session);
				for (DropItem i : items){
					if (i instanceof AbstractValueDropItem){
						((AbstractValueDropItem)i).setEncounterRateOptions(PatrolQueryOptions.SUMMARY_ENCOUNTER_RATE_OPTIONS);
					}
				}
			}
			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.VALUE.name(), items);//$NON-NLS-1$
					
			
		}else if(proxy.getQuery().getType().getClass().equals(PatrolGridQueryType.class)){
			PatrolGriddedQuery q = (PatrolGriddedQuery) proxy.getQuery();
			GridQueryDefinition def = q.getQueryDefinition();
			
			
			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.RATE.name(), def.getRateFilter() == null ? null : asDropItems(def.getRateFilter().getFilter(), session)); //$NON-NLS-1$
			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.VALUE.name(), def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); //$NON-NLS-1$
			
			DropItem valueItem = null;
			try{
				valueItem = def.getValuePart().asDropItem(session);
				if (valueItem instanceof AbstractValueDropItem){
					((AbstractValueDropItem)valueItem).setEncounterRateOptions(PatrolQueryOptions.GRID_ENCOUNTER_RATE_OPTIONS);
				}
			}catch(Exception ex){
				QueryPlugIn.log(ex.getMessage(), ex);
				valueItem = new ErrorDropItem(ex.getMessage());
			}
			proxy.setDropItems(PatrolGriddedQueryDefinitionPanel.VALUE_PANEL_ID,
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
