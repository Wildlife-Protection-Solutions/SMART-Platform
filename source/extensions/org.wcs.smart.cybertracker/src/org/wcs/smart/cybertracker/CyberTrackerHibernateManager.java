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
package org.wcs.smart.cybertracker;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.OptionID;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

/**
 * CyberTracker related database functions.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerHibernateManager {

	/**
	 * A name for default CyberTracker properties profile.
	 */
	public static final String DEFAULT_PROFILE_NAME = "Default"; //$NON-NLS-1$

	/**
	 * Returns all {@link CyberTrackerPropertiesOption}
	 * 
	 * @param session
	 * @return  {@link List} of {@link CyberTrackerPropertiesOption}
	 */
	public static List<CyberTrackerPropertiesOption> getAllStorageOptions(Session session) {
		Criteria query = session.createCriteria(CyberTrackerPropertiesOption.class).add(Restrictions.eq("optionId", OptionID.STORAGE_TIME)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<CyberTrackerPropertiesOption> list = query.list();
		return list;
	}

	/**
	 * Fetches {@link CyberTrackerProperties} for current conservation area
	 * 
	 * @param session session
	 * @return {@link CyberTrackerProperties}
	 */
	public static CyberTrackerProperties getProperties(Session session) {
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		Criteria query = session.createCriteria(CyberTrackerPropertiesOption.class).add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<CyberTrackerPropertiesOption> options = query.list();
		CyberTrackerProperties properties = new CyberTrackerProperties();
		for (Object object : options) {
			CyberTrackerPropertiesOption o = (CyberTrackerPropertiesOption) object;
			properties.getOptions().put(o.getOptionId(), o);
		}
		return properties;
	}
	
	/**
	 * Saves a given properties to the database.
	 * 
	 * @param properties the properties to save
	 * @param session session
	 * @return <code>true</code> if saved successfully, <code>false</code> if error
	 */
	public static boolean saveProperties(CyberTrackerProperties properties, Session session) {
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		session.beginTransaction();
		try {
			for (CyberTrackerPropertiesOption option : properties.getOptions().values()) {
				option.setConservationArea(ca);
				session.saveOrUpdate(option);
			}
			session.getTransaction().commit();
			return true;
		} catch (Exception ex) {
			session.getTransaction().rollback();
			return false;
		}
	}
	
	/**
	 * Fetches a list of {@link CyberTrackerPropertiesProfile} for current conservation area
	 * 
	 * @param session session
	 * @return List of {@link CyberTrackerPropertiesProfile}
	 */
	public static List<CyberTrackerPropertiesProfile> getPropertiesProfiles(Session session) {
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		Criteria query = session.createCriteria(CyberTrackerPropertiesProfile.class).add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<CyberTrackerPropertiesProfile> profiles = query.list();
		if (profiles.isEmpty()) {
			CyberTrackerPropertiesProfile defaultProfile = createDefaultProfile(session);
			return Arrays.asList(defaultProfile);
		}
		return profiles;
	}

	/**
	 * Returns a profile associated with given {@link ConfigurableModel}
	 * @param session
	 * @param configurableModel
	 * @return
	 */
	public static CyberTrackerPropertiesProfile getAssociatedProfile(Session session, ConfigurableModel configurableModel) {
		if (configurableModel == null || configurableModel.getUuid() == null) {
			return getDefaultProfile(session);
		}
		//TODO: need associated profile!!!!
		return getDefaultProfile(session);
	}

	private static CyberTrackerPropertiesProfile getDefaultProfile(Session session) {
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		Criteria query = session.createCriteria(CyberTrackerPropertiesProfile.class)
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
				.add(Restrictions.eq("default", true)); //$NON-NLS-1$
		
		CyberTrackerPropertiesProfile profile = (CyberTrackerPropertiesProfile) query.uniqueResult();
		return profile != null ? profile : createDefaultProfile(session);
	}
	
	/**
	 * This method must create and persist a default profile,
	 * but it is not allowed to use current session for that (as transaction may be reverted)
	 * At the same time returned(created) profile must be attached to current session.
	 * 
	 * @param session
	 * @return default CyberTracker properties profile
	 */
	private static CyberTrackerPropertiesProfile createDefaultProfile(Session session) {
		final CyberTrackerPropertiesProfile defaultProfile = new CyberTrackerPropertiesProfile();
		defaultProfile.setConservationArea(SmartDB.getCurrentConservationArea());
		defaultProfile.setDefault(true);
		defaultProfile.setName(DEFAULT_PROFILE_NAME);
		defaultProfile.updateName(SmartDB.getCurrentLanguage(), defaultProfile.getName());
		
		Thread thread = new Thread() { //new thread is created so it will have it's own hibernate session
		    public void run() {
	        	Session s = HibernateManager.openSession();
	    		try {
	    			s.beginTransaction();
	    			s.save(defaultProfile);
	    			s.getTransaction().commit();
	    		} catch (Exception e) {
	    			SmartPlugIn.displayLog(Messages.CyberTrackerHibernateManager_CreateDefaultProfile_Error, e);
	    		}finally {
	    			s.close();
	    		}
		    }  
		};
		thread.start();
		try {
			thread.join(); //we need to wait till thread is completed
		} catch (InterruptedException e) {
			SmartPlugIn.displayLog(Messages.CyberTrackerHibernateManager_CreateDefaultProfile_Error, e);
		}
		
		return (CyberTrackerPropertiesProfile) session.merge(defaultProfile); //attaching create object to current session
	}

	public static boolean isEmptyTag0(String uuid) {
		return uuid == null || uuid.isEmpty() || ElementsUtil.NULL_VALUE.equals(uuid);
	}
	
	public static <T> T fetchByUuid(Class<T> clazz, String uuid, Session session) {
		if (isEmptyTag0(uuid))
			return null;
		try {
			return fetchByUuid(clazz, UuidUtils.stringToUuid(uuid), session);
		} catch (Exception e) {
			CyberTrackerPlugIn.log(e.getMessage(), e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T fetchByUuid(Class<T> clazz, UUID byteUuid, Session session) {
		if (byteUuid == null)
			return null;
		Criteria query = session.createCriteria(clazz)
				.add(Restrictions.eq("uuid", byteUuid)); //$NON-NLS-1$
		return (T) query.uniqueResult();
	}

}
