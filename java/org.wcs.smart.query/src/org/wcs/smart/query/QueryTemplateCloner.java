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
package org.wcs.smart.query;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.ObservationQuery;
import org.wcs.smart.query.model.PatrolQuery;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.QueryFactory;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.model.WaypointQuery;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOptionType;
import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.filter.QueryFilter;
import org.wcs.smart.query.parser.internal.filter.PatrolFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.GridQueryDefinition;
import org.wcs.smart.query.parser.internal.summary.GroupByPart;
import org.wcs.smart.query.parser.internal.summary.IGroupBy;
import org.wcs.smart.query.parser.internal.summary.IGroupBy.GroupByType;
import org.wcs.smart.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.query.parser.internal.summary.SumQueryDefinition;
import org.wcs.smart.util.SmartUtils;

/**
 * Template cloner that copies query data 
 * from the template to the new conservation area.
 * <p>Data copied includes shared queries, and shared query folders.
 * Items not copied include user folders/queries.</p>
 * Note queries are parsed and uuid items updated to references
 * the new items.
 * 
 * @author Emily
 *
 */
public class QueryTemplateCloner implements
		IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.QueryTemplateCloner_ProgressQuery, 6);
		try{
			//	need to clone: shared query folders
			monitor.subTask(Messages.QueryTemplateCloner_ProgressCopyFolders);
			cloneFolders(engine);		
			monitor.worked(1);
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
		List<GriddedQuery> queries = (List<GriddedQuery>) engine.getSession().createCriteria(GriddedQuery.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for(GriddedQuery query : queries){
			GriddedQuery clone = (GriddedQuery) QueryFactory.createBlankQuery(QueryType.GRIDDED);
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
		List<SummaryQuery> queries = (List<SummaryQuery>) engine.getSession().createCriteria(SummaryQuery.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for(SummaryQuery query : queries){
			SummaryQuery clone = (SummaryQuery) QueryFactory.createBlankQuery(QueryType.SUMMARY);
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
			PatrolQuery clone = (PatrolQuery) QueryFactory.createBlankQuery(QueryType.PATROL);
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
		List<ObservationQuery> queries = (List<ObservationQuery>) engine.getSession().createCriteria(ObservationQuery.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).add(Restrictions.eq("isShared", true)).list();  //$NON-NLS-1$//$NON-NLS-2$
		
		for(ObservationQuery query : queries){
			ObservationQuery clone = (ObservationQuery) QueryFactory.createBlankQuery(QueryType.OBSERVATION);
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
		List<WaypointQuery> queries = (List<WaypointQuery>) engine.getSession().createCriteria(WaypointQuery.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for(WaypointQuery query : queries){
			WaypointQuery clone = (WaypointQuery) QueryFactory.createBlankQuery(QueryType.WAYPOINT);
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
				if (pgb.getRawItems() != null){
					for (int i = 0; i < pgb.getRawItems().length; i ++){
						UuidItem it = engine.getNewConservationItem(SmartUtils.decodeHex(pgb.getRawItems()[i]));
						if (it != null){
							pgb.getRawItems()[i] = SmartUtils.encodeHex(it.getUuid());
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
	private void updateQueryFilter(QueryFilter queryfilter, ConservationAreaClonerEngine engine) throws Exception{
		//if we have a uuid filter item
		if (queryfilter == null){
			return;
		}
		List<IFilter> toProcess = new ArrayList<IFilter>();
		toProcess.add(queryfilter.getFilter());
		
		while(toProcess.size() > 0){
			IFilter filter = toProcess.remove(0);
			if (filter instanceof PatrolFilter){
				PatrolFilter pFilter = (PatrolFilter)filter;
				if (pFilter.getPatrolOption().getType() == PatrolQueryOptionType.UUID){
					//need to find the old uuid items and match to new items
					String templateUuid = pFilter.getValue();
					UuidItem newItem = engine.getNewConservationItem(SmartUtils.decodeHex(templateUuid));
					if (newItem != null){
						pFilter.setValue(SmartUtils.encodeHex(newItem.getUuid()));
					}
				}
			}
			if (filter.getChildren() != null){
				toProcess.addAll(filter.getChildren());
			}
		}
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
	
	private void cloneFolders(ConservationAreaClonerEngine engine){
		@SuppressWarnings("unchecked")
		List<QueryFolder> queryFolder = engine.getSession().createCriteria(QueryFolder.class)
				.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
				.add(Restrictions.isNull("employee")) //$NON-NLS-1$
				.add(Restrictions.isNull("parentFolder")).list(); //$NON-NLS-1$
		for (QueryFolder q : queryFolder){
			processQueryFolder(q, null, engine);
		}
		engine.getSession().flush();
	}

	private QueryFolder processQueryFolder(QueryFolder templateFolder, QueryFolder newParent, ConservationAreaClonerEngine engine){
		QueryFolder clone = new QueryFolder();
		engine.copyLabels(templateFolder, clone);
		clone.setConservationArea(engine.getNewCa());
		clone.setEmployee(null);
		clone.setParentFolder(newParent);
		clone.setRootFolder(false);
		engine.getSession().save(clone);
		engine.addConservationItemMapping(templateFolder, clone);
			
		for (QueryFolder kid : templateFolder.getChildren()){
			QueryFolder clonedKid = processQueryFolder(kid, clone, engine);
			clone.getChildren().add(clonedKid);
		}
		return clone;
	}
}
