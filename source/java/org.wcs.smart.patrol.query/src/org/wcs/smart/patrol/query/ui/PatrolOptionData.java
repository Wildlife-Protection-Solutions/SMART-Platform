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
package org.wcs.smart.patrol.query.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.query.hibernate.MultiCaPatrolQueryHibernateManagerImpl;
import org.wcs.smart.patrol.query.hibernate.PatrolQueryHibernateManager;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.IPatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Patrol optoin data for fixed patrol options.
 * 
 * @author Emily
 *
 */
public class PatrolOptionData implements IPatrolOptionData{
	
	private IPatrolQueryOption option;
	
	public PatrolOptionData(PatrolQueryOption option){
		this.option = option;
	}
	
	/**
	 * Given a set of keys (hex encoded uuids or string keys), returns
	 * a list of listitems that represent the objects
	 * with the given keys.
	 * 
	 * @param session
	 * @param keys
	 * @return
	 */
	public List<ListItem> getValues(Session session, String[] keys){
		List<ListItem> results = new ArrayList<ListItem>();
		PatrolQueryOptionType type = option.getType();
		if (type == PatrolQueryOptionType.UUID){
			UUID[] uuidkeys = new UUID[keys.length];
			try {
				for (int i = 0; i < keys.length; i++) {
					uuidkeys[i] = UuidUtils.stringToUuid(keys[i]);
				}
			} catch (Exception ex) {
				QueryPlugIn.log(
						Messages.PatrolQueryOptions_ErrorInvalidPatrolFilterValue
								+ ex.getLocalizedMessage(), ex);
				return results;
			}
			Collection<?> data = session.createCriteria(option.getSourceClass()).add(Restrictions.in("uuid", uuidkeys)).list(); //$NON-NLS-1$
			
			for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
				Object object = (Object) iterator.next();
				if (object instanceof NamedItem){
					results.add(new ListItem(((NamedItem) object).getUuid(), ((NamedItem) object).getName()));
				}else if (object instanceof Employee){
					Employee e = (Employee)object;
					if (e.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
						results.add(new ListItem(e.getUuid(), SmartLabelProvider.getShortLabel((Employee) e)));
					}
				}else if (object instanceof ConservationArea){
					ConservationArea ca = (ConservationArea)object;
					results.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
				}else if (object instanceof ListItem){
					results.add((ListItem)object);
				}
			}
		}else if (type == PatrolQueryOptionType.KEY){
			Collection<?> data = null;
			if (SmartDB.isMultipleAnalysis()){
				if (option == PatrolQueryOption.TEAM_KEY){
					data = ((MultiCaPatrolQueryHibernateManagerImpl)PatrolQueryHibernateManager.getInstance()).getTeams(session);
				}else if (option == PatrolQueryOption.PATROL_TRANSPORT_TYPE_KEY){
					data = ((MultiCaPatrolQueryHibernateManagerImpl)PatrolQueryHibernateManager.getInstance()).getTransportTypes(session);
				}else if (option == PatrolQueryOption.MANDATE_KEY){
					data = ((MultiCaPatrolQueryHibernateManagerImpl)PatrolQueryHibernateManager.getInstance()).getMandates(session);
				}
			}
			if (data != null){
				for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
					ListItem it = (ListItem) iterator.next();
					if (Arrays.asList(keys).contains(it.getKey())){
						results.add(it);
					}
				}
			}
		}else if (type == PatrolQueryOptionType.STRING){
			if (option == PatrolQueryOption.ID){
				List<?> data = session.createCriteria(option.getSourceClass())
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
						.add(Restrictions.in(option.getColumnName(), keys)).list(); 
				for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					if (object instanceof Patrol){
						//results.add(new ListItem(((Patrol) object).getUuid(), ((Patrol) object).getId()));
						results.add(new ListItem( null, ((Patrol) object).getId(),((Patrol) object).getId() ));
					}
				}
			}else if (option == PatrolQueryOption.PATROL_TYPE){
				for (int i = 0; i < keys.length; i ++){
					results.add(new ListItem(null, PatrolType.Type.valueOf(keys[i]).getGuiName(Locale.getDefault()), keys[i]));
				}
			}
		}
		return results;
	}

	/**
	 * @param session
	 * @return a list of listitems that represent all
	 * active values for a given object 
	 */
	public List<ListItem> getAllActiveValues(Session session){
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		if (option == PatrolQueryOption.ID){
			List<String> pids = PatrolHibernateManager.getPatrolIds(session);
			for (String pid : pids){
				items.add(new ListItem(null, pid, pid));
			}
		}else if (option == PatrolQueryOption.CONSERVATION_AREA){
			for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
				items.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
			}
		}else if (option == PatrolQueryOption.STATION){
			List<Station> stations = PatrolHibernateManager.getActiveStations(SmartDB.getCurrentConservationArea(), session);
			for (Station s : stations){
				items.add(new ListItem(s.getUuid(), s.getName()));
			}
		}else if (option == PatrolQueryOption.TEAM || option == PatrolQueryOption.TEAM_KEY){
			items.addAll(PatrolQueryHibernateManager.getInstance().getActiveTeams(session));
		}else if (option == PatrolQueryOption. MANDATE || option == PatrolQueryOption.MANDATE_KEY){
			items.addAll(PatrolQueryHibernateManager.getInstance().getActiveMandates(session));
			
		}else if (option == PatrolQueryOption.PATROL_TYPE){
			if (SmartDB.isMultipleAnalysis()){
				for (PatrolType.Type t : PatrolType.Type.values()){
					items.add(new ListItem(null, t.getGuiName(Locale.getDefault()), t.name()));
				}
			}else{
				List<PatrolType> types= PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session);
				for (PatrolType t : types){
					items.add(new ListItem(null, t.getType().getGuiName(Locale.getDefault()), t.getType().name() ));
				}
			}
		}else if (option == PatrolQueryOption.PATROL_TRANSPORT_TYPE || option == PatrolQueryOption.PATROL_TRANSPORT_TYPE_KEY){
			items.addAll(PatrolQueryHibernateManager.getInstance().getActiveTransportTypes(session));
		}else if (option == PatrolQueryOption.LEADER ||
				option == PatrolQueryOption.PILOT || 
						option == PatrolQueryOption.EMPLOYEE){
			List<Employee> employees = PatrolHibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), session);
			for (Employee t : employees){
				items.add(new ListItem(t.getUuid(), SmartLabelProvider.getShortLabel(t) ));
			}
		}else if (option == PatrolQueryOption.AGENCY){
			List<Agency> agencies = HibernateManager.getAgencies(SmartDB.getCurrentConservationArea(), session);
			for (Agency a : agencies){
				items.add(new ListItem(a.getUuid(), a.getName()));
			}
		}else if (option == PatrolQueryOption.RANK){
			List<Agency> agencies = HibernateManager.getAgencies(SmartDB.getCurrentConservationArea(), session);
			for (Agency a : agencies){
				for (Rank r : a.getRanks()){
					items.add(new ListItem(r.getUuid(), r.getName() + " - " + a.getName())); //$NON-NLS-1$
				}
			}
		}
		Collections.sort(items);
		return items;
	}

	@Override
	public ListItem getDefaultListItem() {
		return null;
	}
}
