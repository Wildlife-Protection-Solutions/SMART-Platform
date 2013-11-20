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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.query.ui.model.ListItem;

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
		
			Criteria c = session
					.createCriteria(clazz)
					.add(Restrictions.in("conservationArea", SmartDB //$NON-NLS-1$
							.getConservationAreaConfiguration()
							.getConservationAreas()));
			if (onlyActive){
				c.add(Restrictions.eq("isActive", true)); //$NON-NLS-1$
			}
			List<?> teams = c.list();

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
}
