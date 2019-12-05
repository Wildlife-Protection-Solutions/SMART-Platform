/*
 * Copyright (C) 2016 Wildlife Conservation Society
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.model.IntelRecordSummaryQuery;
import org.wcs.smart.i2.query.CaQueryItemProvider;
import org.wcs.smart.i2.query.DesktopCcaaQueryItemProvider;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.security.IntelSecurityManager;

/**
 * Query manager for intelligence queries.
 * 
 * 
 * @author Emily
 *
 */
public enum InternalQueryManager {

	INSTANCE;
	
	private volatile IQueryItemProvider queryItemProvider = null;
	
	/**
	 * 
	 * @return the array of classes that represent Intelligence queries
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends AbstractIntelQuery>[] getQueryTypeClasses() {
		return new Class[] {
			IntelRecordObservationQuery.class,
			IntelEntityRecordQuery.class,
			IntelEntitySummaryQuery.class,
			IntelRecordSummaryQuery.class,
			IntelRecordQuery.class};
	}
	/**
	 * Returns the query deleted from the database if a query is deleted; otherwise
	 * returns null
	 * @param queryUuid
	 * @return
	 */
	public AbstractIntelQuery deleteQuery(UUID queryUuid, String queryType){
		AbstractIntelQuery removed = null;
		
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				for (Class<?extends AbstractIntelQuery> c : getQueryTypeClasses()) {
					removed = s.get(c, queryUuid);
					if (removed != null) break;
				}
				
				if (removed == null) throw new Exception(Messages.QueryManager_NotFoundError);
				
				Query<?> wsQuery = s.createQuery("DELETE FROM IntelWorkingSetQuery WHERE id.query = :query"); //$NON-NLS-1$
				wsQuery.setParameter("query", removed.getUuid()); //$NON-NLS-1$
				wsQuery.executeUpdate();
				
				s.delete(removed);
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(Messages.QueryManager_DeleteError + ex.getMessage(), ex);
				return null;
			}
		}
		
		return removed;
	}
	
	/**
	 * 
	 * @return the supported query types as a two element array where the first element
	 * is the key and the second is the query type name.
	 */
	public String[][] getSupportQueryTypes() {
		return new String[][] {
			{ IntelRecordObservationQuery.KEY, Messages.InternalQueryManager_RecordObservationQueryName},
			{ IntelEntitySummaryQuery.KEY, Messages.InternalQueryManager_EntitySummaryQueryName},
			{ IntelEntityRecordQuery.KEY, Messages.InternalQueryManager_EntityRecordQuery},
			{ IntelRecordSummaryQuery.KEY, "Record Summary Query"},
			{ IntelRecordQuery.KEY, "Record Query"}
		};
	}
	
	public void resetQueryItemProvider() {
		synchronized (INSTANCE) {
			queryItemProvider = null;
		}
	}
	
	/**
	 * 
	 * @return the query item provider for the current conservation are a configuration
	 * 
	 */
	public IQueryItemProvider getQueryItemProvider() {
		if (queryItemProvider == null) {
			synchronized (INSTANCE) {
				if (queryItemProvider != null) return queryItemProvider;
				
				if (SmartDB.isMultipleAnalysis()) {
					//determine all profiles which the current user has access to query
					Set<IntelProfile> profiles = new HashSet<>();
					try(Session session = HibernateManager.openSession()){
						for (IntelProfile p : QueryFactory.buildQuery(session, IntelProfile.class).list()) {
							if (IntelSecurityManager.INSTANCE.canViewQuery(p)) {
								profiles.add(p);
							}
						}
						queryItemProvider = new DesktopCcaaQueryItemProvider(profiles, SmartDB.getCurrentConservationArea()) {
							@Override
							protected ConservationArea getMainConservationArea() {
								return SmartDB.getConservationAreaConfiguration().getMainConservationArea();
							}
						};
					}
					
				}else {
					queryItemProvider = new CaQueryItemProvider(SmartDB.getCurrentConservationArea(), SmartDB.getCurrentConservationArea());
				}	
			}
			
		}
		return queryItemProvider;
	}
	
	
}
