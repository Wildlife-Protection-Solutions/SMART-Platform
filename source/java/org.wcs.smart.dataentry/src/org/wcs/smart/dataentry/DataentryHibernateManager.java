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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;

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
		return QueryFactory.buildQuery(session, ConfigurableModel.class, "conservationArea", ca).list(); //$NON-NLS-1$
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
		try(Session session = openSession()){
			session.beginTransaction();
			try {
				return getFullConfigurableModel(uuid, session);
			} finally {
				session.getTransaction().commit();
			}
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
		ConfigurableModel model = session.get(ConfigurableModel.class,  uuid);
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
	
	public static List<CmAttributeConfig> getCmAttributeConfigs(Session session, ConfigurableModel cm, Attribute attribute) {
		if (cm.getUuid() == null) {
			return new ArrayList<>();
		}
		return QueryFactory.buildQuery(session, CmAttributeConfig.class,
				new Object[] {"attribute", attribute}, //$NON-NLS-1$
				new Object[] {"model", cm}).list(); //$NON-NLS-1$
	}
	
	/**
	 * Delete a filestore for given {@link ConfigurableModel}.
	 * 
	 * @param {@link ConfigurableModel} who's filestore to delete
	 */
	public static void deleteFilestore(ConfigurableModel model) {
		Path fileStore = Paths.get(model.getFileDataStoreLocation());
		if (Files.exists(fileStore)) {
			try {
				SmartUtils.deleteDirectory(fileStore);
			} catch(Exception ex) {
				SmartPlugIn.displayLog(MessageFormat.format(Messages.DataentryHibernateManager_DeleteConfigurableModelFilestoreError, fileStore.toAbsolutePath().toString()), ex);
			}
		}
	}
	
}
