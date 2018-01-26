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
package org.wcs.smart.asset.query.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.wcs.smart.asset.query.hibernate.MultiCaAssetQueryHibernateManagerImpl;
import org.wcs.smart.asset.query.hibernate.AssetQueryHibernateManager;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetQueryOptionType;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryFilterConfigManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Asset option data for fixed asset options.
 * 
 * @author Emily
 *
 */
public class AssetOptionData implements IAssetOptionData{
	
	private AssetFilterOption option;
	
	public AssetOptionData(AssetFilterOption option){
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
		//TODO: implement me
		return null;
//		List<ListItem> results = new ArrayList<ListItem>();
//		AssetQueryOptionType type = option.getType();
//		if (type == AssetQueryOptionType.UUID){
//			List<UUID> uuidkeys = new ArrayList<UUID>(keys.length);
//			try {
//				for (int i = 0; i < keys.length; i++) {
//					uuidkeys.add(UuidUtils.stringToUuid(keys[i]));
//				}
//			} catch (Exception ex) {
//				QueryPlugIn.log(
//						Messages.PatrolQueryOptions_ErrorInvalidPatrolFilterValue
//								+ ex.getLocalizedMessage(), ex);
//				return results;
//			}
//			
//			CriteriaBuilder cb = session.getCriteriaBuilder();
//			CriteriaQuery<?> c = cb.createQuery(option.getSourceClass());
//			Root<?> root = c.from(option.getSourceClass());
//			c.where(root.get("uuid").in(uuidkeys)); //$NON-NLS-1$
//			Collection<?> data = session.createQuery(c).getResultList();
//			
//			for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
//				Object object = (Object) iterator.next();
//				if (object instanceof NamedItem){
//					results.add(new ListItem(((NamedItem) object).getUuid(), ((NamedItem) object).getName()));
//				}else if (object instanceof Employee){
//					Employee e = (Employee)object;
//					if (e.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
//						results.add(new ListItem(e.getUuid(), SmartLabelProvider.getShortLabel((Employee) e), e.isActive()));
//					}
//				}else if (object instanceof ConservationArea){
//					ConservationArea ca = (ConservationArea)object;
//					results.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
//				}else if (object instanceof ListItem){
//					results.add((ListItem)object);
//				}
//			}
//		}else if (type == AssetQueryOptionType.KEY){
//			Collection<?> data = null;
//			
//			if (data != null){
//				for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
//					ListItem it = (ListItem) iterator.next();
//					if (Arrays.asList(keys).contains(it.getKey())){
//						results.add(it);
//					}
//				}
//			}
//		}else if (type == AssetQueryOptionType.STRING){
//			if (option == AssetQueryOption.ID){
//				List<String> keyCollection = new ArrayList<String>(keys.length);
//				for (String k : keys) keyCollection.add(k);
//				
//				CriteriaBuilder cb = session.getCriteriaBuilder();
//				CriteriaQuery<?> c = cb.createQuery(option.getSourceClass());
//				Root<?> root = c.from(option.getSourceClass());
//				c.where(cb.and(
//						cb.equal(root.get("conservationArea"), SmartDB.getCurrentConservationArea()), //$NON-NLS-1$
//						root.get(option.getColumnName()).in(keyCollection) 
//						));
//				List<?> data = session.createQuery(c).getResultList();
//				
//				for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
//					Object object = (Object) iterator.next();
//					if (object instanceof Patrol){
//						//results.add(new ListItem(((Patrol) object).getUuid(), ((Patrol) object).getId()));
//						results.add(new ListItem( null, ((Patrol) object).getId(),((Patrol) object).getId() ));
//					}
//				}
//			}else if (option == AssetQueryOption.PATROL_TYPE){
//				for (int i = 0; i < keys.length; i ++){
//					results.add(new ListItem(null, PatrolType.Type.valueOf(keys[i]).getGuiName(Locale.getDefault()), keys[i]));
//				}
//			}
//		}
//		return results;
	}

	/**
	 * @param session
	 * @return a list of listitems that represent all
	 * active values for a given object 
	 */
	public List<ListItem> getAllValues(Session session){
		//TODO: implement me;
		return null;
//		ArrayList<ListItem> items = new ArrayList<ListItem>();
//		if (option == AssetQueryOption.ID){
//			List<String> pids = PatrolHibernateManager.getPatrolIds(session);
//			for (String pid : pids){
//				items.add(new ListItem(null, pid, pid));
//			}
//		}else if (option == AssetQueryOption.CONSERVATION_AREA){
//			for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
//				items.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
//			}
//		}else if (option == AssetQueryOption.STATION){
//			List<Station> stations = PatrolHibernateManager.getActiveStations(SmartDB.getCurrentConservationArea(), session);
//			for (Station s : stations){
//				items.add(new ListItem(s.getUuid(), s.getName()));
//			}
//		}else if (option == AssetQueryOption.TEAM || option == AssetQueryOption.TEAM_KEY){
//			items.addAll(AssetQueryHibernateManager.getInstance().getActiveTeams(session));
//		}else if (option == AssetQueryOption. MANDATE || option == AssetQueryOption.MANDATE_KEY){
//			items.addAll(AssetQueryHibernateManager.getInstance().getActiveMandates(session));
//			
//		}else if (option == AssetQueryOption.PATROL_TYPE){
//			if (SmartDB.isMultipleAnalysis()){
//				for (PatrolType.Type t : PatrolType.Type.values()){
//					items.add(new ListItem(null, t.getGuiName(Locale.getDefault()), t.name()));
//				}
//			}else{
//				List<PatrolType> types= PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session);
//				for (PatrolType t : types){
//					items.add(new ListItem(null, t.getType().getGuiName(Locale.getDefault()), t.getType().name() ));
//				}
//			}
//		}else if (option == AssetQueryOption.PATROL_TRANSPORT_TYPE || option == AssetQueryOption.PATROL_TRANSPORT_TYPE_KEY){
//			items.addAll(AssetQueryHibernateManager.getInstance().getActiveTransportTypes(session));
//		}else if (option == AssetQueryOption.LEADER || option == AssetQueryOption.PILOT || option == AssetQueryOption.EMPLOYEE){
//			boolean showInactive = QueryFilterConfigManager.getInstance().getCurrentConfig().isShowInactiveItems();
//			List<Employee> employees = showInactive ?
//					PatrolHibernateManager.getAllEmployees(SmartDB.getCurrentConservationArea(), session) : 
//					PatrolHibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), session); 
//			for (Employee t : employees){
//				items.add(new ListItem(t.getUuid(), SmartLabelProvider.getShortLabel(t), t.isActive()));
//			}
//		}else if (option == AssetQueryOption.AGENCY){
//			List<Agency> agencies = HibernateManager.getAgencies(SmartDB.getCurrentConservationArea(), session);
//			for (Agency a : agencies){
//				items.add(new ListItem(a.getUuid(), a.getName()));
//			}
//		}else if (option == AssetQueryOption.RANK){
//			List<Agency> agencies = HibernateManager.getAgencies(SmartDB.getCurrentConservationArea(), session);
//			for (Agency a : agencies){
//				for (Rank r : a.getRanks()){
//					items.add(new ListItem(r.getUuid(), r.getName() + " - " + a.getName())); //$NON-NLS-1$
//				}
//			}
//		}
//		Collections.sort(items);
//		return items;
	}

	@Override
	public ListItem getDefaultListItem() {
		return null;
	}
	
}
