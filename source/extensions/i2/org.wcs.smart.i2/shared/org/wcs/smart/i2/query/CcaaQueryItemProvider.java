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
package org.wcs.smart.i2.query;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModelMerger;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.util.UuidUtils;

/**
 * Multiple conservation area item provider for cross CCAA
 * queries.
 * 
 * @author Emily
 *
 */
public class CcaaQueryItemProvider implements IQueryItemProvider {

	private Set<IntelProfile> profiles = null;
	private Set<ConservationArea> areas = null;
	private ConservationArea queryCa = null;
		
	protected volatile SimpleDataModel mergedDataModel = null;
	
	protected Set<IntelRecordSource> recordSources = null;
	
	/**
	 * 
	 * @param profiles set of profiles this user has query permission for 
	 * @param queryCa
	 */
	public CcaaQueryItemProvider(Set<IntelProfile> profiles, ConservationArea queryCa) {
		this.profiles = profiles;
		this.queryCa = queryCa;
		this.areas = profiles.stream().map(a->a.getConservationArea()).collect(Collectors.toSet());
	}
	
	@Override
	public ConservationArea getQueryConservationArea() {
		return this.queryCa;
	}
	
	@Override
	public Collection<ConservationArea> getConservationAreas(){
		return areas;
	}
	
	@Override
	public List<Employee> getEmployees(Session session){
		List<Employee> employees = session.createQuery("FROM Employee WHERE conservationArea in (:cas)", Employee.class) //$NON-NLS-1$
		.setParameterList("cas", getConservationAreas()) //$NON-NLS-1$
		.list();
		employees.sort((a,b)-> (a.getGivenName() + a.getFamilyName() + a.getId()).compareTo(b.getGivenName() + b.getFamilyName() + b.getId()));
		return employees;
	}
	
	@Override
	public List<IntelEntityTypeAttribute> getEntityTypeAttributes(IntelEntityType entityType, Session session){
		
		HashMap<String, IntelEntityTypeAttribute> attributes = new HashMap<>();
		List<IntelEntityType> allTypes = session.createQuery("FROM IntelEntityType WHERE keyId = :keyid AND conservationArea in (:cas)", IntelEntityType.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()) //$NON-NLS-1$
				.setParameter("keyid",  entityType.getKeyId()) //$NON-NLS-1$
				.list();
		
		for (IntelEntityType type : allTypes) {
			for (IntelEntityTypeAttribute attribute : type.getAttributes()) {
				IntelEntityTypeAttribute newAttribute = attributes.get(attribute.getAttribute().getKeyId());
				if (newAttribute == null) {
					IntelAttribute aNewAttribute = new IntelAttribute();
					aNewAttribute.setName(attribute.getAttribute().getName());
					aNewAttribute.setKeyId(attribute.getAttribute().getKeyId());
					aNewAttribute.setType(attribute.getAttribute().getType());
					
					newAttribute = new IntelEntityTypeAttribute();
					newAttribute.setAttribute(aNewAttribute);
					newAttribute.setEntityType(entityType);
					
					attributes.put(aNewAttribute.getKeyId(), newAttribute);
				}
				if (attribute.getAttribute().getConservationArea().equals(getMainConservationArea())) {
					newAttribute.getAttribute().setName(attribute.getAttribute().getName());
				}
				
			}
		}
		
		return attributes.values().stream().collect(Collectors.toList());
	}
	
	public List<IntelRecordSource> getRecordSources(Session session) {
		HashMap<String, IntelRecordSource> types = new HashMap<>();
		for (IntelRecordSource type : getRecordSourcesInternal(session)) {
			IntelRecordSource newType = types.get(type.getKeyId());
			if (newType == null) {
				newType = new IntelRecordSource();
				newType.setKeyId(type.getKeyId());
				newType.setIcon(type.getIcon());
				newType.setName(type.getName());
				
				types.put(type.getKeyId(), newType);
			}
			if (type.getConservationArea().equals(getMainConservationArea())) {
				newType.setName(type.getName());
			}
		}
		List<IntelRecordSource> newTypes = new ArrayList<>();
		newTypes.addAll(types.values());
		return newTypes;
	}
	
