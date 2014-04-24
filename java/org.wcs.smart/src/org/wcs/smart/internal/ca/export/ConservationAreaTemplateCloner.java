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
package org.wcs.smart.internal.ca.export;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;

/**
 * Template cloner that copies conservation
 * area information from the template to the new conservation area.
 * <p>Data copied includes agencies, ranks, stations, projections
 * data model (categories, attributes, list items, tree items, aggregations)
 * and all associated labels.  Items not copied include maps, employess, and
 * area geometries.
 * 
 * @author Emily
 *
 */
public class ConservationAreaTemplateCloner implements
		IConservationAreaTemplateCloner {

	
	
	/**
	 * @see org.wcs.smart.ca.IConservationAreaTemplateCloner#cloneTemplateData(org.wcs.smart.ca.ConservationAreaClonerEngine, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {

		//need to clone: agencies, ranks, stations, projections,
		//data model (categories, attributes, attribute list items, 
		//attribute tree items, attribute aggregations
		//and all associated labels
		monitor.beginTask(Messages.ConservationAreaTemplateCloner_Progress_CopyCaInfo, 4);
		try{
		//DO NOT CLONE: employees, saved maps, area geometries
			monitor.subTask(Messages.ConservationAreaTemplateCloner_Progress_CopyAgencyRank);
			cloneAgenciesRank(engine);
			monitor.worked(1);
			monitor.subTask(Messages.ConservationAreaTemplateCloner_Progress_CopyStation);
			cloneStations(engine);
			monitor.worked(1);
			monitor.subTask(Messages.ConservationAreaTemplateCloner_Progress_CopyProjection);
			cloneProjections(engine);
			monitor.worked(1);
			monitor.subTask(Messages.ConservationAreaTemplateCloner_Progress_CopyDataModel);
			cloneDataModel(engine, new SubProgressMonitor(monitor, 1));
			monitor.worked(1);
		}finally{
			monitor.done();
		}
	}
	
	/*
	 * clone the data model
	 */
	private void cloneDataModel(ConservationAreaClonerEngine engine, IProgressMonitor monitor){
		DataModel templateDm = HibernateManager.loadDataModel(engine.getTemplateCa(), engine.getSession());
		DataModel clonedDm = templateDm.clone(engine.getNewCa(), null, new SubProgressMonitor(monitor, 1));
		Session session = engine.getSession();
		for (Attribute att : clonedDm.getAttributes()) {
			session.save(att);
			session.flush();
		}

		for (Category c : clonedDm.getCategories()) {
			session.save(c);
			session.flush();
		}
	}
	
	/*
	 * clone projections
	 */
	private void cloneProjections(ConservationAreaClonerEngine engine){
		List<Projection> projections = HibernateManager.getCaProjectionList(engine.getTemplateCa(), engine.getSession());
		for (Projection p : projections){
			Projection clone = new Projection();
			clone.setConservationArea(engine.getNewCa());
			clone.setDefinition(p.getDefinition());
			clone.setIsDefault(p.getIsDefault());
			clone.setName(p.getName());
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(p, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clone stations 
	 */
	private void cloneStations(ConservationAreaClonerEngine engine){
		List<Station> stations = HibernateManager.getStations(engine.getTemplateCa(), engine.getSession());
		for (Station s : stations){
			Station clone = new Station();
			clone.setConservationArea(engine.getNewCa());
			clone.setIsActive(s.getIsActive());
			engine.copyLabels(s, clone);
			engine.copyDescriptions(s, clone);
			
			engine.getSession().save(clone);
			engine.addConservationItemMapping(s, clone);
		}
		engine.getSession().flush();
	}
	
	/*
	 * clones the agency and rank information for a data model
	 */
	private void cloneAgenciesRank(ConservationAreaClonerEngine engine){
		List<Agency> agencies = HibernateManager.getAgencies(engine.getTemplateCa(), engine.getSession());
		
		for (Agency a : agencies){
			Agency clone = new Agency();
			clone.setConservationArea(engine.getNewCa());
			engine.copyLabels(a, clone);
			
			for (Rank r : a.getRanks()){
				Rank rclone = new Rank();
				rclone.setAgency(clone);
				engine.copyLabels(r, rclone);
				clone.getRanks().add(rclone);
			}
			
			engine.getSession().save(clone);
		}
		engine.getSession().flush();
	}


	
}
