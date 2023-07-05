package org.wcs.smart.connect.internal.server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.server.replication.DerbyChangeLogDeserializer;
import org.wcs.smart.connect.internal.server.replication.SyncHistoryManager;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.replication.changelog.ChangeLogItemSerializer;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

public class ConflictDataRecoveryEngine {

	private ConservationArea ca;

	private Path workingDir;
	private Path changeLogFile;

	
	
	public ConflictDataRecoveryEngine(ConservationArea ca) {
		this.ca = ca;
		this.workingDir = SmartUtils.createTemporaryDirectory();
		this.changeLogFile = workingDir.resolve("changelog" + ConnectSyncHistoryRecord.CHANGELOG_FILE_SUFFIX); //$NON-NLS-1$
	}

	public void showRecoveryError(Exception ex) {
		ConnectPlugIn.displayLog("Unable to re-apply 'new' records. The Conservaiton Area has been updated with the data from Connect, but the new records could were not added to the local Conservation Area. You will have to re-create these records manually." + "\n\n" + ex.getMessage(), ex);
	}

	public void throwPackageException(Exception ex) throws Exception{
		throw new Exception(MessageFormat.format("Cannot download Conservation Area - could not set aside new records: {0}", ex.getMessage()), ex);
	}
	
	public void cleanUp() {
		try {
			SmartUtils.deleteDirectory(workingDir);
		} catch (IOException e) {
			ConnectPlugIn.log(e.getMessage(), e);
		}
	}
	
	
	public void applyRecoveryPackage(IProgressMonitor monitor) throws Exception {
		
		DerbyChangeLogDeserializer processor = new DerbyChangeLogDeserializer(changeLogFile, workingDir, ca, true, monitor);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				if (true) throw new Exception("This is a test");
				processor.processFile(session);	
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}	
		}
		
	}
	
	public void buildRecoveryPackage() {

		ConnectSyncHistoryRecord lastUpload = SyncHistoryManager.INSTANCE.getLastNonErrorSyncRecord(ca,
				ConnectSyncHistoryRecord.Type.UPLOAD);
		
		long revision = -1;
		if (lastUpload != null && lastUpload.getEndRevision() != null) {
			revision = lastUpload.getEndRevision();
		}

		try (Session session = HibernateManager.openSession()) {

			String sql = "FROM ChangeLogItem WHERE conservationArea = :ca and revision > :max order by revision asc"; //$NON-NLS-1$
			List<ChangeLogItem> newItems = session.createQuery(sql, ChangeLogItem.class)
					.setParameter("ca", ca.getUuid()) //$NON-NLS-1$
					.setParameter("max", revision) //$NON-NLS-1$
					.list();

			List<ChangeLogItem> itemsToKeep = new ArrayList<>();

			for (ChangeLogItem item : newItems) {
				switch (item.getAction()) {
				case FS_INSERT:
				case INSERT:
				case DELETE:
				case FS_DELETE:
					itemsToKeep.add(item);
					break;
				case FS_UPDATE:
					//skip filestore updates
					//if a new file was added then updated
					//the latest file will be copied
					break;
				case UPDATE:
					if (exists(item, itemsToKeep))
						itemsToKeep.add(item);
					break;
				}
			}

			// package up items to keep and put them location for use after

			session.doWork(new Work() {

				@Override
				public void execute(Connection connection) throws SQLException {
					try (OutputStream fout = Files.newOutputStream(changeLogFile);
							ObjectOutputStream oout = new ObjectOutputStream(fout)) {

						oout.writeInt(itemsToKeep.size());

						ChangeLogItemSerializer serializer = new ChangeLogItemSerializer() {
							@Override
							public void prepareUuid(PreparedStatement ps, int index, UUID value) throws SQLException {
								ps.setBytes(index, UuidUtils.uuidToByte(value));
							}

							@Override
							protected int convertType(int type, int precision) {
								if (type == Types.BINARY && precision == 16) {
									// uuid
									return Types.OTHER;
								}
								return type;
							}
						};
						for (ChangeLogItem i : itemsToKeep) {
							serializer.serialize(oout, i, workingDir, connection);
						}
					} catch (Exception ex) {
						throw new SQLException(ex);
					}
				}
			});
		}
	}

	private boolean exists(ChangeLogItem needle, List<ChangeLogItem> items) {
		for (ChangeLogItem i : items) {
			if (equals(i.getFieldName1(), needle.getFieldName1()) && equals(i.getKey1(), needle.getKey1())
					&& equals(i.getFieldName2(), needle.getFieldName2()) && equals(i.getKey2(), needle.getKey2())
					&& equals(i.getKey2String(), needle.getKey2String())
					&& equals(i.getTableName(), needle.getTableName())) {
				return true;
			}
		}
		return false;
	}

	private boolean equals(Object x, Object y) {
		if (x == null && y == null)
			return true;
		if (x == null && y != null)
			return false;
		if (x != null && y == null)
			return false;
		return x.equals(y);
	}
}