	@Override
	public List<IntelEntityType> getEntityTypes(Set<UUID> profiles, Session session){
		HashMap<String, IntelEntityType> types = new HashMap<>();
		
		List<IntelEntityType> allTypes = session.createQuery("FROM IntelEntityType WHERE conservationArea in (:cas)", IntelEntityType.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()).list(); //$NON-NLS-1$
		
		for (IntelEntityType type : allTypes) {
			boolean keep = false;
			for (IntelProfile ip : type.getProfiles()) {
				if (profiles.contains(ip.getUuid())) {
					keep = true;
					break;
				}
			}
			if (!keep) continue;
			
			IntelEntityType newType = types.get(type.getKeyId());
			if (newType == null) {
				newType = new IntelEntityType();
				newType.setKeyId(type.getKeyId());
				newType.setIcon(type.getIcon());
				newType.setName(type.getName());
				
				types.put(type.getKeyId(), newType);
			}
			if (type.getConservationArea().equals(getMainConservationArea())) {
				newType.setName(type.getName());
			}
		}
		List<IntelEntityType> newTypes = new ArrayList<>();
		newTypes.addAll(types.values());
		return newTypes;
	}
	
	@Override
	public List<Area> getAreas(Area.AreaType areaType, Session session){
		List<Area> allAreas = session.createQuery("FROM Area WHERE type = :atype and conservationArea = :ca", Area.class) //$NON-NLS-1$
				.setParameter("ca",  getQueryConservationArea()) //$NON-NLS-1$
				.setParameter("atype",  areaType) //$NON-NLS-1$
				.list();
		allAreas.sort((a,b)-> (a.getKeyId() + UuidUtils.uuidToString(a.getUuid())).compareTo(b.getKeyId() + UuidUtils.uuidToString(b.getUuid())));
		return allAreas;
	}
	
	@Override
	public IntelRecordSource getRecordSource(String recordsourceKey, Session session) {
		for (IntelRecordSource s : getRecordSourcesInternal(session)) {
			if (s.getConservationArea().equals(getMainConservationArea()) && s.getKeyId().equalsIgnoreCase(recordsourceKey)) {
				return s;
			}
		}
		
		for (IntelRecordSource s : getRecordSourcesInternal(session)) {
			if (s.getKeyId().equalsIgnoreCase(recordsourceKey)) {
				return s;
			}
		}
		return null;
	}
	

	@Override
	public Collection<IntelProfile> getProfiles(Set<String> profileKeys, Session session) {
		return session.createQuery("FROM IntelProfile WHERE conservationArea IN (:ca) and keyId IN (:keys)", IntelProfile.class)
				.setParameterList("ca", getConservationAreas())
				.setParameterList("keys", profileKeys)
				.list();
	}
	
	@Override
	public IntelEntityType getEntityType(String entityTypeKey, Session session) {
		List<IntelEntityType> allAttributes = session.createQuery("FROM IntelEntityType WHERE keyId = :attribute and conservationArea in (:cas)", IntelEntityType.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()) //$NON-NLS-1$
				.setParameter("attribute",  entityTypeKey) //$NON-NLS-1$
				.list();
		if (allAttributes.size() == 0) return null;
		
		for (IntelEntityType type : allAttributes) {
			if (type.getConservationArea().equals(getMainConservationArea())) {
				IntelEntityType a = new IntelEntityType();
				a.setKeyId(type.getKeyId());
				a.setName(type.getName());
				a.setIcon(type.getIcon());
				return a;
			}
		}
		
		//pick one of the conservation areas
		IntelEntityType a = new IntelEntityType();
		a.setKeyId(allAttributes.get(0).getKeyId());
		a.setName(allAttributes.get(0).getName());
		a.setIcon(allAttributes.get(0).getIcon());
		return a;
		
	}
	
