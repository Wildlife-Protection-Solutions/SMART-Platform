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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.SmartStyle;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
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
	 * 
	 */
	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.ConservationAreaTemplateCloner_Progress_CopyCaInfo, 6);
		//need to clone: agencies, ranks, stations, projections,
		//data model (categories, attributes, attribute list items, 
		//attribute tree items, attribute aggregations
		//and all associated labels
		
		//DO NOT CLONE: employees, saved maps, area geometries
		progress.subTask(Messages.ConservationAreaTemplateCloner_Progress_CopyAgencyRank);
		cloneAgenciesRank(engine);
		progress.worked(1);
			
		progress.subTask(Messages.ConservationAreaTemplateCloner_Progress_CopyStation);
		cloneStations(engine);
		progress.worked(1);
			
		progress.subTask(Messages.ConservationAreaTemplateCloner_Progress_CopyProjection);
		cloneProjections(engine);
		progress.worked(1);
			
		progress.subTask(Messages.ConservationAreaTemplateCloner_CloningIcons);
		Collection<Icon> icons = cloneIconSets(engine);
		progress.worked(1);
		
		progress.subTask(Messages.ConservationAreaTemplateCloner_Progress_CopyDataModel);
		cloneDataModel(engine, icons, progress.split(1));
			
		progress.subTask(Messages.ConservationAreaTemplateCloner_Progress_CopyStyles);
		cloneMapStyles(engine);
		progress.worked(1);
		
	}
	
	/**
	 * Clone icon sets and icons
	 * @param engine
	 */
	private Collection<Icon> cloneIconSets(ConservationAreaClonerEngine engine) {
		Session session = engine.getSession();
		
		//clone icon sets
		List<IconSet> sets = QueryFactory.buildQuery(session, IconSet.class, new Object[] {"conservationArea",engine.getTemplateCa()}).list(); //$NON-NLS-1$
		for (IconSet s : sets) {
			IconSet newSet = new IconSet();
			newSet.setConservationArea(engine.getNewCa());
			engine.copyLabels(s, newSet);
			newSet.setIsDefault(s.getIsDefault());
			newSet.setKeyId(s.getKeyId());
			
			session.save(newSet);
			
			engine.addConservationItemMapping(s, newSet);
		}
		session.flush();
		
		//clone icons
		List<Icon> icons = QueryFactory.buildQuery(session, Icon.class, new Object[] {"conservationArea",engine.getTemplateCa()}).list(); //$NON-NLS-1$
		List<Icon> cloned = new ArrayList<>();
		for (Icon icon : icons) {
			Icon newIcon = new Icon();
			newIcon.setKeyId(icon.getKeyId());
			engine.copyLabels(icon, newIcon);
			newIcon.setConservationArea(engine.getNewCa());
			newIcon.setFiles(new ArrayList<>());
			
			for (IconFile file : icon.getFiles()) {
				IconFile newFile = new IconFile();
				newIcon.getFiles().add(newFile);
				newFile.setIcon(newIcon);
				newFile.setFilename(file.getFilename());
				newFile.setIconSet((IconSet) engine.getNewConservationItem(file.getIconSet()));
				
				if (!newFile.isSystemIcon()) {
					file.computeFileLocation(session);
					newFile.computeFileLocation(session);
					Path from = file.getAttachmentFile();
					Path to = newFile.getAttachmentFile();
					
					try {
						Files.createDirectories(to.getParent());
						Files.copy(from, to);
					} catch (IOException e) {
						SmartPlugIn.log(e.getMessage(), e);
					}
				}
			}
			
			session.save(newIcon);
			engine.addConservationItemMapping(icon, newIcon);
			cloned.add(newIcon);
		}
		session.flush();
		return cloned;
	}
	
	/*
	 * clone saved map styles
	 */
	private void cloneMapStyles(ConservationAreaClonerEngine engine){
		Session session = engine.getSession();
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<SmartStyle> query = cb.createQuery(SmartStyle.class);
		Root<SmartStyle> root = query.from(SmartStyle.class);
		query.where(cb.equal(root.get("conservationArea"), engine.getTemplateCa())); //$NON-NLS-1$
		List<SmartStyle> stylesToClone = session.createQuery(query).getResultList();
		
		for (SmartStyle style : stylesToClone){
			SmartStyle clone = new SmartStyle();
			clone.setConservationArea(engine.getNewCa());
			clone.setStyleString(style.getStyleString());
			engine.copyLabels(style, clone);
			session.save(clone);
			engine.addConservationItemMapping(style, clone);
		}
		session.flush();
	}
	
	
	/*
	 * clone the data model (including icons)
	 */
	private void cloneDataModel(ConservationAreaClonerEngine engine, Collection<Icon> icons, IProgressMonitor monitor){
		DataModel templateDm = HibernateManager.loadDataModel(engine.getTemplateCa(), engine.getSession());
		DataModel clonedDm = templateDm.clone(engine.getNewCa(), null, icons, monitor);
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
			clone.setKeyId(a.getKeyId());
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
