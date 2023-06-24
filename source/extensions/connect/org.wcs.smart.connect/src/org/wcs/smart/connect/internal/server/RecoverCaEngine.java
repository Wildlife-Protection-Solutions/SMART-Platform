package org.wcs.smart.connect.internal.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.ca.in.CaImporter;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.ConnectClient;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.ZipUtil;
import org.wcs.smart.util.ZipUtilCommon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.icu.text.MessageFormat;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.DatatypeConverter;

public class RecoverCaEngine {
	
	private ConservationArea ca;
	private SmartConnect connect;
	private ProgressMonitorDialog dialog;
	
	public RecoverCaEngine(ConservationArea ca, SmartConnect connect, ProgressMonitorDialog dialog){
		this.ca = ca;
		this.connect = connect;
		this.dialog = dialog;
	}
	
	/**
	 * Downloads the Conservation Area export package and imports it.
	 * 
	 * @param monitor the progress monitor to use for reporting progress to the user. 
	 * 
	 * @return true if download and install completed. false if user cancelled
	 * 
	 * @throws Exception
	 */
	public void downloadImport(IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DownloadCaEngine_TaskName, 5);
		
		List<Path> toDelete = new ArrayList<>();
		Path importFile;
		
		try {
			/* build hash file of filestore files */
			
			Path filepath = buildFilestoreHash(progress.split(1));
			
			ConnectClient service =  connect.getClient()
	        		.target(connect.getServer().getServerUrl() + SmartConnect.API_URL)
	        		.proxy(ConnectClient.class);
	        
			String statusUrl = null;
			try(InputStream is = Files.newInputStream(filepath)){
				try(Response r = service.recoverCa(ca.getUuid().toString(), is)){					
					if (r.getStatus() == Status.ACCEPTED.getStatusCode()){
						statusUrl = r.getHeaderString(HttpHeaders.LOCATION);
					}else{
						throw new WebApplicationException(r);
					}
				}
			}finally {
				Files.delete(filepath);
			}
						
			/* wait for ca export to be created */
			progress.subTask(Messages.DownloadCaEngine_WaitSubTaskName);
			Long start = System.nanoTime();
			WorkItemStatus status = null ;
			int waitTime = ConnectServerOption.ConnectionOption.RETY_WAIT_TIME.getIntegerValue(connect.getServer());
			
			SubMonitor waitMonitor = progress.split(1);
			waitMonitor.beginTask(Messages.DownloadCaEngine_WaitSubTaskName, 100);
			int last = 0;
			while(status == null || status.getStatus() == WorkItemStatus.Status.PROCESSING){
				Long current = System.nanoTime();
				
				if ( (current - start) > ConnectServerOption.ConnectionOption.MAX_PROCESSING_WAIT_TIME.getIntegerValue(connect.getServer()) * 1000000l) throw new Exception(Messages.DownloadCaEngine_Timeout);
				Thread.sleep(waitTime);
				try{
					
					status = connect.getWorkItemStatus(statusUrl);
					waitMonitor.subTask(
							MessageFormat.format("{0} ({1} - {2}%)", //$NON-NLS-1$
							Messages.DownloadCaEngine_WaitSubTaskName,
							status.getMessage(),
							status.getPercentComplete()));
					waitMonitor.worked(status.getPercentComplete() - last);
					last = status.getPercentComplete();
					
				}catch (Exception ex){
					ConnectPlugIn.log("Error requesting ca download status.", ex); //$NON-NLS-1$
				}
				progress.checkCanceled();	
			}		
			//progress.worked(1);
			
			if (status.getStatus() == WorkItemStatus.Status.ERROR){
				throw new Exception(Messages.DownloadCaEngine_CaDownloadError + SmartConnect.parseErrorMessage(status.getMessage()));
			}
	
			/* download file */
			progress.subTask(Messages.DownloadCaEngine_DownloadSubtaskName);
			String message = status.getMessage();
			JsonNode nd = (new ObjectMapper()).readTree(message);
			String downloadUrl = nd.get("file_url").asText(); //$NON-NLS-1$
			
			importFile = connect.downloadFileFromUrl(downloadUrl, null, progress.split(1));
			
			//start by validating the package
			progress.subTask(Messages.DownloadCaEngine_VersionValidation);
			if (!CaImporter.validateCaImport(importFile)) {
				return ;
			}

			//extract recover.deletelist.txt from the zip file
			
			byte[] data = ZipUtilCommon.readFileFromZip(importFile, Paths.get(ICaDataExportEngine.DELETEFILES_FILENAME));
			if (data.length > 0) {
				String deleteFiles = new String(data);
				String[] files = deleteFiles.split(ICaDataExportEngine.FILE_SEPARATOR);
				for (String file : files) {
					if (file.isBlank()) continue;
					Path deleteFile = Paths.get(ca.getFileDataStoreLocation()).resolve(file);
					toDelete.add(deleteFile);
				}
			}
			progress.checkCanceled();
		} catch (OperationCanceledException ex) {
			return ;
		}
		