	@Override
	public IntelAttribute getAttribute(String attributeKey, Session session) {
		List<IntelAttribute> allAttributes = session.createQuery("FROM IntelAttribute WHERE keyId = :attribute and conservationArea in (:cas)", IntelAttribute.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()) //$NON-NLS-1$
				.setParameter("attribute",  attributeKey) //$NON-NLS-1$
				.list();
		if (allAttributes.size() == 0) return null;
		for (IntelAttribute type : allAttributes) {
			if (type.getConservationArea().equals(getMainConservationArea())) {
				IntelAttribute a = new IntelAttribute();
				a.setKeyId(type.getKeyId());
				a.setName(type.getName());
				a.setType(type.getType());
				return a;
			}
		}
		//pick one of the conservation areas
		IntelAttribute a = new IntelAttribute();
		a.setKeyId(allAttributes.get(0).getKeyId());
		a.setName(allAttributes.get(0).getName());
		a.setType(allAttributes.get(0).getType());
		return a;
	}
	
	@Override
	public List<IntelAttributeListItem> getAttributeListItems(String attributeKey, Session session){
		List<IntelAttribute> allAttributes = session.createQuery("FROM IntelAttribute WHERE keyId = :attribute and conservationArea in (:cas)", IntelAttribute.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()) //$NON-NLS-1$
				.setParameter("attribute",  attributeKey) //$NON-NLS-1$
				.list();
		
		IntelAttribute attributeClone = null;;
		
		HashMap<String, IntelAttributeListItem> listItems = new HashMap<>();
		for (IntelAttribute attribute : allAttributes) {
			if (attributeClone == null) {
				attributeClone = new IntelAttribute();
				attributeClone.setKeyId(attribute.getKeyId());
				attributeClone.setName(attribute.getName());
				attributeClone.setAttributeList(new ArrayList<>());
				attributeClone.setType(attribute.getType());
			}
			for (IntelAttributeListItem item : attribute.getAttributeList()) {
				IntelAttributeListItem newItem = listItems.get(item.getKeyId());
				if (newItem == null) {
					newItem = new IntelAttributeListItem();
					newItem.setKeyId(item.getKeyId());
					newItem.setName(item.getName());
					newItem.setAttribute(attributeClone);
					newItem.setOrder(item.getOrder());
					attributeClone.getAttributeList().add(newItem);
					
					listItems.put(item.getKeyId(), newItem);
				}
				
				if (item.getAttribute().getConservationArea().equals(getMainConservationArea())) {
					newItem.setName(item.getName());
				}
			}
		}
		
		List<IntelAttributeListItem> items = new ArrayList<>();
		items.addAll(listItems.values());
		items.sort((a,b) -> Collator.getInstance().compare(a.getKeyId(), b.getKeyId()));
		return items;
	}


	/**
	 * By default the main conservation area is the first one in the list;
	 * you can change this by overwriting this function
	 * 
	 */
	protected ConservationArea getMainConservationArea() {
		return areas.iterator().next();
	}

	
	
	@Override
	public List<IntelEntity> getEntities(Set<UUID> profiles, String entityTypeKey, Session session){
		return session.createQuery("SELECT i FROM IntelEntity i join i.entityType t WHERE t.keyId = :entityType and i.profile.uuid IN (:profiles)", IntelEntity.class) //$NON-NLS-1$
			.setParameter("entityType", entityTypeKey) //$NON-NLS-1$
			.setParameterList("profiles", profiles)
			.list();
	}
	
	
	@Override
	public List<IntelAttribute> getAttributes(Session session){
		HashMap<String, IntelAttribute> attributes = new HashMap<>();
		
		List<IntelAttribute> allAttributes = session.createQuery("FROM IntelAttribute WHERE conservationArea in (:cas)", IntelAttribute.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()).list(); //$NON-NLS-1$
		
		for (IntelAttribute type : allAttributes) {
			IntelAttribute newType = attributes.get(type.getKeyId());
			if (newType == null) {
				newType = new IntelAttribute();
				newType.setKeyId(type.getKeyId());
				newType.setName(type.getName());
				newType.setType(type.getType());
				
				attributes.put(type.getKeyId(), newType);
			}
			if (type.getConservationArea().equals(getMainConservationArea())) {
				newType.setName(type.getName());
			}
		}
		List<IntelAttribute> newAttributes = new ArrayList<>();
		newAttributes.addAll(attributes.values());
		return newAttributes;
	}
	
