/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.i2;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileEntityType;
import org.wcs.smart.i2.model.IntelProfileRecordSource;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.Resources;

/**
 * Tools for managing profiles and maintaining the active
 * profiles. 
 * 
 * @author Emily
 *
 */
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
	
	/**
	 * Reset the active profiles
	 */
	public void resetActiveProfiles() {
		active = null;
		getActiveProfiles();
	}
	
	/**
	 * 
	 * @return set of unique keys associated with active profiles
	 */
	public Set<String> getActiveProfileKeys(){
		return getActiveProfiles().stream().map(e->e.getKeyId()).collect(Collectors.toSet());
	}
	
	/**
	 * 
	 * @return set of profiles uuids associated with active profiles
	 */
	public Set<UUID> getActiveProfileIds(){
		return getActiveProfiles().stream().map(e->e.getUuid()).collect(Collectors.toSet());
	}
	
	public void setActiveProfiles(Set<IntelProfile> active, IEventBroker event) {
		//TODO: ensure all editors (entities and records) are saved before setting profiles
		synchronized (INSTANCE) {
			//only do something if actually changed
			if (this.active.containsAll(active) && active.containsAll(this.active)) return ;
			this.active = Collections.unmodifiableSet(active);
		}
		event.post(IntelEvents.ACTIVE_PROFILES, this.active);
	}
	
	/**
	 * Load all profiles.  If filterUser is true than this
	 * filters it to only profiles the current user has some permission
	 * to use.  In the case of CCAA analysis this returns all  profiles
	 * that users have permission to query
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public List<IntelProfile> getProfiles(Session session, boolean filterUser){
		List<IntelProfile> temp;
		
		if (SmartDB.isMultipleAnalysis()) {
			List<IntelProfile> items = session.createQuery("FROM IntelProfile WHERE conservationArea in (:cas)", IntelProfile.class) //$NON-NLS-1$
					.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas()) //$NON-NLS-1$
					.list();
			
			temp = new ArrayList<>();
			for (IntelProfile p : items) {
				if (IntelSecurityManager.INSTANCE.canViewQuery(p)) {
					temp.add(p);
					Resources.INSTANCE.getImage(p);
				}
			}
		}else {
			temp = QueryFactory.buildQuery(session, IntelProfile.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
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
			temp.forEach(p->Resources.INSTANCE.getImage(p));
		}
		return temp;
	}
	
	/**
	 * Determine if the profile can be deleted
	 * 
	 * @param profile
	 * @param session
	 * @throws Exception
	 */
	public void canDelete(IntelProfile profile, Session session) throws Exception{
		if (!DeleteManager.canDelete(profile, session)){
			throw new Exception(Messages.ProfilesManager_DeleteError);
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
			Query<?> q = session.createQuery("DELETE FROM IntelRelationshipType WHERE sourceProfile = :profile OR targetProfile = :profile2"); //$NON-NLS-1$
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
			
			for (Class<? extends AbstractIntelQuery> c : InternalQueryManager.INSTANCE.getQueryTypeClasses()) {
			
				List<? extends AbstractIntelQuery> query = session.createQuery("FROM " + c.getName() + " WHERE profile_filter like :profile and conservationArea = :ca", AbstractIntelQuery.class) //$NON-NLS-1$ //$NON-NLS-2$
						.setParameter("profile", profile.getKeyId() + "%") //$NON-NLS-1$ //$NON-NLS-2$
						.setParameter("ca", profile.getConservationArea()) //$NON-NLS-1$
						.list();
				
				for (AbstractIntelQuery item : query) {
					if (item.queriesProfile(profile)) {
						Set<String> filters = AbstractIntelQuery.convertFromProfileFilter(item.getProfileFilter());
						filters.remove(profile.getKeyId());
						if (filters.isEmpty()) {
							session.delete(item);
						}else {
							item.setProfileFilter(AbstractIntelQuery.convertKeysToProfileFilter(filters));
						}
					}
				}
			}
		
	}
	
	/**
	 * Validates that the entity type attributes associated with the
	 * record source are also associated with the source profile. 
	 * 
	 * @param source
	 * @return error message or null if ok
	 */
	public String validateRecords(List<IntelRecordSource> sources) {
		for (IntelRecordSource s : sources) {
			String x = validateRecords(s);
			if (x != null) return x;
		}
		return null;
	}
	
	/**
	 * Validates that the entity type attributes associated with the
	 * record source are also associated with the source profile. 
	 * 
	 * @param source
	 * @return error message or null if ok
	 */
	public String validateRecords(IntelRecordSource source) {
		for (IntelRecordSourceAttribute ia : source.getAttributes()) {
			if (ia.getAttribute() != null) continue;
			IntelEntityType etype = ia.getEntityType();
			
			for (IntelProfileRecordSource ip : source.getProfiles()) {
				
				boolean found = false;
				for (IntelProfileEntityType map : etype.getProfiles()) {
					if (map.getProfile().equals(ip.getProfile())) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					String name = IIntelligenceLabelProvider.getName(ia);
					String msg = Messages.ProfilesManager_deleteProblem;
					return MessageFormat.format(msg, source.getName(), ip.getProfile().getName(), name, name, ip.getProfile().getName()); 
				}
			}
		}
		
		return null;
	}
}
