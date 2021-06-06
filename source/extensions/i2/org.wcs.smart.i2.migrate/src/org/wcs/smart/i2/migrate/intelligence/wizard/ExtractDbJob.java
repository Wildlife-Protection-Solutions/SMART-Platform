package org.wcs.smart.i2.migrate.intelligence.wizard;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.migrate.intelligence.Smart6Database;
import org.wcs.smart.util.ZipUtil;

public class ExtractDbJob implements IRunnableWithProgress{

	private String filename;
	
	private Smart6Database smart6db;
	private List<ConservationArea> toProcess;
	
	public ExtractDbJob(String filename) {
		this.filename = filename;
	}
	
	public Smart6Database getDatabase() {
		return this.smart6db;
	}
	
	public List<ConservationArea> getConservationAreas(){
		return this.toProcess;
	}
	
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

		SubMonitor task = SubMonitor.convert(monitor);
		task.beginTask("Extracting Backup", 2);
		
		Path p = Paths.get(filename);
		if (!Files.exists(p)) {
			throw new InvocationTargetException(new Exception("File not found."));
		}
		
		task.split(1).beginTask("unzipping backup", 1);
		Path dir = null;
		try {
			//TODO: delete when done
			dir = ZipUtil.unzip(p);
		}catch (Exception ex) {
			throw new InvocationTargetException(ex);
		}
		
		task.split(1).beginTask("validating version", 1);
		try {
			Smart6Database smart6db = new Smart6Database(dir);
			try {
				if (!smart6db.validateIntelligenceVersion()){
					throw new Exception("Invalid smart backup - must be a SMART6 backup with an intelligence database version of 4.0");
				}
				
				task.split(1).beginTask("loading conservation areas", 1);

				List<ConservationArea> s6 = smart6db.getConservationAreasWithData();
				Set<UUID> uuids6 = s6.stream().map(e->e.getUuid()).collect(Collectors.toSet());
				
				toProcess= new ArrayList<>();
				try(Session session = HibernateManager.openSession()){
					List<ConservationArea> cas = QueryFactory.buildQuery(session, ConservationArea.class).list();
					for (ConservationArea c : cas) {
						if (uuids6.contains(c.getUuid())) {
							toProcess.add(c);
						}
					}
				}
				
				
				this.smart6db = smart6db;
			}catch (Exception ex) {
				smart6db.close();
				throw ex;
			}
			
		}catch (Exception ex) {
			throw new InvocationTargetException(ex);
		}
	}

}
