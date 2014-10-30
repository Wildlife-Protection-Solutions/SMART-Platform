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
package org.wcs.smart.er.updatesite;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes all Ecological Records plug-in related tabled from the database
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class RemoveERJob extends Job {

	private String[] LABELTABLES = new String[]{
			"SURVEY_DESIGN", //$NON-NLS-1$
			"SAMPLING_UNIT_ATTRIBUTE", //$NON-NLS-1$
			"SAMPLING_UNIT_ATTRIBUTE_LIST", //$NON-NLS-1$
			"MISSION_ATTRIBUTE", //$NON-NLS-1$
			"MISSION_ATTRIBUTE_LIST", //$NON-NLS-1$
	};
	
	private String[] TABLES = new String[]{
			"MISSION_MEMBER", //$NON-NLS-1$
			"MISSION_PROPERTY_VALUE", //$NON-NLS-1$
			"SURVEY_WAYPOINT", //$NON-NLS-1$
			"MISSION_TRACK", //$NON-NLS-1$
			"MISSION_PROPERTY", //$NON-NLS-1$
			"SAMPLING_UNIT_ATTRIBUTE_VALUE", //$NON-NLS-1$
			"SURVEY_DESIGN_SAMPLING_UNIT", //$NON-NLS-1$
			"SAMPLING_UNIT", //$NON-NLS-1$
			"MISSION_DAY", //$NON-NLS-1$
			"MISSION", //$NON-NLS-1$
			"SURVEY", //$NON-NLS-1$
			"SURVEY_DESIGN_PROPERTY", //$NON-NLS-1$
			"SURVEY_DESIGN", //$NON-NLS-1$
			"SAMPLING_UNIT_ATTRIBUTE_LIST", //$NON-NLS-1$
			"SAMPLING_UNIT_ATTRIBUTE", //$NON-NLS-1$
			"MISSION_ATTRIBUTE_LIST", //$NON-NLS-1$
			"MISSION_ATTRIBUTE", //$NON-NLS-1$
	};
	
	public RemoveERJob() {
		super(Messages.RemoveERJob_Title);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<ConservationArea> cas = null;
		//drop tables
		final Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			cas = HibernateManager.getConservationAreas(session);
			//delete all waypoints associated with type SURVEY
			Query q = session.createQuery("DELETE Waypoint WHERE source = :src"); //$NON-NLS-1$
			q.setParameter("src", SurveyWaypointSource.KEY); //$NON-NLS-1$
			q.executeUpdate();
			
			
			for (String table : LABELTABLES){
				if (DerbyHibernateExtensions.tableExists(session, table)){
					session.createSQLQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart." + table + ")").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			
			for (String table : TABLES){
				if (DerbyHibernateExtensions.tableExists(session, table)){
					session.createSQLQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
				}
			}		
			
			HibernateManager.setPlugInVersion(EcologicalRecordsPlugIn.PLUGIN_ID, null, session);
			session.getTransaction().commit();

		} catch (Exception e) {
			try{
				session.getTransaction().rollback();
			}catch (Exception ex){
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex);	
			}
			EcologicalRecordsPlugIn.displayLog(Messages.RemoveERJob_UninstallError, e);
			return new Status(Status.ERROR,EcologicalRecordsPlugIn.PLUGIN_ID,e.getMessage());
		} finally {
			try {
				session.close();
			} catch (Exception ex) {
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
			}
		}
		if (cas != null){
			for (ConservationArea ca : cas){
				try {
					File deleteMe = new File(ca.getFileDataStoreLocation(), SurveyDesign.SURVEY_FILESTORE_LOC);
					FileUtils.deleteDirectory(deleteMe);
				} catch (IOException ex) {
					//some errors deleting filestore
					EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		
		return Status.OK_STATUS;
	}

}
