package org.wcs.smart.er.query.ui.columns;

import java.text.Collator;
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
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Query column cache.
 * 
 * @author Emily
 *
 */
public class SurveyQueryColumnCache {

	private static SurveyQueryColumnCache instance = null;
	
	public static SurveyQueryColumnCache getInstance(){
		if (instance == null){
			synchronized (SurveyQueryColumnCache.class) {
				if (instance == null){
					instance = new SurveyQueryColumnCache();
				}
			}			
		}
		return instance;
	}
	
	private QueryColumn[] dataModelColumns = null;
	
	private final Object DATAMODELLOCK = new Object();
	
	private SurveyQueryColumnCache(){
	
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
	 * This function will access the database the first
	 * time it is called, subsequent calls return cached values. 
	 */
	//TODO: we could try to be smart here and only include
	//attribute columns in the survey design configurable
	//model
	public  QueryColumn[] getObservationQueryColumns(SurveyDesign sd) {
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
			if (add){
				cols.add(new SurveyQueryColumn(c));
			}
		}
		
		//mission property columns
		if (sd == null){
			//TODO: return all mission attributes defined in CA
			//we should look at caching these maybe
			Job j = new Job("loading mission attributes"){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						List<MissionAttribute> all = s.createCriteria(MissionAttribute.class)
								.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
						for (MissionAttribute ma : all){
							cols.add(new MissionPropertyQueryColumn(ma));
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
			for (MissionProperty mp : sd.getMissionProperties()){
				cols.add(new MissionPropertyQueryColumn(mp.getAttribute()));
			}
		}
		
		//data model columns
		for (QueryColumn q : getDataModelColumns()){
			cols.add(q);
		}
		
		return cols.toArray(new QueryColumn[cols.size()]);
	}

	
	private QueryColumn[] getDataModelColumns(){
		if (dataModelColumns == null){
			synchronized (DATAMODELLOCK) {
				
				//outside job to prevent deadlocking
				final DataModel dataModel = QueryDataModelManager.getInstance().getDataModel();
				
				Job j = new Job("Generating datamodel columns"){

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
						
						int numCategory = QueryDataModelManager.getInstance().getActiveDepth();
						for (int i = 0; i < numCategory; i++) {
							cols.add(new SurveyCategoryQueryColumn("Category " + i, i));
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

	
	private QueryColumn[] cloneColumns(QueryColumn[] cols){
		QueryColumn[] copies = new QueryColumn[cols.length];
		for (int i = 0; i < copies.length; i ++){
			copies[i] = cols[i].clone();
		}
		return copies;
	}

	
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
}
