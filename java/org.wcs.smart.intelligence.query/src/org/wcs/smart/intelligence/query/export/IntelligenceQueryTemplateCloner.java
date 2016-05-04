
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
package org.wcs.smart.intelligence.query.export;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.query.QueryTemplateCloner;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Clones intelligence query data.
 * 
 * @author Emily
 *
 */
public class IntelligenceQueryTemplateCloner implements
		IConservationAreaTemplateCloner {

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.IntelligenceQueryTemplateCloner_CloneIntelQuery, 2);
		try{
			monitor.subTask(Messages.IntelligenceQueryTemplateCloner_CloneRecords);
			cloneRecordQueries(engine);
			monitor.worked(1);
			monitor.subTask(Messages.IntelligenceQueryTemplateCloner_CloneSummaries);
			cloneSummaryQuery(engine);
			monitor.worked(1);
		}finally{
			monitor.done();
		}
	}

	
	/*
	 * clone summary queries
	 */
	private void cloneSummaryQuery(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		@SuppressWarnings("unchecked")
		List<IntelligenceSummaryQuery> queries = (List<IntelligenceSummaryQuery>) engine
			.getSession()
			.createCriteria(IntelligenceSummaryQuery.class)
			.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
			.add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ 
		
		for(IntelligenceSummaryQuery query : queries){
			IntelligenceSummaryQuery clone = new IntelligenceSummaryQuery();
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
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}

	/*
	 * clone patrol queries
	 */
	private void cloneRecordQueries(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		@SuppressWarnings("unchecked")
		List<IntelligenceRecordQuery> queries = (List<IntelligenceRecordQuery>) engine.getSession()
			.createCriteria(IntelligenceRecordQuery.class)
			.add(Restrictions.eq("conservationArea", engine.getTemplateCa())) //$NON-NLS-1$
			.add(Restrictions.eq("isShared", true)).list(); //$NON-NLS-1$ 
		
		for(IntelligenceRecordQuery query : queries){
			IntelligenceRecordQuery clone = new IntelligenceRecordQuery();
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
//			clone.setQueryFilter(cloneQueryFilter(query.getQueryFilter(), engine));
			clone.setQueryFilter(query.getQueryFilter());
			clone.setStyle(QueryTemplateCloner.updateStyleString(engine, query.getStyle()));
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(query, clone);
		}
		engine.getSession().flush();
	}
	
}
