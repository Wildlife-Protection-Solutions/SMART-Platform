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

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.OptionID;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

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
		List<CyberTrackerPropertiesOption> list =
				QueryFactory.buildQuery(session, CyberTrackerPropertiesOption.class,"optionId", OptionID.STORAGE_TIME.name()).getResultList(); //$NON-NLS-1$
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
		List<CyberTrackerPropertiesOption>  options =
				QueryFactory.buildQuery(session, CyberTrackerPropertiesOption.class,"conservationArea", ca).getResultList(); //$NON-NLS-1$
		CyberTrackerProperties properties = new CyberTrackerProperties();
		for (Object object : options) {
			CyberTrackerPropertiesOption o = (CyberTrackerPropertiesOption) object;
			try {
				CyberTrackerPropertiesOption.OptionID opId = CyberTrackerPropertiesOption.OptionID.valueOf( o.getOptionId() );
				properties.getOptions().put(opId, o);
			}catch (Exception ex) {
				//this property is not supported by this plugin
			}
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
				session.merge(option);
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
		List<CyberTrackerPropertiesProfile>  profiles =
				QueryFactory.buildQuery(session, CyberTrackerPropertiesProfile.class,"conservationArea", ca).getResultList(); //$NON-NLS-1$
		if (profiles.isEmpty()) {
			CyberTrackerPropertiesProfile defaultProfile = getDefaultProfile(session, ca);
			return Arrays.asList(defaultProfile);
		}
		return profiles;
	}

	/**
	 * Delete a profile and related records
	 * @throws Exception 
	 */
	public static void deleteProfile(Session session, CyberTrackerPropertiesProfile profile) {
		session.remove(profile);
	}
	
	/**
	 * Gets default profile for the current Conservation area
	 * @param session
	 * @return
	 */
	public static CyberTrackerPropertiesProfile getDefaultProfile(Session session) {
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		return getDefaultProfile(session, ca);
	}
	
	/**
	 * This method gets the default profile and creates 
	 *  one if the default doesn't exists. 
	 * 
	 * @param session
	 * @return default CyberTracker properties profile
	 */
	public static CyberTrackerPropertiesProfile getDefaultProfile(Session session, ConservationArea ca) {
		
		CyberTrackerPropertiesProfile profile = QueryFactory.buildQuery(session, CyberTrackerPropertiesProfile.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"default", true}).uniqueResult(); //$NON-NLS-1$
		
		if (profile != null) return profile;
		
		final CyberTrackerPropertiesProfile defaultProfile = new CyberTrackerPropertiesProfile();
		defaultProfile.setConservationArea(ca);
		defaultProfile.setDefault(true);
		defaultProfile.setName(DEFAULT_PROFILE_NAME);
		defaultProfile.updateName(ca.getDefaultLanguage(), defaultProfile.getName());
		session.persist(defaultProfile);
		
		return defaultProfile;
	}


}
