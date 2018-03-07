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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Multiple conservation area item provider for cross CCAA
 * queries.
 * 
 * @author Emily
 *
 */
public class CcaaQueryItemProvider implements IQueryItemProvider {

	private Collection<ConservationArea> conservationAreas = null;
	private ConservationArea queryCa;
	
	public CcaaQueryItemProvider(Collection<ConservationArea> conservationAreas, ConservationArea queryCa) {
		this.conservationAreas = conservationAreas;
		this.queryCa = queryCa;
	}
	
	/**
	 * The conservation area associated with the query
	 */
	public ConservationArea getQueryConservationArea() {
		return this.queryCa;
	}
	
	/**
	 * Get all the conservation areas to be included in the query
	 * 
	 * @return
	 */
	public Collection<ConservationArea> getConservationAreas(){
		return conservationAreas;
	}
	
	public List<Employee> getEmployees(Session session){
		return session.createQuery("FROM Employee WHERE conservationArea in (:cas)", Employee.class) //$NON-NLS-1$
		.setParameterList("cas", getConservationAreas()) //$NON-NLS-1$
		.list();
	}
	
	public List<IntelEntityType> getEntityTypes(Session session){
		HashMap<String, IntelEntityType> types = new HashMap<>();
		
		List<IntelEntityType> allTypes = session.createQuery("FROM IntelEntityType WHERE conservationArea in (:cas)", IntelEntityType.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()).list(); //$NON-NLS-1$
		
		for (IntelEntityType type : allTypes) {
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
	
	public List<Area> getAreas(Area.AreaType areaType, Session session){
		
		HashMap<String, Area> areas = new HashMap<>();
		
		List<Area> allAreas = session.createQuery("FROM Area WHERE type = :atype and conservationArea in (:cas)", Area.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()) //$NON-NLS-1$
				.setParameter("atype",  areaType) //$NON-NLS-1$
				.list();
		
		for (Area type : allAreas) {
			Area newArea = areas.get(type.getKeyId());
			if (newArea == null) {
				newArea = new Area();
				newArea.setKeyId(type.getKeyId());
				newArea.setName(type.getName());
				areas.put(type.getKeyId(), newArea);
			}
			if (type.getConservationArea().equals(getMainConservationArea())) {
				newArea.setName(type.getName());
			}
		}
		List<Area> newAreas = new ArrayList<>();
		newAreas.addAll(areas.values());
		return newAreas;
	}
	

	@Override
	public IntelEntityType getEntityType(String entityTypeKey, Session session) {
		List<IntelEntityType> allAttributes = session.createQuery("FROM IntelEntityType WHERE keyId = :attribute and conservationArea in (:cas)", IntelEntityType.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()) //$NON-NLS-1$
				.setParameter("attribute",  entityTypeKey) //$NON-NLS-1$
				.list();
		
		for (IntelEntityType type : allAttributes) {
			if (type.getConservationArea().equals(getMainConservationArea())) {
				IntelEntityType a = new IntelEntityType();
				a.setKeyId(type.getKeyId());
				a.setName(type.getName());
				a.setIcon(type.getIcon());
				return a;
			}
		}
		return null;
	}
	
	public IntelAttribute getAttribute(String attributeKey, Session session) {
		List<IntelAttribute> allAttributes = session.createQuery("FROM IntelAttribute WHERE keyId = :attribute and conservationArea in (:cas)", IntelAttribute.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()) //$NON-NLS-1$
				.setParameter("attribute",  attributeKey) //$NON-NLS-1$
				.list();
		
		for (IntelAttribute type : allAttributes) {
			if (type.getConservationArea().equals(getMainConservationArea())) {
				IntelAttribute a = new IntelAttribute();
				a.setKeyId(type.getKeyId());
				a.setName(type.getName());
				a.setType(type.getType());
				return a;
			}
		}
		return null;
	}
	
	public Attribute getDataModelAttribute(String attributeKey, Session session) {
		List<Attribute> allAttributes = session.createQuery("FROM Attribute WHERE keyId = :attribute and conservationArea in (:cas)", Attribute.class) //$NON-NLS-1$
				.setParameterList("cas",  getConservationAreas()) //$NON-NLS-1$
				.setParameter("attribute",  attributeKey) //$NON-NLS-1$
				.list();
		
		for (Attribute type : allAttributes) {
			if (type.getConservationArea().equals(getMainConservationArea())) {
				Attribute a = new Attribute();
				a.setKeyId(type.getKeyId());
				a.setName(type.getName());
				a.setType(type.getType());				
				return a;
			}
		}
		return null;
	}
	
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
		return items;
	}


	/**
	 * By default the main conservation area is the first one in the list;
	 * you can change this by overwriting this function
	 * 
	 * @return
	 */
	protected ConservationArea getMainConservationArea() {
		return conservationAreas.iterator().next();
	}

	@Override
	public AttributeListItem getDataModelAttributeListItem(String attributeKey, String listItemKey, Session session) {
		Query<AttributeListItem> q = session.createQuery(" SELECT ali From AttributeListItem ali join ali.attribute as a where a.conservationArea in (:cas) and ali.keyId = :key and a.keyId = :attributeKey", AttributeListItem.class); //$NON-NLS-1$
		q.setParameterList("cas", getConservationAreas()); //$NON-NLS-1$
		q.setParameter("key", listItemKey); //$NON-NLS-1$
		q.setParameter("attributeKey", attributeKey); //$NON-NLS-1$
		q.setCacheable(true);

		List<AttributeListItem> results = q.list();
		for (AttributeListItem i : results) {
			if (i.getAttribute().getConservationArea().equals(getMainConservationArea())) return i;
		}
		if (results.size() > 0) return results.get(0);
		return null;
	}

	@Override
	public AttributeTreeNode getDataModelAttributeTreeNode(String attributeKey, String hkey, Session session) {
		Query<AttributeTreeNode> q = session.createQuery(" SELECT ali From AttributeTreeNode ali join ali.attribute as a where a.conservationArea IN (:cas) and ali.hkey = :key and a.keyId = :attribute", AttributeTreeNode.class); //$NON-NLS-1$
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
		q.setParameter("key", hkey); //$NON-NLS-1$
		q.setParameter("attribute", attributeKey); //$NON-NLS-1$
		q.setCacheable(true);
		
		List<AttributeTreeNode> results = q.list();
		for (AttributeTreeNode i : results) {
			if (i.getAttribute().getConservationArea().equals(getMainConservationArea())) return i;
		}
		if (results.size() > 0) return results.get(0);
		return null;
	}
}
