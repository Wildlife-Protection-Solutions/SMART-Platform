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
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.filter.BooleanFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.query.ObservationQueryPlugIn;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.model.ObservationGriddedQuery;
import org.wcs.smart.observation.query.model.ObservationSummaryQuery;
import org.wcs.smart.observation.query.model.filter.WaypointIdFilter;
import org.wcs.smart.observation.query.model.filter.WaypointSourceFilter;
import org.wcs.smart.observation.query.model.filter.WaypointSourceGroupBy;
import org.wcs.smart.observation.query.ui.WaypointCmGroupByViewer;
import org.wcs.smart.observation.query.ui.WaypointSourceGroupByViewer;
import org.wcs.smart.observation.query.ui.itempanel.GeneralContentProvider;
import org.wcs.smart.observation.query.ui.itempanel.GeneralContentProvider.GeneralItem;
import org.wcs.smart.observation.query.ui.itempanel.GriddedItemPanel;
import org.wcs.smart.observation.query.ui.itempanel.SummaryFilterPanel;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.ui.itempanel.SummaryDataModelContentProvider;
import org.wcs.smart.query.common.ui.itempanel.SummaryDmObject;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.WaypointCmFilter;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IGroupByViewer;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.model.summary.ValuePart;
import org.wcs.smart.query.model.summary.WaypointCmGroupBy;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.definition.BasicGridDefinitionPanel;
import org.wcs.smart.query.ui.model.IQueryDropItemFactory;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ErrorDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.util.UuidUtils;
/**
 * Drop item factory for observation queries
 * @author Emily
 *
 */
