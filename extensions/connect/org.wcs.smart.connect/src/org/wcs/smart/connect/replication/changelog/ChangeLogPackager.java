package org.wcs.smart.connect.replication.changelog;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.replication.MetadataPackager;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem.Action;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

public class ChangeLogPackager {

	private ConnectSyncHistoryRecord record;

	private long endRevision;
	
	private Path changelogFile;
	private Path metadataFile;
	
	private Path zipFile;
	
	public ChangeLogPackager(ConnectSyncHistoryRecord record){
		this.record = record;
		
		changelogFile = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogFile());
		metadataFile = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogMetadataFile() );
		zipFile = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogZipFile());
	}
	
	public long getLastRevision(){
		return endRevision;
	}
	
	public void cleanUp() throws IOException{
		Files.deleteIfExists(changelogFile);
		Files.deleteIfExists(metadataFile);
	}
	
	public void createPackage(IProgressMonitor monitor) throws Exception{
		monitor.beginTask("Creating change log package", 3);
		try{
			packageMetadata();
			monitor.worked(1);
			packageChangLog();
			monitor.worked(1);
			zipPackage(new SubProgressMonitor(monitor, 1));
		}finally{
			monitor.done();
			cleanUp();
		}
	}
	
	private void zipPackage(IProgressMonitor monitor) throws Exception{
		ZipUtil.createZip(new File[]{changelogFile.toFile(), metadataFile.toFile()}, zipFile.toFile(), monitor);
	}
	
	private void packageMetadata() throws Exception{
		Session s = HibernateManager.openSession();
		try{
			MetadataPackager.generateMetadata(s, record.getServer(), metadataFile, record.getStartRevision());
		}finally{
			s.close();
		}
	}
	private void packageChangLog() throws Exception{
		final List<ChangeLogItem> items = getChangeLogItems();
		if (items.size() == 0){
			endRevision = record.getStartRevision();
		}else{
			endRevision = items.get(items.size() - 1).getRevision();
		}
		
		Session s = HibernateManager.openSession();
		
		s.doWork(new Work(){

			@Override
			public void execute(Connection connection) throws SQLException {
				try(OutputStream fout = Files.newOutputStream(changelogFile);
						ObjectOutputStream oout = new ObjectOutputStream(fout)){
					oout.writeInt(items.size());
					for (ChangeLogItem i : items){
						oout.writeObject(i);
						ChangeLogItemDataSerializer.serializeData(oout, i, connection);
					}
				}catch (Exception ex){		
					throw new SQLException (ex);
				}
			}});
		
	}
	
	private List<ChangeLogItem> getChangeLogItems(){
		Session s = HibernateManager.openSession();
		try{
			return ChangeLogTableManager.INSTANCE.getAll(s, record.getConservationArea(), record.getStartRevision());
		}finally{
			s.close();
		}
	}
}
