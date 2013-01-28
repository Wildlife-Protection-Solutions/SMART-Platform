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
package org.wcs.smart.intelligence;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Intelligence related database functions.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceHibernateManager extends HibernateManager {

	/**
	 * Loads patrols from database 
	 * 
	 * @param session session 
	 * @return list of patrols
	 */
	public static List<Patrol> getPatrols(){
		Session session = SmartHibernateManager.openSession();
		try {
			ConservationArea ca = SmartDB.getCurrentConservationArea();
			Criteria query = session.createCriteria(Patrol.class).add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
			@SuppressWarnings("unchecked")
			List<Patrol> list = query.list();
			return list;
		} finally {
			session.close();
		}
	}

	/**
	 * Saves a given intelligence to the database.
	 * 
	 * @param intelligence the intelligence to save
	 * @return <code>true</code> if saved successfully, <code>false</code> if error
	 */
	public static boolean saveIntelligence(Intelligence intelligence) {
		Interceptor interceptor = new AttachmentInterceptor();
		Session session = SmartHibernateManager.openSession(interceptor);
		try {
			session.beginTransaction();
			try {
				session.saveOrUpdate(intelligence);
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				IntelligencePlugIn.displayLog(Messages.IntelligenceHibernateManager_SaveIntelligence_Error + "\n"+ ex.getLocalizedMessage(), ex); //$NON-NLS-1$
				return false;
			}
		} finally {
			session.close();
		}
		return true;
	}

	/**
	 * Delete a given intelligence from the database.
	 * 
	 * @param uuid uuid of the intelligence to delete
	 * @return intelligence that was deleted or <code>null</code> in case of error
	 */
	public static Intelligence deleteIntelligence(byte[] uuid) {
		//no need to add interceptor as files will be deleted manually
		Session session = SmartHibernateManager.openSession();
		Intelligence intelligence = null;
		try {
			session.beginTransaction();
			try {
				intelligence = (Intelligence) session.load(Intelligence.class, uuid);
				session.delete(intelligence);
				session.getTransaction().commit();
				deleteFilestore(intelligence);
			} catch (Exception ex) {
				session.getTransaction().rollback();
				IntelligencePlugIn.displayLog(Messages.IntelligenceHibernateManager_DeleteIntelligence_Error + "\n"+ ex.getLocalizedMessage(), ex); //$NON-NLS-1$
				return null;
			}
		} finally {
			session.close();
		}
		return intelligence;
	}

	/**
	 * Delete a filestore for given intelligence.
	 * 
	 * @param intelligence who's filestore to delete
	 */
	public static void deleteFilestore(Intelligence intelligence) {
		File fileStore = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation() + File.separator + intelligence.getIntelligenceDatastorePath());
		if (fileStore.exists()){
			try{
				FileUtils.forceDelete(fileStore);
			}catch(Exception ex){
				IntelligencePlugIn.displayLog(Messages.IntelligenceHibernateManager_Error_CouldNotDeleteFilestore + fileStore.getAbsolutePath(), ex);
			}
		}
	}
	
}
