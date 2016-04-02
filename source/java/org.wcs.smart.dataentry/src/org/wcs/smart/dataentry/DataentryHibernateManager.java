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
package org.wcs.smart.dataentry;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Dataentry related database functions.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class DataentryHibernateManager extends HibernateManager {

	/**
	 * Returns all configurable models for the active conservation area. This does not load
	 * the tree structure of the model.  @see getFullConfigurableModel if you need the entire
	 * tree structure.
	 * 
	 * @param session
	 * @return all ConfigurableModels
	 */
	public static List<ConfigurableModel> getConfigurableModels(Session session) {
		return getConfigurableModels(SmartDB.getCurrentConservationArea(), session);
	}
	
	/**
	 * Returns all configurable models for a given conservation area.  This does not load
	 * the tree structure of the model. @see getFullConfigurableModel if you need the entire
	 * tree structure.
	 * 
	 * @param ca the conservation are to load from
	 * @param session
	 * @return all ConfigurableModels
	 */
	public static List<ConfigurableModel> getConfigurableModels(ConservationArea ca, Session session) {
		Criteria query = session.createCriteria(ConfigurableModel.class).add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<ConfigurableModel> list = query.list();
		return list;
	}

	/**
	 * Loads a configurable model and all nodes into memory.  This function
	 * opens (and closes) a new session.
	 * 
	 * 
	 * @param uuid the uuid of the configurable model to load
	 * @return the loaded configurable model
	 */
	public static ConfigurableModel getFullConfigurableModel(UUID uuid) {
		if (uuid == null)
			return null;
		Session session = openSession();
		session.beginTransaction();
		try {
			return getFullConfigurableModel(uuid, session);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}
	
	/**
	 * Loads a configurable model and all nodes into memory using the provided
	 * session object.
	 * 
	 * @param uuid the uuid of the configurable model to load
	 * @param session
	 * @return all ConfigurableModels
	 */
	public static ConfigurableModel getFullConfigurableModel(UUID uuid, Session session) {
		if (uuid == null)
			return null;
		Criteria query = session.createCriteria(ConfigurableModel.class)
				.add(Restrictions.eq("uuid", uuid)); //$NON-NLS-1$
		ConfigurableModel model = (ConfigurableModel) query.uniqueResult();
		model.getNames().size();
		fetchNodesData(model.getNodes());
		return model;
	}

	private static void fetchNodesData(List<CmNode> nodes) {
		if (nodes == null)
			return;
		for (CmNode cmNode : nodes) {
			cmNode.getCmAttributes().size();
			fetchNodesData(cmNode.getChildren());
		}
	}
	
	/**
	 * Saves a given ConfigurableModel to the database.
	 * 
	 * @param model the ConfigurableModel to save
	 * @return <code>true</code> if saved successfully, <code>false</code> if error
	 */
	public static boolean saveConfigurableModel(ConfigurableModel model) {
		Session session = openSession();
		try {
			return saveConfigurableModel(model, session);
		} finally {
			session.close();
		}
	}

	/**
	 * Saves a given ConfigurableModel to the database.
	 * 
	 * @param model the ConfigurableModel to save
	 * @param session session
	 * @return <code>true</code> if saved successfully, <code>false</code> if error
	 */
	public static boolean saveConfigurableModel(ConfigurableModel model, Session session) {
		session.beginTransaction();
		try {
			//save a name
			if (model.getName() != null) {
				model.updateName(SmartDB.getCurrentLanguage(), model.getName());
			}
			session.saveOrUpdate(model);
			session.getTransaction().commit();
			return true;
		} catch (Exception ex) {
			session.getTransaction().rollback();
			SmartPlugIn.displayLog(Messages.DataentryHibernateManager_ConfigurableModel_Save_Error + "\n"+ ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return false;
		}
	}

	/**
	 * Delete a filestore for given {@link ConfigurableModel}.
	 * 
	 * @param {@link ConfigurableModel} who's filestore to delete
	 */
	public static void deleteFilestore(ConfigurableModel model) {
		File fileStore = new File(model.getFileDataStoreLocation());
		if (fileStore.exists()) {
			try {
				FileUtils.forceDelete(fileStore);
			} catch(Exception ex) {
				SmartPlugIn.displayLog(MessageFormat.format(Messages.DataentryHibernateManager_DeleteConfigurableModelFilestoreError, fileStore.getAbsolutePath()), ex);
			}
		}
	}
	
}
