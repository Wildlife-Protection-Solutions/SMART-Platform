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
package org.wcs.smart.entity.query.ui.definition;

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
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.EntityQueryPlugIn;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.model.EntityGriddedQuery;
import org.wcs.smart.entity.query.model.EntitySummaryQuery;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeGroupBy;
import org.wcs.smart.entity.query.parser.internal.EntityTypeFilter;
import org.wcs.smart.entity.query.ui.EntityAttributeGroupByViewer;
import org.wcs.smart.entity.query.ui.itempanel.ConservationAreaTreeNode;
import org.wcs.smart.entity.query.ui.itempanel.EntityGriddedItemPanel;
import org.wcs.smart.entity.query.ui.itempanel.EntityQueryFilterPanel;
import org.wcs.smart.entity.query.ui.itempanel.EntitySummaryItemPanel;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.query.model.filter.WaypointSourceFilter;
import org.wcs.smart.observation.query.model.filter.WaypointSourceGroupBy;
import org.wcs.smart.observation.query.ui.WaypointSourceGroupByViewer;
import org.wcs.smart.observation.query.ui.definition.ObservationDropItemFactory;
import org.wcs.smart.observation.query.ui.definition.WaypointSourceFilterDropItem;
import org.wcs.smart.observation.query.ui.itempanel.GeneralContentProvider;
import org.wcs.smart.observation.query.ui.itempanel.GeneralContentProvider.GeneralItem;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.ui.itempanel.SummaryDataModelContentProvider;
import org.wcs.smart.query.common.ui.itempanel.SummaryDmObject;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.AttributeFilter;
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
import org.wcs.smart.query.ui.definition.BasicGridDefinitionPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
/**
 * Drop item factory for observation queries
 * @author Emily
 *
 */
public class EntityDropItemFactory extends BasicDropItemFactory implements IDropItemFactory {

	public static EntityDropItemFactory INSTANCE = new EntityDropItemFactory();
	
