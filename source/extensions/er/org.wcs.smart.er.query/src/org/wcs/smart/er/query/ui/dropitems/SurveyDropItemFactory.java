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
package org.wcs.smart.er.query.ui.dropitems;

import java.text.MessageFormat;
import java.util.ArrayList;
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
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.panels.definition.FilterDefintionPanel;
import org.wcs.smart.er.query.ui.panels.item.FilterContentProvider;
import org.wcs.smart.er.query.ui.panels.item.FilterItemPanel;
import org.wcs.smart.er.query.ui.panels.item.SurveyGroupByContentProvider;
import org.wcs.smart.er.query.ui.panels.item.GroupByValueItemPanel;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.ui.itempanel.SummaryDmObject;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.AttributeListValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeTreeValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeValueDropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.CategoryValueDropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Drop item factory for survey queries.
 * @author Emily
 *
 */
public class SurveyDropItemFactory extends BasicDropItemFactory implements IDropItemFactory {

	public static SurveyDropItemFactory INSTANCE = new SurveyDropItemFactory();
	
	protected SurveyDropItemFactory(){
		
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
		}else if (source instanceof Area){
			if (queryItemPanelId.equals(FilterItemPanel.ID)){
				items = new DropItem[]{ createAreaDropItem((Area)source, AreaFilter.AreaFilterGeometryType.WAYPOINT) };
			}else if (queryItemPanelId.equals(GroupByValueItemPanel.ID)){
				items = new DropItem[]{createAreaGroupByDropItem((Area)source)};
				
//				if (items != null){
//				for (int i = 0; i < items.length; i ++){
//					if (items[i] instanceof AbstractValueDropItem){
//						((AbstractValueDropItem)items[i]).setEncounterRateOptions(PatrolQueryOptions.SUMMARY_ENCOUNTER_RATE_OPTIONS);
//					}
//				}
//			}
			}
		}else if (source instanceof AreaType){
			if (queryItemPanelId.equals(FilterItemPanel.ID)){
				items = new DropItem[]{createAreaGroupByDropItem((AreaType)source)};
				
//				if (items != null){
//				for (int i = 0; i < items.length; i ++){
//					if (items[i] instanceof AbstractValueDropItem){
//						((AbstractValueDropItem)items[i]).setEncounterRateOptions(PatrolQueryOptions.SUMMARY_ENCOUNTER_RATE_OPTIONS);
//					}
//				}
//			}
			}

//		}else if (source == SummaryDataModelContentProvider.DataModelItem.CATEGORIES_VALUE){
//			if (queryItemPanelId.equals(SummaryFilterPanel.ID) ||
//					queryItemPanelId.equals(GriddedFilterPanel.ID)){
//				items = new DropItem[]{createCategoryValueDropItem(null)};
//			}
		}else if (source == FilterContentProvider.Node.SURVEY_ID){
			items = new DropItem[]{createSurveyIdDropItem()};
		}else if (source == FilterContentProvider.Node.MISSION_ID){
			items = new DropItem[]{createMissionIdDropItem()};
		}else if (source instanceof Survey){
			items = new DropItem[]{createSurveyUuidIdDropItem((Survey)source)};
		}else if (source instanceof Mission){
			items = new DropItem[]{createMissionUuidIdDropItem((Mission)source)};
			
		}else if (source instanceof MissionAttribute){
			if (queryItemPanelId.equals(FilterItemPanel.ID)){
				items = new DropItem[]{createMissionAttributeDropItem((MissionAttribute)source)};
			}else if (queryItemPanelId.equals(GroupByValueItemPanel.ID)){
				items = new DropItem[]{createMissionAttributeGroupByDropItem((MissionAttribute)source)};
			}
			
		}else if (source instanceof MissionProperty){
			if (queryItemPanelId.equals(FilterItemPanel.ID)){
				items = new DropItem[]{createMissionAttributeDropItem(((MissionProperty)source).getAttribute())};
			}else if (queryItemPanelId.equals(GroupByValueItemPanel.ID)){
				items = new DropItem[]{createMissionAttributeGroupByDropItem(((MissionProperty)source).getAttribute())};
			}
		}else if (source == SurveyGroupByContentProvider.Node.MISSION_ID){
			items = new DropItem[]{createMissionIdGroupByDropItem()};
		}else if (source == SurveyGroupByContentProvider.Node.SURVEY_ID){
			items = new DropItem[]{createSurveyIdGroupByDropItem()};
		}else if (source == SurveyGroupByContentProvider.Node.SAMPLING_UNITS){
			items = new DropItem[]{createSamplingUnitGroupByDropItem()};
		
		}else if (source instanceof SamplingUnit){
			if (queryItemPanelId.equals(FilterItemPanel.ID)){
				items = new DropItem[]{createSamplingUnitDropItem((SamplingUnit)source)};
			}
			
		}else if (source instanceof MissionTrack){
			if (queryItemPanelId.equals(FilterItemPanel.ID)){
				items = new DropItem[]{createSamplingUnitDropItem((MissionTrack)source)};
			}
			
		}
		
