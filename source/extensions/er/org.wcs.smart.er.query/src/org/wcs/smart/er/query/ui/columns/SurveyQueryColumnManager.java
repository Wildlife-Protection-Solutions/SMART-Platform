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
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.IProjectionProvider;
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
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.column.MissionPropertyQueryColumn;
import org.wcs.smart.er.query.model.column.SamplingUnitAttributeQueryColumn;
import org.wcs.smart.er.query.model.column.SurveyAttributeQueryColumn;
import org.wcs.smart.er.query.model.column.SurveyCategoryQueryColumn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.common.ui.QueryColumnLabelProvider;
import org.wcs.smart.query.common.ui.ReprojectingQueryColumnLabelProvder;
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
		DataModelManager.INSTANCE.addChangeListener(new IDataModelListener() {
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
		if (SmartDB.isMultipleAnalysis()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_ID, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_NAME, Locale.getDefault()));
		}
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_LEADER, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_ID, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DATE, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_TIME, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_X, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_Y, Locale.getDefault()));
		if (sd == null || sd.getTrackDistanceDirection()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE, Locale.getDefault()));
		}
		if (sd == null || sd.getTrackObserver()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_OBSERVER, Locale.getDefault()));
		}
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_COMMENT, Locale.getDefault()));
				
		//mission property columns
		Job j = new Job(Messages.SurveyQueryColumnManager_missionattributejobname){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					SurveyDesign sd2 = null;
					if (sd != null){
						sd2 = (SurveyDesign) s.load(SurveyDesign.class, sd.getUuid());
					}
					cols.addAll(getMissionPropertyColumns(s, sd2));
					cols.addAll(getSamplingUnitAttributeColumns(s, sd2));
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
		
		//data model columns
		for (QueryColumn q : getDataModelColumns()){
			cols.add(q.clone());
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
		if (SmartDB.isMultipleAnalysis()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_ID, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_NAME, Locale.getDefault()));
		}
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_ID, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DATE, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_TIME, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_X, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_Y, Locale.getDefault()));
		if (sd == null || sd.getTrackDistanceDirection()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE, Locale.getDefault()));
		}
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.WAYPOINT_COMMENT, Locale.getDefault()));
						
		//mission property columns
		Job j = new Job(Messages.SurveyQueryColumnManager_missionattributejobname){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					SurveyDesign sd2 = null;
					if (sd != null){
						sd2 = (SurveyDesign) s.load(SurveyDesign.class, sd.getUuid());
					}
					cols.addAll(getMissionPropertyColumns(s, sd2));
					cols.addAll(getSamplingUnitAttributeColumns(s, sd2));
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
			tmp[i] = new GridQueryColumn(item, Locale.getDefault()); 
		}
		return tmp;
	}

	/**
	 * Gets the label provider for a given query column.
	 * 
	 * @param column
	 * @return
	 */
	public static ColumnLabelProvider getLabelProvider(QueryColumn column, List<QueryColumn> allColumns, IProjectionProvider prjProvider){
		if (column.getKey().equalsIgnoreCase(SurveyQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
			for (QueryColumn qc : allColumns){
				if (qc.getKey().equalsIgnoreCase(SurveyQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
					return new ReprojectingQueryColumnLabelProvder(column,column,qc, prjProvider);
				}
			}
		}
		if (column.getKey().equalsIgnoreCase(SurveyQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
			for (QueryColumn qc : allColumns){
				if (qc.getKey().equalsIgnoreCase(SurveyQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
					return new ReprojectingQueryColumnLabelProvder(column,qc,column, prjProvider);
				}
			}
		}
		return new QueryColumnLabelProvider(column);
	}
	
	public  QueryColumn[] getMissionQueryColumns(final SurveyDesign sd) {
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
	
		if (SmartDB.isMultipleAnalysis()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_ID, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_NAME, Locale.getDefault()));
		}

		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_END, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_LEADER, Locale.getDefault()));
		
		//mission property columns
		LoadMissionPropertiesJob j = new LoadMissionPropertiesJob(sd);
		j.schedule();
		try {
			j.join();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		cols.addAll(j.getColumns());
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END, Locale.getDefault()));
	
		
		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
	public  QueryColumn[] getMissionTrackQueryColumns(final SurveyDesign sd) {
		final List<QueryColumn> cols = new ArrayList<QueryColumn>();
	
		if (SmartDB.isMultipleAnalysis()){
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_ID, Locale.getDefault()));
			cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.CA_NAME, Locale.getDefault()));
		}
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_TRACKID, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_TRACKLENGTH, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_TRACKTYPE, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_TRACKDATE, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SAMPLING_UNIT, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.MISSION_END, Locale.getDefault()));
		
		//mission property columns
		
		Job j = new Job(Messages.SurveyQueryColumnManager_missionattributejobname){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					SurveyDesign sd2 = null;
					if (sd != null){
						sd2 = (SurveyDesign) s.load(SurveyDesign.class, sd.getUuid());
					}
					cols.addAll(getMissionPropertyColumns(s, sd2));
					cols.addAll(getSamplingUnitAttributeColumns(s, sd2));
				}finally{
					s.close();
				}
				return Status.OK_STATUS;
			}
		};
		
