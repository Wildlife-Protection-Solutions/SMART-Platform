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
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.ext.IExtensionFilter;
import org.wcs.smart.patrol.query.ext.IExtensionFilterViewer;
import org.wcs.smart.patrol.query.ext.IExtensionGroupBy;
import org.wcs.smart.patrol.query.ext.IExtensionGroupByViewer;
import org.wcs.smart.patrol.query.ext.PatrolContributionFinder;
import org.wcs.smart.patrol.query.hibernate.PatrolQueryHibernateManager;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.parser.internal.filter.PatrolFilter;
import org.wcs.smart.patrol.query.parser.internal.filter.PatrolUuidFilter;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolAttributeValueItem;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolCategoryValueItem;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.patrol.query.ui.PatrolOptionData;
import org.wcs.smart.patrol.query.ui.definition.PatrolGriddedQueryDefinitionPanel;
import org.wcs.smart.patrol.query.ui.definition.PatrolSummaryGroupByValuePanel;
import org.wcs.smart.patrol.query.ui.definition.SimpleValueRateFilterPanel;
import org.wcs.smart.patrol.query.ui.definition.dropItems.BooleanPatrolDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolDropItems;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolGroupByDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolIdDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolListDropItem;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolValueDropItem;
import org.wcs.smart.patrol.query.ui.itempanel.GriddedFilterPanel;
import org.wcs.smart.patrol.query.ui.itempanel.SummaryFilterPanel;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.ui.itempanel.SummaryDataModelContentProvider;
import org.wcs.smart.query.common.ui.itempanel.SummaryDmObject;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.BooleanExpression;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IGroupByViewer;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.model.summary.ValuePart;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.definition.ValueRateFilterDeifnitionPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeListValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeTreeValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeValueDropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.CategoryValueDropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.SharedUtils;

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

		} else if (source instanceof PatrolQueryOption) {
			if (queryItemPanelId == SummaryFilterPanel.ID){
				items = new DropItem[]{createPatrolGroupByDropItem((PatrolQueryOption) source)};
			}else{
				items = new DropItem[]{createPatrolFilterDropItem((PatrolQueryOption) source)};
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
		}else if (source instanceof IExtensionFilterViewer){
			items = new DropItem[]{((IExtensionFilterViewer) source).asDropItem()};
		}else if (source instanceof IExtensionGroupByViewer){
			items = new DropItem[]{((IExtensionGroupByViewer) source).asDropItem()};
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
	public DropItem createPatrolGroupByDropItem(PatrolQueryOption item){
		DropItem di = new PatrolGroupByDropItem(item);
		di.initializeData(new Object[]{new PatrolOptionData(item)});
		return di;
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
		if (item instanceof PatrolListDropItem &&
				option instanceof PatrolQueryOption){
			item.initializeData(new Object[]{new PatrolOptionData((PatrolQueryOption)option)});
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
				if ( ((CategoryAttribute)object.getObject()).getAttribute().getType() == AttributeType.LIST ){
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
	 * Creates anew attribute value drop item
	 * @param att
	 * @return
	 */
	@Override
	public DropItem createAttributeValueDropItem(Attribute att){
		return new AttributeValueDropItem(true, att);
	}
	
	/**
	 * Creates a new category attribute value drop item
	 * @param catatt
	 * @return
	 */
	@Override
	public DropItem createAttributeValueDropItem(CategoryAttribute catatt){
		return new AttributeValueDropItem(true, catatt);
	}
	
	/**
	 * Creates a new attribute list drop item
	 * @param item
	 * @return
	 */
	@Override
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item){
		return new AttributeListValueDropItem(true, item);
	}
	
	/**
	 * Creates a new attribute list item associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	@Override
	public DropItem createAttributeListItemValueDropItem(AttributeListItem item, Category cat){
		return new AttributeListValueDropItem(true, item,cat);
	}
	
	/**
	 * Creates a new attribute tree node drop item
	 * @param item
	 * @return
	 */
	@Override
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item ){
		return new AttributeTreeValueDropItem(true, item);
	}
	
	/**
	 * Creates a new attribute tree node associated with a category
	 * @param item
	 * @param cat
	 * @return
	 */
	@Override
	public DropItem createAttributeTreeNodeValueDropItem(AttributeTreeNode item, Category cat){
		return new AttributeTreeValueDropItem(true, item,cat);
	}
	
	/**
	 * Creates a category value drop item
	 * @param cat
	 * @return
	 */
	@Override
	public DropItem createCategoryValueDropItem(Category cat){
		if (cat == null){
			return new CategoryValueDropItem(true);
		}
		return new CategoryValueDropItem(true, cat);
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

		}else if (proxy.getQuery().getTypeKey().equals(PatrolSummaryQuery.KEY)){
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
					def == null || def.getColumnGroupByPart() == null ? null :
						groupByToDropItems(def.getColumnGroupByPart(), session));
						
			//row group by
			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.ROW.name(), //$NON-NLS-1$
					def == null || def.getRowGroupByPart() == null ? null : 
						groupByToDropItems(def.getRowGroupByPart(), session));

			//values
			List<DropItem> items = null;
			if (def != null && def.getValuePart() != null){
				items = valuePartToDropItems(def.getValuePart(),session);
				for (DropItem i : items){
					if (i instanceof AbstractValueDropItem){
						((AbstractValueDropItem)i).setEncounterRateOptions(PatrolDropItems.SUMMARY_ENCOUNTER_RATE_DROP_OPTIONS);
					}
				}
			}
			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.VALUE.name(), items);//$NON-NLS-1$
					
			
		}else if(proxy.getQuery().getTypeKey().equals(PatrolGriddedQuery.KEY)){
			PatrolGriddedQuery q = (PatrolGriddedQuery) proxy.getQuery();
			GridQueryDefinition def = q.getQueryDefinition();
			
			
			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.RATE.name(), def.getRateFilter() == null ? null : asDropItems(def.getRateFilter().getFilter(), session)); //$NON-NLS-1$
			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.VALUE.name(), def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); //$NON-NLS-1$
			
			DropItem valueItem = null;
			try{
				valueItem = valueItemToDropItem(def.getValuePart(), session);
				if (valueItem instanceof AbstractValueDropItem){
					((AbstractValueDropItem)valueItem).setEncounterRateOptions(PatrolDropItems.GRID_ENCOUNTER_RATE_DROP_OPTIONS);
				}
			}catch(Exception ex){
				QueryPlugIn.log(ex.getMessage(), ex);
				valueItem = new ErrorDropItem(ex.getMessage());
			}
			proxy.setDropItems(PatrolGriddedQueryDefinitionPanel.VALUE_PANEL_ID,
					Collections.singletonList(valueItem));	
		}
		}catch (Exception ex){
			PatrolQueryPlugIn.log(ex.getMessage(), ex);
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
		if (f instanceof PatrolFilter){
			return createDropItems((PatrolFilter)f, session);
		}else if (f instanceof PatrolUuidFilter){
			return createDropItems((PatrolUuidFilter)f, session);
		}else if (f instanceof IExtensionFilter){
			return createDropItems((IExtensionFilter)f, session);
		}else if (f instanceof BooleanExpression){
			return createDropItems((BooleanExpression)f, session);
		}
		return super.filterToDropItem(f, session);
		
	}
	
	public DropItem[] createDropItems(BooleanExpression exp, Session session) throws Exception{
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
	@Override
	public DropItem valueItemToDropItem(IValueItem item, Session session) throws Exception{
		if (item instanceof PatrolAttributeValueItem){
			return asDropItem((PatrolAttributeValueItem) item, session);
		}else if (item instanceof PatrolCategoryValueItem){
			return asDropItem((PatrolCategoryValueItem) item, session);
		}else if (item instanceof PatrolValueItem){
			return asDropItem((PatrolValueItem) item, session);
		}
		return super.valueItemToDropItem(item, session);
	}
	
	
	public DropItem asDropItem(PatrolValueItem item, Session session) throws Exception{
		return createPatrolValueDropItem(item.getPatrolValueOption());
	}
	
	public DropItem asDropItem(PatrolCategoryValueItem item, Session session) throws Exception{
		try{
			String categoryHkey = item.getCategoryHKey();
			DropItem di = null;
			if (categoryHkey == null){
				di = PatrolDropItemFactory.INSTANCE.createCategoryValueDropItem(null);
			}else{
				Category category = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
				if (category == null){
					throw new Exception(MessageFormat.format(Messages.PatrolCategoryValueItem_CategoryNotFound, new Object[]{categoryHkey}));
				}
				category.getFullCategoryName();		//cache this
				di = PatrolDropItemFactory.INSTANCE.createCategoryValueDropItem(category);
			}
			
			di.initializeData(new Object[]{getInitializeData(item), null});
			return di;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
		
	}
	public DropItem asDropItem(PatrolAttributeValueItem item, Session session) throws Exception{
		String attributeKey = item.getAttributeKey();
		String categoryKey = item.getCategoryKey();
		String itemKey = item.getItemKey();
		
		Attribute.AttributeType attributeType = item.getAttributeType();
		
		try{
			Attribute att = QueryDataModelManager.getInstance().getAttribute(session,attributeKey);
			if (att == null){
				throw new Exception(MessageFormat.format(Messages.PatrolAttributeValueItem_AttributeNotFound, new Object[]{attributeKey}));
			}
			DropItem di = null;
			Category cat = null;
			if (categoryKey != null){
				cat = QueryDataModelManager.getInstance().getCategory(session, categoryKey);
				if (cat == null){
					throw new Exception(MessageFormat.format(Messages.PatrolAttributeValueItem_CategoryNotFound, new Object[]{categoryKey}));
				}
				cat.getFullCategoryName();			
			}
			if (attributeType == AttributeType.NUMERIC){
				if (cat == null){
					di = PatrolDropItemFactory.INSTANCE.createAttributeValueDropItem(att);
				}else{
					di = PatrolDropItemFactory.INSTANCE.createAttributeValueDropItem(new CategoryAttribute(cat, att));
				}
			}else if (attributeType == AttributeType.LIST){
				AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, itemKey);
				if (ali == null){
					throw new Exception(MessageFormat.format(Messages.PatrolAttributeValueItem_ListItemNotFound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = PatrolDropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali);
				}else{
					di = PatrolDropItemFactory.INSTANCE.createAttributeListItemValueDropItem(ali,cat);
				}
			
			}else if (attributeType == AttributeType.TREE){
				AttributeTreeNode atn = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, itemKey);
				if (atn == null){
					throw new Exception(MessageFormat.format(Messages.PatrolAttributeValueItem_TreeNodeNotFound, new Object[]{attributeKey, itemKey}));		
				}
				if (cat == null){
					di = PatrolDropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn);
				}else{
					di = PatrolDropItemFactory.INSTANCE.createAttributeTreeNodeValueDropItem(atn,cat);
				}
			}
			if (di != null){
				di.initializeData(new Object[]{getInitializeData(item), null});
			}
			return di;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
	}
	
	
	private DropItem[] createDropItems(PatrolUuidFilter f, Session session) throws Exception {
		return null;
	}
	
	private DropItem[] createDropItems(IExtensionFilter f, Session session) throws Exception {
		for (IExtensionFilterViewer b : PatrolContributionFinder.getFilterUiContributions()){
			if (b.getFilterClass().isAssignableFrom(f.getClass())){
				return b.getDropItems(f, session);
			}
		}
		return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.PatrolDropItemFactory_ProcessError, f.asString()))};
		
	
	}
	private DropItem[] createDropItems(PatrolFilter f, Session session) throws Exception {
		PatrolQueryOption option = f.getPatrolOption();
		Object value = f.getValue();
		
		DropItem it = PatrolDropItemFactory.INSTANCE
				.createPatrolFilterDropItem(option);

		String value1 = null;
		if (value != null) {
			value1 = SharedUtils.stripQuotes((String) value);
		}
		if (option == PatrolQueryOption.ID) {
			it.initializeData(new String[] { f.getOperator().getGuiValue(), value1 });
		} else if (option == PatrolQueryOption.MANDATE) {
			ListItem m = PatrolQueryHibernateManager.getInstance()
					.getPatrolMandate(session, value1);
			if (m == null) {
				it = new ErrorDropItem(MessageFormat.format(
						Messages.PatrolFilter_MandateNotFound,
						new Object[] { value1 }));
			} else {
				it.initializeData(new Object[]{new PatrolOptionData(option), m});
			}
		} else if (option == PatrolQueryOption.STATION) {
			ListItem m = PatrolQueryHibernateManager.getInstance().getStation(
					session, value1);
			if (m == null) {
				it = new ErrorDropItem(MessageFormat.format(
						Messages.PatrolFilter_StationNotFound,
						new Object[] { value1 }));
			} else {
				it.initializeData(new Object[]{new PatrolOptionData(option), m});
			}
		} else if (option == PatrolQueryOption.TEAM) {
			ListItem m = PatrolQueryHibernateManager.getInstance().getTeam(
					session, value1);
			if (m == null) {
				it = new ErrorDropItem(MessageFormat.format(
						Messages.PatrolFilter_TeamNotFound,
						new Object[] { value1 }));
			} else {
				it.initializeData(new Object[]{new PatrolOptionData(option), m});
			}
		} else if (option == PatrolQueryOption.TEAM_KEY
				|| option == PatrolQueryOption.PATROL_TRANSPORT_TYPE_KEY
				|| option == PatrolQueryOption.MANDATE_KEY) {
			List<ListItem> items = null;
			// TODO: should get all not just active
			if (option == PatrolQueryOption.TEAM_KEY) {
				items = PatrolQueryHibernateManager.getInstance()
						.getActiveTeams(session);
			} else if (option == PatrolQueryOption.PATROL_TRANSPORT_TYPE_KEY) {
				items = PatrolQueryHibernateManager.getInstance()
						.getActiveTransportTypes(session);
			} else if (option == PatrolQueryOption.MANDATE_KEY) {
				items = PatrolQueryHibernateManager.getInstance()
						.getActiveMandates(session);
			}
			boolean found = false;
			if (items != null) {
				for (ListItem item : items) {
					if (item.getKey().equals(value1)) {
						it.initializeData(new Object[]{new PatrolOptionData(option), item});
						found = true;
						break;
					}
				}
			}
			if (!found) {
				it = new ErrorDropItem(MessageFormat.format(
						Messages.PatrolFilter_TeamNotFound,
						new Object[] { value1 }));
			}
		} else if (option == PatrolQueryOption.PATROL_TRANSPORT_TYPE) {
			ListItem m = PatrolQueryHibernateManager.getInstance()
					.getTransportType(session, value1);
			if (m == null) {
				it = new ErrorDropItem(MessageFormat.format(
						Messages.PatrolFilter_TransportTypeNotFound,
						new Object[] { value1 }));
			} else {
				it.initializeData(new Object[]{new PatrolOptionData(option), m});
			}

		} else if (option == PatrolQueryOption.PATROL_TYPE) {
			PatrolType.Type t = PatrolType.Type.valueOf(value1);
			ListItem m = new ListItem(null, t.getGuiName(Locale.getDefault()), t.name());
			it.initializeData(new Object[]{new PatrolOptionData(option), m});
		} else if (option == PatrolQueryOption.EMPLOYEE
				|| option == PatrolQueryOption.LEADER
				|| option == PatrolQueryOption.PILOT) {
			ListItem m = PatrolQueryHibernateManager.getInstance().getEmployee(
					session, value1);
			if (m == null) {
				it = new ErrorDropItem(MessageFormat.format(
						Messages.PatrolFilter_EmployeeNotFound,
						new Object[] { value1 }));
			} else {
				it.initializeData(new Object[]{new PatrolOptionData(option), m});
			}
		}
		return new DropItem[] { it };
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
		if (groupBy instanceof IExtensionGroupBy){
			for (IExtensionGroupByViewer v : PatrolContributionFinder.getGroupByUiContributions()){
				if (v.getGroupByClass().isAssignableFrom(groupBy.getClass())){
					return v.createViewer(groupBy);
				}
			}
			return null;
		}
		if (groupBy instanceof PatrolGroupBy){
			return new PatrolGroupByViewer((PatrolGroupBy) groupBy, new PatrolOptionData(((PatrolGroupBy)groupBy).getOption()));
		}
		return super.findViewer(groupBy);
	}
	
	@Override
	public DropItem groupByToDropItem(IGroupBy item, Session session) throws Exception{
		return findViewer(item).asDropItem(session);
	}
}