	@Override
	public List<Category> getRootCategories(Session session){
		return getDataModel(session).getCategories();
	}
	
	@Override
	public List<Attribute> getDmAttributes(Session session){
		return getDataModel(session).getAttributes();
	}
	
	@Override
	public List<Category> getChildren(Category category, Session session){
		return category.getChildren();
	}
	
	@Override
	public Category getCategory(String categoryHkey, Session session){
		List<Category> toSearch = new ArrayList<>();
		toSearch.addAll(getDataModel(session).getCategories());
		while(!toSearch.isEmpty()) {
			Category temp = toSearch.remove(0);
			if (temp.getHkey().equals(categoryHkey)) {
				return temp;
			}
			if (temp.getChildren() != null) toSearch.addAll(temp.getChildren());
		}
		return null;
	}
	

	
	@Override
	public Attribute getDmAttribute(String attributeKey, Session session) {
		for (Attribute a : getDmAttributes(session)) {
			if (a.getKeyId().equals(attributeKey)) return a;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<AttributeListItem> getDmAttributeListItem(Attribute attribute, Session session){
		//we need to only include items that are shared across all conservation areas
		String query = "SELECT a.keyId FROM AttributeListItem a join a.attribute b WHERE b.keyId = :attributeKey AND b.conservationArea IN (:cas) group by a.keyId having count(*) = :cnt"; //$NON-NLS-1$
		Query<?> q = session.createQuery(query);
		q.setParameterList("cas", getConservationAreas()); //$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
		q.setParameter("cnt", Long.valueOf(getConservationAreas().size())); //$NON-NLS-1$
				
		List<?> keys = q.list();
		if (keys.size() == 0){
			//return empty list
			return new ArrayList<AttributeListItem>();
		}
		query = "FROM AttributeListItem a WHERE a.attribute.keyId = :attributeKey AND a.attribute.conservationArea = :ca AND a.keyId IN (:keys)"; //$NON-NLS-1$
		q = session.createQuery(query);
		q.setParameter("ca", getMainConservationArea()); //$NON-NLS-1$
		q.setParameterList("keys", keys); //$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
				
		List<AttributeListItem> items = (List<AttributeListItem>) q.list();
		return items;	
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<AttributeTreeNode> getDmAttributeTreeNodes(Attribute attribute, Session session){
		//we need to only include items that are shared across all conservation areas
		String query = "SELECT a.hkey FROM AttributeTreeNode a join a.attribute b WHERE b.keyId = :attributeKey AND b.conservationArea in (:cas) group by a.hkey having count(*) = :cnt"; //$NON-NLS-1$
		Query<?> q = session.createQuery(query);
		q.setParameterList("cas", getConservationAreas()); //$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
		q.setParameter("cnt", Long.valueOf(getConservationAreas().size())); //$NON-NLS-1$
		
		List<String> hkeys = (List<String>) q.list();
		if (hkeys.size() == 0){
			return new ArrayList<AttributeTreeNode>();
		}
		query = "FROM AttributeTreeNode a WHERE a.attribute.keyId = :attributeKey AND a.attribute.conservationArea = :ca and a.hkey IN (:keys) and parent is null"; //$NON-NLS-1$
		q = session.createQuery(query);
		q.setParameter("ca", getMainConservationArea()); //$NON-NLS-1$
		q.setParameterList("keys", hkeys); //$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
		
		List<AttributeTreeNode> roots = (List<AttributeTreeNode>) q.list();			
		return roots;		
	}
	
	@Override
	public int getMaxDmCategoryDepth(Session session) {
		Integer cnt = (Integer) session.createNativeQuery("SELECT max(smart.hkeylength(hkey)) from smart.dm_category WHERE ca_uuid in (:ca)") //$NON-NLS-1$
				.setParameterList("ca",  getConservationAreas().stream().map(c->c.getUuid()).collect(Collectors.toList())).uniqueResult(); //$NON-NLS-1$
		return cnt.intValue();
	}
	
	protected SimpleDataModel getDataModel(final Session session) {
		if (mergedDataModel == null) {
			synchronized (this) {
				if (mergedDataModel != null) return mergedDataModel;
				DataModelMerger merger = new DataModelMerger();
				final ConservationArea[] cas =getConservationAreas().toArray(new ConservationArea[getConservationAreas().size()]);
				final ConservationArea defaultCa = getMainConservationArea();
				mergedDataModel = merger.mergeDataModels(cas, defaultCa, session, null, new NullProgressMonitor());
			}
		}
		return mergedDataModel;
	}

	@Override
	public List<IntelRecordSource> getRecordSources(Set<UUID> profiles, Session session) {
		
		HashMap<String, IntelRecordSource> types = new HashMap<>();
		
		for (IntelRecordSource type : getRecordSourcesInternal(session)) {
			boolean add = false;
			for (IntelProfile ip : type.getProfiles()) {
				if (profiles.contains(ip.getUuid())) {
					add = true;
				}
			}
			if (!add) continue;
			
			IntelRecordSource newType = types.get(type.getKeyId());
			if (newType == null) {
				newType = new IntelRecordSource();
				newType.setKeyId(type.getKeyId());
				newType.setIcon(type.getIcon());
				newType.setName(type.getName());
				
				types.put(type.getKeyId(), newType);
			}
			if (type.getConservationArea().equals(getMainConservationArea())) {
				newType.setName(type.getName());
			}
		}
		List<IntelRecordSource> newTypes = new ArrayList<>();
		newTypes.addAll(types.values());
		return newTypes;
	}

	@Override
	public List<IntelRecordSourceAttribute> getRecordSourceAttributes(IntelRecordSource recordSource, Session session) {
		//find all attributes that are associated with record source in
		
		Set<String> donotuse = new HashSet<>();
		HashMap<String, IntelRecordSourceAttribute> attributes = new HashMap<>();
		
		Set<IntelRecordSource> items = getRecordSourcesInternal(session);
		for (IntelRecordSource source : items) {
			if (!source.getKeyId().equalsIgnoreCase(recordSource.getKeyId())) continue;
			
			for (IntelRecordSourceAttribute a : source.getAttributes()) {
				if (donotuse.contains(a.getKeyId())) continue;
				
				if (attributes.containsKey(a.getKeyId())){
					//if they are the same skip
					if (!areSimilar(a, attributes.get(a.getKeyId()))) {
						//if they are different add to donouse, remove from attributes and continue
						attributes.remove(a.getKeyId());
						donotuse.add(a.getKeyId());
					}
				}else {
					//add attribute
					attributes.put(a.getKeyId(), a);
				}
			}	
		}
		return new ArrayList<>(attributes.values());
	}
	
	private boolean areSimilar(IntelRecordSourceAttribute a, IntelRecordSourceAttribute b) {
		if (a.getIsMultiple() != b.getIsMultiple()) return false;
		if (a.getAttribute() != null && b.getAttribute() != null && a.getAttribute().getKeyId().equalsIgnoreCase(b.getAttribute().getKeyId())) {
			return true;
		}
		if (a.getEntityType() != null && b.getEntityType() != null && a.getEntityType().getKeyId().equalsIgnoreCase(b.getKeyId())) {
			return true;
		}
		return false;
	}
	
	public Set<IntelRecordSource> getRecordSourcesInternal(Session session){
		if (recordSources != null) return recordSources;
		
		Set<IntelRecordSource> items = new HashSet<>();
		
		List<IntelRecordSource> allTypes = session.createQuery("FROM IntelRecordSource WHERE conservationArea in (:cas)", IntelRecordSource.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()).list(); //$NON-NLS-1$
		
		
		for (IntelRecordSource type : allTypes) {
			boolean found = false;
			for (IntelProfile t : type.getProfiles()) {
				if (profiles.contains(t)) {
					found = true;
					break;
				}
			}
			if (!found) continue;
			items.add(type);
		}
		recordSources = items;
		return recordSources;
	}
}
