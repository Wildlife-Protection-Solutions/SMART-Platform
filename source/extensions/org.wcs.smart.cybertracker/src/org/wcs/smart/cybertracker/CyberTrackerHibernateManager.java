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

import java.util.List;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.OptionID;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * CyberTracker related database functions.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerHibernateManager {

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
		Criteria query = session.createCriteria(clazz).add(Restrictions.eq("uuid", byteUuid)); //$NON-NLS-1$
		return (T) query.uniqueResult();
	}
	
}
