package org.wcs.smart.ca.datamodel;

import org.hibernate.Session;

public interface IDataModelDeleteListener {

	/**
	 * This event is called after the item is validated for delete
	 * but before the item is removed from the database.  The session
	 * object has an open transaction.
	 * <p>
	 * An exception should be thrown if an error occurs during processing.
	 * </p>
	 * 
	 * @param currentSession
	 * @param itemToDelete Category, Attribute, CategoryAttribute, AttributeListItem or AttributeTreeNode
	 * @throws Exception
	 */
	public void deleteItem(Session currentSession, Object itemToDelete) throws Exception;
}
