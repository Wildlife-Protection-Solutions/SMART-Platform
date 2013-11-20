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
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Interceptor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.PatrolIntelligence;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Intelligence related database functions.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceHibernateManager extends HibernateManager {

	/**
	 * Returns all intelligences
	 * 
	 * @param session
	 * @return all intelligences
	 */
	public static List<Intelligence> getIntelligences(Session session) {
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		Criteria query = session.createCriteria(Intelligence.class).add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Intelligence> list = query.list();
		return list;
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
			return saveIntelligence(intelligence, session);
		} finally {
			session.close();
		}
	}

	/**
	 * Saves a given intelligence to the database.
	 * 
	 * @param intelligence the intelligence to save
	 * @param session session
	 * @return <code>true</code> if saved successfully, <code>false</code> if error
	 */
	public static boolean saveIntelligence(Intelligence intelligence, Session session) {
		session.beginTransaction();
		try {
			//save a name
			if (intelligence.getName() != null) {
				intelligence.updateName(SmartDB.getCurrentLanguage(), intelligence.getName());
			}
			session.saveOrUpdate(intelligence);
			session.getTransaction().commit();
			return true;
		} catch (Exception ex) {
			session.getTransaction().rollback();
			IntelligencePlugIn.displayLog(Messages.IntelligenceHibernateManager_SaveIntelligence_Error + "\n"+ ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return false;
		}
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
	 * Returns Patrol IDs that were motivated by given intelligence
	 * 
	 * @param uuid uuid of the intelligence
	 * @return list of Patrol IDs
	 */
	public static List<?> fetchRelatedPatrolIDs(byte[] intelligenceUuid) {
		Session session = SmartHibernateManager.openSession();
		try {
			Query query = session.createQuery("SELECT pi.id.patrol.id FROM PatrolIntelligence pi WHERE pi.id.intelligence.uuid = :uuid ORDER BY pi.id.patrol.id asc"); //$NON-NLS-1$
			query.setParameter("uuid", intelligenceUuid); //$NON-NLS-1$
			List<?> list = query.list();
			return list;
		} finally {
			session.close();
		}
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

	/**
	 * Saves patrol motivation intelligences to database.
	 * This call will also remove intelligence object previously assigned to patrol if they are not in provided list.
	 * 
	 * @param session
	 * @param patrol
	 * @param intelligences
	 */
	public static void savePatrolIntelligences(Session session, Patrol patrol, List<Intelligence> intelligences) {
		deletePatrolIntelligences(session, patrol, intelligences);
		for (Intelligence intelligence : intelligences) {
			PatrolIntelligence pi = new PatrolIntelligence();
			pi.setPatrol(patrol);
			pi.setIntelligence(intelligence);
			session.saveOrUpdate(pi);
		}
	}

	/**
	 * Removes all intelligence records from {@link PatrolIntelligence} table associated with given patrol
	 * excluding items in provided list
	 * 
	 * @param session
	 * @param patrol
	 * @param exclude
	 */
	private static void deletePatrolIntelligences(Session session, Patrol patrol, List<Intelligence> exclude) {
		if (patrol.getUuid() == null) {
			return; //patrol is just created and there will be no records related to it
		}
		String queryString = "DELETE FROM PatrolIntelligence WHERE id.patrol = :patrol"; //$NON-NLS-1$
		boolean hasExcludes = exclude != null && !exclude.isEmpty();
		if (hasExcludes) {
			queryString += " AND id.intelligence not in (:exclude)"; //$NON-NLS-1$
		}
		Query query = session.createQuery(queryString);
		query.setParameter("patrol", patrol); //$NON-NLS-1$
		if (hasExcludes) {
			query.setParameterList("exclude", exclude); //$NON-NLS-1$
		}
		query.executeUpdate();
	}
	
	/**
	 * Returns the list of intelligences reported by this patrol
	 * 
	 * @param patrol
	 * @return the list of intelligences reported by this patrol
	 */
	public static List<Intelligence> getReportedIntelligences(Patrol patrol) {
		Session session = SmartHibernateManager.openSession();
		session.beginTransaction();
		try {
			return getReportedIntelligences(patrol, session);
		} finally {
			try{
				session.getTransaction().rollback();
			}catch(Exception ex){}
			session.close();
		}
	}

	/**
	 * Returns the list of intelligences reported by this patrol
	 * 
	 * @param patrol
	 * @return the list of intelligences reported by this patrol
	 */
	public static List<Intelligence> getReportedIntelligences(Patrol patrol, Session session) {
		Criteria query = session.createCriteria(Intelligence.class).add(Restrictions.eq("patrol", patrol)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Intelligence> list = query.list();
		return list;
	}

	/**
	 * Returns the list of intelligences that motivated patrol
	 * 
	 * @param patrol
	 * @return the list of intelligences that motivated patrol
	 */
	public static List<Intelligence> getMotivatedIntelligences(Patrol patrol) {
		Session session = SmartHibernateManager.openSession();
		session.beginTransaction();
		try {
			return getMotivatedIntelligences(patrol, session);
		} finally {
			try{
				session.getTransaction().rollback();
			}catch (Exception ex){}
			session.close();
		}
	}

	/**
	 * Returns the list of intelligences that motivated patrol
	 * 
	 * @param patrol
	 * @return the list of intelligences that motivated patrol
	 */
	public static List<Intelligence> getMotivatedIntelligences(Patrol patrol, Session session) {
		Query query = session.createQuery("SELECT pi.id.intelligence FROM PatrolIntelligence pi WHERE pi.id.patrol = :patrol"); //$NON-NLS-1$
		query.setParameter("patrol", patrol); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Intelligence> list = query.list();
		return list;
	}
	
	/**
	 * @throws Exception 
	 * @throws  
	 * Loads the list item for the given intelligence uuid
	 * 
	 * @return a list item for the given intelligence
	 * @throws Exception 
	 * @throws  
	 */
	public static ListItem getIntelligence(Session session, String id) throws Exception {
		Query q = session.createQuery("SELECT uuid, name FROM Intelligence WHERE uuid =:uuid"); //$NON-NLS-1$
		q.setParameter("uuid", SmartUtils.decodeHex(id)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.list();
		if (results.size() == 1) {
			return new ListItem( (byte[])((Object[])results.get(0))[0], (String)((Object[])results.get(0))[1]);
		} else {
			IntelligencePlugIn.displayLog(MessageFormat.format(Messages.IntelligenceHibernateManager_Intelligence_NotFound_Error, id), null);
			return null;
		}
	}

}
