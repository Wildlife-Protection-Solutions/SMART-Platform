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
package org.wcs.smart.i2;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.IDataModelItemListener;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.observation.filter.DataModelFilter;
import org.wcs.smart.i2.query.observation.filter.IFilterVisitor;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.ParsedObservationQuery;
import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;


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
public enum ProfileDataModelItemListener implements IDataModelItemListener {
	
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
		sql.append(" INSERT INTO smart.i_observation_attribute_list "); //$NON-NLS-1$
		sql.append(" (observation_attribute_uuid, list_element_uuid)"); //$NON-NLS-1$
		sql.append(" SELECT uuid, list_element_uuid "); //$NON-NLS-1$
		sql.append(" FROM smart.i_observation_attribute "); //$NON-NLS-1$
		sql.append( " WHERE attribute_uuid = :att "); //$NON-NLS-1$
		sql.append(" AND uuid is not null and list_element_uuid is not null "); //$NON-NLS-1$
		
		currentSession.createNativeQuery(sql.toString())
			.setParameter("att", attribute.getUuid()) //$NON-NLS-1$
			.executeUpdate();
		
		sql = new StringBuilder();
		sql.append(" UPDATE IntelObservationAttribute "); //$NON-NLS-1$
		sql.append(" SET attributeListItem = null "); //$NON-NLS-1$
		sql.append( " WHERE attribute = :att "); //$NON-NLS-1$
		
		currentSession.createQuery(sql.toString())
			.setParameter("att", attribute) //$NON-NLS-1$
			.executeUpdate();
		
		//process queries
			
		List<IntelEntityRecordQuery> queries = QueryFactory.buildQuery(currentSession, IntelEntityRecordQuery.class, 
				new Object[] {"conservationArea", attribute.getConservationArea()}) //$NON-NLS-1$
				.list();
		
		for (IntelEntityRecordQuery q : queries) {
			try {
				ParsedObservationQuery filter = IntelEntityRecordQuery.parseQuery(q.getQueryString());
				if (filter.getFilter() == null) continue;
				processFilter(filter.getFilter(), attribute);
				q.setQueryString(filter.asString());
			}catch (Exception ex) {
				System.out.println(q.getName());
				ex.printStackTrace();
			}
		}
		
		List<IntelRecordObservationQuery> queries2 = QueryFactory.buildQuery(currentSession, IntelRecordObservationQuery.class, 
				new Object[] {"conservationArea", attribute.getConservationArea()}) //$NON-NLS-1$
				.list();
		
		for (IntelRecordObservationQuery q : queries2) {
			try {
				ParsedObservationQuery filter = IntelRecordObservationQuery.parseQuery(q.getQueryString());
				if (filter.getFilter() == null) continue;
				processFilter(filter.getFilter(), attribute);
				q.setQueryString(filter.asString());
			}catch (Exception ex) {
				System.out.println(q.getName());
				ex.printStackTrace();
			}
		}
		
		//the other query types don'	t use data model filters/groups by etc. so we don't need to upgrade
		//them at this time

	}
	
	private void processFilter(IQueryFilter filter, Attribute attribute) {
		IFilterVisitor visitor = new IFilterVisitor() {
			@Override
			public void visitElement(IQueryFilter filter) {
				if (filter instanceof DataModelFilter) {
					DataModelFilter afilter = (DataModelFilter) filter;
					
					String attributeKey = afilter.getAttributeKey();
					if (attribute.getKeyId().equalsIgnoreCase(attributeKey) && afilter.getKeyValue() != null) {
						//change to multi-select
						List<String> keyValues = new ArrayList<>();
						if (afilter.getKeyValue().equalsIgnoreCase(DataModelFilter.ANY_OPTION_KEY)) {
							for(AttributeListItem li : attribute.getAttributeList()) {
								keyValues.add(li.getKeyId());
							}
						}else {
							keyValues.add(afilter.getKeyValue());
						}
						
						afilter.convertToMultiList(keyValues);
					}
				}
			}
		};
		filter.accept(visitor);
	}
}
