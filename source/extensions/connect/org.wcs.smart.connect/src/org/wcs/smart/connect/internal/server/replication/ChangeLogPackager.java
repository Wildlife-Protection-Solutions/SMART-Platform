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
package org.wcs.smart.connect.internal.server.replication;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.replication.DerbyMetadataPackager;
import org.wcs.smart.connect.replication.changelog.ChangeLogItemSerializer;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Packager for packing up change log file and related
 * metadata.
 * 
 * @author Emily
 *
 */
public class ChangeLogPackager {

	private ConnectSyncHistoryRecord record;

	private long endRevision;
	
	private Path changelogFile;
	private Path metadataFile;
	private Path filestorePath;
	private Path zipFile;
	
	public ChangeLogPackager(ConnectSyncHistoryRecord record){
		this.record = record;
		filestorePath = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), record.getFilestoreDirectory());
		changelogFile = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogFile());
		metadataFile = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogMetadataFile() );
		zipFile = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogZipFile());
	}
	
	/**
	 * 
	 * @return the last revision number in the change log file
	 */
	public long getLastRevision(){
		return endRevision;
	}
	
	/**
	 * Removes temporary files created during packaging process.
	 *  
	 * @throws IOException
	 */
	public void cleanUp() throws IOException{
		Files.deleteIfExists(changelogFile);
		Files.deleteIfExists(metadataFile);
		
		if (Files.exists(filestorePath.getParent())){
			FileUtils.forceDelete(filestorePath.getParent().toFile());
		}
	}
	
	/**
	 * Packages metadata and change log.
	 * 
	 * @param monitor
	 * @throws Exception
	 */
	public void createPackage(IProgressMonitor monitor) throws Exception{
		monitor.beginTask(Messages.ChangeLogPackager_TaskName, 3);
		try{
			packageMetadata();
			monitor.worked(1);
			packageChangLog();
			monitor.worked(1);
			zipPackage(new SubProgressMonitor(monitor, 1));
		}finally{
			monitor.done();
			try{
				cleanUp();
			}catch (IOException ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
		}
	}
	
	/*
	 * Zips changelog file and metadata file
	 */
	private void zipPackage(IProgressMonitor monitor) throws Exception{
		File[] filesToZip;
		if (Files.exists(filestorePath)){
			filesToZip = new File[]{changelogFile.toFile(), 
					metadataFile.toFile(),
					filestorePath.toFile()};
		}else{
			filesToZip = new File[]{changelogFile.toFile(), 
					metadataFile.toFile()};
		}
		ZipUtil.createZip(filesToZip, zipFile.toFile(), monitor);
	}
	
	/*
	 * Creates metadata package
	 */
	private void packageMetadata() throws Exception{
		Session s = HibernateManager.openSession();
		try{
			DerbyMetadataPackager.INSTANCE.generateMetadata(s, record.getServer(), metadataFile, record.getStartRevision());
		}finally{
			s.close();
		}
	}
	
	/*
	 * Creates change log package
	 */
	private void packageChangLog() throws Exception{
		final List<ChangeLogItem> items = getChangeLogItems();
		if (items.size() == 0){
			endRevision = record.getStartRevision();
		}else{
			endRevision = items.get(items.size() - 1).getRevision();
		}
		
		//must be admin user otherwise you might not have permission to access tables
		SmartDB.DbUser currentUser = SmartDB.DbUser.ADMIN;
		if (SmartDB.getCurrentEmployee() != null){
			currentUser = SmartDB.getCurrentUser();
		}
		Session s = HibernateManager.lockDatabase(SmartDB.DbUser.ADMIN.getUserName(), SmartDB.DbUser.ADMIN.getPassword());
		try{
			s.beginTransaction();
			s.doWork(new Work(){
	
				@Override
				public void execute(Connection connection) throws SQLException {
	
					try(OutputStream fout = Files.newOutputStream(changelogFile);
							ObjectOutputStream oout = new ObjectOutputStream(fout)){
						oout.writeInt(items.size());
						
						ChangeLogItemSerializer serializer = new ChangeLogItemSerializer() {
							@Override
							public void prepareUuid(PreparedStatement ps, int index, UUID value) throws SQLException{
								ps.setBytes(index, UuidUtils.uuidToByte(value));
							}
	
							@Override
							protected int convertType(int type, int precision) {
								if (type == Types.BINARY && precision == 16){
									//uuid
									return Types.OTHER;
								}
								return type;
							}
						};
						for (ChangeLogItem i : items){
							serializer.serialize(oout, i, filestorePath, connection);
						}
					}catch (Exception ex){		
						throw new SQLException (ex);
					}
				}});
		}finally{
			try{
				s.getTransaction().rollback();
				s.close();
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
			
			HibernateManager.unlockDatabase(currentUser.getUserName(), currentUser.getPassword());
		}
		
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
