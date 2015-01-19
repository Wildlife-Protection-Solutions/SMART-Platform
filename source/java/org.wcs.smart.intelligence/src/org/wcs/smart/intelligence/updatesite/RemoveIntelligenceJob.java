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
package org.wcs.smart.intelligence.updatesite;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;

/**
 * Job removes all intelligence related tabled from the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RemoveIntelligenceJob extends Job {

	public RemoveIntelligenceJob() {
		super(Messages.RemoveIntelligenceJob_Title);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final Session session = HibernateManager.openSession();
		final List<ConservationArea> caList = HibernateManager.getConservationAreas(session);
		session.beginTransaction();
		try {
			//delete labels
			if (DerbyHibernateExtensions.tableExists(session, "intelligence_source")){ //$NON-NLS-1$
				session.createSQLQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart.intelligence_source)").executeUpdate(); //$NON-NLS-1$
			}
			if (DerbyHibernateExtensions.tableExists(session, "intelligence")){ //$NON-NLS-1$
				session.createSQLQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart.intelligence)").executeUpdate(); //$NON-NLS-1$
			}

			//delete actual tables
			if (DerbyHibernateExtensions.tableExists(session, "intelligence_attachment")){ //$NON-NLS-1$
				session.createSQLQuery("DROP TABLE smart.intelligence_attachment").executeUpdate(); //$NON-NLS-1$
			}
			if (DerbyHibernateExtensions.tableExists(session, "intelligence_point")){ //$NON-NLS-1$
				session.createSQLQuery("DROP TABLE smart.intelligence_point").executeUpdate(); //$NON-NLS-1$
			}
			if (DerbyHibernateExtensions.tableExists(session, "patrol_intelligence")){ //$NON-NLS-1$
				session.createSQLQuery("DROP TABLE smart.patrol_intelligence").executeUpdate(); //$NON-NLS-1$
			}
			//NOTE: derby acts stupid if this constraint is not manually removed
			if (DerbyHibernateExtensions.tableExists(session, "intelligence")){ //$NON-NLS-1$
				session.createSQLQuery("ALTER TABLE smart.intelligence DROP CONSTRAINT intelligence_source_uuid_fk").executeUpdate(); //$NON-NLS-1$
				session.createSQLQuery("DROP TABLE smart.intelligence").executeUpdate(); //$NON-NLS-1$
			}
			if (DerbyHibernateExtensions.tableExists(session, "intelligence_source")){ //$NON-NLS-1$
				session.createSQLQuery("DROP TABLE smart.intelligence_source").executeUpdate(); //$NON-NLS-1$
			}	
			if (DerbyHibernateExtensions.tableExists(session, "informant")){ //$NON-NLS-1$
				session.createSQLQuery("DROP TABLE smart.informant").executeUpdate(); //$NON-NLS-1$
			}	
			
			//delete filestore entries
			for (ConservationArea ca : caList) {
				File folder = new File(ca.getFileDataStoreLocation() + File.separator + IntelligencePlugIn.INTELLIGENCE_DIR);
				FileUtils.deleteDirectory(folder);
			}
	
			//remove from plugin table
			HibernateManager.setPlugInVersion(IntelligencePlugIn.PLUGIN_ID, null, session);
			try {
				session.getTransaction().commit();
			} catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
			}
			
		} catch (Exception e) {
			IntelligencePlugIn.displayLog(Messages.RemoveIntelligenceJob_Error, e);
		} finally {
			try {
				session.close();
			} catch (Exception ex) {
				IntelligencePlugIn.log(ex.getMessage(), ex);
			}
		}
		
		return Status.OK_STATUS;
	}

}
