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
package org.wcs.smart.er.query.internal;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyQueryFactory;
import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTemplateCloner;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

/**
 * Survey query cloner
 * 
 * @author Emily
 *
 */
public class SurveyQueryCloner implements IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.SurveyQueryCloner_CopyQueryProgress, 6);
		try{
			monitor.subTask(Messages.SurveyQueryCloner_GriddedTask);
			cloneGriddedQuery(engine);
			monitor.worked(1);
			
			monitor.subTask(Messages.SurveyQueryCloner_SummaryTask);
			cloneSummaryQuery(engine);
			monitor.worked(1);
			
			monitor.subTask(Messages.SurveyQueryCloner_ObservationTask);
			cloneObservationQuery(engine);
			monitor.worked(1);
			
			monitor.subTask(Messages.SurveyQueryCloner_IncidentTask);
			cloneWaypointQuery(engine);
			monitor.worked(1);
			
			monitor.subTask(Messages.SurveyQueryCloner_MissionTask);
			cloneMissionQuery(engine);
			monitor.worked(1);
			
			monitor.subTask(Messages.SurveyQueryCloner_MissionTrackTask);
			cloneMissionTrackQuery(engine);
			monitor.worked(1);
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
		List<SurveyGriddedQuery> queries = (List<SurveyGriddedQuery>) engine.getSession()
			.createCriteria(SurveyGriddedQuery.class)
			.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
			.add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ 
		
		for(SurveyGriddedQuery query : queries){
			SurveyGriddedQuery clone = (SurveyGriddedQuery) SurveyQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( SurveyGriddedQuery.KEY) );
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
			clone.setSurveyDesign(query.getSurveyDesign());
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
		List<SurveySummaryQuery> queries = (List<SurveySummaryQuery>) engine.getSession()
			.createCriteria(SurveySummaryQuery.class)
			.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
			.add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ 
		
		for(SurveySummaryQuery query : queries){
			SurveySummaryQuery clone = (SurveySummaryQuery) SurveyQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( SurveySummaryQuery.KEY) );
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
			clone.setSurveyDesign(query.getSurveyDesign());
			
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
		List<SurveyObservationQuery> queries = (List<SurveyObservationQuery>) engine.getSession()
		.createCriteria(SurveyObservationQuery.class)
		.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
		.add(Restrictions.eq("isShared", true)).list();  //$NON-NLS-1$
		
		for(SurveyObservationQuery query : queries){
			SurveyObservationQuery clone = (SurveyObservationQuery) SurveyQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( SurveyObservationQuery.KEY) );

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
			clone.setSurveyDesign(query.getSurveyDesign());
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
		List<SurveyWaypointQuery> queries = (List<SurveyWaypointQuery>) engine.getSession()
		.createCriteria(SurveyWaypointQuery.class)
		.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
		.add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ 
		
		for(SurveyWaypointQuery query : queries){
			SurveyWaypointQuery clone = (SurveyWaypointQuery) SurveyQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( SurveyWaypointQuery.KEY) );
			
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
			clone.setSurveyDesign(query.getSurveyDesign());
			clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone mission queries
	 */
	private void cloneMissionQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		@SuppressWarnings("unchecked")
		List<MissionQuery> queries = (List<MissionQuery>) engine.getSession()
		.createCriteria(MissionQuery.class)
		.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
		.add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ 
		
		for(MissionQuery query : queries){
			MissionQuery clone = (MissionQuery) SurveyQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( MissionQuery.KEY) );
			
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
			clone.setSurveyDesign(query.getSurveyDesign());
			clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone mission track queries
	 */
	private void cloneMissionTrackQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		@SuppressWarnings("unchecked")
		List<MissionTrackQuery> queries = (List<MissionTrackQuery>) engine.getSession()
		.createCriteria(MissionTrackQuery.class)
		.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
		.add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ 
		
		for(MissionTrackQuery query : queries){
			MissionTrackQuery clone = (MissionTrackQuery) SurveyQueryFactory.createBlankQuery(QueryTypeManager.INSTANCE.findQueryType( MissionTrackQuery.KEY) );
			
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
			clone.setQueryFilter(cloneMissionTrackQueryFilter(query.getQueryFilter(), engine));
			clone.setSurveyDesign(query.getSurveyDesign());
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
			return queryfilter.asString();
		}catch (Throwable t){
			QueryPlugIn.log("Error cloning query definition: " + strFilter, t); //$NON-NLS-1$
			return strFilter;
		}
	}
	
	/*
	 * Updates the uuid references in the query filter to reference
	 * the new conservation area uuid items 
	 */
	private String cloneMissionTrackQueryFilter(String strFilter, ConservationAreaClonerEngine engine){
		if (strFilter.length() == 0){
			return strFilter;
		}
		try{
			Parser parser = new Parser(new ByteArrayInputStream(strFilter.getBytes()));
			return parser.ExpressionPart().asString();
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
		try{
			Parser parser = new Parser(new ByteArrayInputStream(griddedQueryStr.getBytes()));
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
		try{
			Parser parser = new Parser(new ByteArrayInputStream(griddedQueryStr.getBytes()));
			SumQueryDefinition def = parser.SumQuery();
			return def.asQuery();
		}catch (Throwable ex){
			QueryPlugIn.log("Error cloning query definition: " + griddedQueryStr, ex); //$NON-NLS-1$
		}
		return griddedQueryStr;
	}
	
	
}
