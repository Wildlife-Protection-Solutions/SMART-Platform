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
package org.wcs.smart.connect.dataqueue.installer;

import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes all entity plug-in related tabled from the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RemoveConnectDataQueueJob extends Job {
	
	private static String[] TABLES = new String[]{
		"connect_data_queue", //$NON-NLS-1$
		"data_queue_processing_op", //$NON-NLS-1$
	};
	
	public RemoveConnectDataQueueJob() {
		super("Uninstall Connect Data Processing Queue Plugin");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		return dropTables();
	}

	
	private IStatus dropTables(){
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			//drop all tables
			for (int i = 0; i < TABLES.length; i ++){
				if (DerbyHibernateExtensions.tableExists(session, TABLES[i])){
					session.createSQLQuery("DROP TABLE SMART."+ TABLES[i]).executeUpdate(); //$NON-NLS-1$
				}
			}
			
			//we need to remove any data into the data queue associated
			//with this plugin (for each ca the /connect/dataqueue folder
			Path filestore = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation());
			try(DirectoryStream<Path> dirs = Files.newDirectoryStream(filestore)){
				for (Path p : dirs){
					Path dataqueue = p.resolve(ConnectDataQueuePlugin.DATA_QUEUE_DIR);
					if (Files.exists(dataqueue) && Files.isDirectory(dataqueue)){
						try{
							FileUtils.deleteDirectory(dataqueue.toFile());
						}catch (Exception ex){
							ConnectDataQueuePlugin.log("Could not remove data queue folder from filestore when uninstalled Connect DataQueue Plugin", ex);
						}
					}
				}
			}catch (Exception ex){
				ConnectDataQueuePlugin.log("Could not remove data queue folders from filestore when uninstalled Connect DataQueue Plugin", ex);
			}
			
			
			
			//clear version
			HibernateManager.setPlugInVersion(ConnectDataQueuePlugin.PLUGIN_ID, null, session);
			session.getTransaction().commit();
		} catch (final Exception e) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					SmartPlugIn.displayLog("Error uninstalling SMART Connect module.  Could not remove database tables.  Please contact your system administrator.", e);
				}
			});
			return new Status(IStatus.ERROR, ConnectDataQueuePlugin.PLUGIN_ID, 1, "Error uninstalling Connect data processing queue plugin." + e.getLocalizedMessage(), e); 
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			session.close();
		}
		return Status.OK_STATUS;
	}
}
