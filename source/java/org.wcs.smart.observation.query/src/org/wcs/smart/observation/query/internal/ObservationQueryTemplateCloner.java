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
package org.wcs.smart.observation.query.internal;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationGriddedQuery;
import org.wcs.smart.observation.query.model.ObservationQueryFactory;
import org.wcs.smart.observation.query.model.ObservationSummaryQuery;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
import org.wcs.smart.observation.query.parser.internal.parser.Parser;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTemplateCloner;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

/**
 * Clones the various shared patrol queries.
 *  
 * @author Emily
 *
 */
public class ObservationQueryTemplateCloner implements
		IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.ObservationQueryTemplateCloner_TaskName, 4);
		
		progress.subTask(Messages.ObservationQueryTemplateCloner_GridProgress);
		cloneGriddedQuery(engine);
		progress.worked(1);
			
		progress.subTask(Messages.ObservationQueryTemplateCloner_SummaryProgress);
		cloneSummaryQuery(engine);
		progress.worked(1);
			
		progress.subTask(Messages.ObservationQueryTemplateCloner_ObservationProgress);
		cloneObservationQuery(engine);
		progress.worked(1);
			
		progress.subTask(Messages.ObservationQueryTemplateCloner_IncidentProgress);
		cloneWaypointQuery(engine);
		progress.worked(1);
		
	}
	
	/*
	 * clone gridded queries
	 */
	private void cloneGriddedQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		
		List<ObservationGriddedQuery> queries = QueryFactory.buildQuery(engine.getSession(), ObservationGriddedQuery.class, 
					new Object[] {"conservationArea", engine.getTemplateCa()}, //$NON-NLS-1$
					new Object[] {"isShared", true}).getResultList(); //$NON-NLS-1$
		
		for(ObservationGriddedQuery query : queries){
			ObservationGriddedQuery clone = (ObservationGriddedQuery) ObservationQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( ObservationGriddedQuery.KEY) );
			clone.setConservationArea(engine.getNewCa());
			engine.copyLabels(query, clone);
			clone.setConservationAreaFilter( (new ConservationAreaFilter(true, engine.getNewCa())).asString());
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
			
			engine.getSession().persist(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone summary queries
	 */
	private void cloneSummaryQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		
		List<ObservationSummaryQuery> queries = QueryFactory.buildQuery(engine.getSession(), ObservationSummaryQuery.class, 
				new Object[] {"conservationArea", engine.getTemplateCa()}, //$NON-NLS-1$
				new Object[] {"isShared", true}).getResultList(); //$NON-NLS-1$
		
		for(ObservationSummaryQuery query : queries){
			ObservationSummaryQuery clone = (ObservationSummaryQuery) ObservationQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( ObservationSummaryQuery.KEY) );
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
			
			engine.getSession().persist(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone observation queries
	 */
	private void cloneObservationQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		
		List<ObsObservationQuery> queries = QueryFactory.buildQuery(engine.getSession(), ObsObservationQuery.class, 
				new Object[] {"conservationArea", engine.getTemplateCa()}, //$NON-NLS-1$
				new Object[] {"isShared", true}).getResultList(); //$NON-NLS-1$
		
		for(ObsObservationQuery query : queries){
			ObsObservationQuery clone = (ObsObservationQuery) ObservationQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( ObsObservationQuery.KEY) );

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
			clone.setQueryFilter(cloneQueryFilter(query.getQueryFilter(), engine));
			clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
			
			engine.getSession().persist(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone waypoint queries
	 */
	private void cloneWaypointQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		
		List<ObservationWaypointQuery> queries = QueryFactory.buildQuery(engine.getSession(), ObservationWaypointQuery.class, 
				new Object[] {"conservationArea", engine.getTemplateCa()}, //$NON-NLS-1$
				new Object[] {"isShared", true}).getResultList(); //$NON-NLS-1$
		
		for(ObservationWaypointQuery query : queries){
			ObservationWaypointQuery clone = (ObservationWaypointQuery) ObservationQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( ObservationWaypointQuery.KEY) );
			
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
			clone.setQueryFilter(cloneQueryFilter(query.getQueryFilter(), engine));
			clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
			
			engine.getSession().persist(clone);
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
		try(Reader is = new StringReader(strFilter)){
			Parser parser = new Parser(is);
			QueryFilter queryfilter = parser.QueryFilter();
			return queryfilter.asString();
		}catch (Throwable t){
			QueryPlugIn.log("Error cloning query definition: " + strFilter, t); //$NON-NLS-1$
			return strFilter;
		}
	}
	
	
	/*
	 * Updates the gridded query definitions so that uuid references in the query 
	 * reference
	 * the new conservation area uuid items 
	 */
	private String cloneGriddedQueryDefinition(String griddedQueryStr, ConservationAreaClonerEngine engine) {
		try(Reader is = new StringReader(griddedQueryStr)){
			Parser parser = new Parser(is);
			GridQueryDefinition def = parser.GridQuery();
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
		try(Reader is = new StringReader(griddedQueryStr)){
			Parser parser = new Parser(is);
			SumQueryDefinition def = parser.SumQuery();
			return def.asQuery();
		}catch (Throwable ex){
			QueryPlugIn.log("Error cloning query definition: " + griddedQueryStr, ex); //$NON-NLS-1$
		}
		return griddedQueryStr;
	}
	
	

}
