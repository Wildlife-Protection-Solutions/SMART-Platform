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
package org.wcs.smart.connect.server;

import java.io.File;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.type.UUIDBinaryType;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.export.CaExporter;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Upload ca engine that processes the initial upload request,
 * checking server status and packaging ca export.
 * 
 * @author Emily
 *
 */
public class UploadCaEngine {

	
	
	/**
	 * Creates necessary local database records 
	 * and starts a job to upload the export to the server  
	 * 
	 * @param sc
	 * @param monitor
	 * @throws Exception 
	 */
	public void upload(ConnectServer server, SmartConnect connect, IProgressMonitor monitor) throws Exception{

		monitor.beginTask("Uploading Conservation Area to SMART Connect", 3);
		monitor.subTask("Connecting to SMART Connect");
		ConservationAreaInfo serverInfo = connect.getCaInfo(SmartDB.getCurrentConservationArea().getUuid());
		monitor.worked(1);
		
		monitor.subTask("Configuring upload");
		ConnectServerStatus localStatus = null;
		Session s = HibernateManager.openSession();

		try{
			s.beginTransaction();
			localStatus = getLocalStatus(s);
	
			if (serverInfo != null){
				if (serverInfo.getStatus() == ConservationAreaInfo.Status.DATA){
					throw new Exception("This conservation area already exists on the server.  You cannot upload to the server again without removing it from the server first.");
				}
				
				if (serverInfo.getStatus() == ConservationAreaInfo.Status.UPLOADING){
					//somebody is uploading data;  is it us (check versions)?
					if (localStatus == null || !(localStatus.getVersion().equals(serverInfo.getVersion()))){
						throw new Exception("Another desktop client is currently uploading this ConservationArea to the server.  You cannot upload your data at the same time.");
					}
					//continue using local file
					if (localStatus != null){
						if (localStatus.getLocalFile() == null){
						//we have a problem because we do not have a local file to upload anymore
							throw new Exception("Could not resume upload as local file could not be found.");
						}else{
							File f = new File(SmartContext.INSTANCE.getFilestoreLocation(), localStatus.getLocalFile());
							if (!f.exists()){
								throw new Exception("Could not resume upload as local file no longer exists.");	
							}
						}
					}
				}
			}
			
			if (serverInfo == null || 
					(serverInfo != null && serverInfo.getStatus() == ConservationAreaInfo.Status.NODATA)){
				//update ca to server (new ca will be created if required; otherwise we update existing)
				if (localStatus != null){
					s.delete(localStatus);
					localStatus = null;
				}
				localStatus = createNewLocalStatus(server, s);
				localStatus.setLocalFile(getExportFilename());
				s.save(localStatus);
			}
			
			s.getTransaction().commit();
			
			monitor.worked(1);
			
		}catch(Exception ex){
			throw new Exception("Failed to configure upload.\n\nTo resolve you may need to to log into SMART Connect, delete the conservation area data and try again.\n\n" + ex.getMessage(), ex);
		}finally{
			s.close();
		}
		try{
			if (localStatus.getUploadUrl() == null){
				monitor.subTask("Exporting conservation area for upload.");
				packageCa(localStatus.getLocalFile(), new SubProgressMonitor(monitor, 1));
				
				String uploadURL = connect.getCaUploadUrl(localStatus.getUuid(), 
						localStatus.getVersion(), 
						new File(SmartContext.INSTANCE.getFilestoreLocation(), localStatus.getLocalFile()));
				
				localStatus.setUploadUrl(uploadURL);
				s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					s.saveOrUpdate(localStatus);
					s.getTransaction().commit();
				}finally{
					s.close();
				}
			}else{
				monitor.worked(1);
			}
		}catch(Exception ex){
			throw new Exception("Failed to configure upload.\n\nTo resolve you may need to to log into SMART Connect, delete the conservation area data and try again.\n\n" + ex.getMessage(), ex);
		}
		
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Upload", "SMART will now upload the data to connect in the background.  You will be notified with the upload is complete.");		
			}
			
		});
		
		UploadCaJob job = new UploadCaJob(connect, localStatus);
		job.schedule();
		
		monitor.done();
	}
	
	/**
	 * The export filename.
	 * @return
	 */
	private String getExportFilename(){
		return ConnectPlugIn.CONNECT_FILESTORE_DIR + File.separator + "sc_" +UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid())+ "_" + System.nanoTime() + ".zip";
	}

	/**
	 * Get the local status object from the database for the current conservation area
	 * @param s
	 * @return
	 */
	private ConnectServerStatus getLocalStatus(Session s){
		return (ConnectServerStatus) s.get(ConnectServerStatus.class, SmartDB.getCurrentConservationArea().getUuid());
	}
	
	/**
	 * Creates a new local status object
	 * @param server
	 * @param s
	 * @return
	 */
	private ConnectServerStatus createNewLocalStatus(ConnectServer server, Session s){

		Properties prop = new Properties();
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY, StandardRandomStrategy.INSTANCE);
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS, UUIDGenerationStrategy.class.getName());
		UUIDGenerator uuidGenerator = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();
		uuidGenerator.configure(new UUIDBinaryType(), prop, null);
		
		ConnectServerStatus status = new ConnectServerStatus();
		
		UUID client = (UUID) uuidGenerator.generate((SessionImplementor) s, status);
		status.setClientId(client);
	
		UUID version = (UUID) uuidGenerator.generate((SessionImplementor) s, status);
		status.setVersion(version);
	
		status.setRevision(null);
		status.setServer(server);
		status.setConservationArea(SmartDB.getCurrentConservationArea());
		status.setStatus(ConnectServerStatus.Status.ACTIVE);
		return status;

	}
	
	
	/**
	 * Export the current conservation area to a temporary file.
	 * 
	 * This needs to lock the database so this must prevent the
	 * users from doing anything else while processing
	 * @throws Exception 
	 * 
	 */
	private void packageCa(String filename, IProgressMonitor monitor) throws Exception{
		monitor.beginTask("Packaging conservation area for upload.", 1);
		
		File f = new File(SmartContext.INSTANCE.getFilestoreLocation(), filename);
		if (!f.getParentFile().exists()){
			SmartUtils.createDirectory(f.getParentFile());
		}
	
		CaExporter exporter = new CaExporter();
		exporter.export(f, new SubProgressMonitor(monitor, 1));
	}
}
