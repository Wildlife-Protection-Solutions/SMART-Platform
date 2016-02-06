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
package org.wcs.smart.connect.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.DeleteConservationAreaHandler;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.connect.ConnectDatastore;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.server.replication.ChangeLogTableManager;
import org.wcs.smart.connect.server.replication.SyncHistoryManager;

/**
 * Delete handler for deleting smart connect information attached to conservation area.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CaConnectDeleteHandler implements ICaDeleteHandler {

	/**
	 * To be executed before the conservation area and patrol is deleted
	 */
	public static final int EXECUTE_ORDER = DeleteConservationAreaHandler.EXECUTE_ORDER + 1;
	
	public CaConnectDeleteHandler() {}

	@Override
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CaConnectDeleteHandler_TaskName);
		
		//SMART Connect Users
		Query q = session.createQuery("delete from ConnectUser cu where cu in (SELECT cu2.uuid FROM ConnectUser cu2 where cu2.server.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();

		//SMART Connect Changelog
		ChangeLogTableManager.INSTANCE.deleteAll(session, ca);

		//SMART Server Status
		q = session.createQuery("delete from ConnectServerStatus where uuid = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca.getUuid()); //$NON-NLS-1$
		q.executeUpdate();
		
		//Sync History
		SyncHistoryManager.INSTANCE.deleteAll(session, ca);
		
		//ConnectServerOptions
		q = session.createQuery("delete from ConnectServerOption op where op.id.server in (SELECT op2.id.server FROM ConnectServerOption op2 WHERE op2.id.server.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
				
		//ConnectServer
		q = session.createQuery("delete from ConnectServer where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete files from file store
		Path fs = Paths.get(ca.getFileDataStoreLocation(), ConnectDatastore.CONNECT_FILESTORE_DIR);
		if (Files.exists(fs)){
			try{
				FileUtils.forceDelete(fs.toFile());
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
		}
	}

}
