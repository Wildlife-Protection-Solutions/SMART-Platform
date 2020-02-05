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
package org.wcs.smart.connect.query.engine.i2;

import java.math.BigInteger;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.type.PostgresUUIDType;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.security.AdvIntelAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.i2.IIntelQueryEngine;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.query.CaQueryItemProvider;
import org.wcs.smart.i2.query.CcaaQueryItemProvider;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;

/**
 * Query engine for intelligence observation queries.
 * 
 * @author Emily
 *
 */
public class IntelRecordQueryEngine implements IIntelQueryEngine {
	
	/**
	 * Parameters required are session, monitor, and date filter object
	 * @param query
	 * @param parameters
	 * @return
	 */
	public IPagedQueryResultSet executeQuery(AbstractIntelQuery iquery,  HashMap<String, Object> parameters) throws Exception{
		
		IntelRecordQuery query = (IntelRecordQuery) iquery;
		
		Session session = (Session) parameters.get(Session.class.getName());
		String username = ((String)parameters.get(Principal.class.getName()));
		
		//one or both element of array may be null
		Date[] dfilter = (Date[]) parameters.get(Date.class.getName());
		if (dfilter == null) return null;
		
		Locale locale = (Locale) parameters.get(Locale.class.getName());
		if (locale == null){
			locale = Locale.getDefault();
		}
		
		@SuppressWarnings("unchecked")
		Collection<ConservationArea> cas = (Collection<ConservationArea>)parameters.get(ConservationArea.class.getName());
		if (cas == null){
			 throw new Exception("A valid Conservation Area must be provided in the query parameters.");
		}
		
		Set<IntelProfile> profiles = new HashSet<>();
		for (String ip : IntelEntityRecordQuery.convertFromProfileFilter(query.getProfileFilter())) {
			List<IntelProfile> items = session.createQuery("FROM IntelProfile WHERE keyId = :keyId and conservationArea in (:cas)", IntelProfile.class)
					.setParameter("keyId",  ip)
					.setParameter("cas", cas).list();
			
			if (SecurityManager.INSTANCE.canAccess(session, username, AdvIntelAction.RUNQUERY_KEY, iquery.getUuid()) ||
					SecurityManager.INSTANCE.canAccess(session, username, AdvIntelAction.RUNQUERY_KEY, iquery.getConservationArea().getUuid())) { 
				//we have permission to run this query so use all profiles
				profiles.addAll(items);
			}
		}
		if (profiles.isEmpty()) {
			throw new Exception("No valid profile filters for query");
		}
		
		IQueryItemProvider itemProvider = null;
		if (!query.getConservationArea().getIsCcaa()) {
			itemProvider = new CaQueryItemProvider(cas.iterator().next(), query.getConservationArea());
		}else {
			itemProvider = new CcaaQueryItemProvider(profiles, query.getConservationArea());
		}		
		IQueryFilter filter = IntelRecordQuery.parseQuery(query.getQueryString());
		RecordFilterProcessor p = new RecordFilterProcessor();
		Set<UUID> puuids = profiles.stream().map(e->e.getUuid()).collect(Collectors.toSet());
		String data = p.processFilter(filter, puuids, dfilter, cas, session);

		addProfile(session, data);
		addRecordSource(session, data);
		
		Integer cnt = ((BigInteger) session.createNativeQuery("SELECT count(*) FROM " + data).uniqueResult()).intValue(); //$NON-NLS-1$
		
		List<IQueryColumn> columns = IntelQueryColumnProvider.getInstance().getQueryColumns(query, itemProvider, locale, session);
		
		IntelRecordQueryResults results = new IntelRecordQueryResults(locale);
		results.setResultCount(cnt);
		results.setResultsTable(data);
		results.setQueryColumns(columns);
		
		HashMap<String, Integer> cols = p.colName2Index;
		cols.put("profile_name", cols.size());
		cols.put("profile_key", cols.size());
		cols.put("record_source_name", cols.size());
		
		session.createNativeQuery("ALTER TABLE " + data + " add column sort_column varchar(1028)").executeUpdate();
		
		results.setColumnNameToIndexMap(p.colName2Index);
		return results;
	}
	
	@SuppressWarnings("unchecked")
	private void addProfile(Session session, String datatable) {
		session.createNativeQuery("ALTER TABLE " + datatable + " ADD COLUMN profile_name varchar(1024)").executeUpdate();
		session.createNativeQuery("ALTER TABLE " + datatable + " ADD COLUMN profile_key varchar(128)").executeUpdate();
		
		List<UUID> uuids = session.createNativeQuery("SELECT distinct profile_uuid FROM " + datatable)
				.addScalar("profile_uuid",  PostgresUUIDType.INSTANCE)
				.list();
		for (UUID u : uuids) {
			IntelProfile p = session.get(IntelProfile.class, u);
			
			session.createNativeQuery("UPDATE " + datatable + " SET profile_name = :name, profile_key = :pkey WHERE profile_uuid = :uuid")
				.setParameter("name", p.getName())
				.setParameter("uuid", p.getUuid())
				.setParameter("pkey", p.getKeyId())
				.executeUpdate();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addRecordSource(Session session, String datatable) {
		session.createNativeQuery("ALTER TABLE " + datatable + " ADD COLUMN record_source_name varchar(1024)")
			.executeUpdate();
		
		List<UUID> uuids = session.createNativeQuery("SELECT distinct source_uuid FROM " + datatable + " WHERE source_uuid is not null ")
				.addScalar("source_uuid", PostgresUUIDType.INSTANCE).list();
		for (UUID u : uuids) {
			IntelRecordSource p = session.get(IntelRecordSource.class, u);
			
			session.createNativeQuery("UPDATE " + datatable + " SET record_source_name = :name WHERE source_uuid = :uuid")
				.setParameter("name", p.getName())
				.setParameter("uuid", p.getUuid())
				.executeUpdate();
		}
	}
}

