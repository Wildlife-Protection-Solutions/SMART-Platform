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
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.ui.itempanel.GriddedItemPanel;
import org.wcs.smart.observation.query.ui.itempanel.QueryFilterContentProvider;
import org.wcs.smart.observation.query.ui.itempanel.SummaryDmObject;
import org.wcs.smart.observation.query.ui.itempanel.SummaryFilterPanel;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.AllCategory;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.ui.definition.ValueRateFilterDeifnitionPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.AttributeListValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeTreeValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeValueDropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.CategoryValueDropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

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
		
		if (source instanceof QueryFilterContentProvider.OtherItems) {
			items = createOtherDropItem((QueryFilterContentProvider.OtherItems)source);
		} else if (source instanceof IDateGroupBy) {
			items = new DropItem[]{createDateGroupByDropItem(
							(IDateGroupBy) source)};
		
		} else if (source instanceof SummaryDmObject) {
			items = new DropItem[]{createSummaryDmDropItem((SummaryDmObject)source)};
			
		}else if (source instanceof AreaType){
			if (queryItemPanelId == SummaryFilterPanel.ID){
				items = new DropItem[]{createAreaGroupByDropItem((AreaType)source)};
			}
		}else if (source instanceof Area){
			if (queryItemPanelId == SummaryFilterPanel.ID){
				items = new DropItem[]{createAreaGroupByDropItem((Area)source)};
			}
		}else if (source instanceof AllCategory){
			if (queryItemPanelId == SummaryFilterPanel.ID ||
					queryItemPanelId == GriddedItemPanel.ID){
				items = new DropItem[]{createCategoryValueDropItem(null)};
			}
		}
		return items;
		
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
	private DropItem[] createOtherDropItem(QueryFilterContentProvider.OtherItems other){
		if (other == QueryFilterContentProvider.OtherItems.BRACKETS){
			return createBracketIems();
		}else if (other == QueryFilterContentProvider.OtherItems.NOT){
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
		
//		if (proxy.getQuery() instanceof SimpleQuery){
//			
//			IFilter queryFilter = ((SimpleQuery)proxy.getQuery()).getFilter().getFilter();
//			proxy.setDropItems(SimplePatrolFilterPanel.ID, asDropItems(queryFilter, session));
//					
//		}else if (proxy.getQuery().getType().getClass().equals(PatrolSummaryQueryType.class)){
//			PatrolSummaryQuery q = (PatrolSummaryQuery) proxy.getQuery();
//			SumQueryDefinition def = q.getQueryDefinition();
//			
//			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.RATE, def.getRateFilter() == null ? null : asDropItems(def.getRateFilter().getFilter(), session)); //$NON-NLS-1$
//			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.VALUE, def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); //$NON-NLS-1$
//			
//			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.COLUMN.name(), //$NON-NLS-1$
//					def.getColumnGroupByPart() == null ? null : def.getColumnGroupByPart().getDropItems(session));
//			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.ROW.name(), //$NON-NLS-1$
//					def.getRowGroupByPart() == null ? null : def.getRowGroupByPart().getDropItems(session));
//			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.VALUE.name(), //$NON-NLS-1$
//					def.getValuePart() == null ? null : def.getValuePart().getDropItems(session));
//			
//		}else if(proxy.getQuery().getType().getClass().equals(PatrolGridQueryType.class)){
//			PatrolGriddedQuery q = (PatrolGriddedQuery) proxy.getQuery();
//			GridQueryDefinition def = q.getQueryDefinition();
//			
//			
//			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.RATE.name(), def.getRateFilter() == null ? null : asDropItems(def.getRateFilter().getFilter(), session)); //$NON-NLS-1$
//			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.VALUE.name(), def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); //$NON-NLS-1$
//			
//			DropItem valueItem = null;
//			try{
//				valueItem = def.getValuePart().asDropItem(session);
//			}catch(Exception ex){
//				QueryPlugIn.log(ex.getMessage(), ex);
//				valueItem = new ErrorDropItem(ex.getMessage());
//			}
//			proxy.setDropItems(PatrolGriddedQueryDefinitionPanel.VALUE_PANEL_ID,
//					Collections.singletonList(valueItem));
//					
//			
//		}
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