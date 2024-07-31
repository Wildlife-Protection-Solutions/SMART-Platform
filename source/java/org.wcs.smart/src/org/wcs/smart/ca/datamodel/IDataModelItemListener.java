package org.wcs.smart.ca.datamodel;

import java.sql.SQLException;

import org.hibernate.Session;

public interface IDataModelItemListener {

	/**
	 * This event is called before a category, attribute, categoryAttribute,
	 * attributeListItem, or attributeTreeNode is removed from the data model.
	 * <p>
	 * An exception should be thrown if an error occurs during processing.
	 * </p>
	 * 
	 * @param currentSession
	 * @param itemToDelete Category, Attribute, CategoryAttribute
	 * @throws Exception
	 */
	public void deleteItem(Session currentSession, Object itemToDelete) throws Exception;
	
	
	/**
	 * This event is called after an item is added to
	 * the data model. The session is in open
	 * transaction. This is called after the item
	 * is added to the database and all references are updated.
	 * <p>Exceptions should be dealt with internally.</p>
	 * 
	 * In the case of attributes getting added to a category, this
	 * event only gets called once for the root CategoryAttribute object
	 * created.
	 * 
	 * @param currentSession open session
	 * @param itemToAdd item added
	 */
	public void addItem(Session currentSession, Object itemToAdd);
	
	/**
	 * This event is called if category, categoryAttribute enabled state 
	 * has changed. Called after state is changed.
	 * 
	 * Not called for attribute list items or attribute tree nodes
	 * @param currentSession
	 * @param itemToAdd
	 */
	public void itemEnabledStateChanged(Session currentSession, Object itemToAdd);
	
	public default void singleToMulti(Session currentSession, Attribute attribute) throws SQLException{}
}
