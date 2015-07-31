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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.LabelConstants;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.hibernate.SurveyDesignFilter;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.MissionTrack.TrackType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.filter.CmCategoryAttributeFilter;
import org.wcs.smart.er.query.filter.MissionFilter;
import org.wcs.smart.er.query.filter.MissionFilter.Type;
import org.wcs.smart.er.query.filter.MissionMemberFilter;
import org.wcs.smart.er.query.filter.MissionPropertyFilter;
import org.wcs.smart.er.query.filter.SamplingUnitAttributeFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SurveyFilter;
import org.wcs.smart.er.query.filter.TrackTypeFilter;
import org.wcs.smart.er.query.filter.summary.MissionAttributeGroupBy;
import org.wcs.smart.er.query.filter.summary.MissionIdGroupBy;
import org.wcs.smart.er.query.filter.summary.MissionValueItem;
import org.wcs.smart.er.query.filter.summary.MissionValueItem.ValueItem;
import org.wcs.smart.er.query.filter.summary.SamplingUnitAttributeGroupBy;
import org.wcs.smart.er.query.filter.summary.SamplingUnitGroupBy;
import org.wcs.smart.er.query.filter.summary.SurveyIdGroupBy;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.er.query.ui.filter.summary.MissionAttributeGroupByViewer;
import org.wcs.smart.er.query.ui.filter.summary.MissionIdGroupByViewer;
import org.wcs.smart.er.query.ui.filter.summary.SamplingUnitAttributeGroupByViewer;
import org.wcs.smart.er.query.ui.filter.summary.SamplingUnitGroupByViewer;
import org.wcs.smart.er.query.ui.filter.summary.SurveyIdGroupByViewer;
import org.wcs.smart.er.query.ui.panels.definition.FilterDefintionPanel;
import org.wcs.smart.er.query.ui.panels.definition.GriddedDefinitionPanel;
import org.wcs.smart.er.query.ui.panels.definition.SimpleValueRateFilterPanel;
import org.wcs.smart.er.query.ui.panels.definition.SummaryDefinitionPanel;
import org.wcs.smart.er.query.ui.panels.definition.TrackFilterDefinitionPanel;
import org.wcs.smart.er.query.ui.panels.item.FilterContentProvider;
import org.wcs.smart.er.query.ui.panels.item.FilterItemPanel;
import org.wcs.smart.er.query.ui.panels.item.GriddedValueItemPanel;
import org.wcs.smart.er.query.ui.panels.item.GroupByValueItemPanel;
import org.wcs.smart.er.query.ui.panels.item.MissionTrackFilterItemPanel;
import org.wcs.smart.er.query.ui.panels.item.SamplingUnitWrapper;
import org.wcs.smart.er.query.ui.panels.item.SurveyGroupByContentProvider;
import org.wcs.smart.er.query.ui.panels.item.SurveyValuesTreeNode;
import org.wcs.smart.er.query.ui.panels.item.TrackObservationFilterItemPanel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.ui.itempanel.SummaryDataModelContentProvider;
import org.wcs.smart.query.common.ui.itempanel.SummaryDmObject;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.CombinedValueItem;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IGroupByViewer;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.model.summary.ValuePart;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.IValueDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeListDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeListValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeTreeDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeTreeValueDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeValueDropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.CategoryDropItem;
import org.wcs.smart.query.ui.model.impl.CategoryValueDropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Drop item factory for survey queries.
 * @author Emily
 *
 */
public class SurveyDropItemFactory extends BasicDropItemFactory implements IDropItemFactory {

	public static SurveyDropItemFactory INSTANCE = new SurveyDropItemFactory();
	