	protected EntityDropItemFactory(){
		
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
		}else if (source instanceof ConservationAreaTreeNode){
			items = new DropItem[]{createConservationAreaGroupByDropItem()};
		} else if (source instanceof SummaryDmObject) {
			items = new DropItem[]{createSummaryDmDropItem((SummaryDmObject)source)};
			
		}else if (source instanceof AreaType){
			if (queryItemPanelId.equals(EntitySummaryItemPanel.ID)){
				items = new DropItem[]{createAreaGroupByDropItem((AreaType)source)};
			}
		}else if (source instanceof Area){
			if (queryItemPanelId.equals(EntitySummaryItemPanel.ID)){
				items = new DropItem[]{createAreaGroupByDropItem((Area)source)};
			}
		}else if (source == SummaryDataModelContentProvider.DataModelItem.CATEGORIES_VALUE){
			if (queryItemPanelId.equals(EntitySummaryItemPanel.ID) ||
					queryItemPanelId.equals(EntityGriddedItemPanel.ID)){
				items = new DropItem[]{createCategoryValueDropItem(null)};
			}
		}else if (source instanceof EntityAttribute){
			if (queryItemPanelId.equals(EntityQueryFilterPanel.ID)){
				items = new DropItem[]{createEntityAttributeDropItem((EntityAttribute)source)};
			}
		}else if (source instanceof EntityType){
			if (queryItemPanelId.equals(EntityQueryFilterPanel.ID)){
				items = new DropItem[]{createEntityTypeDropItem((EntityType)source)};
			}
		}else if (source instanceof GeneralContentProvider.GeneralItem){
			if (queryItemPanelId.equals(EntitySummaryItemPanel.ID)){
				if (source == GeneralItem.WAYPOINT_SOURCE){
					items = new DropItem[]{ObservationDropItemFactory.INSTANCE.createWaypointSourceGroupByDropItem()};
				}else if (source == GeneralItem.CONSERVATION_AREA){
					items = new DropItem[]{super.createConservationAreaGroupByDropItem()};
				}
			}else{
				if (source == GeneralItem.WAYPOINT_SOURCE){
					items = new DropItem[]{ObservationDropItemFactory.INSTANCE.createWaypointSourceFilterDropItem((GeneralContentProvider.GeneralItem)source)};
				}
			}
		}
		return items;
		
	}

	public DropItem createEntityAttributeDropItem(EntityAttribute ea){
		if (ea.getDmAttribute().getType() == Attribute.AttributeType.LIST){
			return new EntityAttributeListDropItem(ea);
		}else if (ea.getDmAttribute().getType() == Attribute.AttributeType.TREE){
			return new EntityAttributeTreeDropItem(ea);
		}
		return new EntityAttributeDropItem(ea);
	}
	
	public DropItem createEntityTypeDropItem(EntityType et){
		return new EntityTypeDropItem(et);
	}
	
	public DropItem createEntityAttributeListGroupByDropItem(EntityAttribute ea){
		return new EntityAttributeListGroupByDropItem(ea);
	}
	
	public DropItem createEntityAttributeTreeNodeGroupByDropItem(EntityAttribute ea, int level){
		return new EntityAttributeTreeGroupByDropItem(ea, level);
	}

	public DropItem createEntityAttributeTreeNodeGroupByDropItem(EntityAttribute ea, AttributeTreeNode node){
		return new EntityAttributeTreeGroupByDropItem(ea, node);
	}
	
	/*
	 * Creates a drop item from a SummaryDmObject
	 */
	private DropItem createSummaryDmDropItem(SummaryDmObject object) {
		if (object.isValue()) {
			if (object.getObject() instanceof Attribute ) {
				if (((Attribute)object.getObject()).getType() == AttributeType.NUMERIC){
					return createAttributeValueDropItem(
						(Attribute) object.getObject());
				}
				return null;
			}else if (object.getObject() instanceof EntityAttribute ) {
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
					if (object.getObject2() instanceof Category){
						return createAttributeListItemValueDropItem((AttributeListItem)object.getObject(),(Category)object.getObject2());
					}
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
			}else if (object.getObject() instanceof EntityAttribute){
				if (((EntityAttribute)object.getObject()).getDmAttribute().getType() == AttributeType.LIST ){
					return createEntityAttributeListGroupByDropItem((EntityAttribute) object.getObject());
				}
			} else if (object.getObject() instanceof CategoryAttribute) {
				if ( ((CategoryAttribute)object.getObject()).getAttribute().getType() == AttributeType.LIST ){
					return createAttributeListGroupByDropItem((CategoryAttribute) object.getObject());
				}
			} else if (object.getObject() instanceof AttributeTreeNode) {
				
				if (object.getObject2() != null) {
					if (object.getObject2() instanceof Category){
						return createAttributeTreeNodeGroupByDropItem(
									(AttributeTreeNode) object.getObject(),
									(Category) object.getObject2());
					}else if (object.getObject2() instanceof EntityAttribute){
						return createEntityAttributeTreeNodeGroupByDropItem((EntityAttribute)object.getObject2(), 
								(AttributeTreeNode)object.getObject());
					}
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
						
			}else if (proxy.getQuery().getTypeKey().equals(EntitySummaryQuery.KEY)){
				EntitySummaryQuery q = (EntitySummaryQuery) proxy.getQuery();
				SumQueryDefinition def = q.getQueryDefinition();
				
				proxy.setDropItems(BasicFilterDefintionPanel.ID, def == null || def.getValueFilter() == null || def.getValueFilter().getFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); 
				
				proxy.setDropItems(EntitySummaryGroupByValuePanel.ID + "." + EntitySummaryGroupByValuePanel.ListTargetType.COLUMN.name(), //$NON-NLS-1$
						def == null || def.getColumnGroupByPart() == null ? null :groupByToDropItems(def.getColumnGroupByPart(), session));
				proxy.setDropItems(EntitySummaryGroupByValuePanel.ID + "." + EntitySummaryGroupByValuePanel.ListTargetType.ROW.name(), //$NON-NLS-1$
						def == null || def.getRowGroupByPart() == null ? null : groupByToDropItems(def.getRowGroupByPart(), session));
				proxy.setDropItems(EntitySummaryGroupByValuePanel.ID + "." + EntitySummaryGroupByValuePanel.ListTargetType.VALUE.name(), //$NON-NLS-1$
						def == null || def.getValuePart() == null ? null : valuePartToDropItems(def.getValuePart(), session));	
				
			}else if(proxy.getQuery().getTypeKey().equalsIgnoreCase(EntityGriddedQuery.KEY)){
				EntityGriddedQuery q = (EntityGriddedQuery) proxy.getQuery();
				GridQueryDefinition def = q.getQueryDefinition();
				
				proxy.setDropItems(BasicFilterDefintionPanel.ID, def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session));
				
				DropItem valueItem = null;
				try{
					valueItem = valueItemToDropItem(def.getValuePart(), session);
				}catch(Exception ex){
					EntityQueryPlugIn.log(ex.getMessage(), ex);
					valueItem = new ErrorDropItem(ex.getMessage());
				}
				proxy.setDropItems(BasicGridDefinitionPanel.ID + BasicGridDefinitionPanel.VALUE_PANEL_SUFFIX,
						Collections.singletonList(valueItem));
			}
		}catch (Exception ex){
			EntityQueryPlugIn.displayLog(ex.getMessage(), ex);
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
		if (f instanceof EntityAttributeFilter){
			return createDropItems((EntityAttributeFilter)f, session);
		}else if (f instanceof EntityTypeFilter){
			return createDropItems((EntityTypeFilter)f, session);
		}else if (f instanceof WaypointSourceFilter){
			return createDropItems((WaypointSourceFilter)f, session);
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
	private DropItem[] createDropItems(WaypointSourceFilter filter, Session session) throws Exception {
		
		IWaypointSource src = WaypointSourceEngine.INSTANCE.getSource(filter.getWaypointSourceKey());
		DropItem di;
		if (src == null){
			di = new ErrorDropItem(MessageFormat.format(Messages.EntityDropItemFactory_SourceNotFound, new Object[]{filter.getWaypointSourceKey()}));
		}else{
	
			di = new WaypointSourceFilterDropItem();
			di.initializeData(new Object[]{filter.getOperator(), src});
		}
		return new DropItem[]{di};
	}
	
	private DropItem[] createDropItems(EntityAttributeFilter filter, Session session) throws Exception {
        try{
            EntityAttribute ea = getEntityAttribute(filter.getEntityKey(), filter.getEntityAttributeKey(), session);
            if (ea == null){
                throw new Exception(MessageFormat.format(Messages.EntityAttributeFilter_EntityAttributeNotFound, new Object[]{filter.getEntityAttributeKey(), filter.getEntityKey()}));
            }
           
            DropItem it = EntityDropItemFactory.INSTANCE.createEntityAttributeDropItem(ea);
           
            Attribute.AttributeType attributeType = filter.getAttributeType();
            Object value1 = filter.getValue();
            if (attributeType == AttributeType.TEXT ||
                    attributeType == AttributeType.NUMERIC){
                it.initializeData(new String[]{filter.getOperator().getGuiValue(), String.valueOf(value1)});
            }else if (attributeType == AttributeType.LIST){
                ListItem li = null;
                if (AttributeFilter.ANY_OPTION_KEY.equals((String)value1)){
                    li = ANY_OPTION;
                }else{
                    try{
                        AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, ea.getDmAttribute().getKeyId(), (String)value1);
                        if (ali == null){
                            throw new IllegalStateException(MessageFormat.format(Messages.EntityAttributeFilter_ListItemNotFound, new Object[]{(String)value1, ea.getDmAttribute().getKeyId()}));
                        }
                        li = new ListItem(ali.getUuid(), ali.getName(), ali.getKeyId());
                    }catch (Exception ex){
                        throw new IllegalStateException(ex.getMessage());
                    }
                }
                it.initializeData(li);
            }else if (attributeType == AttributeType.TREE){
                try{
                    AttributeTreeNode ali = QueryDataModelManager.getInstance().getAttributeTreeNode(session, ea.getDmAttribute().getKeyId(), (String)value1);
                    if (ali == null){
                        throw new IllegalStateException(MessageFormat.format(Messages.EntityAttributeFilter_TreeNodeNotFound, new Object[]{(String)value1, ea.getDmAttribute().getKeyId()}));
                    }
                    it.initializeData(ali);
                }catch (Exception ex){
                    throw new IllegalStateException(ex.getMessage());
                }

            }else if (attributeType == AttributeType.DATE){
                it.initializeData(new String[]{(String)value1, (String)filter.getValue2(), filter.getOperator().getGuiValue()});
            }
            
            return new DropItem[]{it};
        }catch (Exception ex){
            return new DropItem[]{new ErrorDropItem(ex.getMessage())};
        }
	}
	
	private DropItem[] createDropItems(EntityTypeFilter filter, Session session) throws Exception {

		try{
			EntityType ea = getEntityType(filter.getEntityTypeKey(), session);
			if (ea == null){
				throw new Exception(MessageFormat.format(Messages.EntityTypeFilter_EntityTypeNotFound, new Object[]{filter.getEntityTypeKey()}));
			}
			DropItem it = EntityDropItemFactory.INSTANCE.createEntityTypeDropItem(ea);
			
			super.initAttributeDropItem(filter, it, session);
			return new DropItem[]{it};
		}catch (Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
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
		}else if (groupBy instanceof EntityAttributeGroupBy){
			return new EntityAttributeGroupByViewer((EntityAttributeGroupBy) groupBy);
		}
		return super.findViewer(groupBy);
	}
	
	/**
	 * Loads the attribute from the database
	 * @param session
	 * @return
	 * @throws Exception
	 */
	private EntityAttribute getEntityAttribute(String entityKey, String entityAttributeKey, Session session) throws Exception{
		return EntityHibernateManager.getInstance().getEntityAttribute(entityKey, entityAttributeKey, session);
	}
	
	/**
	 * Loads the attribute from the database
	 * @param session
	 * @return
	 * @throws Exception
	 */
	private EntityType getEntityType(String entityKey, Session session) throws Exception{
		return EntityHibernateManager.getInstance().getEntityType(entityKey, session);
	}
}