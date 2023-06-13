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

import org.eclipse.core.runtime.Assert;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ConfigurableModelCtPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.OptionID;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
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
			CyberTrackerPropertiesProfile defaultProfile = createDefaultProfile(session);
			return Arrays.asList(defaultProfile);
		}
		return profiles;
	}

	/**
	 * Delete a profile and related records
	 * @throws Exception 
	 */
	public static void deleteProfile(Session session, CyberTrackerPropertiesProfile profile) {
		List<ConfigurableModelCtPropertiesProfile>  usedProfiles =
				QueryFactory.buildQuery(session, ConfigurableModelCtPropertiesProfile.class,"profile", profile).getResultList(); //$NON-NLS-1$
		for (ConfigurableModelCtPropertiesProfile ct : usedProfiles){
			session.remove(ct);
		}
		session.remove(profile);
	}
	
	/**
	 * Returns a profile associated with given {@link ConfigurableModel}
	 * @param session
	 * @param configurableModel
	 * @return
	 */
	public static ConfigurableModelCtPropertiesProfile getAssociatedCmProfile(Session session, ConfigurableModel configurableModel) {
		Assert.isNotNull(configurableModel, "Configurable model"); //$NON-NLS-1$
		if (configurableModel.getUuid() == null) {
			return createDefaultCmProfile(session, configurableModel);
		}
		
		ConfigurableModelCtPropertiesProfile item = QueryFactory.buildQuery(session, ConfigurableModelCtPropertiesProfile.class,"id.model", configurableModel).uniqueResult(); //$NON-NLS-1$
		if (item == null){
			return createDefaultCmProfile(session, configurableModel);
		}
		return item;
	}

	private static ConfigurableModelCtPropertiesProfile createDefaultCmProfile(Session session, ConfigurableModel configurableModel) {
		ConfigurableModelCtPropertiesProfile cm2ctp = new ConfigurableModelCtPropertiesProfile();
		cm2ctp.setModel(configurableModel);
		cm2ctp.setProfile(getDefaultProfile(session));
		return cm2ctp;
	}
	
	public static CyberTrackerPropertiesProfile getDefaultProfile(Session session) {
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		
		CyberTrackerPropertiesProfile profile = QueryFactory.buildQuery(session, CyberTrackerPropertiesProfile.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"default", true}).uniqueResult(); //$NON-NLS-1$
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
		    	try(Session s = HibernateManager.openSession()){
		    		try {
		    			s.beginTransaction();
		    			s.persist(defaultProfile);
		    			s.getTransaction().commit();
		    		} catch (Exception e) {
		    			SmartPlugIn.displayLog(Messages.CyberTrackerHibernateManager_CreateDefaultProfile_Error, e);
		    		}
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
			T value = fetchByUuid(clazz, UuidUtils.stringToUuid(uuid), session);
			
			if (value instanceof Category){
				if (((Category)value).getConservationArea().equals(SmartDB.getCurrentConservationArea())) return value;
				return null;
			}
			if (value instanceof Attribute){
				if (((Attribute)value).getConservationArea().equals(SmartDB.getCurrentConservationArea())) return value;
				return null;
			}
			if (value instanceof AttributeListItem){
				if (((AttributeListItem)value).getAttribute().getConservationArea().equals(SmartDB.getCurrentConservationArea())) return value;
				return null;
			}
			if (value instanceof AttributeTreeNode){
				if (((AttributeTreeNode)value).getAttribute().getConservationArea().equals(SmartDB.getCurrentConservationArea())) return value;
				return null;
			}
			if (value instanceof Employee){
				if (((Employee)value).getConservationArea().equals(SmartDB.getCurrentConservationArea())) return value;
				return null;
			}

			return value;
		} catch (Exception e) {
			CyberTrackerPlugIn.log(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * The function selects based on the uuid.  It is up to the user to ensure the 
	 * returning objects is valid for the current conservation area.
	 * 
	 * @param clazz
	 * @param byteUuid
	 * @param session
	 * @return
	 */
	public static <T> T fetchByUuid(Class<T> clazz, UUID byteUuid, Session session) {
		if (byteUuid == null) return null;
		return (T) session.get(clazz, byteUuid);
	}

}
