package org.wcs.smart.entity.query.model;

import java.io.Reader;
import java.io.StringReader;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.entity.query.IEntityQueryColumnProvider;
import org.wcs.smart.entity.query.parser.internal.parser.Parser;
import org.wcs.smart.query.common.model.IQueryColumnProvider;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.QueryFilter;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name="entity_observation_query", schema="smart")
public class EntityObservationQuery extends ObservationQuery {
	
	private static final long serialVersionUID = 1L;
	
	public static final String KEY = "entityobservation"; //$NON-NLS-1$


	@Override
	protected QueryFilter parseQueryFilter() throws Exception {
		if (strQueryFilter == null || strQueryFilter.length() == 0){
			return new QueryFilter(EmptyFilter.INSTANCE);
		}
		if(queryFilter != null){
			return queryFilter;
		}
		try(Reader is = new StringReader(strQueryFilter)){
			Parser parser = new Parser(is);
			QueryFilter myQuery = parser.QueryFilter();
			queryFilter = myQuery;
			return myQuery;
		}
	}

	@Override
	@Transient
	public String getTypeKey() {
		return KEY;
	}

	@Override
	public Query clone(Employee newOwner) {
		EntityObservationQuery q = new EntityObservationQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(newOwner);
		q.setQueryFilter(getQueryFilter());
		q.setVisibleColumns(getVisibleColumns());
		q.setStyle(getStyle());
		q.setShowDataColumnsOnly(isShowDataColumnsOnly());
		return q;
	}

	@Override
	@Transient
	protected Class<? extends IQueryColumnProvider> getColumnProviderClass() {
		return IEntityQueryColumnProvider.class;
	}
}
