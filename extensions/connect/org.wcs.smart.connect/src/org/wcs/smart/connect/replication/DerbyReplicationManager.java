package org.wcs.smart.connect.replication;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartContext;

public enum DerbyReplicationManager {
 
	INSTANCE;
	
	DerbyReplicationManager(){
		
	}

	public static final String LOGGING_DB_PROPERTY = "org.wcs.smart.isLogging";

	private Thread fileStoreReplication;
	private FileStoreWatcher watcher;
	
	public void enableReplication(Session session) throws Exception{
		setReplicationEnabled(session, true);
		
//		watcher = new FileStoreWatcher();
//		watcher.register( new File(SmartContext.INSTANCE.getFilestoreLocation()).toPath() );
//
//		//run filestore watcher in new thread (background)		
//		fileStoreReplication = new Thread(watcher);
//		fileStoreReplication.start();
	}
	
	public void disableReplication(Session session) throws Exception{
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
	
	private void setReplicationEnabled(Session session, final boolean isEnabled){
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				connection.createStatement().execute("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('" + LOGGING_DB_PROPERTY + "', " + (isEnabled ? "true" : "null") + ")");
			}});
	}
	
	public boolean isReplicationEnabled(Session session){
		return session.doReturningWork(new ReturningWork<Boolean>() {

			@Override
			public Boolean execute(Connection connection) throws SQLException {
				String sql = "values syscs_util.syscs_get_database_property( '" + LOGGING_DB_PROPERTY + "' )";
				
				ResultSet rs = connection.createStatement().executeQuery(sql);
				if (rs.next()){
					Object x = rs.getObject(1);
					if (x == null) return false;
					if (x instanceof Boolean && ((Boolean)x)) return true;
				}
				return false;
			}
		});
		
	}
}
