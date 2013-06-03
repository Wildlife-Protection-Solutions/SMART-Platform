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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.hibernate.SmartDB;

/**
 * CyberTracker related database functions.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerHibernateManager {

	/**
	 * Fetches {@link CyberTrackerProperties} for current conservation area
	 * 
	 * @param session session
	 * @return {@link CyberTrackerProperties}
	 */
	public static CyberTrackerProperties getProperties(Session session) {
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		Criteria query = session.createCriteria(CyberTrackerProperties.class).add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		CyberTrackerProperties properties = (CyberTrackerProperties) query.uniqueResult();
		return properties != null ? properties : new CyberTrackerProperties();
	}
	
	/**
	 * Saves a given properties to the database.
	 * 
	 * @param properties the properties to save
	 * @param session session
	 * @return <code>true</code> if saved successfully, <code>false</code> if error
	 */
	public static boolean saveProperties(CyberTrackerProperties properties, Session session) {
		properties.setConservationArea(SmartDB.getCurrentConservationArea());
		session.beginTransaction();
		try {
			session.saveOrUpdate(properties);
			session.getTransaction().commit();
			return true;
		} catch (Exception ex) {
			session.getTransaction().rollback();
			return false;
		}
	}
	
}
