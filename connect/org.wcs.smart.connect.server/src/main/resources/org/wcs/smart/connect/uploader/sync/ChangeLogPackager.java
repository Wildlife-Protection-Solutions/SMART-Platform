package org.wcs.smart.connect.uploader.sync;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.ZipUtil;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem;
import org.wcs.smart.connect.replication.changelog.ChangeLogItemDataSerializer;
import org.wcs.smart.connect.uploader.PackageMetadata;
import org.wcs.smart.util.UuidUtils;

public class ChangeLogPackager {

	private long startRevision;
	private long endRevision;
	
	private UUID caUuid;
	
	private Path changelogFile;
	private Path metadataFile;
	private Path zipFile;
	
	private Session session;
	
	public ChangeLogPackager(Session session, UUID caUuid, long startRevision){
		this.startRevision = startRevision;
		this.session = session;
		this.caUuid = caUuid;
		
		File tempDir = ZipUtil.createTemporaryDirectory();
		
		changelogFile = tempDir.toPath().resolve(UuidUtils.uuidToString(caUuid) + "." + System.nanoTime() + ".changelog.metadata");
		metadataFile = tempDir.toPath().resolve(UuidUtils.uuidToString(caUuid) + "." + System.nanoTime() + ".changelog");
		zipFile = tempDir.toPath().resolve(UuidUtils.uuidToString(caUuid) + "." + System.nanoTime() + ".changelog.zip");
	}
	
	
	public void cleanUp() throws IOException{
		Files.deleteIfExists(changelogFile);
		Files.deleteIfExists(metadataFile);
	}
	
	public Path createPackage() throws Exception{
		try{
			packageMetadata();
			packageChangLog();
			zipPackage();
		}finally{
			cleanUp();
		}
		return zipFile;
	}
	
	private void zipPackage() throws Exception{
		ZipUtil.createZip(new File[]{changelogFile.toFile(), metadataFile.toFile()}, zipFile.toFile());
	}
	
	private void packageMetadata() throws Exception{
		PackageMetadata.generateMetadata(session, caUuid, metadataFile, endRevision);
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
		return ChangeLogManager.INSTANCE.getItems(session, caUuid, startRevision);
	}
}
