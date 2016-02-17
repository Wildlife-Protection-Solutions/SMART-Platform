/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.downloader.sync;

import java.io.File;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.ZipUtil;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.replication.changelog.ChangeLogItemSerializer;
import org.wcs.smart.connect.uploader.PostgresqlMetadataCreator;
import org.wcs.smart.connect.uploader.sync.ChangeLogManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Packages all changes and package metadata files.
 * 
 * @author Emily
 *
 */
public class ChangeLogPackager {

	private final Logger logger = Logger.getLogger(ChangeLogPackager.class.getName());
	
	private long startRevision;
	private long endRevision;
	
	private UUID caUuid;
	
	private Path changelogFile;
	private Path metadataFile;
	private Path zipFile;
	private Path filestorePath;
	private File tempDir;
	private Session session;
	
	public ChangeLogPackager(Session session, UUID caUuid, long startRevision){
		this.startRevision = startRevision;
		this.session = session;
		this.caUuid = caUuid;
		
		tempDir = ZipUtil.createTemporaryDirectory();
		filestorePath = tempDir.toPath().resolve(ConnectSyncHistoryRecord.PACKAGE_FILESTORE_DIR);
		metadataFile = tempDir.toPath().resolve(UuidUtils.uuidToString(caUuid) + "." + System.nanoTime() + ".changelog.metadata"); //$NON-NLS-1$ //$NON-NLS-2$
		changelogFile = tempDir.toPath().resolve(UuidUtils.uuidToString(caUuid) + "." + System.nanoTime() + ".changelog"); //$NON-NLS-1$ //$NON-NLS-2$
		zipFile = tempDir.toPath().getParent().resolve(UuidUtils.uuidToString(caUuid) + "." + System.nanoTime() + ".changelog.zip"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	
	public void cleanUp(){
		try{
			FileUtils.forceDelete(tempDir);
		}catch (Exception ex){
			logger.log(Level.WARNING, "could not delete directory " + tempDir.toString(), ex); //$NON-NLS-1$
		}
	}
	
	/**
	 * 
	 * @return the relative file of the change log packag
	 * @throws Exception
	 */
	public Path createPackage() throws Exception{
		try{
			packageChangLog();
			packageMetadata();
			
			zipPackage();
		}finally{
			cleanUp();
		}
		
		return DataStoreManager.INSTANCE.getRootDirectory().toPath().relativize(zipFile);
	}
	
	private void zipPackage() throws Exception{
		ZipUtil.createZip(new File[]{changelogFile.toFile(), 
				metadataFile.toFile(), 
				filestorePath.toFile()}, zipFile.toFile());
	}
	
	private void packageMetadata() throws Exception{
		PostgresqlMetadataCreator.generateMetadata(session, caUuid, metadataFile, endRevision);
	}
	
	private void packageChangLog() throws Exception{
		final List<ChangeLogItem> items = getChangeLogItems();
		if (items.size() == 0){
			endRevision = startRevision;
		}else{
			endRevision = items.get(items.size() - 1).getRevision();
		}
		session.doWork(new Work(){

			@Override
			public void execute(Connection connection) throws SQLException {
				try(OutputStream fout = Files.newOutputStream(changelogFile);
						ObjectOutputStream oout = new ObjectOutputStream(fout)){
					oout.writeInt(items.size());
					ChangeLogItemSerializer serializer = new ChangeLogItemSerializer() {
						
						@Override
						public void prepareUuid(PreparedStatement ps, int index, UUID value)
								throws SQLException {
							ps.setObject(index, value);							
						}

						@Override
						protected int convertType(int type, int precision) {
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
		
	}
	
	private List<ChangeLogItem> getChangeLogItems() throws SmartConnectException{
		return ChangeLogManager.INSTANCE.getItems(session, caUuid, startRevision);
	}
}
