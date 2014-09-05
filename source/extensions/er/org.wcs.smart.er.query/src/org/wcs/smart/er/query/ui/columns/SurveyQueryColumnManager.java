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
package org.wcs.smart.er.query.ui.columns;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Survey query column manager for creating
 * and caching query columns;
 * 
 * @author Emily
 *
 */
public class SurveyQueryColumnManager {

	private static SurveyQueryColumnManager instance = null;
	/**
	 * 
	 * @return the manager instance
	 */
	public static SurveyQueryColumnManager getInstance(){
		if (instance == null){
			synchronized (SurveyQueryColumnManager.class) {
				if (instance == null){
					instance = new SurveyQueryColumnManager();
				}
			}			
		}
		return instance;
	}
	
	
	private QueryColumn[] dataModelColumns = null;
	
	private final Object DATAMODELLOCK = new Object();
	
	/*
	 * Creates a new manager
	 */
	private SurveyQueryColumnManager(){
		DataModelManager.getInstance().addChangeListener(new IDataModelListener() {
			@Override
			public void modified() {
				dataModelColumns = null;
			}
		});
	}
	
	
	/**
	 * 
	 * @return query columns available to a waypoint query based
	 * on the survey options and the data model of the conservation
	 * area.
	 */
	//TODO: we could try to be smart here and only include
	//attribute columns in the survey design configurable
	//model
	public  QueryColumn[] getObservationQueryColumns(final SurveyDesign sd) {
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
		
		// survey columns 
		for (SurveyQueryColumn.FixedColumns c : SurveyQueryColumn.FixedColumns.values()){
			boolean add = true;
			if (c == SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||
					c == SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE){
				if (sd != null && !sd.getTrackDistanceDirection()){
					add = false;
				}
			}
			if (c == SurveyQueryColumn.FixedColumns.CA_ID || 
				c == SurveyQueryColumn.FixedColumns.CA_NAME ){
				
				if (!SmartDB.isMultipleAnalysis()){
					add = false;
				}
			}
			
			if (add){
				cols.add(new SurveyQueryColumn(c));
			}
		}
		
		//mission property columns
		if (sd == null){
			Job j = new Job(Messages.SurveyQueryColumnManager_missionattributejobname){

				@SuppressWarnings("unchecked")
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						
						List<MissionAttribute> all = s.createCriteria(MissionAttribute.class)
								.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
						for (MissionAttribute ma : all){
							cols.add(new MissionPropertyQueryColumn(ma));
						}
						
						List<SamplingUnitAttribute> su = s.createCriteria(SamplingUnitAttribute.class)
								.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
								.list();
						for (SamplingUnitAttribute sua : su){
							cols.add(new SamplingUnitAttributeQueryColumn(sua));
						}
						
					}finally{
						s.close();
					}
					return Status.OK_STATUS;
				}
			};
			j.schedule();
			try{
				j.join();
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
		}else{
			Job j = new Job(Messages.SurveyQueryColumnManager_missionattributejobname){
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						SurveyDesign sd2 = (SurveyDesign) s.load(SurveyDesign.class, sd.getUuid());
						for (MissionProperty mp : sd2.getMissionProperties()){
							cols.add(new MissionPropertyQueryColumn(mp.getAttribute()));
						}
						
						@SuppressWarnings("unchecked")
						List<SurveyDesignSamplingUnitAttribute> atts = s.createCriteria(SurveyDesignSamplingUnitAttribute.class)
								.add(Restrictions.eq("id.surveyDesign", sd2)) //$NON-NLS-1$
								.list();
						for (SurveyDesignSamplingUnitAttribute a : atts){
							cols.add(new SamplingUnitAttributeQueryColumn(a.getSamplingUnitAttribute()));
						}
					}finally{
						s.close();
					}
					return Status.OK_STATUS;
				}
			};
			j.schedule();
			try{
				j.join();
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
			
		}
		
		//data model columns
		for (QueryColumn q : getDataModelColumns()){
			cols.add(q);
		}
		
		return cols.toArray(new QueryColumn[cols.size()]);
	}

	
	/**
	 * 
	 * @return query columns available to a waypoint query based
	 * on the survey options and the data model of the conservation
	 * area.
	 */
	public  QueryColumn[] getWaypointQueryColumns(final SurveyDesign sd) {
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
		
		// survey columns 
		for (SurveyQueryColumn.FixedColumns c : SurveyQueryColumn.FixedColumns.values()){
			boolean add = true;
			if (c == SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||
					c == SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE){
				if (sd != null && !sd.getTrackDistanceDirection()){
					add = false;
				}
			}
			if (c == SurveyQueryColumn.FixedColumns.CA_ID || 
				c == SurveyQueryColumn.FixedColumns.CA_NAME ){
				
				if (!SmartDB.isMultipleAnalysis()){
					add = false;
				}
			}
			
			if (add){
				cols.add(new SurveyQueryColumn(c));
			}
		}
		
		//mission property columns
		Job j = new Job(Messages.SurveyQueryColumnManager_missionattributejobname){

			@SuppressWarnings("unchecked")
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					if (sd == null){
						List<MissionAttribute> all = s.createCriteria(MissionAttribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
						for (MissionAttribute ma : all){
							cols.add(new MissionPropertyQueryColumn(ma));
						}
					
						List<SamplingUnitAttribute> su = s.createCriteria(SamplingUnitAttribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
							.list();
						for (SamplingUnitAttribute sua : su){
							cols.add(new SamplingUnitAttributeQueryColumn(sua));
						}
					}else{
						SurveyDesign sd2 = (SurveyDesign) s.load(SurveyDesign.class, sd.getUuid());
						for (MissionProperty mp : sd2.getMissionProperties()){
							cols.add(new MissionPropertyQueryColumn(mp.getAttribute()));
						}
						
						@SuppressWarnings("unchecked")
						List<SurveyDesignSamplingUnitAttribute> atts = s.createCriteria(SurveyDesignSamplingUnitAttribute.class)
								.add(Restrictions.eq("id.surveyDesign", sd2)) //$NON-NLS-1$
								.list();
						for (SurveyDesignSamplingUnitAttribute a : atts){
							cols.add(new SamplingUnitAttributeQueryColumn(a.getSamplingUnitAttribute()));
						}
					}
					
				}finally{
					s.close();
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		try{
			j.join();
		}catch (Exception ex){
			throw new IllegalStateException(ex);
		}
		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
	/**
	 * Gets all data model columns
	 * @return
	 */
	private QueryColumn[] getDataModelColumns(){
		if (dataModelColumns == null){
			synchronized (DATAMODELLOCK) {
				
				//outside job to prevent deadlocking
				final DataModel dataModel = QueryDataModelManager.getInstance().getDataModel();
				
				Job j = new Job(Messages.SurveyQueryColumnManager_datamodelcolumnjobname){

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
						
						int numCategory = QueryDataModelManager.getInstance().getActiveDepth();
						for (int i = 0; i < numCategory; i++) {
							cols.add(new SurveyCategoryQueryColumn(MessageFormat.format(Messages.SurveyQueryColumnManager_CategoryColumnLabel, new Object[]{i}), i));
						}
							
						//sort attributes alphabetically
						List<Attribute> atts = new ArrayList<Attribute>();
						atts.addAll( dataModel.getAttributes() );
						Collections.sort(atts, new Comparator<Attribute>(){
							@Override
							public int compare(Attribute o1, Attribute o2) {
								return Collator.getInstance().compare(o1.getName(),o2.getName());
							}});
							
						for (Attribute att : atts) {
							String name = att.getName();
							cols.add(new SurveyAttributeQueryColumn(name, att.getKeyId(), att.getType()));
						}
						dataModelColumns = cols.toArray(new QueryColumn[cols.size()]);
						
						
						return Status.OK_STATUS;
					}
					};
					j.schedule();
					try{
						j.join();
					}catch (Exception ex){
						throw new IllegalStateException(ex);
					}
			}
		}
		return dataModelColumns;
	}
	
	
	/**
	 * Columns for gridded query.
	 * @return
	 */
	public QueryColumn[] getGridColumns() {
		QueryColumn[] tmp = new QueryColumn[GridQueryColumn.GridColumns.values().length];	
		for (int i = 0; i < GridQueryColumn.GridColumns.values().length; i++) {
			GridQueryColumn.GridColumns item = GridQueryColumn.GridColumns.values()[i];
			tmp[i] = new GridQueryColumn(item); 
		}
		return tmp;
	}

	/**
	 * Gets the label provider for a given query column.
	 * 
	 * @param column
	 * @return
	 */
	public static ColumnLabelProvider getLabelProvider(QueryColumn column){
//		if (column instanceof SurveyQueryColumn){
			return new FixedColumnLabelProvider(column);
//		}else if (column instanceof SurveyAttributeQueryColumn){
//			return new AttributeColumnLabelProvider(column);
//		}else if (column instanceof SurveyCategoryQueryColumn){
//			return new CategoryColumnLabelProvider(column);
//		}else if (column instanceof GridQueryColumn){
//			return new GridColumnLabelProvider(column);
//		}
//		return null;
	}
	
	public  QueryColumn[] getMissionQueryColumns(final SurveyDesign sd) {
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
	
		if (SmartDB.isMultipleAnalysis()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_ID));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_NAME));
		}
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_START));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_END));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END));
	
		//mission property columns
		Job j = new Job(Messages.SurveyQueryColumnManager_missionattributejobname){

			@SuppressWarnings("unchecked")
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					if (sd == null){
						List<MissionAttribute> all = s.createCriteria(MissionAttribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
						for (MissionAttribute ma : all){
							cols.add(new MissionPropertyQueryColumn(ma));
						}
					}else{
						SurveyDesign sd2 = (SurveyDesign) s.load(SurveyDesign.class, sd.getUuid());
						for (MissionProperty mp : sd2.getMissionProperties()){
							cols.add(new MissionPropertyQueryColumn(mp.getAttribute()));
						}
					}
				}finally{
					s.close();
				}
				return Status.OK_STATUS;
			}};
		
		j.schedule();
		try{
			j.join();
		}catch (Exception ex){
			throw new IllegalStateException(ex);
		}		
		return cols.toArray(new QueryColumn[cols.size()]);
	}
}
