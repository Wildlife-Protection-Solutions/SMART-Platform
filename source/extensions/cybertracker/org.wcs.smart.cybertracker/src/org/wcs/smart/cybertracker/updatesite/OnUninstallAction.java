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
package org.wcs.smart.cybertracker.updatesite;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.p2.common.updatesite.UninstallProvisioningAction;
import org.wcs.smart.util.SmartUtils;

/**
 * Action that is called when CyberTracker plug-in is uninstalled
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class OnUninstallAction extends UninstallProvisioningAction {

	@Override
	protected String getPluginId() {
		return CyberTrackerPlugIn.PLUGIN_ID;
	}
	
	@Override
	protected void performRemove() {
		String[] tables = new String[] { "CM_CT_PROPERTIES_PROFILE", //$NON-NLS-1$
				"CT_PROPERTIES_OPTION", //$NON-NLS-1$
				"CT_PROPERTIES_PROFILE_OPTION", //$NON-NLS-1$
				"CT_PROPERTIES_PROFILE", //$NON-NLS-1$
				"CT_INCIDENT_LINK", //$NON-NLS-1$
				"CT_METADATA_VALUE_UUID", //$NON-NLS-1$
				"CT_METADATA_VALUE", //$NON-NLS-1$
				"CT_NAVIGATION_LAYER" //$NON-NLS-1$
		};

		try (final Session session = HibernateManager.openSession()) {

			final List<ConservationArea> caList = HibernateManager.getConservationAreas(session);
			session.beginTransaction();
			try {
				// delete labels
				if (DerbyHibernateExtensions.tableExists(session, "CT_PROPERTIES_PROFILE")) { //$NON-NLS-1$
					session.createNativeMutationQuery(
							"delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart.CT_PROPERTIES_PROFILE)") //$NON-NLS-1$
							.executeUpdate();
				}
				// delete tables
				for (String table : tables) {
					if (DerbyHibernateExtensions.tableExists(session, table)) {
						session.createNativeMutationQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
					}
				}
				// clean filestore
				for (ConservationArea ca : caList) {
					SmartUtils.deleteDirectory(ICyberTrackerConstants.getDowloadFolder(ca));
				}
				HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, null, session);
				session.getTransaction().commit();

			} catch (Exception e) {
				SmartPlugIn.displayLog(Messages.RemoveCyberTrackerTablesJob_Error, e);
				return;
			}
		}
	}



}