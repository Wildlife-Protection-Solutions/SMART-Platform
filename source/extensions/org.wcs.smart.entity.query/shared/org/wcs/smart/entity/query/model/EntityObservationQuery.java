package org.wcs.smart.entity.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.entity.query.IEntityQueryColumnProvider;
import org.wcs.smart.entity.query.parser.internal.parser.Parser;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.QueryFilter;

@Entity
@Table(name="smart.entity_observation_query")
public class EntityObservationQuery extends ObservationQuery {
	
	public static final String KEY = "entityobservation"; //$NON-NLS-1$

	@Override
	protected void initQueryColumns() {
		if (this.queryColumns != null){
			return;
		}
		
		synchronized (this) {
			if (this.queryColumns != null){
				return;
			}
			QueryColumn[] cols = SmartContext.INSTANCE.getClass(IEntityQueryColumnProvider.class).getQueryColumns(this);
			queryColumns = new ArrayList<QueryColumn>();
			HashSet<String> visible = null;
			if (visibleColumns != null){
				String[] bits = visibleColumns.split(","); //$NON-NLS-1$
				visible = new HashSet<String>();
				for (int i = 0; i < bits.length; i ++){
					visible.add(bits[i]);
				}
			}
			
			for (int i = 0; i < cols.length; i ++){
				queryColumns.add(cols[i]);	
			}
			
			for (QueryColumn c : queryColumns){
				if (visible == null){
					c.setVisible(true);
				}else if (visible.contains(c.getKey())){
					c.setVisible(true);
				}else{
					c.setVisible(false);
				}
			}
		}
	}


	@Override
	protected QueryFilter parseQueryFilter() throws Exception {
		if (strQueryFilter == null || strQueryFilter.length() == 0){
			return new QueryFilter(EmptyFilter.INSTANCE);
		}
		if(queryFilter != null){
			return queryFilter;
		}
		try(InputStream is = new ByteArrayInputStream(strQueryFilter.getBytes())){
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
		return q;
	}

}
