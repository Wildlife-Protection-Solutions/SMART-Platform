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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.core.runtime.Assert;
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
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;

/**
 * Job which is responsible for loading data for {@link IntelEntity} relationships graph.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public abstract class RelationshipGraphLoadDataJob extends Job {

	private IntelEntity[] roots;
	private RelationshipGraphFilterData filterData = new RelationshipGraphFilterData();

	public RelationshipGraphLoadDataJob() {
		super(Messages.RelationshipGraphLoadDataJob_JobTitle);
	}
	
	public void setRoots(IntelEntity... roots) {
		this.roots = roots;
	}
	
	public void setFilterData(RelationshipGraphFilterData filterData) {
		Assert.isNotNull(filterData);
		this.filterData = filterData;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IntelEntity[] copyRoots = roots;
		RelationshipGraphFilterData fd = filterData;
		
		if (roots == null || copyRoots.length == 0) {
			processData(new GraphData());
			return Status.OK_STATUS;
		}
		Set<IntelEntity> entities = new HashSet<>();
		Set<IntelEntityRelationship> relationships = new HashSet<>();
		List<IntelEntity> rootEntities;
		
		try (Session s = HibernateManager.openSession()) {
			for (IntelEntity e : roots) {
				IntelEntity tmp = (IntelEntity) s.get(IntelEntity.class, e.getUuid());
				if (tmp == null) continue;	//not found; probably deleted
				entities.add(tmp);
			}

			rootEntities = new ArrayList<>(entities);
			for (IntelEntity e : rootEntities) {
				extractRelations(s, entities, relationships, e, fd, fd.getDepth()-1);
			}
			
			//load required lazy data
			for (IntelEntity e : entities) {
				e.getEntityType().getAttributes().size();
				for (IntelEntityTypeAttribute a : e.getEntityType().getAttributes()) {
					a.getAttribute().getNames().size();
				}
				for (IntelEntityAttributeValue v : e.getAttributes()) {
					v.getAttribute().getNames().size();
					if (v.getAttributeListItem() != null) {
						v.getAttributeListItem().getNames().size();
					}
					if (v.getEmployee() != null) {
						v.getEmployee().getId();
					}
				}
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
			for (IntelEntityRelationship r : relationships) {
				r.getRelationshipType().getNames().size();
			}
		}
		
		GraphData graphData = new GraphData(rootEntities, entities, relationships);
		processData(graphData);

		return Status.OK_STATUS;
	}

	private void extractRelations(Session s, Set<IntelEntity> entities, Set<IntelEntityRelationship> relationships, IntelEntity intelEntity, RelationshipGraphFilterData fd, int depth) {
		List<IntelEntityRelationship> newRepationships = fetchRelations(s, intelEntity, fd);
		
		//remove known relationships
		newRepationships.removeAll(relationships);
		
		Set<IntelEntity> newEntities = new HashSet<>();
		for (IntelEntityRelationship r : newRepationships) {
			if (fd.getEntityTypes().contains(r.getSourceEntity().getEntityType())) {
				newEntities.add(r.getSourceEntity());
			}
			if (fd.getEntityTypes().contains(r.getTargetEntity().getEntityType())) {
				newEntities.add(r.getTargetEntity());
			}
		}

		//remove known entities
		newEntities.removeAll(entities);
		
		//update graph data with newly found objects
		relationships.addAll(newRepationships);
		entities.addAll(newEntities);

		//look deeper into the graph data
		if (depth > 0) {
			depth --;
			for (IntelEntity ie : newEntities) {
				extractRelations(s, entities, relationships, ie, fd, depth);
			}
		}
	}

	private List<IntelEntityRelationship> fetchRelations(Session s, IntelEntity intelEntity, RelationshipGraphFilterData fd) {
		if (fd.getRelationshipTypes().isEmpty()) {
			return Collections.emptyList(); 
		}
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<IntelEntityRelationship> c = cb.createQuery(IntelEntityRelationship.class);
		Root<IntelEntityRelationship> from = c.from(IntelEntityRelationship.class);
		Predicate entityPredicate = cb.or(
				cb.equal(from.get("sourceEntity"), intelEntity), //$NON-NLS-1$
				cb.equal(from.get("targetEntity"), intelEntity) //$NON-NLS-1$
				);
		Predicate rtPredicate = from.get("relationshipType").in(fd.getRelationshipTypes()); //$NON-NLS-1$
		c.where(cb.and(entityPredicate, rtPredicate));
		List<IntelEntityRelationship> newRepationships = s.createQuery(c).getResultList();
		return newRepationships;
	}
	
	protected abstract void processData(IRelationshipGraphData graphData);
	
	/**
	 * Data for relationship diagram.
	 * 
	 * @author elitvin
	 * @since 6.0.0
	 */
	private static class GraphData implements IRelationshipGraphData {
		private Set<IntelEntity> roots;
		private IntelEntity[] entities;
		private Map<IntelEntity, Set<IntelEntity>> targetsMap;
		private Map<IntelEntityPair, IntelEntityRelationship> relMap;

		private GraphData() {
			this(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
		}

		private GraphData(Collection<IntelEntity> rootsCol, Collection<IntelEntity> entitiesCol, Collection<IntelEntityRelationship> relationshipsCol) {
			Assert.isNotNull(rootsCol);
			Assert.isNotNull(entitiesCol);
			Assert.isNotNull(relationshipsCol);
			roots = new HashSet<>(rootsCol);
			this.entities = entitiesCol.toArray(new IntelEntity[entitiesCol.size()]);
			targetsMap = new HashMap<>();
			relMap = new HashMap<>();
			for (IntelEntityRelationship r : relationshipsCol) {
				IntelEntity s = r.getSourceEntity();
				IntelEntity t = r.getTargetEntity();
				if (entitiesCol.contains(t)) { //we may have some relationships that point to targets that are excluded due to filtering
					Set<IntelEntity> tSet = targetsMap.get(s);
					if (tSet == null) {
						tSet = new HashSet<>();
						targetsMap.put(s, tSet);
					}
					tSet.add(t);
				}
				
				relMap.put(new IntelEntityPair(s, t), r);
			}
		}

		@Override
		public boolean isRootNode(Object entity) {
			return roots.contains(entity);
		}
		
		@Override
		public IntelEntity[] getEntities() {
			return entities;
		}

		@Override
		public IntelEntity[] getTargets(Object source) {
			Set<IntelEntity> tSet = targetsMap.get(source);
			return tSet != null ? tSet.toArray(new IntelEntity[tSet.size()]) : null;
		}
		
		@Override
		public IntelEntityRelationship getRelationship(Object source, Object target) {
			if (source instanceof IntelEntity && target instanceof IntelEntity) {
				return relMap.get(new IntelEntityPair((IntelEntity)source, (IntelEntity)target));
			}
			return null;
		}
	}

	/**
	 * Used for references search optimization.
	 * 
	 * @author elitvin
	 * @since 6.0.0
	 *
	 */
	private static class IntelEntityPair {
		private IntelEntity source;
		private IntelEntity target;
		
		public IntelEntityPair(IntelEntity source, IntelEntity target) {
			Assert.isNotNull(source);
			Assert.isNotNull(target);
			this.source = source;
			this.target = target;
		}

		@Override
		public boolean equals(Object other) {
			if (other != null && other instanceof IntelEntityPair) {
				IntelEntityPair x = (IntelEntityPair)other;
				return source.equals(x.source) && target.equals(x.target);
			}
			return false;
		}
		
		public int hashCode() {
			return source.hashCode() ^ (target.hashCode() << 1);
		}
	}
	
}
