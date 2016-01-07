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
package org.wcs.smart.connect.replication;

import java.nio.file.FileSystems;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.server.replication.ChangeLogTableManager;
import org.wcs.smart.connect.server.replication.SyncHistoryManager;
import org.wcs.smart.connect.util.DerbyUtil;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

/**
 * Manager for starting and stopping the derby change logging.
 * 
 * The database uses the functions DerbyUtil.isReplicationEnabled(cauuid) to determine
 * if a change to a table should be logged. There are two separate controls for change logging:
 * One is the conservation area.  If the conservation area has been uploaded or is uploading
 * to a connect server than changes to that conservation area can be logged.  Otherwise
 * changes will never be logged. 
 * The second is the LOGGING_DB_PROPERTY.  This allows the application to turn off all
 * logging regardless of what is set for the conservation area.  This is useful when 
 * applying change logs or deleting a conservation area.  By default this should be turned on 
 * and only turned off when specifically wanting to disable change logging.
 * 
 *  
 * 
 * @author Emily
 *
 */
public enum DerbyReplicationManager {
 
	INSTANCE;
	
	DerbyReplicationManager(){		
	}

	/**
	 * Maximum delay in days between replications.  If longer than this period
	 * has occurred you will no longer be able to replicate changes.  This is
	 * also used for cleaning up the change log table and sync history table.
	 */
	public static final long REPLICATION_MAXTIME_DAYS = 180;
	
	/**
	 * The database property to identify if logging should occur.  Used
	 * by the triggers to determine if changes should be logged.
	 */
	public static final String LOGGING_DB_PROPERTY = "org.wcs.smart.isLogging"; //$NON-NLS-1$
	
	
	private Thread fileStoreReplication;
	private FileStoreWatcher watcher;
	private boolean replicationState = false;
	
	/**
	 * This MUST BE wrapped in a transaction that is committed
	 * @param session
	 * @throws Exception
	 */
	public void enableReplication(Session session) throws Exception{
		setSystemReplicationEnabled(session, true);

		//file watch for changes to file store
		watcher = new FileStoreWatcher();
		//ignore certificate files
		watcher.addIgnorePath(ConnectServer.getDefaultCertificateFileName(SmartDB.getCurrentConservationArea()));
		watcher.register( FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation()) );
		//run filestore watcher in new thread (background)		
		fileStoreReplication = new Thread(watcher);
		fileStoreReplication.start();
	}
	
	/**
	 * This MUST BE wrapped in a transaction that is committed
	 * @param session
	 * @throws Exception
	 */
	public void disableReplication(Session session) throws Exception{
		setSystemReplicationEnabled(session, false);
		
		if (watcher != null){
			watcher.deregister();
			watcher = null;
		}
		
		if (fileStoreReplication != null){
			fileStoreReplication.interrupt();
			fileStoreReplication = null;
		}
	}

	/**
	 * 
	 * @return true if the last call was to enableReplication, otherwise false.
	 */
	public boolean getSystemReplicationState(){
		return this.replicationState;
	}
	/**
	 * Sets the state of the database property LOGGING_DB_PROPERTY.  
	 * This MUST BE wrapped in a transaction and be committed.
	 * 
	 * @param session
	 * @param isEnabled
	 */
	private void setSystemReplicationEnabled(Session session, final boolean isEnabled){
		this.replicationState = isEnabled;
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				connection.createStatement().execute("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('" + LOGGING_DB_PROPERTY + "', " + (isEnabled ? "true" : "null") + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}});
	}
	
	/**
	 * Determines if replication should be enabled for a given
	 * Conservation Area.   A ConservationArea can be replicated if a connect
	 * server is configured, and the associated status is one of UPLOAD or DONE.  This
	 * does not check if logging is enabled. If no connect server
	 * status is found, or the status is backup or error false is returned.
	 * 
	 * @return
	 */
	public boolean canReplicate(Session session, ConservationArea ca){
		
		ConnectServerStatus status = (ConnectServerStatus)session
				.get(ConnectServerStatus.class, ca.getUuid());
		if (status == null){
			return false;
		}
		if (status.getStatus() == ConnectServerStatus.Status.UPLOAD ||
			status.getStatus() == ConnectServerStatus.Status.DONE ){
			return true;
		}
		//ERROR or BACKUP
		return false;
	}
	
	/**
	 * 
	 * @param session
	 * @return the state of the LOGGING_DB_PROPERTY database value
	 */
	public boolean isReplicationSystemEnabled(Session session){
		return session.doReturningWork(new ReturningWork<Boolean>() {
			@Override
			public Boolean execute(Connection connection) throws SQLException {
				String sql = "values syscs_util.syscs_get_database_property( '" + LOGGING_DB_PROPERTY + "' )"; //$NON-NLS-1$ //$NON-NLS-2$
				
				ResultSet rs = connection.createStatement().executeQuery(sql);
				if (rs.next()){
					try{
						Boolean x = rs.getBoolean(1);
						return x;
					}catch(Exception ex){
						ConnectPlugIn.log(ex.getMessage(), ex);
						return false;
					}
				}
				return false;
			}
		});	
	}

	/**
	 * Determines if replication is enabled for a given
	 * Conservation Area.  This checks the status of the 
	 * LOGGING_DB_PROPERTY and the Conservation Area connect
	 * status.
	 * This calls the same function used by the database triggers
	 * to determine if a change should be logged.
	 * @param cauuid 
	 * @param session
	 * @return
	 * @throws SQLException
	 */
	public Boolean isReplicationEnabled(final UUID cauuid, Session session) {
		return session.doReturningWork(new ReturningWork<Boolean>() {
			@Override
			public Boolean execute(Connection connection) throws SQLException {
				return DerbyUtil.isReplicationEnabled(UuidUtils.uuidToByte(cauuid), connection);
			}
		});
	}
	
	public Boolean hasLocalChanges(Session session){
		if (!canReplicate(session, SmartDB.getCurrentConservationArea())) return null;

		ConnectSyncHistoryRecord  rec = SyncHistoryManager.INSTANCE.getLastNonErrorSyncRecord(session, 
				SmartDB.getCurrentConservationArea(), ConnectSyncHistoryRecord.Type.UPLOAD);
		Long currentRevision = ChangeLogTableManager.INSTANCE.getMaxLocalRevision(session, 
				SmartDB.getCurrentConservationArea());
		
		if (rec == null && currentRevision == null){
			return Boolean.FALSE;
		}else if (rec == null && currentRevision != null){
			return Boolean.TRUE;
		}else if (rec != null && currentRevision == null){
			if (rec.getEndRevision().longValue() != -1){				
				return null;	//unknown error
			}else{
				return Boolean.FALSE;
			}
		}else if (rec != null && currentRevision != null){
			if (currentRevision.longValue() > rec.getEndRevision().longValue()){
				return Boolean.TRUE;
			}else if (currentRevision.longValue() == rec.getEndRevision().longValue()){
				return Boolean.FALSE;
			}else{
				return null;	//unknown error
			}
		}
		return null;
	}
}
