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
package org.wcs.smart.patrol.query.hibernate;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Utility functions for supporting query module
 * when logged in to multiple conservation areas
 *  
 * @author Emily
 *
 */
public class MultiCaPatrolQueryHibernateManagerImpl extends
		AbstractPatrolQueryHibernateManager {

	
	
	private Collection<ListItem> getNamedKeyItem(Session session, Class<? extends NamedKeyItem> clazz, boolean onlyActive) {
		
		HashMap<String, ListItem> keyToItem = new HashMap<String, ListItem>();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		
		CriteriaQuery<? extends NamedKeyItem> c = cb.createQuery(clazz);
		Root<? extends NamedKeyItem> from = c.from(clazz);
		Predicate[] where = new Predicate[onlyActive? 2 : 1];
		where[0] = from.get("conservationArea").in(SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
		if (onlyActive) where[1] = cb.equal(from.get("isActive"), true); //$NON-NLS-1$
		c.where(cb.and(where));
		
		List<?> teams = session.createQuery(c).getResultList();
		for (Iterator<?> iterator = teams.iterator(); iterator.hasNext();) {
			NamedKeyItem namedItem = (NamedKeyItem) iterator.next();
			ListItem item = keyToItem.get(namedItem.getKeyId());
			if (item == null) {
				item = new ListItem(null, namedItem.getName(), namedItem.getKeyId());
				keyToItem.put(namedItem.getKeyId(), item);
			} else if (namedItem.getNames().iterator().next().getLanguage().getCa().equals(
					SmartDB.getConservationAreaConfiguration()
							.getMainConservationArea())) {
				item.updateName(namedItem.getName());
			}
		}
		return keyToItem.values();

	}


	@Override
	public List<ListItem> getAgencies(Session session) {
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		items.addAll(getNamedKeyItem(session, Agency.class, false));
		return items;
	}
	
	public Collection<ListItem> getTeams(Session session) {
		return getNamedKeyItem(session, Team.class, false);
	}
	
	@Override
	public List<ListItem> getActiveTeams(Session session) {
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		items.addAll(getNamedKeyItem(session, Team.class, true));
		return items;
	}
	
	public Collection<ListItem> getMandates(Session session) {
		return getNamedKeyItem(session, PatrolMandate.class, false);
	}
	
	@Override
	public List<ListItem> getActiveMandates(Session session) {
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		items.addAll(getNamedKeyItem(session, PatrolMandate.class, true));
		return items;
	}
	
	public Collection<ListItem> getTransportTypes(Session session) {
		return getNamedKeyItem(session, PatrolTransportType.class, false);
	}
	
	@Override
	public List<ListItem> getActiveTransportTypes(Session session) {
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		items.addAll(getNamedKeyItem(session, PatrolTransportType.class, true));
		return items;
	}
	
	
	@Override
	public List<PatrolAttribute> getCustomPatrolAttributes(Session session) {
		List<PatrolAttribute> pas = session.createQuery("FROM PatrolAttribute a WHERE a.conservationArea in (:cas)", //$NON-NLS-1$
				PatrolAttribute.class)
				.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas()) //$NON-NLS-1$
				.list();
		
		HashMap<String, PatrolAttribute> attributes = new HashMap<>();
		HashSet<String> toExclude = new HashSet<>();
		
		for (PatrolAttribute pa : pas) {
			PatrolAttribute current = attributes.get(pa.getKeyId());
			if (current == null && !toExclude.contains(pa.getKeyId())) {
				PatrolAttribute temp = new PatrolAttribute();
				temp.setKeyId(pa.getKeyId());
				temp.setIsActive(true);
				temp.setName(pa.getName());
				temp.setType(pa.getType());
				if (pa.getAttributeList() != null) {
					temp.setAttributeList(new ArrayList<>());
					for (PatrolAttributeListItem li : pa.getAttributeList()) {
						PatrolAttributeListItem clone = new PatrolAttributeListItem();
						clone.setIsActive(true);
						clone.setKeyId(li.getKeyId());
						clone.setName(li.getName());
						clone.setListOrder(li.getListOrder());
						temp.getAttributeList().add(clone);
					}
				}
				attributes.put(pa.getKeyId(), temp);
			}else {
				if (pa.getType() != current.getType()) {
					toExclude.add(pa.getKeyId());
					attributes.remove(pa.getKeyId());
				}else {
					//merge list items
					if (pa.getAttributeList() != null) {
						HashMap<String, PatrolAttributeListItem> items = new HashMap<>();
						for (PatrolAttributeListItem i : current.getAttributeList()) items.put(i.getKeyId(), i);
						
						for (PatrolAttributeListItem i : pa.getAttributeList()) {
							if (!items.containsKey(i.getKeyId())) {
								PatrolAttributeListItem clone = new PatrolAttributeListItem();
								clone.setIsActive(true);
								clone.setKeyId(i.getKeyId());
								clone.setName(i.getName());
								clone.setListOrder(i.getListOrder());
								items.put(clone.getKeyId(), clone);
							}
						}
						
						current.getAttributeList().clear();
						current.getAttributeList().addAll(items.values());
					}
				}
			}
		}
		
		pas = new ArrayList<>(attributes.values());
		for (PatrolAttribute pa : pas) {
			if (pa.getAttributeList() != null) Collections.sort(pa.getAttributeList(), (a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		}
		Collections.sort(pas, (a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		
		return pas;
	}

	@Override
	public PatrolAttribute getPatrolAttribute(Session session, String value) {

		List<PatrolAttribute> pas = session.createQuery("FROM PatrolAttribute a WHERE a.conservationArea in (:cas) AND a.keyId = :keyid", //$NON-NLS-1$
				PatrolAttribute.class)
				.setParameter("keyid", value) //$NON-NLS-1$
				.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas()) //$NON-NLS-1$
				.list();
		//anything found?
		if (pas.isEmpty()) return null;
		
		PatrolAttribute pa = pas.get(0);
		
		//validate all are the same type otherwise return null
		for(PatrolAttribute p : pas) {
			if (p.getType() != pa.getType()) return null;
		}
		
		//make a clone and sort out list items
		PatrolAttribute temp = new PatrolAttribute();
		temp.setKeyId(pa.getKeyId());
		temp.setIsActive(true);
		temp.setName(pa.getName());
		temp.setType(pa.getType());

		if (pa.getAttributeList() != null) {
			temp.setAttributeList(new ArrayList<>());
			
			Set<String> listItems = new HashSet<>();
			
			for (PatrolAttributeListItem li : pa.getAttributeList()) {
				PatrolAttributeListItem clone = new PatrolAttributeListItem();
				clone.setIsActive(true);
				clone.setKeyId(li.getKeyId());
				clone.setName(li.getName());
				clone.setListOrder(li.getListOrder());
				temp.getAttributeList().add(clone);
				listItems.add(li.getKeyId());
			}
			
			for (int i = 1; i < pas.size(); i ++) {
				PatrolAttribute p = pas.get(i);
				if (p.getAttributeList() == null) continue;
				for (PatrolAttributeListItem li : p.getAttributeList()) {	
					if (listItems.contains(li.getKeyId())) continue;
					PatrolAttributeListItem clone = new PatrolAttributeListItem();
					clone.setIsActive(true);
					clone.setKeyId(li.getKeyId());
					clone.setName(li.getName());
					clone.setListOrder(li.getListOrder());
					temp.getAttributeList().add(clone);
					listItems.add(li.getKeyId());
				}
			}
			
			Collections.sort(temp.getAttributeList(), (a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		}
		
		return temp;
		
	}
}