	public final static IValueDropItem[] SUMMARY_ENCOUNTER_RATE_ITEMS;
	public final static IValueDropItem[] GRID_ENCOUNTER_RATE_ITEMS;
	static{
		SUMMARY_ENCOUNTER_RATE_ITEMS =  new IValueDropItem[] {
				(IValueDropItem)INSTANCE.createMissionLengthValueItem(),
				(IValueDropItem)INSTANCE.createTotalMissionLengthValueItem(),
				(IValueDropItem)INSTANCE.createMissionCountValueItem(),
				(IValueDropItem)INSTANCE.createTotalMissionCountValueItem(),
				(IValueDropItem)INSTANCE.createSurveyCountValueItem(),
				(IValueDropItem)INSTANCE.createTotalSurveyCountValueItem()
		};
		GRID_ENCOUNTER_RATE_ITEMS =  new IValueDropItem[] {
				(IValueDropItem)INSTANCE.createMissionLengthValueItem(),
				(IValueDropItem)INSTANCE.createMissionCountValueItem(),
				(IValueDropItem)INSTANCE.createSurveyCountValueItem(),
		};
	}
	
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
			items = new DropItem[]{createDateGroupByDropItem((IDateGroupBy) source)};		
		} else if (source instanceof SummaryDmObject) {
			items = new DropItem[]{createSummaryDmDropItem((SummaryDmObject)source)};
		}else if (source instanceof Area){
			if (queryItemPanelId.equals(FilterItemPanel.ID) ||
					queryItemPanelId.equals(TrackObservationFilterItemPanel.ID)){
				items = new DropItem[]{ createAreaDropItem((Area)source, AreaFilter.AreaFilterGeometryType.WAYPOINT) };
			}else if (queryItemPanelId.equals(MissionTrackFilterItemPanel.ID) ){
				items = new DropItem[]{ createAreaDropItem((Area)source, AreaFilter.AreaFilterGeometryType.TRACK) };
			}else if (queryItemPanelId.equals(GroupByValueItemPanel.ID)){
				items = new DropItem[]{createAreaGroupByDropItem((Area)source)};
			}
		}else if (source instanceof AreaType){
			if (queryItemPanelId.equals(GroupByValueItemPanel.ID)){
				items = new DropItem[]{createAreaGroupByDropItem((AreaType)source)};
			}
		}else if (source == SummaryDataModelContentProvider.DataModelItem.CATEGORIES_VALUE){
			if (queryItemPanelId.equals(GroupByValueItemPanel.ID) ||
					queryItemPanelId.equals(GriddedValueItemPanel.ID)){
				items = new DropItem[]{createCategoryValueDropItem(null)};
			}
		}else if (source == FilterContentProvider.Node.SURVEY_ID){
			items = new DropItem[]{createSurveyIdDropItem()};
		}else if (source == FilterContentProvider.Node.MISSION_ID){
			items = new DropItem[]{createMissionIdDropItem()};
		}else if (source == FilterContentProvider.Node.OBSERVER){
			items = new DropItem[]{createObserverDropItem()};
		}else if (source == FilterContentProvider.Node.MISSION_LEADER){
			items = new DropItem[]{createMissionLeaderDropItem()};
		}else if (source == FilterContentProvider.Node.MISSION_MEMBER){
			items = new DropItem[]{createMissionMemberDropItem()};
		}else if (source instanceof MissionTrack.TrackType){
			items = new DropItem[]{new TrackTypeDropItem((TrackType) source)};
		}else if (source instanceof Survey){
			items = new DropItem[]{createSurveyUuidIdDropItem((Survey)source)};
		}else if (source instanceof Mission){
			items = new DropItem[]{createMissionUuidIdDropItem((Mission)source)};
			
		}else if (source instanceof MissionAttribute){
			if (queryItemPanelId.equals(FilterItemPanel.ID) ||
					queryItemPanelId.equals(MissionTrackFilterItemPanel.ID) || 
					queryItemPanelId.equals(TrackObservationFilterItemPanel.ID)){
				items = new DropItem[]{createMissionAttributeDropItem((MissionAttribute)source)};
			}else if (queryItemPanelId.equals(GroupByValueItemPanel.ID)){
				items = new DropItem[]{createMissionAttributeGroupByDropItem((MissionAttribute)source)};
			}
			
		}else if (source instanceof MissionProperty){
			if (queryItemPanelId.equals(FilterItemPanel.ID) ||
					queryItemPanelId.equals(MissionTrackFilterItemPanel.ID) ||
					queryItemPanelId.equals(TrackObservationFilterItemPanel.ID)){
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
		}else if (source == SurveyGroupByContentProvider.Node.OBSERVER){
			items = new DropItem[]{createObserverGroupByDropItem()};
		}else if (source instanceof SamplingUnit){
			
			if (queryItemPanelId.equals(FilterItemPanel.ID) ||
					queryItemPanelId.equals(MissionTrackFilterItemPanel.ID) ){
				items = new DropItem[]{createSamplingUnitDropItem((SamplingUnit)source)};
			}
		}else if (source instanceof SamplingUnitWrapper){
			SamplingUnitWrapper wrapper = (SamplingUnitWrapper)source;
			if (wrapper.getSamplingUnit() instanceof SamplingUnit){
				items = new DropItem[]{createSamplingUnitDropItem((SamplingUnit)wrapper.getSamplingUnit(), wrapper.getSource())};
			}else if(wrapper.getSamplingUnit() instanceof SamplingUnitAttribute){
				items = new DropItem[]{createSamplingUnitAttributeDropItem((SamplingUnitAttribute)wrapper.getSamplingUnit(), wrapper.getSource())};
			}
			
		}else if (source instanceof SamplingUnitAttribute){
			if (queryItemPanelId.equals(FilterItemPanel.ID) ||
					queryItemPanelId.equals(MissionTrackFilterItemPanel.ID) ){
				items = new DropItem[]{createSamplingUnitAttributeDropItem((SamplingUnitAttribute) source)};
			}else if (queryItemPanelId.equals(GroupByValueItemPanel.ID)){
				items = new DropItem[]{createSamplingUnitAttributeGroupByDropItem((SamplingUnitAttribute)source)};
			}
			
		}else if (source instanceof MissionTrack){
			if (queryItemPanelId.equals(FilterItemPanel.ID) ||
					queryItemPanelId.equals(MissionTrackFilterItemPanel.ID) ){
				items = new DropItem[]{createSamplingUnitDropItem((MissionTrack)source)};
			}
			
		}else if (source instanceof SurveyValuesTreeNode.Node){
			if (source == SurveyValuesTreeNode.Node.MISSION_LENGTH){
				items = new DropItem[]{createMissionLengthValueItem()};
			}else if (source == SurveyValuesTreeNode.Node.MISSION_COUNT){
				items = new DropItem[]{createMissionCountValueItem()};
			}else if (source == SurveyValuesTreeNode.Node.SURVEY_COUNT){
				items = new DropItem[]{createSurveyCountValueItem()};
			}else if (source == SurveyValuesTreeNode.Node.MISSION_DAY_COUNT){
				items = new DropItem[]{createMissionDayCountValueItem()};
			}else if (source == SurveyValuesTreeNode.Node.MISSION_HOUR_COUNT){
				items = new DropItem[]{createMissionHourCountValueItem()};
			}else if (source == SurveyValuesTreeNode.Node.MISSION_PERSONHOUR_COUNT){
				items = new DropItem[]{createMissionPersonHourCountValueItem()};
			}
		}else if (source instanceof CmNode){
			DropItem di = processCmNode((CmNode)source);
			if (di != null){
				items = new DropItem[]{di};
			}
		}else if (source instanceof CmAttribute){
			DropItem di = createCmAttribute((CmAttribute)source);
			if (di != null){
				items = new DropItem[]{di};
			}
		}
		return items;	
	}
	
	public DropItem createCmAttribute(final CmAttribute node){
		//need our own connection to load category
		final DropItem[] di = new DropItem[]{null};
		Job j = new Job(""){ //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					Category c = (Category) s.load(Category.class, node.getNode().getCategory().getUuid());
					Attribute att = (Attribute) s.load(Attribute.class, node.getAttribute().getUuid());
					if (att.getType() == AttributeType.BOOLEAN ||
							att.getType() == AttributeType.TEXT ||
							att.getType() == AttributeType.NUMERIC ||
							att.getType() == AttributeType.DATE){
						di[0] = createAttributeDropItem(new CategoryAttribute(c, att));
					}else if (att.getType() == AttributeType.LIST){
						CmAttributeListDropItem ddi = new CmAttributeListDropItem(node, new CategoryAttribute(c, att));
						di[0] = ddi;
					}else if (att.getType() == AttributeType.TREE){
						//for this version we are just going to treat this like the attribute from the datamodel;
						//in the next version (with fully customized trees) we'll look at displaying the custom tree here.
//						di[0] = new AttributeTreeDropItem(new CategoryAttribute(c,att));
						di[0] = new CmAttributeTreeDropItem(node, new CategoryAttribute(c, att));
					}
				}finally{
					s.close();
				}
				return Status.OK_STATUS;
			}
		};
		j.setSystem(false);
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return di[0];
		
	}
	private DropItem processCmNode(final CmNode node){
		if (node.getCategory() != null){
			//need our own connection to load category
			final DropItem[] di = new DropItem[]{null};
			Job j = new Job("") { //$NON-NLS-1$
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						di[0] = createCategoryDropItem( (Category)s.load(Category.class, node.getCategory().getUuid()));
					}finally{
						s.close();
					}
					return Status.OK_STATUS;
				}
			};
			j.setSystem(true);
			j.schedule();
			try {
				j.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return di[0];
		}
		return null;
	}
	public DropItem createMissionLengthValueItem(){
		return new MissionValueDropItem(MissionValueItem.ValueItem.TRACK_LENGTH);
	}
	
	public DropItem createTotalMissionLengthValueItem(){
		return new MissionValueDropItem(MissionValueItem.ValueItem.TRACK_LENGTH_TOTAL);
	}
	
	public DropItem createMissionCountValueItem(){
		return new MissionValueDropItem(MissionValueItem.ValueItem.MISSION_COUNT);
	}
	
	public DropItem createTotalMissionCountValueItem(){
		return new MissionValueDropItem(MissionValueItem.ValueItem.MISSION_COUNT_TOTAL);
	}
	
	public DropItem createMissionDayCountValueItem(){
		return new MissionValueDropItem(MissionValueItem.ValueItem.DAY_COUNT);
	}
	
	public DropItem createMissionHourCountValueItem(){
		return new MissionValueDropItem(MissionValueItem.ValueItem.HOUR_COUNT);
	}
	
	
	public DropItem createMissionPersonHourCountValueItem(){
		return new MissionValueDropItem(MissionValueItem.ValueItem.MANHOURS_COUNT);
	}
	
	public DropItem createSurveyCountValueItem(){
		return new MissionValueDropItem(MissionValueItem.ValueItem.SURVEY_COUNT);
	}
	public DropItem createTotalSurveyCountValueItem(){
		return new MissionValueDropItem(MissionValueItem.ValueItem.SURVEY_COUNT_TOTAL);
	}
	
	public DropItem createSamplingUnitGroupByDropItem(){
		return new SamplingUnitGroupByDropItem();
	}
	
	public DropItem createSamplingUnitDropItem(SamplingUnit unit, SamplingUnitFilter.Source source){
		return new SamplingUnitDropItem(unit, source);
	}
	
	public DropItem createSamplingUnitDropItem(SamplingUnit unit){
		return new SamplingUnitDropItem(unit);
	}
	
	public DropItem createSamplingUnitDropItem(MissionTrack mt){
		return new SamplingUnitDropItem(mt);
	}
	
	public DropItem createSamplingUnitAttributeDropItem(SamplingUnitAttribute attribute){
		return new SamplingUnitAttributeDropItem(attribute);
	}
	
	public DropItem createSamplingUnitAttributeDropItem(SamplingUnitAttribute attribute, SamplingUnitFilter.Source source){
		return new SamplingUnitAttributeDropItem(attribute, source);
	}
	
	public DropItem createMissionAttributeGroupByDropItem(MissionAttribute attribute){
		return new MissionAttributeGroupByDropItem(attribute);
	}

	public DropItem createSamplingUnitAttributeGroupByDropItem(SamplingUnitAttribute attribute){
		return new SamplingUnitAttributeGroupByDropItem(attribute);
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
	 * Creates a mission member drop item
	 * @return
	 */
	public DropItem createMissionMemberDropItem(){
		return new MissionMemberDropItem(false);
	}
	
	/**
	 * Creates a mission leader drop item
	 * @return
	 */
	public DropItem createMissionLeaderDropItem(){
		return new MissionMemberDropItem(true);
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
		
		if (proxy.getQuery() instanceof SimpleQuery){
			try{
				IFilter queryFilter = ((SimpleQuery)proxy.getQuery()).getFilter().getFilter();
			
				if (proxy.getQuery().getTypeKey().equals(MissionTrackQuery.KEY)){
					proxy.setDropItems(TrackFilterDefinitionPanel.ID, asDropItems(queryFilter, session));
				}else{
					proxy.setDropItems(FilterDefintionPanel.ID, asDropItems(queryFilter, session));	
				}
			}catch(Exception ex){
				EcologicalRecordsPlugIn.displayLog(ex.getMessage(), ex);
			}
					
		}else if (proxy.getQuery().getTypeKey().equals(SurveySummaryQuery.KEY)){
			try{
				SurveySummaryQuery q = (SurveySummaryQuery) proxy.getQuery();
				SumQueryDefinition def = q.getQueryDefinition();
				
				//value filter panel
				proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + SimpleValueRateFilterPanel.PanelType.RATE,  //$NON-NLS-1$
						def == null || def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session));
	//			//rate filter panel
				proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + SimpleValueRateFilterPanel.PanelType.VALUE, //$NON-NLS-1$
						def == null || def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); 
				//column group by
				proxy.setDropItems(SummaryDefinitionPanel.ID + "." + SummaryDefinitionPanel.ListTargetType.COLUMN.name(), //$NON-NLS-1$
						def == null || def.getColumnGroupByPart() == null ? null : groupByToDropItems(def.getColumnGroupByPart(), session));
				//row group by
				proxy.setDropItems(SummaryDefinitionPanel.ID + "." + SummaryDefinitionPanel.ListTargetType.ROW.name(), //$NON-NLS-1$
						def == null || def.getRowGroupByPart() == null ? null : groupByToDropItems(def.getRowGroupByPart(), session));
	
				//values
				List<DropItem> items = null;
				if (def != null && def.getValuePart() != null){
					items = valuePartToDropItems(def.getValuePart(), session);
					for (DropItem i : items){
						if (i instanceof AbstractValueDropItem){
							((AbstractValueDropItem)i).setEncounterRateOptions(SUMMARY_ENCOUNTER_RATE_ITEMS);
						}
					}
				}
				proxy.setDropItems(SummaryDefinitionPanel.ID + "." + SummaryDefinitionPanel.ListTargetType.VALUE.name(), items);//$NON-NLS-1$
			}catch(Exception ex){
				EcologicalRecordsPlugIn.displayLog(ex.getMessage(), ex);
			}	
