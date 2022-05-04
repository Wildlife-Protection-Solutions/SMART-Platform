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
package org.wcs.smart.asset.query.plugin;

import org.hibernate.Session;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.p2.common.updatesite.UninstallProvisioningAction;

/**
 * Action that is called when Asset query plug-in is uninstalled
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class OnUninstallAction extends UninstallProvisioningAction {

	private String[] LABELTABLES = new String[]{
			"ASSET_OBSERVATION_QUERY", //$NON-NLS-1$$
			"ASSET_SUMMARY_QUERY", //$NON-NLS-1$
			"ASSET_WAYPOINT_QUERY" //$NON-NLS-1$

	};
	
	@Override
	protected String getPluginId() {
		return AssetQueryPlugIn.PLUGIN_ID;
	}
	
	@Override
	protected void performRemove() {
		//drop tables
		try(final Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (String table : LABELTABLES){
					if (DerbyHibernateExtensions.tableExists(session, table)){
						session.createNativeQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart." + table + ")").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				
				for (String table : LABELTABLES){
					if (DerbyHibernateExtensions.tableExists(session, table)){
						session.createNativeQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
					}
				}		
				
				HibernateManager.setPlugInVersion(AssetQueryPlugIn.PLUGIN_ID, null, session);
				session.getTransaction().commit();
			} catch (Exception e) {
				try{
					session.getTransaction().rollback();
				}catch (Exception ex){
					AssetQueryPlugIn.log(ex.getMessage(), ex);	
				}
				AssetQueryPlugIn.displayLog(Messages.RemoveAssetQueryJob_ErrorMsg, e);
			}
		}
	}
	
}