public class ObservationDropItemFactory extends BasicDropItemFactory implements IQueryDropItemFactory {

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
		}else if (source instanceof GeneralContentProvider.GeneralItem){
			if (queryItemPanelId.equals(SummaryFilterPanel.ID)){
				if (source == GeneralItem.WAYPOINT_SOURCE){
					items = new DropItem[]{createWaypointSourceGroupByDropItem()};
				}else if (source == GeneralItem.CONSERVATION_AREA){
					items = new DropItem[]{super.createConservationAreaGroupByDropItem()};
				}else if (source == GeneralItem.WAYPOINT_CM) {
					items = new DropItem[]{createWaypointCmGroupByDropItem()};
				}
			}else if (source == GeneralItem.WAYPOINT_SOURCE ||
						source == GeneralItem.OBSERVER || 
						source == GeneralItem.WAYPOINT_ID || 
						source == GeneralItem.WAYPOINT_CM){
					items = new DropItem[]{createWaypointSourceFilterDropItem((GeneralContentProvider.GeneralItem)source)};
			}
		}
		return items;		
	}

	public DropItem createWaypointSourceGroupByDropItem(){
		return new WaypointSourceGroupByDropItem();
	}
	
	public DropItem createWaypointCmGroupByDropItem(){
		return new WaypointCmGroupByDropItem();
	}
	public DropItem createWaypointSourceFilterDropItem(GeneralContentProvider.GeneralItem source){
		if (source == GeneralContentProvider.GeneralItem.WAYPOINT_SOURCE){
			return new WaypointListOpFilterDropItem(WaypointListOpFilterDropItem.Type.SOURCE);
		}else if (source == GeneralContentProvider.GeneralItem.WAYPOINT_CM){
				return new WaypointListOpFilterDropItem(WaypointListOpFilterDropItem.Type.CM);
		}else if (source == GeneralContentProvider.GeneralItem.WAYPOINT_ID){
				return new WaypointIdFilterDropItem();
		}else if (source == GeneralContentProvider.GeneralItem.OBSERVER){
			return createObserverDropItem();
		}
		throw new IllegalStateException(MessageFormat.format(Messages.ObservationDropItemFactory_QueryItemNotSupported, new Object[]{source.guiName}));
	}
	
	
	
	/*
	 * Creates a drop item from a SummaryDmObject
	 */
	private DropItem createSummaryDmDropItem(SummaryDmObject object) {
		if (object.isValue()) {
			if (object.getObject() instanceof Attribute attribute) {
				if (attribute.getType() == AttributeType.NUMERIC || attribute.getType().isGeometry()) {
					return createAttributeValueDropItem(attribute);
				}
				return null;
			} else if (object.getObject() instanceof CategoryAttribute catatt) {
				if (catatt.getAttribute().getType() == AttributeType.NUMERIC || catatt.getAttribute().getType().isGeometry()){
					return createAttributeValueDropItem(catatt);
				}
				return null;
			} else if (object.getObject() instanceof Category category) {
				return createCategoryValueDropItem(category);
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
				if (((Attribute)object.getObject()).getType().isList() ){
					return createAttributeListGroupByDropItem((Attribute) object.getObject());
				}
			} else if (object.getObject() instanceof CategoryAttribute) {
				if ( ((CategoryAttribute)object.getObject()).getAttribute().getType().isList() ){
					return createAttributeListGroupByDropItem((CategoryAttribute) object.getObject());
				}
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
		try{
		if (proxy.getQuery() instanceof SimpleQuery){
			
			IFilter queryFilter = ((SimpleQuery)proxy.getQuery()).getFilter().getFilter();
			proxy.setDropItems(BasicFilterDefintionPanel.ID, asDropItems(queryFilter, session));
					
		}else if (proxy.getQuery().getTypeKey().equals(ObservationSummaryQuery.KEY)){
			ObservationSummaryQuery q = (ObservationSummaryQuery) proxy.getQuery();
			SumQueryDefinition def = q.getQueryDefinition();
			
			proxy.setDropItems(BasicFilterDefintionPanel.ID, def == null || def.getValueFilter() == null || def.getValueFilter().getFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); 
			
			proxy.setDropItems(ObservationSummaryGroupByValuePanel.ID + "." + ObservationSummaryGroupByValuePanel.ListTargetType.COLUMN.name(), //$NON-NLS-1$
					def == null || def.getColumnGroupByPart() == null ? null : 
						groupByToDropItems(def.getColumnGroupByPart(), session));
			proxy.setDropItems(ObservationSummaryGroupByValuePanel.ID + "." + ObservationSummaryGroupByValuePanel.ListTargetType.ROW.name(), //$NON-NLS-1$
					def == null || def.getRowGroupByPart() == null ? null : 
						groupByToDropItems(def.getRowGroupByPart(), session));
			proxy.setDropItems(ObservationSummaryGroupByValuePanel.ID + "." + ObservationSummaryGroupByValuePanel.ListTargetType.VALUE.name(), //$NON-NLS-1$
					def == null || def.getValuePart() == null ? null : 
						valuePartToDropItems(def.getValuePart(), session));						
			
		}else if(proxy.getQuery().getTypeKey().equalsIgnoreCase(ObservationGriddedQuery.KEY)){
			ObservationGriddedQuery q = (ObservationGriddedQuery) proxy.getQuery();
			GridQueryDefinition def = q.getQueryDefinition();
			
			proxy.setDropItems(BasicFilterDefintionPanel.ID, def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session));
			
			DropItem valueItem = null;
			try{
				valueItem = valueItemToDropItem(def.getValuePart(), session);
			}catch(Exception ex){
				ObservationQueryPlugIn.log(ex.getMessage(), ex);
				valueItem = new ErrorDropItem(ex.getMessage());
			}
			proxy.setDropItems(BasicGridDefinitionPanel.ID + BasicGridDefinitionPanel.VALUE_PANEL_SUFFIX,
					Collections.singletonList(valueItem));
		}
		}catch (Exception ex){
			ObservationQueryPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	
	/*
	 * Converts a filter to a set of drop items
	 */
	private List<DropItem> asDropItems(IFilter filter, Session session){
		List<DropItem> items = new ArrayList<DropItem>();
		try{
			DropItem[] filterItems = filterToDropItem(filter, session);
			for(DropItem i : filterItems){
				items.add(i);
			}
			
		}catch (Exception ex){
			items.add(new ErrorDropItem(MessageFormat.format(Messages.SimpleQuery_DropItemParseError, new Object[]{ex.getMessage()})));
		}
		return items;
	}
	
	@Override
	public DropItem[] filterToDropItem(IFilter f, Session session) throws Exception{
		if (f instanceof WaypointSourceFilter){
			return createDropItems((WaypointSourceFilter)f, session);
		}else if (f instanceof WaypointCmFilter){
			return createDropItems((WaypointCmFilter)f, session);			
		}else if (f instanceof WaypointIdFilter) {
			return createDropItems((WaypointIdFilter)f, session);
		}else if (f instanceof BooleanFilter){
			return createDropItems((BooleanFilter)f, session);
		}
		return super.filterToDropItem(f, session);
	}
	
	public DropItem[] createDropItems(BooleanFilter exp, Session session) throws Exception{
		DropItem[] its1 = filterToDropItem(exp.getFilter1(), session);
		DropItem opDropItem = BasicDropItemFactory.createBooleanOpDropItem();
		opDropItem.initializeData(exp.getOperator().asSmartValue());
		
		DropItem[] its2 = filterToDropItem(exp.getFilter2(), session);
		
		DropItem[] results = new DropItem[its1.length + its2.length + 1];
		for (int i = 0; i < its1.length; i ++){
			results[i] = its1[i];
		}
		results[its1.length] = opDropItem;
		for (int i = 0; i < its2.length; i++){
			results[its1.length + i + 1] = its2[i];
		}
		return results;
	}

	private DropItem[] createDropItems(WaypointIdFilter filter, Session session) throws Exception {
		DropItem di = new WaypointIdFilterDropItem();
		di.initializeData(new Object[]{filter.getOperator(), filter.getWaypointIdFilter()});
		return new DropItem[]{di};
	}
	
	private DropItem[] createDropItems(WaypointSourceFilter filter, Session session) throws Exception {
		IWaypointSource src = WaypointSourceEngine.INSTANCE.getSource(filter.getWaypointSourceKey());
		DropItem di;
		if (src == null){
			di = new ErrorDropItem(MessageFormat.format(Messages.WaypointSourceFilter_InvalidSourceFilter, new Object[]{filter.getWaypointSourceKey()}));
		}else{
	
			di = new WaypointListOpFilterDropItem(WaypointListOpFilterDropItem.Type.SOURCE);
			di.initializeData(new Object[]{filter.getOperator(), src});
		}
		return new DropItem[]{di};
	}
	
	private DropItem[] createDropItems(WaypointCmFilter filter, Session session) throws Exception {
		
		DropItem di = new WaypointListOpFilterDropItem(WaypointListOpFilterDropItem.Type.CM);
		
		if (filter.getValue().equals(IFilter.NULL_OP)){
			di.initializeData(new Object[] {new ListItem(null, null, IFilter.NULL_OP)});
		}else {
			try {
				UUID cmuuid = UuidUtils.stringToUuid(filter.getValue());
				ConfigurableModel m = session.get(ConfigurableModel.class, cmuuid);				
				if (m == null) throw new Exception(Messages.ObservationDropItemFactory_CmNotFound);
				m.getName();
				di.initializeData(new Object[] {m});
			}catch (Exception ex) {
				di = new ErrorDropItem(MessageFormat.format(Messages.ObservationDropItemFactory_CmError, ex.getMessage()));

			}
		}
		return new DropItem[] {di};
	}
	
	/**
	 * Converts all value items to drop items.
	 * @param session
	 * @return
	 */
	public List<DropItem> valuePartToDropItems(ValuePart part, Session session) {
		ArrayList<DropItem> item = new ArrayList<DropItem>();
		try{
			for (IValueItem valueItem : part.getValueItems()){
				item.add(valueItemToDropItem(valueItem, session));
			}
		}catch (Exception ex){
			QueryPlugIn.log(ex.getMessage(), ex);
			item.clear();
			item.add(new ErrorDropItem(ex.getMessage()));
		}
		return item;

	}
	
	/**
	 * Converts the group by part into a collection of
	 * drop items.
	 * 
	 * @param session
	 * @return
	 */
	public List<DropItem> groupByToDropItems(GroupByPart groupBy, Session session) {
		ArrayList<DropItem> item = new ArrayList<DropItem>();
		try{
			for (IGroupBy groupByItem : groupBy.getGroupBys()){
				item.add(groupByToDropItem(groupByItem, session));
			}
		}catch (Exception ex){
			QueryPlugIn.log(ex.getMessage(), ex);
			item.clear();
			item.add(new ErrorDropItem(ex.getMessage()));
		}
		return item;
	}
	
	@Override
	public IGroupByViewer<?> findViewer(IGroupBy groupBy){
		if (groupBy instanceof WaypointSourceGroupBy){
			return new WaypointSourceGroupByViewer((WaypointSourceGroupBy) groupBy);
		}else if (groupBy instanceof WaypointCmGroupBy) {
			return new WaypointCmGroupByViewer((WaypointCmGroupBy) groupBy);
		}
		return super.findViewer(groupBy);
	}
}