//			
		}else if(proxy.getQuery().getTypeKey().equals(SurveyGriddedQuery.KEY)){
			SurveyGriddedQuery q = (SurveyGriddedQuery) proxy.getQuery();
			DropItem valueItem = null;
			try{
				GridQueryDefinition def = q.getQueryDefinition();			
				proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + SimpleValueRateFilterPanel.PanelType.RATE.name(), def.getRateFilter() == null ? null : asDropItems(def.getRateFilter().getFilter(), session)); //$NON-NLS-1$
				proxy.setDropItems(SimpleValueRateFilterPanel.ID + "." + SimpleValueRateFilterPanel.PanelType.VALUE.name(), def.getValueFilter() == null ? null : asDropItems(def.getValueFilter().getFilter(), session)); //$NON-NLS-1$
			
				valueItem = valueItemToDropItem(def.getValuePart(), session);
				if (valueItem instanceof AbstractValueDropItem){
					((AbstractValueDropItem)valueItem).setEncounterRateOptions(GRID_ENCOUNTER_RATE_ITEMS);
				}
			}catch(Exception ex){
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
				valueItem = new ErrorDropItem(ex.getMessage());
			}
			proxy.setDropItems(GriddedDefinitionPanel.VALUE_PANEL_ID,
					Collections.singletonList(valueItem));			
		}
	}
	
	public DropItem[] filterToDropItem(IFilter f, Session session) throws Exception{
		if (f instanceof CmCategoryAttributeFilter){
			return createDropItems((CmCategoryAttributeFilter)f, session);
		}else if (f instanceof MissionFilter){
			return createDropItems((MissionFilter)f, session);
		}else if (f instanceof MissionMemberFilter){
			return createDropItems((MissionMemberFilter)f, session);
		}else if (f instanceof MissionPropertyFilter){
			return createDropItems((MissionPropertyFilter)f, session);
		}else if (f instanceof SamplingUnitAttributeFilter){
			return createDropItems((SamplingUnitAttributeFilter)f, session);
		}else if (f instanceof SamplingUnitFilter){
			return createDropItems((SamplingUnitFilter)f, session);
		}else if (f instanceof SurveyDesignFilter){
			return null;
		}else if (f instanceof SurveyFilter){
			return createDropItems((SurveyFilter)f, session);
		}else if (f instanceof TrackTypeFilter){
			return createDropItems((SurveyFilter)f, session);
		}
		return null;
		
	}
	
	
	public DropItem valueItemToDropItem(IValueItem item, Session session) throws Exception{
		if (item instanceof MissionValueItem){
			return createDropItems((CategoryValueItem)item, session);
			
		}else if (item instanceof CombinedValueItem){
			return createDropItems((MissionValueItem)item, session);	
		}
		return null;
		
	}
	
	public DropItem[] createDropItems(CmCategoryAttributeFilter af, Session session) throws Exception{
		Category c = null;
		Attribute att = null;
		
		try{
			c = getCategory(af.getCategoryFilter().getCategoryKey(), session);
			att = getAttribute(af.getAttributeFilter().getAttributeKey(), session);

			CmAttribute node = (CmAttribute) session.load(CmAttribute.class, af.getCmAttributeUuid());
			
			boolean found = false;
			for (Attribute a : QueryDataModelManager.getInstance().getAttributes(session, c.getHkey())){
				if (a.getKeyId().equals(att.getKeyId())){
					found = true;
					break;
				}
			}
			if (!found){
				throw new Exception(MessageFormat.format(Messages.CmCategoryAttributeFilter_invalidCatAttribute, new Object[]{c.getKeyId(), att.getKeyId()}));
			}
		
			DropItem it = null;
			if (node != null){
				it = createCmAttribute(node);
			}else{
				//for whatever reason this configurable model node doesn't exist anymore but the category/attribute combination does so lets just create
				//a normal category/attribute drop item
				CategoryAttribute ca = new CategoryAttribute(c,  att);
				it = createAttributeDropItem(ca);
			}
			initAttributeDropItem(af.getAttributeFilter(), it, session);
			return new DropItem[]{it};
		}catch (Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
		
	}
	
	

	public DropItem[] createDropItems(MissionFilter filter, Session session) throws Exception{
		if (filter.getType() == Type.ID){
			DropItem di = createMissionIdDropItem();
			di.initializeData(new String[]{filter.getOperator().asSmartValue(), filter.getValue()});
			return new DropItem[]{di};
		}else if (filter.getType() == Type.UUID){
			try{
				Mission mission = (Mission) session.get(Mission.class, UuidUtils.stringToUuid(filter.getValue()));
				if (mission == null){
					return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.MissionFilter_MissionNotFound, new Object[]{filter.getValue()}))};
				}
				mission.getId();
				mission.getSurvey().getId();
				return new DropItem[]{createMissionUuidIdDropItem(mission)};
			}catch (Exception ex){
				ERQueryPlugIn.log(ex.getMessage(), ex);
				return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.MissionFilter_MissionNotFound, new Object[]{filter.getValue()}))};
			}
		}
		return null;
	}
	
	

	public DropItem[] createDropItems(MissionMemberFilter exp, Session session) throws Exception{
		Employee e = (Employee) session.get(Employee.class, exp.getUuid());
		if (e == null){
			return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.MissionMemberFilter_EmployeeNotFound, new Object[]{UuidUtils.uuidToString(exp.getUuid())}))};
		}
		LabelConstants.getFullLabel(e);

		DropItem di = null;
		if (exp.isLeader() ){
			di = createMissionLeaderDropItem();
		}else{
			di = createMissionMemberDropItem();
		}
		di.initializeData(e);
		
		return new DropItem[]{di};
	}
	

	public DropItem[] createDropItems(MissionPropertyFilter filter, Session session) throws Exception{
		try{
			Object value = filter.getValue();
			Attribute.AttributeType type = filter.getAttributeType();
			
			MissionAttribute ma = (MissionAttribute) session.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", filter.getAttributeKey())).list().get(0); //$NON-NLS-1$
			DropItem di = SurveyDropItemFactory.INSTANCE.createMissionAttributeDropItem(ma);
			
			if (type.equals(AttributeType.NUMERIC) ||
					type.equals(AttributeType.TEXT)){
				di.initializeData(new String[]{filter.getOperator().asSmartValue(), value.toString()}); 
			}else if (type.equals(AttributeType.LIST)){
				
				boolean ok = false;
				if (value.equals(AttributeFilter.ANY_OPTION_KEY)){
					di.initializeData(ANY_OPTION);
					ok = true;
				}else{
					for (MissionAttributeListItem item : ma.getAttributeList()){
						if (item.getKeyId().equals(value)){
							ok = true;
							di.initializeData(new ListItem(item.getUuid(), item.getName(), item.getKeyId()));
						}
					}
				}
				if (!ok){
					return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.MissionAttributeFilter_ListItemNotFoundError, new Object[]{filter.getAttributeKey()}))};		
				}
			}
			return new DropItem[]{di};
		}catch (Exception ex){
			ERQueryPlugIn.log(ex.getMessage(), ex);
			return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.MissionAttributeFilter_AttributeNotFoundError, new Object[]{filter.getAttributeKey()}))};
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getDropItems(org.hibernate.Session)
	 * 
	 * @return {@link AttributeDropItem} or {@link AttributeListDropItem} 
	 * or {@link AttributeTreeDropItem} depending on 
	 * attribute type.
	 */
	public DropItem[] createDropItems(SamplingUnitAttributeFilter filter, Session session) throws Exception {
		String samplingUnitAttributeKey = filter.getSamplingUnitAttributeKey();
		Attribute.AttributeType type = filter.getAttributeType();
		Object value = filter.getValue();
		
		try{
			SamplingUnitAttribute sa = (SamplingUnitAttribute) session.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", samplingUnitAttributeKey)).list().get(0); //$NON-NLS-1$
			DropItem di = SurveyDropItemFactory.INSTANCE.createSamplingUnitAttributeDropItem(sa, filter.getSource());
			
			if (type.equals(AttributeType.NUMERIC) ||
					type.equals(AttributeType.TEXT)){
				di.initializeData(new String[]{filter.getOperator().asSmartValue(), value.toString()}); 
			}else if (type.equals(AttributeType.LIST)){
				
				boolean ok = false;
				if (value.equals(AttributeFilter.ANY_OPTION_KEY)){
					di.initializeData(ANY_OPTION);
					ok = true;
				}else{
					for (SamplingUnitAttributeListItem item : sa.getAttributeList()){
						if (item.getKeyId().equals(value)){
							ok = true;
							di.initializeData(new ListItem(item.getUuid(), item.getName(), item.getKeyId()));
						}
					}
				}
				if (!ok){
					return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.SamplingUnitAttributeFilter_ListItemNotFound, new Object[]{samplingUnitAttributeKey}))};		
				}
			}
			return new DropItem[]{di};
		}catch (Exception ex){
			ERQueryPlugIn.log(ex.getMessage(), ex);
			return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.SamplingUnitAttributeFilter_AttributeNotFound, new Object[]{samplingUnitAttributeKey}))};
		}
	}
	
	/**
	 * @return {@link CategoryDropItem} or {@link ErrorDropItem} if 
	 * category cannot be found.
	 */
	public DropItem[] createDropItems(SamplingUnitFilter filter, Session session) throws Exception{
		SamplingUnitFilter.Source joinTable = filter.getSource();
		SamplingUnitFilter.Type unitType = filter.getType();
		String uuid = filter.getUuid();
		if (unitType == SamplingUnitFilter.Type.SAMPLINGUNIT){
			if (uuid.equals(SamplingUnitFilter.NONE_KEY)){
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSamplingUnitDropItem(SamplingUnitFilter.NONE, joinTable)};
			}
			
			SamplingUnit su = (SamplingUnit) session.get(SamplingUnit.class, UuidUtils.stringToUuid(uuid));
			if (su != null){
				su.getId();
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSamplingUnitDropItem(su, joinTable)};
			}else{
				return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.SamplingUnitFilter_SamplingUnitNotFound, new Object[]{uuid}))}; 
			}
		}
		if (unitType == SamplingUnitFilter.Type.TRACK){
			MissionTrack su = (MissionTrack) session.get(MissionTrack.class, UuidUtils.stringToUuid(uuid));
			if (su != null){
				su.getId();
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSamplingUnitDropItem(su)};
			}else{
				return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.SamplingUnitFilter_MissionTrackNotFound, new Object[]{uuid}))}; 
			}
		}
		return null;
	}
	

	public DropItem[] createDropItems(SurveyFilter filter, Session session) throws Exception {
		SurveyFilter.Type type = filter.getType();
		String value = filter.getValue();
		if (type == SurveyFilter.Type.ID){
			DropItem di = SurveyDropItemFactory.INSTANCE.createSurveyIdDropItem();
			di.initializeData(new String[]{filter.getOperator().asSmartValue(), value});
			return new DropItem[]{di};
		}else if (type == SurveyFilter.Type.UUID){
			try{
				Survey s = (Survey) session.get(Survey.class, UuidUtils.stringToUuid(value));
				if (s == null){
					return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.SurveyFilter_SurveyNotFound, new Object[]{value}))};
				}
				s.getId();
				return new DropItem[]{SurveyDropItemFactory.INSTANCE.createSurveyUuidIdDropItem(s)};
			}catch (Exception ex){
				ERQueryPlugIn.log(ex.getMessage(), ex);
				return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.SurveyFilter_SurveyNotFound, new Object[]{value}))};
			}
		}
		return null;
	}
	
	public DropItem[] createDropItems(TrackTypeFilter filter, Session session) throws Exception {
		return new DropItem[]{new TrackTypeDropItem(filter.getTrackType())};
	}

	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	public DropItem createDropItems(MissionValueItem item, Session session) throws Exception{
		if (item.getValueItem() == ValueItem.TRACK_LENGTH){
			return SurveyDropItemFactory.INSTANCE.createMissionLengthValueItem();
		}else if (item.getValueItem() == ValueItem.MISSION_COUNT){
			return SurveyDropItemFactory.INSTANCE.createMissionCountValueItem();
		}else if (item.getValueItem() == ValueItem.SURVEY_COUNT){
			return SurveyDropItemFactory.INSTANCE.createSurveyCountValueItem();
		}else if (item.getValueItem() == ValueItem.DAY_COUNT){
			return SurveyDropItemFactory.INSTANCE.createMissionDayCountValueItem();
		}else if (item.getValueItem() == ValueItem.HOUR_COUNT){
			return SurveyDropItemFactory.INSTANCE.createMissionHourCountValueItem();
		}else if (item.getValueItem() == ValueItem.MANHOURS_COUNT){
			return SurveyDropItemFactory.INSTANCE.createMissionPersonHourCountValueItem();
		}else if (item.getValueItem() == ValueItem.TRACK_LENGTH_TOTAL){
			return SurveyDropItemFactory.INSTANCE.createTotalMissionLengthValueItem();
		}else if (item.getValueItem() == ValueItem.SURVEY_COUNT_TOTAL){
			return SurveyDropItemFactory.INSTANCE.createTotalSurveyCountValueItem();
		}else if (item.getValueItem() == ValueItem.MISSION_COUNT_TOTAL){
			return SurveyDropItemFactory.INSTANCE.createTotalMissionCountValueItem();
		}
		return new ErrorDropItem(Messages.MissionValueItem_ValueItemNotSupported + item.getValueItem().key);
	}
		
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	public DropItem createDropItems(CombinedValueItem item, Session session) throws Exception {
		DropItem d1 = valueItemToDropItem(item.getPart1(), session);
		DropItem d2 = valueItemToDropItem(item.getPart2(), session);
		
		Object[] data = new Object[2];
		data[0] = getInitializeData(item.getPart1());
		data[1] = d2;
		d1.initializeData(data);
		return d1;
	}
	
	public DropItem groupByToDropItem(IGroupBy gb, Session session) throws Exception{
		return findViewer(gb).asDropItem(session);
	}
	
	public IGroupByViewer<?> findViewer(IGroupBy groupBy){
		if (groupBy instanceof MissionAttributeGroupBy){
			return  new MissionAttributeGroupByViewer((MissionAttributeGroupBy) groupBy);
		}else if(groupBy instanceof MissionIdGroupBy){
			return new MissionIdGroupByViewer((MissionIdGroupBy)groupBy);
		}else if (groupBy instanceof SamplingUnitAttributeGroupBy){
			return new SamplingUnitAttributeGroupByViewer((SamplingUnitAttributeGroupBy)groupBy);
		}else if (groupBy instanceof SamplingUnitGroupBy){
			return new SamplingUnitGroupByViewer((SamplingUnitGroupBy)groupBy);
		}else if (groupBy instanceof SurveyIdGroupBy){
			return new SurveyIdGroupByViewer((SurveyIdGroupBy)groupBy);
		}
		return super.findViewer(groupBy);
	}
	
	public List<DropItem> groupByToDropItems(GroupByPart groupBy, Session session) {
		ArrayList<DropItem> item = new ArrayList<DropItem>();
		try{
			for (IGroupBy groupByItem : groupBy.getGroupBys()){
				item.add(groupByToDropItem(groupByItem, session));
			}
		}catch (Exception ex){
			ERQueryPlugIn.log(ex.getMessage(), ex);
			item.clear();
			item.add(new ErrorDropItem(ex.getMessage()));
		}
		return item;
	}
	
	private List<DropItem> asDropItems(IFilter filter, Session session){
		List<DropItem> items = new ArrayList<DropItem>();
		try{
			DropItem[] filterItems = filterToDropItem(filter, session);
			for(DropItem i : filterItems){
				items.add(i);
			}
			
		}catch (Exception ex){
			items.add(new ErrorDropItem(MessageFormat.format(Messages.SurveyDropItemFactory_ParseError, new Object[]{ex.getMessage()})));
		}
		return items;
	}
	
	private List<DropItem> valuePartToDropItems(ValuePart part, Session session) {
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
}
