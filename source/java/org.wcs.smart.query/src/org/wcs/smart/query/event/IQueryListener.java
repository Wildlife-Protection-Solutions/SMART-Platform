package org.wcs.smart.query.event;

import org.hibernate.Session;
import org.wcs.smart.query.model.Query;

public interface IQueryListener {
	
	/**
	 * A folder added event
	 */
	public static final int FOLDER_ADDED = 1;
	/**
	 * A folder renamed event
	 */
	public static final int FOLDER_RENAMED = 2;
	/**
	 * A folder deleted event
	 */
	public static final int FOLDER_DELETED = 3;
	
	/**
	 * A query added event
	 */
	public static final int QUERY_ADDED = 4;
	
	/**
	 * A query saved event
	 */
	public static final int QUERY_SAVED = 5;
	/**
	 * A query deleted event
	 */
	public static final int QUERY_DELETED = 6;
	
	/**
	 * A query deleted event
	 */
	public static final int QUERY_NAME_MODIFIED = 7;
	
	/**
	 * A query deleted event
	 */
	public static final int QUERY_DEFINITION_MODIFIED = 8;
	
	
	/**
	 * Fired when the given query should be run 
	 * 
	 * @param query the query to run
	 */
	public void queryRun(Query query);
	
	/**
	 * Fired when the query should be refreshed because of a non-user change.
	 * <p>For example when a database key has changed.</p>
	 * @param query
	 */
	public void queryRefreshed(Query query);
	
	
	/**
	 * Fired when the query name has been modified.
	 * 
	 * The object may be a Query or QueryEditorInput object
	 * 
	 * @param query with newName set
	 */
	public void queryModified(int eventType, Object object);
	

	/**
	 * Called when an event occured that effects
	 * a query folder.
	 *  
	 * @param eventType the type of event
	 * @param object the object either a QueryFolder or Query that
	 * was affect by the given event
	 */
	void folderModified(int eventType, Object object);
	

	/**
	 * Event fired before the query is saved
	 * to the database.
	 * @param query The query to save to the database
	 * @param session the current hiberante database session
	 * @return <code>true</code> if the save should
	 * proceed <code>false</code> if save should
	 * be cancelled.
	 */
	public boolean beforeSave(Query query, Session session);
	
	/**
	 * Event fired before the query is deleted
	 * from the database.
	 * 
	 * @param query The query to delete to the database; NOT attached
	 * to the hiberante session
	 * @param session the current hiberante database session in transaction
	 * 
	 * @return <code>true</code> if the delete should
	 * proceed <code>false</code> if delete should
	 * be cancelled.
	 */
	public boolean beforeDelete(Query query, Session session);
}