		//at this point we don't support canceling
		dialog.setCancelable(false);
		/* delete existing ca */
		if (!deleteCa(ca, progress.split(1))) return ;
		if (!validateCaDeleted()) return ;
		/* import file */
		progress.subTask(Messages.DownloadCaEngine_InstallSubtaskName);
		try{
			//delete files from filestore
			for (Path file: toDelete) Files.deleteIfExists(file);
							
			//copy over new files as part of import
			progress.checkCanceled();	
			CaImporter.importCa(importFile, progress.split(1));
			
		}finally{
			Files.delete(importFile);
		}

		Display.getDefault().asyncExec(()->PlatformUI.getWorkbench().restart());
		
	}
	
	
	private Path buildFilestoreHash(IProgressMonitor pmonitor) throws IOException, NoSuchAlgorithmException {
		SubMonitor monitor = SubMonitor.convert(pmonitor, "", 10); //$NON-NLS-1$
		monitor.subTask("Scanning Local Filestore");
		
		Path fsLocation = Paths.get(ca.getFileDataStoreLocation());
		List<Path> allFiles = new ArrayList<>();
		try(Stream<Path> spath = Files.walk(fsLocation)){
			spath.filter(p->!Files.isDirectory(p)).forEach(p->allFiles.add(p));
		}
		monitor.setWorkRemaining(allFiles.size());
		final MessageDigest digest = MessageDigest.getInstance(ICaDataExportEngine.RECOVERY_HASH_ALGORITHM);

		
		Path hashFile = Files.createTempFile("smartcafilehash", ""); //$NON-NLS-1$ //$NON-NLS-2$
		try(BufferedWriter writer = Files.newBufferedWriter(hashFile, StandardCharsets.UTF_8)){
			for(Path path : allFiles) {
				byte[] hash = DigestUtils.digest(digest, path);
				String hex = DatatypeConverter.printHexBinary(hash);
				digest.reset();
				
				writer.write(fsLocation.relativize(path).toString());
				writer.write(ICaDataExportEngine.DATA_SEPARATOR);
				writer.write(hex);
				writer.write(ICaDataExportEngine.FILE_SEPARATOR);
				monitor.worked(1);
				monitor.checkCanceled();
			}			
		}
		String root = SharedUtils.getFilenameWithoutExtension(hashFile.getFileName().toString());
		
		//zip up the hash file
		Path zip = hashFile.getParent().resolve(root + ".zip"); //$NON-NLS-1$
		try {
			ZipUtil.createZip(Collections.singleton(hashFile), zip, monitor.split(10));
		}catch (Exception ex) {
			if (Files.exists(zip)) Files.delete(zip);
			throw ex;
		}finally {
			Files.delete(hashFile);
		}
		return zip;
		
	}
	
	
	private boolean validateCaDeleted(){
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				ConservationArea desktopCa = (ConservationArea)s.get(ConservationArea.class, ca.getUuid());
				if (desktopCa != null){
					//at some point something went wrong
					return false;
				}
			}finally {
				s.getTransaction().rollback();
			}
		}
		return true;
	}
	
	
	private boolean deleteCa(final ConservationArea ca, final IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DownloadCaEngine_DeleteTaskName, 1);
		try{
			ConservationAreaManager.getInstance().deleteConservationArea(ca, progress.split(1), false, false);
		}catch (final Exception ex){
			ConnectPlugIn.log(Messages.DownloadCaEngine_CaDataError, ex);
			throw new Exception("Unable to delete Conservation Area before installing new Conservation Area.", ex); //$NON-NLS-1$
		}
		return true;
	}
}
