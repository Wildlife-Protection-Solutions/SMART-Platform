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
package org.wcs.smart.paws.plugin;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.p2.common.updatesite.UninstallProvisioningAction;
import org.wcs.smart.paws.PawsFileManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Action that is called when PAWS plug-in is uninstalled
 * 
 * @author Emily
 */
public class OnUninstallAction extends UninstallProvisioningAction {

	@Override
	protected String getPluginId() {
		return PawsPlugIn.PLUGIN_ID;
	}
	
	@Override
	protected void performRemove() {
		List<ConservationArea> cas = null;
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				cas = HibernateManager.getConservationAreas(session);
				uninstall(session);
				session.getTransaction().commit();
			} catch (Exception e) {
				try{
					session.getTransaction().rollback();
				}catch (Exception ex){
					PawsPlugIn.log(ex.getMessage(), ex);	
				}
				PawsPlugIn.displayLog(Messages.RemovePawsJob_Error + e.getMessage(), e);
				return;
			}
		}	

		//remove filestore data  
		if (cas != null){
			//delete intelligence data from the filestore 
			for (ConservationArea ca : cas){
				try {
					Path folder = Paths.get(ca.getFileDataStoreLocation()).resolve(PawsFileManager.PAWS_DIR);
					SmartUtils.deleteDirectory(folder);
				} catch (IOException ex) {
					PawsPlugIn.log("Error removing PAWS folder for Conservation Area datastore: " + ca.getId(), ex); //$NON-NLS-1$
				}
			}
		}
	}
	
	private void uninstall(Session session){
		//to drop and the label tables (objects that extend NamedItem or NamedKeyItem)
		String[] TABLES = new String[]{	
			"paws_parameter", //$NON-NLS-1$
			"paws_query_class", //$NON-NLS-1$
			"paws_simple_class", //$NON-NLS-1$
			"paws_run", //$NON-NLS-1$
			"paws_service", //$NON-NLS-1$
			"paws_configuration", //$NON-NLS-1$
		};
		
		String[] LABELTABLES = new String[]{
		};
		
		//drop labels
		for (String table : LABELTABLES){
			if (DerbyHibernateExtensions.tableExists(session, table)){
				session.createNativeQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart." + table + ")").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		//drop the tables
		for (String table : TABLES){
			if (DerbyHibernateExtensions.tableExists(session, table)){
				session.createNativeQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
			}
		}		

		//remove from plugin table
		HibernateManager.setPlugInVersion(PawsPlugIn.PLUGIN_ID, null, session);
	}

}