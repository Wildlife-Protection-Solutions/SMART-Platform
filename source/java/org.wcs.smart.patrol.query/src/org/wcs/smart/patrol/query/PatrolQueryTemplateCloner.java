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
package org.wcs.smart.patrol.query;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryFactory;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;
import org.wcs.smart.patrol.query.model.PatrolQueryOptions;
import org.wcs.smart.patrol.query.model.PatrolSummaryQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.patrol.query.parser.internal.filter.PatrolFilter;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTemplateCloner;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IGroupBy.GroupByType;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.util.UuidUtils;

/**
 * Clones the various shared patrol queries.
 *  
 * @author Emily
 *
 */
public class PatrolQueryTemplateCloner implements
		IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.QueryTemplateCloner_ProgressQuery, 5);
		try{
			monitor.subTask(Messages.QueryTemplateCloner_ProgressCopyGridded);
			cloneGriddedQuery(engine);
			monitor.worked(1);
			monitor.subTask(Messages.QueryTemplateCloner_ProgressCopySummary);
			cloneSummaryQuery(engine);
			monitor.worked(1);
			monitor.subTask(Messages.QueryTemplateCloner_ProgressCopyPatrols);
			clonePatrolQuery(engine);
			monitor.worked(1);
			monitor.subTask(Messages.QueryTemplateCloner_ProgressCopyObservation);
			cloneObservationQuery(engine);
			monitor.worked(1);
			monitor.subTask(Messages.QueryTemplateCloner_ProgressCopyWaypoint);
			cloneWaypointQuery(engine);
		}finally{
			monitor.done();
		}
	}
	
	/*
	 * clone gridded queries
	 */
	private void cloneGriddedQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		@SuppressWarnings("unchecked")
		List<PatrolGriddedQuery> queries = (List<PatrolGriddedQuery>) engine.getSession().createCriteria(PatrolGriddedQuery.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for(PatrolGriddedQuery query : queries){
			PatrolGriddedQuery clone = (PatrolGriddedQuery) PatrolQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( PatrolGriddedQuery.KEY) );
			clone.setConservationArea(engine.getNewCa());
			engine.copyLabels(query, clone);
			clone.setConservationAreaFilter(query.getConservationAreaFilter());
			clone.setDateFilter(query.getDateFilter());
			clone.setCrsDefinition(query.getCrsDefinition());
			
			if (query.getFolder() != null){
				clone.setFolder((QueryFolder)engine.getNewConservationItem(query.getFolder()));
			}
			clone.setId(query.getId());
			clone.setIsShared(query.getIsShared());
			clone.setOwner(newEmployee);
			clone.setQuery(cloneGriddedQueryDefinition(query.getQuery(), engine));
			clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone summary queries
	 */
	private void cloneSummaryQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		@SuppressWarnings("unchecked")
		List<PatrolSummaryQuery> queries = (List<PatrolSummaryQuery>) engine.getSession().createCriteria(PatrolSummaryQuery.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for(PatrolSummaryQuery query : queries){
			PatrolSummaryQuery clone = (PatrolSummaryQuery) PatrolQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( PatrolSummaryQuery.KEY) );
			clone.setConservationArea(engine.getNewCa());
			engine.copyLabels(query, clone);
			clone.setConservationAreaFilter(query.getConservationAreaFilter());
			clone.setDateFilter(query.getDateFilter());
			if (query.getFolder() != null){
				clone.setFolder((QueryFolder)engine.getNewConservationItem(query.getFolder()));
			}
			clone.setId(query.getId());
			clone.setIsShared(query.getIsShared());
			clone.setOwner(newEmployee);
			clone.setQuery(cloneSummaryQueryDefinition(query.getQuery(), engine));
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}

	/*
	 * clone patrol queries
	 */
	private void clonePatrolQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		@SuppressWarnings("unchecked")
		List<PatrolQuery> queries = (List<PatrolQuery>) engine.getSession().createCriteria(PatrolQuery.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for(PatrolQuery query : queries){
			PatrolQuery clone = (PatrolQuery) PatrolQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( PatrolQuery.KEY) );
			engine.copyLabels(query, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setConservationAreaFilter(query.getConservationAreaFilter());
			clone.setDateFilter(query.getDateFilter());
			if (query.getFolder() != null){
				clone.setFolder((QueryFolder)engine.getNewConservationItem(query.getFolder()));
			}
			clone.setId(query.getId());
			clone.setIsShared(query.getIsShared());
			clone.setOwner(newEmployee);
			clone.setVisibleColumns(query.getVisibleColumns());
			clone.setQueryFilter(cloneQueryFilter(query.getQueryFilter(), engine));
			clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone observation queries
	 */
	private void cloneObservationQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		@SuppressWarnings("unchecked")
		List<PatrolObservationQuery> queries = (List<PatrolObservationQuery>) engine.getSession().createCriteria(PatrolObservationQuery.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).add(Restrictions.eq("isShared", true)).list();  //$NON-NLS-1$//$NON-NLS-2$
		
		for(PatrolObservationQuery query : queries){
			PatrolObservationQuery clone = (PatrolObservationQuery) PatrolQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( PatrolObservationQuery.KEY) );

			engine.copyLabels(query, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setConservationAreaFilter(query.getConservationAreaFilter());
			clone.setDateFilter(query.getDateFilter());
			if (query.getFolder() != null){
				clone.setFolder((QueryFolder)engine.getNewConservationItem(query.getFolder()));
			}
			clone.setId(query.getId());
			clone.setIsShared(query.getIsShared());
			clone.setOwner(newEmployee);
			clone.setVisibleColumns(query.getVisibleColumns());
			clone.setQueryFilter(cloneQueryFilter(query.getQueryFilter(), engine));
			clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone waypoint queries
	 */
	private void cloneWaypointQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		@SuppressWarnings("unchecked")
		List<PatrolWaypointQuery> queries = (List<PatrolWaypointQuery>) engine.getSession().createCriteria(PatrolWaypointQuery.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for(PatrolWaypointQuery query : queries){
			PatrolWaypointQuery clone = (PatrolWaypointQuery) PatrolQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( PatrolWaypointQuery.KEY) );
			
			engine.copyLabels(query, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setConservationAreaFilter(query.getConservationAreaFilter());
			clone.setDateFilter(query.getDateFilter());
			if (query.getFolder() != null){
				clone.setFolder((QueryFolder)engine.getNewConservationItem(query.getFolder()));
			}
			clone.setId(query.getId());
			clone.setIsShared(query.getIsShared());
			clone.setOwner(newEmployee);
			clone.setVisibleColumns(query.getVisibleColumns());
			clone.setQueryFilter(cloneQueryFilter(query.getQueryFilter(), engine));
			clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * Updates the uuid references in the query filter to reference
	 * the new conservation area uuid items 
	 */
	private String cloneQueryFilter(String strFilter, ConservationAreaClonerEngine engine){
		if (strFilter.length() == 0){
			return strFilter;
		}
		try{
			Parser parser = new Parser(new ByteArrayInputStream(strFilter.getBytes()));
		
			QueryFilter queryfilter = parser.QueryFilter();
			updateQueryFilter(queryfilter, engine);
			return queryfilter.asString();
		}catch (Throwable t){
			QueryPlugIn.log("Error cloning query definition: " + strFilter, t); //$NON-NLS-1$
			return strFilter;
		}
	}
	
	/*
	 * Updates the uuid references in the group by items to reference
	 * the new conservation area uuid items 
	 */
	private void updateGroupBy(GroupByPart groupBy, ConservationAreaClonerEngine engine) throws Exception{
		for(IGroupBy gb : groupBy.getGroupBys()){
			if (gb instanceof PatrolGroupBy && gb.getType().equals(GroupByType.BYTE)){
				PatrolGroupBy pgb = (PatrolGroupBy)gb;
				if (pgb.getItems() != null){
					for (int i = 0; i < pgb.getItems().length; i ++){
						UuidItem it = engine.getNewConservationItem(UuidUtils.stringToUuid(pgb.getItems()[i]));
						if (it != null){
							pgb.getItems()[i] = UuidUtils.uuidToString(it.getUuid());
						}
					}
				}
			}
		}
	}
	
	/*
	 * Updates the uuid references in the query filter to reference
	 * the new conservation area uuid items 
	 */
	private void updateQueryFilter(QueryFilter queryfilter, final ConservationAreaClonerEngine engine) throws Exception{
		//if we have a uuid filter item
		if (queryfilter == null){
			return;
		}
		
		final Exception errorex[] = new Exception[]{null};
		IFilterVisitor visitor = new IFilterVisitor() {
			
			@Override
			public void visit(IFilter filter) {
				if (filter instanceof PatrolFilter){
					try{
					PatrolFilter pFilter = (PatrolFilter)filter;
					
					if (pFilter.getPatrolOption().getType() == PatrolQueryOptionType.UUID){
						//need to find the old uuid items and match to new items
						String templateUuid = pFilter.getValue();
						UuidItem newItem = engine.getNewConservationItem(UuidUtils.stringToUuid(templateUuid));
						if (newItem != null){
							pFilter.setValue(UuidUtils.uuidToString(newItem.getUuid()));
						}
					}
					}catch (Exception ex){
						errorex[0] = ex;
					}
				}
			}
		};
		if (errorex[0] != null){
			throw errorex[0];
		}
		queryfilter.getFilter().accept(visitor);
		
	}
	
	
	/*
	 * Updates the gridded query definitions so that uuid references in the query 
	 * reference
	 * the new conservation area uuid items 
	 */
	private String cloneGriddedQueryDefinition(String griddedQueryStr, ConservationAreaClonerEngine engine) {
		try{
			Parser parser = new Parser(new ByteArrayInputStream(griddedQueryStr.getBytes()));
			GridQueryDefinition def = parser.GridQuery();
			updateQueryFilter(def.getRateFilter(), engine);
			updateQueryFilter(def.getValueFilter(), engine);
			return def.asQuery();
		}catch (Throwable ex){
			QueryPlugIn.log("Error cloning query definition: " + griddedQueryStr, ex); //$NON-NLS-1$
		}
		return griddedQueryStr;
	}
	
	/*
	 * updates summary query definitions; updating query uuid items
	 */
	private String cloneSummaryQueryDefinition(String griddedQueryStr, ConservationAreaClonerEngine engine) {
		try{
			Parser parser = new Parser(new ByteArrayInputStream(griddedQueryStr.getBytes()));
			SumQueryDefinition def = parser.SumQuery();
			updateQueryFilter(def.getRateFilter(), engine);
			updateQueryFilter(def.getValueFilter(), engine);
			updateGroupBy(def.getColumnGroupByPart(), engine);
			updateGroupBy(def.getRowGroupByPart(), engine);
			return def.asQuery();
		}catch (Throwable ex){
			QueryPlugIn.log("Error cloning query definition: " + griddedQueryStr, ex); //$NON-NLS-1$
		}
		return griddedQueryStr;
	}
	
	

}
