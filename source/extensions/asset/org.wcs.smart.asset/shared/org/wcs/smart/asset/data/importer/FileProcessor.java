package org.wcs.smart.asset.data.importer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;

public class FileProcessor {

	private List<Path> files;
	private HashMap<Path, FileProxy> fileDetails;
	
	private ConservationArea ca;
	
	public FileProcessor(ConservationArea ca, List<Path> files) {
		this.ca = ca;
		this.files = files;
	}
	
	public void processFiles(IProgressMonitor monitor) {
		fileDetails = new HashMap<>();
	
		monitor.beginTask("Processing Asset Files", files.size());
		files.forEach(f->{
			monitor.subTask(f.toString());
			processFile(f, ca);	
			monitor.worked(1);
			if (monitor.isCanceled()) return;
		});
	}
	
	public void processFile(Path file, ConservationArea ca) {
		try {
			FileProxy proxy = FileMetadataReader.readFile(file, ca);
			try(Session session = HibernateManager.openSession()){
				proxy.updateAssetDeployment(session);
			}
			fileDetails.put(file,  proxy);
		}catch (Exception ex) {
			ex.printStackTrace();
			//TODO: process exception
			FileProxy p = new FileProxy(file, ca);
//			p.setProcessingException(ex);
			fileDetails.put(file,  p);
		}
	}
	
	public FileProxy getFileDetails(Path file) {
		return fileDetails.get(file);
	}
	
	public List<Path> getFiles() {
		return this.files;
	}
	
	public Collection<FileProxy> getFileDetails() {
		return fileDetails.values();
	}
	
	public void removeFile(FileProxy file) {
		files.remove(file.getFile());
		fileDetails.remove(file.getFile());
	}
	
	
	public boolean isValid() {
		if (fileDetails.size() != files.size()) return false;
		
		for (FileProxy proxy : fileDetails.values()) {
			if (!proxy.isValid()) return false;
		}
		return true;
	}
	
	
}