//		LoadMissionPropertiesJob j = new LoadMissionPropertiesJob(sd);
		j.schedule();
		try {
			j.join();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
//		cols.addAll(j.getColumns());
				
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END, Locale.getDefault()));
		
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_START, Locale.getDefault()));
		cols.add(new SurveyQueryColumn(SurveyQueryColumn.FixedColumns.SURVEY_END, Locale.getDefault()));
		
		
		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
		
	private class LoadMissionPropertiesJob extends Job{

		private SurveyDesign sd;
		private List<MissionPropertyQueryColumn> columns;
		
		public LoadMissionPropertiesJob(SurveyDesign sd) {
			super(Messages.SurveyQueryColumnManager_LoadMissionPropJobName);
			this.sd = sd;
		}

		public List<MissionPropertyQueryColumn> getColumns(){
			return this.columns;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			columns = new ArrayList<MissionPropertyQueryColumn>();
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				SurveyDesign sd2 = null;
				if (sd != null){
					sd2 = (SurveyDesign) s.load(SurveyDesign.class, sd.getUuid());
				}
				columns.addAll(getMissionPropertyColumns(s, sd2));
				
				s.getTransaction().rollback();
			}finally{
				s.close();
			}
			return Status.OK_STATUS;
		}
		
	}
	
	private String getMissionPropertyColumnName(MissionAttribute ma){
		return Messages.MissionPropertyQueryColumn_MissionPropertyColumnLabel + "|" + ma.getName(); //$NON-NLS-1$
	}
	
	private String getSamplingUnitAttributeColumnName(SamplingUnitAttribute sua){
		return Messages.SamplingUnitAttributeQueryColumn_SuLabel + "|" + sua.getName(); //$NON-NLS-1$
	}
	
	@SuppressWarnings("unchecked")
	private List<MissionPropertyQueryColumn> getMissionPropertyColumns(Session session, SurveyDesign sd){
		List<MissionPropertyQueryColumn> columns = new ArrayList<MissionPropertyQueryColumn>();
		if (sd == null){
			List<MissionAttribute> all = session.createCriteria(MissionAttribute.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
			for (MissionAttribute ma : all){
				columns.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(ma), ma));
			}
		}else{
			for (MissionProperty mp : sd.getMissionProperties()){
				columns.add(new MissionPropertyQueryColumn(getMissionPropertyColumnName(mp.getAttribute()), mp.getAttribute()));
			}
		}
		Collections.sort(columns, new Comparator<MissionPropertyQueryColumn>() {
			@Override
			public int compare(MissionPropertyQueryColumn o1,
					MissionPropertyQueryColumn o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}
		});
		return columns;
	}
	
	@SuppressWarnings("unchecked")
	private List<SamplingUnitAttributeQueryColumn> getSamplingUnitAttributeColumns(Session session, SurveyDesign sd){
		List<SamplingUnitAttributeQueryColumn> columns = new ArrayList<SamplingUnitAttributeQueryColumn>();
		if (sd == null){
			List<SamplingUnitAttribute> su = session.createCriteria(SamplingUnitAttribute.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.list();
			for (SamplingUnitAttribute sua : su){
				columns.add(new SamplingUnitAttributeQueryColumn(getSamplingUnitAttributeColumnName(sua), sua));
			}
		}else{
			List<SurveyDesignSamplingUnitAttribute> atts = session.createCriteria(SurveyDesignSamplingUnitAttribute.class)
				.add(Restrictions.eq("id.surveyDesign", sd)) //$NON-NLS-1$
				.list();
			for (SurveyDesignSamplingUnitAttribute a : atts){
				columns.add(new SamplingUnitAttributeQueryColumn(getSamplingUnitAttributeColumnName(a.getSamplingUnitAttribute()), a.getSamplingUnitAttribute()));
			}
		}
		Collections.sort(columns, new Comparator<SamplingUnitAttributeQueryColumn>() {
			@Override
			public int compare(SamplingUnitAttributeQueryColumn o1,
					SamplingUnitAttributeQueryColumn o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}
		});
		return columns;
	}
}
