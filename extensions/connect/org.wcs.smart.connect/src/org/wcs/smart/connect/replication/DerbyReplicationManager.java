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

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Manager for starting and stopping the derby change logging.
 * 
 * @author Emily
 *
 */
public enum DerbyReplicationManager {
 
	INSTANCE;
	
	DerbyReplicationManager(){		
	}

	public static final String LOGGING_DB_PROPERTY = "org.wcs.smart.isLogging";

	private Thread fileStoreReplication;
	private FileStoreWatcher watcher;
	
	private Boolean enabledState = null;
	
	/**
	 * This MUST BE wrapped in a transaction that is committed
	 * @param session
	 * @throws Exception
	 */
	public void enableReplication(Session session) throws Exception{
		this.enabledState = true;
		setReplicationEnabled(session, true);

		//file watch for changes to file store
		watcher = new FileStoreWatcher();
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
		this.enabledState = false;
		setReplicationEnabled(session, false);
		
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
	 * This MUST BE wrapped in a transaction that is committed
	 * @param session
	 * @param isEnabled
	 */
	private void setReplicationEnabled(Session session, final boolean isEnabled){
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				connection.createStatement().execute("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('" + LOGGING_DB_PROPERTY + "', " + (isEnabled ? "true" : "null") + ")");
			}});
		if (isEnabled){
			//configure revision 
			String sql = "SELECT max(revision) FROM ChangeLogItem WHERE conservationArea = :ca";
			Long seqstart = (Long)session.createQuery(sql)
					.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid())
					.uniqueResult();
			if (seqstart == null){
				seqstart = 0l;
			}
			seqstart++;
			
			sql = "ALTER TABLE smart.connect_change_log ALTER COLUMN revision RESTART WITH " + seqstart;
			session.createSQLQuery(sql).executeUpdate();
		}
	}
	
	public boolean getLocalReplicationState(){
		if (enabledState == null){
			return false;
		}
		return enabledState;
	}
	/**
	 * Queries the database for the logging_db_property
	 * 
	 * @param session
	 * @return
	 */
	public boolean isReplicationEnabled(Session session){
		return session.doReturningWork(new ReturningWork<Boolean>() {
			@Override
			public Boolean execute(Connection connection) throws SQLException {
				String sql = "values syscs_util.syscs_get_database_property( '" + LOGGING_DB_PROPERTY + "' )";
				
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
}
