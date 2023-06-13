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
package org.wcs.smart.observation;

import java.sql.SQLException;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.IDataModelItemListener;

/**
 * DataModel listener for removing data model items from configured models.
 * 
 * <p>Listeners for category deletes, category attribute deletes,
 * attribute tree node deletes, attribute list item deletes.  It automatically
 * removes categories, category/attributes and all tree node and list item
 * configurations.  In addition removes list items and tree nodes from
 * the default value options for attribute nodes.</p>
 * 
 * @author Emily
 *
 */
public enum DataModelItemListener implements IDataModelItemListener {
	
	INSTANCE;

	@Override
	public void deleteItem(Session currentSession, Object itemToDelete) throws Exception { }

	@Override
	public void addItem(Session currentSession, Object itemToAdd) {	}

	@Override
	public void itemEnabledStateChanged(Session currentSession, Object itemToAdd) { }
	
	@Override
	public void singleToMulti(Session currentSession, Attribute attribute) throws SQLException{
		//update observations
		
		StringBuilder sql = new StringBuilder();
		sql.append(" INSERT INTO smart.wp_observation_attributes_list "); //$NON-NLS-1$
		sql.append(" (observation_attribute_uuid, list_element_uuid)"); //$NON-NLS-1$
		sql.append(" SELECT uuid, list_element_uuid "); //$NON-NLS-1$
		sql.append(" FROM smart.wp_observation_attributes "); //$NON-NLS-1$
		sql.append( " WHERE attribute_uuid = :att "); //$NON-NLS-1$
		sql.append(" AND uuid is not null and list_element_uuid is not null "); //$NON-NLS-1$
		
		currentSession.createNativeMutationQuery(sql.toString())
			.setParameter("att", attribute.getUuid()) //$NON-NLS-1$
			.executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" UPDATE WaypointObservationAttribute "); //$NON-NLS-1$
		sql.append(" SET attributeListItem = null "); //$NON-NLS-1$
		sql.append( " WHERE attribute = :att "); //$NON-NLS-1$
		
		currentSession.createMutationQuery(sql.toString())
			.setParameter("att", attribute) //$NON-NLS-1$
			.executeUpdate();
	}
	
}