		return items;	
	}
	public DropItem createSamplingUnitGroupByDropItem(){
		return new SamplingUnitGroupByDropItem();
	}
	public DropItem createSamplingUnitDropItem(SamplingUnit unit){
		return new SamplingUnitDropItem(unit);
	}
	
	public DropItem createSamplingUnitDropItem(MissionTrack mt){
		return new SamplingUnitDropItem(mt);
	}
	
	public DropItem createMissionAttributeGroupByDropItem(MissionAttribute attribute){
		return new MissionAttributeGroupByDropItem(attribute);
	}
	
	public DropItem createSurveyIdGroupByDropItem(){
		return new SurveyIdGroupByDropItem();
	}
	
	public DropItem createMissionIdGroupByDropItem(){
		return new MissionIdGroupByDropItem();
	}
	
	public DropItem createMissionAttributeDropItem(MissionAttribute ma) {
		return new MissionAttributeDropItem(ma);
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
	 * Creates a survey id drop item
	 * @return
	 */
	public DropItem createSurveyIdDropItem(){
		return new SurveyIdDropItem();
	}

	/**
	 * Creates a survey uuid drop item
	 * @param survey
	 * @return
	 */
	public DropItem createSurveyUuidIdDropItem(Survey survey){
		return new SurveyDropItem(survey);
	}
	
	/**
	 * Creates a mission uuid drop item
	 * @param mission
	 * @return
	 */
	public DropItem createMissionUuidIdDropItem(Mission mission){
		return new MissionDropItem(mission);
	}
	
	/**
	 * Creates a mission id drop item
	 * @return
	 */
	public DropItem createMissionIdDropItem(){
		return new MissionIdDropItem();
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
			proxy.setDropItems(FilterDefintionPanel.ID, asDropItems(queryFilter, session));
					
//		}else if (proxy.getQuery().getType().getClass().equals(PatrolSummaryQueryType.class)){
//			PatrolSummaryQuery q = (PatrolSummaryQuery) proxy.getQuery();
//			SumQueryDefinition def = q.getQueryDefinition();
//			
//			//value filter panel
//			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.RATE, //$NON-NLS-1$
//					def == null || def.getRateFilter() == null ? null : asDropItems(def.getRateFilter().getFilter(), session));
//			//rate filter panel
//			proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + ValueRateFilterDeifnitionPanel.PanelType.VALUE, //$NON-NLS-1$
//					def == null || def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); 
//			//column group by
//			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.COLUMN.name(), //$NON-NLS-1$
//					def == null || def.getColumnGroupByPart() == null ? null : def.getColumnGroupByPart().getDropItems(session));
//			//row group by
//			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.ROW.name(), //$NON-NLS-1$
//					def == null || def.getRowGroupByPart() == null ? null : def.getRowGroupByPart().getDropItems(session));
//
//			//values
//			List<DropItem> items = null;
//			if (def != null && def.getValuePart() != null){
//				items = def.getValuePart().getDropItems(session);
//				for (DropItem i : items){
//					if (i instanceof AbstractValueDropItem){
//						((AbstractValueDropItem)i).setEncounterRateOptions(PatrolQueryOptions.SUMMARY_ENCOUNTER_RATE_OPTIONS);
//					}
//				}
//			}
//			proxy.setDropItems(PatrolSummaryGroupByValuePanel.ID + "." + PatrolSummaryGroupByValuePanel.ListTargetType.VALUE.name(), items);//$NON-NLS-1$
//					
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
//				if (valueItem instanceof AbstractValueDropItem){
//					((AbstractValueDropItem)valueItem).setEncounterRateOptions(PatrolQueryOptions.GRID_ENCOUNTER_RATE_OPTIONS);
//				}
//			}catch(Exception ex){
//				QueryPlugIn.log(ex.getMessage(), ex);
//				valueItem = new ErrorDropItem(ex.getMessage());
//			}
//			proxy.setDropItems(PatrolGriddedQueryDefinitionPanel.VALUE_PANEL_ID,
//					Collections.singletonList(valueItem));
//					
//			
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
			items.add(new ErrorDropItem(MessageFormat.format(Messages.SurveyDropItemFactory_ParseError, new Object[]{ex.getMessage()})));
		}
		return items;
	}
}
