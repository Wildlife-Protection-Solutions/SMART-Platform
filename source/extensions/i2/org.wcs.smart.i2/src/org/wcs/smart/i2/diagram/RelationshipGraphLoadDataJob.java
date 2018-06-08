/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.diagram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRelationship;

/**
 * Job which is responsible for loading data for {@link IntelEntity} relationships graph.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphLoadDataJob extends Job {

	RelationshipGraphFilterData filterData;
	private IntelEntity[] roots;
	private GraphData loadedData;
	
	public RelationshipGraphLoadDataJob(RelationshipGraphFilterData filterData, IntelEntity... roots) {
		super(Messages.RelationshipGraphLoadDataJob_JobTitle);
		this.filterData = filterData;
		this.roots = roots;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		loadedData = new GraphData();
		if (roots == null || roots.length == 0) {
			return Status.OK_STATUS;
		}
		GraphData graphData = new GraphData();
		try (Session s = HibernateManager.openSession()) {
			for (IntelEntity e : roots) {
				IntelEntity tmp = (IntelEntity) s.get(IntelEntity.class, e.getUuid());
				graphData.getEntities().add(tmp);
			}

			List<IntelEntity> rootEntities = new ArrayList<>(graphData.entities);
			for (IntelEntity e : rootEntities) {
				extractRelations(s, graphData, e, filterData.getDepth()-1);
			}
			
			//load required lazy data
			for (IntelEntity e : graphData.getEntities()) {
				e.getIdAttributeAsText();
				IntelAttachment pa = e.getPrimaryAttachment();
				if (pa != null) {
					try {
						pa.computeFileLocation(s);
					} catch (Exception ex) {
						SmartPlugIn.log("Failed to compute file location", ex); //$NON-NLS-1$
					}
				}
			}
			for (IntelEntityRelationship r : graphData.getRelationships()) {
				r.getRelationshipType().getNames().size();
			}
		}
		
		loadedData = graphData;
		
		return Status.OK_STATUS;
	}

	private void extractRelations(Session s, GraphData graphData, IntelEntity intelEntity, int depth) {
		List<IntelEntityRelationship> newRepationships = fetchRelations(s, intelEntity);
		
		//remove known relationships
		newRepationships.removeAll(graphData.getRelationships());
		
		Set<IntelEntity> newEntities = new HashSet<>();
		for (IntelEntityRelationship r : newRepationships) {
			if (filterData.getEntityTypes().contains(r.getSourceEntity().getEntityType())) {
				newEntities.add(r.getSourceEntity());
			}
			if (filterData.getEntityTypes().contains(r.getTargetEntity().getEntityType())) {
				newEntities.add(r.getTargetEntity());
			}
		}

		//remove known entities
		newEntities.removeAll(graphData.getEntities());
		
		//update graph data with newly found objects
		graphData.getRelationships().addAll(newRepationships);
		graphData.getEntities().addAll(newEntities);

		//look deeper into the graph data
		if (depth > 0) {
			depth --;
			for (IntelEntity ie : newEntities) {
				extractRelations(s, graphData, ie, depth);
			}
		}
	}

	private List<IntelEntityRelationship> fetchRelations(Session s, IntelEntity intelEntity) {
		if (filterData.getRelationshipTypes().isEmpty()) {
			return Collections.emptyList(); 
		}
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<IntelEntityRelationship> c = cb.createQuery(IntelEntityRelationship.class);
		Root<IntelEntityRelationship> from = c.from(IntelEntityRelationship.class);
		Predicate entityPredicate = cb.or(
				cb.equal(from.get("sourceEntity"), intelEntity), //$NON-NLS-1$
				cb.equal(from.get("targetEntity"), intelEntity) //$NON-NLS-1$
				);
		Predicate rtPredicate = from.get("relationshipType").in(filterData.getRelationshipTypes()); //$NON-NLS-1$
		c.where(cb.and(entityPredicate, rtPredicate));
		List<IntelEntityRelationship> newRepationships = s.createQuery(c).getResultList();
		return newRepationships;
	}
	
	public GraphData getLoadedData() {
		return loadedData;
	}
	
	protected static class GraphData {
		private Set<IntelEntity> entities;
		private Set<IntelEntityRelationship> relationships;

		public GraphData() {
			this.entities = new HashSet<>();
			this.relationships = new HashSet<>();
		}
		
		public GraphData(Set<IntelEntity> entities, Set<IntelEntityRelationship> relationships) {
			this.entities = entities;
			this.relationships = relationships;
		}

		public Set<IntelEntity> getEntities() {
			return entities;
		}

		public Set<IntelEntityRelationship> getRelationships() {
			return relationships;
		}
	}
}
