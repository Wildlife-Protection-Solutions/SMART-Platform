package org.wcs.smart.i2;

import java.text.Collator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.security.IntelSecurityManager;

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
					allprofiles.addAll( getProfiles(s, true) );
				}	
				this.active = Collections.unmodifiableSet(allprofiles);
				return active;
			}
		}
		return active;
	}
	
	public void setActiveProfiles(Set<IntelProfile> active, IEventBroker event) {
		//TODO: ensure all editors (entities and records) are saved before setting profiles
		synchronized (INSTANCE) {
			this.active = Collections.unmodifiableSet(active);
		}
		//fire events
		event.post(IntelEvents.ACTIVE_PROFILES, this.active);
	}
	
	/**
	 * Load all profiles.  If filterUser is true than this
	 * filters it to only profiles the current user has some permission
	 * to use.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public List<IntelProfile> getProfiles(Session session, boolean filterUser){
		List<IntelProfile> temp;
		temp = QueryFactory.buildQuery(session, IntelProfile.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list();
		temp.forEach(e->e.getNames().size());
		temp.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		
		if (!filterUser) return temp;
		
		for (Iterator<IntelProfile> iterator = temp.iterator(); iterator.hasNext();) {
			IntelProfile profile = iterator.next();
			boolean keep = false;
			if (IntelSecurityManager.INSTANCE.canViewEntities(profile) ||
					IntelSecurityManager.INSTANCE.canViewRecords(profile) ||
					IntelSecurityManager.INSTANCE.canViewQuery(profile)) {
				keep = true;
			}
			if (!keep) iterator.remove();
			
		}
		return temp;
	}
	
	public void canDelete(IntelProfile profile, Session session) throws Exception{
		if (!DeleteManager.canDelete(profile, session)){
			throw new Exception("Unknown error occurs while deleting entity type.");
		}
	}
	
	/**
	 * Delete all entities and records associated with this profile;
	 * then removes the profile
	 * 
	 * @param profile
	 * @param session
	 */
	public void deleteProfile(IntelProfile profile, Session session) {
			
			//update relationships references to null
			Query<?> q = session.createQuery("DELETE FROM IntelRelationshipType WHERE sourceProfile = :profile OR targetProfile = :profile2");
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.setParameter("profile2", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			//delete all entity attribute values
			q = session.createQuery("delete from IntelEntityAttributeValue ieav where ieav.id.entity in (FROM IntelEntity WHERE profile = :profile)"); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			//delete all relationship attribute values
			q = session.createQuery("delete from IntelEntityRelationshipAttributeValue ii where ii.id.relationship in (FROM IntelEntityRelationship r WHERE  r.sourceEntity in (FROM IntelEntity WHERE profile = :profile) or r.targetEntity in (FROM IntelEntity WHERE profile = :profile2))"); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.setParameter("profile2", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			//delete all relationships
			q = session.createQuery("delete from IntelEntityRelationship ii where ii.sourceEntity in (FROM IntelEntity WHERE profile = :profile) or ii.targetEntity in (FROM IntelEntity WHERE profile = :profile2)"); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.setParameter("profile2", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			//delete all entity attachments
			q = session.createQuery("delete from IntelEntityAttachment ii where ii.id.entity in (FROM IntelEntity WHERE profile = :profile) "); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			//delete all entity records
			q = session.createQuery("delete from IntelEntityRecord ii where ii.id.entity in (FROM IntelEntity WHERE profile = :profile) "); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			//delete all locations
			q = session.createQuery("delete from IntelEntityLocation ii where ii.id.entity in (FROM IntelEntity WHERE profile = :profile) "); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			//delete all links to working sets
			q = session.createQuery("delete from IntelWorkingSetEntity ii where ii.id.entity in (FROM IntelEntity WHERE profile = :profile) "); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			//delete all entity 		
			q = session.createQuery("delete from IntelEntity WHERE profile = :profile"); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.executeUpdate();

			
			
			//delete all record source attribute values
			q = session.createQuery("delete from IntelRecordAttributeValue ii where ii.record in ( FROM IntelRecord where profile = :profile)"); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			q = session.createQuery("delete from IntelRecord where profile = :profile"); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			q = session.createQuery("delete from IntelRecord where profile = :profile"); //$NON-NLS-1$
			q.setParameter("profile", profile); //$NON-NLS-1$
			q.executeUpdate();
			
			q = session.createQuery("delete from IntelProfile where uuid = :profile"); //$NON-NLS-1$
			q.setParameter("profile", profile.getUuid()); //$NON-NLS-1$
			q.executeUpdate();
		
	}
}
