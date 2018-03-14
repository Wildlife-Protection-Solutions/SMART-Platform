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
package org.wcs.smart.asset.query;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetObservationQuery;
import org.wcs.smart.asset.query.model.AssetQueryFactory;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.asset.query.parser.internal.parser.Parser;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTemplateCloner;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

/**
 * Clones the various shared asset queries.
 *  
 * @author Emily
 *
 */
public class AssetQueryTemplateCloner implements
		IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.QueryTemplateCloner_ProgressQuery, 5);
		
		
		progress.subTask(Messages.QueryTemplateCloner_ProgressCopySummary);
		cloneSummaryQuery(engine);
		progress.worked(1);
		
		progress.subTask(Messages.QueryTemplateCloner_ProgressCopyObservation);
		cloneObservationQuery(engine);
		progress.worked(1);
		
		progress.subTask(Messages.QueryTemplateCloner_ProgressCopyWaypoint);
		cloneWaypointQuery(engine);
		progress.worked(1);
		
	}
	
	
	/*
	 * clone summary queries
	 */
	private void cloneSummaryQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		List<AssetSummaryQuery> queries = QueryFactory.buildQuery(engine.getSession(), AssetSummaryQuery.class,
				new Object[] {"conservationArea", engine.getTemplateCa()}, //$NON-NLS-1$
				new Object[] {"isShared", true}) //$NON-NLS-1$
				.getResultList();
		
		for(AssetSummaryQuery query : queries){
			AssetSummaryQuery clone = (AssetSummaryQuery) AssetQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( AssetSummaryQuery.KEY) );
			clone.setConservationArea(engine.getNewCa());
			engine.copyLabels(query, clone);
			clone.setConservationAreaFilter( (new ConservationAreaFilter(true, engine.getNewCa())).asString());
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
	 * clone observation queries
	 */
	private void cloneObservationQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		List<AssetObservationQuery> queries = QueryFactory.buildQuery(engine.getSession(), AssetObservationQuery.class,
				new Object[] {"conservationArea", engine.getTemplateCa()}, //$NON-NLS-1$
				new Object[] {"isShared", true}) //$NON-NLS-1$
				.getResultList();
		for(AssetObservationQuery query : queries){
			AssetObservationQuery clone = (AssetObservationQuery) AssetQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( AssetObservationQuery.KEY) );

			engine.copyLabels(query, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setConservationAreaFilter( (new ConservationAreaFilter(true, engine.getNewCa())).asString());
			clone.setDateFilter(query.getDateFilter());
			if (query.getFolder() != null){
				clone.setFolder((QueryFolder)engine.getNewConservationItem(query.getFolder()));
			}
			clone.setId(query.getId());
			clone.setIsShared(query.getIsShared());
			clone.setOwner(newEmployee);
			clone.setVisibleColumns(query.getVisibleColumns());
			String queryFilter = cloneQueryFilter(query.getQueryFilter(), engine);
			if (queryFilter != null) {
				clone.setQueryFilter(queryFilter);
				clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
				
				engine.getSession().save(clone);
				engine.addConservationItemMapping(query, clone);
			}
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone waypoint queries
	 */
	private void cloneWaypointQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		List<AssetWaypointQuery> queries = QueryFactory.buildQuery(engine.getSession(), AssetWaypointQuery.class,
				new Object[] {"conservationArea", engine.getTemplateCa()}, //$NON-NLS-1$
				new Object[] {"isShared", true}) //$NON-NLS-1$
				.getResultList();
		for(AssetWaypointQuery query : queries){
			AssetWaypointQuery clone = (AssetWaypointQuery) AssetQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( AssetWaypointQuery.KEY) );
			
			engine.copyLabels(query, clone);
			clone.setConservationArea(engine.getNewCa());
			clone.setConservationAreaFilter( (new ConservationAreaFilter(true, engine.getNewCa())).asString());
			clone.setDateFilter(query.getDateFilter());
			if (query.getFolder() != null){
				clone.setFolder((QueryFolder)engine.getNewConservationItem(query.getFolder()));
			}
			clone.setId(query.getId());
			clone.setIsShared(query.getIsShared());
			clone.setOwner(newEmployee);
			clone.setVisibleColumns(query.getVisibleColumns());
			String queryFilter = cloneQueryFilter(query.getQueryFilter(), engine);
			if (queryFilter != null) {
				clone.setQueryFilter(queryFilter);
				clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
				
				engine.getSession().save(clone);
				engine.addConservationItemMapping(query, clone);
			}
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
			return null;
		}
	}
	
	/*
	 * Updates the uuid references in the group by items to reference
	 * the new conservation area uuid items 
	 */
	private void updateGroupBy(GroupByPart groupBy, ConservationAreaClonerEngine engine) throws Exception{
		for(IGroupBy gb : groupBy.getGroupBys()){
			if (gb instanceof AssetGroupBy ){
				AssetGroupBy pgb = (AssetGroupBy)gb;
				pgb.clearItems();
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
				if (filter instanceof AssetFilter){
					try{
						AssetFilter pFilter = (AssetFilter)filter;
					
						//need to find the old uuid items and match to new items
						UUID templateUuid = pFilter.getValue();
						if (pFilter.getAssetOption() == AssetFilterOption.ASSETTYPE) {
							UuidItem newItem = engine.getNewConservationItem(templateUuid);
							if (newItem != null){
								//this will only work for asset types
								pFilter.setValue(newItem.getUuid());
							}else {
								//throw exception
								throw new Exception (Messages.AssetQueryTemplateCloner_AssetTypeNotFound);
							}
						}else {
							//we don't clone asset id, stations, or locations so 
							//this is no point in cloning this query.
							throw new Exception(Messages.AssetQueryTemplateCloner_QueryNotCloned);							
						}
					}catch (Exception ex){
						errorex[0] = ex;
					}
				}
			}
		};
		queryfilter.getFilter().accept(visitor);
		if (errorex[0] != null){
			throw errorex[0];
		}
		
		
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
