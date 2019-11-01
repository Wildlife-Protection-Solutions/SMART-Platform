package org.wcs.smart.i2;

import java.text.Collator;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelProfile;

public enum ProfilesManager {

	INSTANCE;
	
	private volatile Set<IntelProfile> active = null;
	
	//sync
	public Set<IntelProfile> getActiveProfiles(){
		if (active == null) {
			//populate
			synchronized (INSTANCE) {
				if (active != null) return active;
				HashSet<IntelProfile> allprofiles = new HashSet<>();
				try(Session s = HibernateManager.openSession()){
					allprofiles.addAll( getProfiles(s) );
				}	
				this.active = Collections.unmodifiableSet(allprofiles);
				return active;
			}
		}
		return active;
	}
	
	public void setActiveProfiles(Set<IntelProfile> active, IEventBroker event) {
		synchronized (INSTANCE) {
			this.active = Collections.unmodifiableSet(active);
		}
		//fire events
		event.post(IntelEvents.ACTIVE_PROFILES, this.active);
	}
	
	/**
	 * Load profiles and sorts them alphabetically
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public List<IntelProfile> getProfiles(Session session){
		List<IntelProfile> temp;
		temp = QueryFactory.buildQuery(session, IntelProfile.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list();
		temp.forEach(e->e.getNames().size());
		temp.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		return temp;
	}
}
