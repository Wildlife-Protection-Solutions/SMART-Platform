package org.wcs.smart.i2.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.query.observation.filter.EntityFilter;
import org.wcs.smart.i2.query.observation.filter.EntityTypeFilter;
import org.wcs.smart.i2.query.observation.filter.IFilterVisitor;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.IntelAttributeFilter;

public class IntelQueryColumnProvider {

	private static IntelQueryColumnProvider instance;
	
	public synchronized static IntelQueryColumnProvider getInstance(){
		if (instance == null){
			instance = new IntelQueryColumnProvider();
		}
		return instance;
		
	}
	
	
	//TODO: cache data model ???
	public List<AbstractQueryColumn> getQueryColumns (IntelRecordQuery query, Locale l, Session session) {
			
			//add one column for each filter item that is true or false depending on column
			//entity types -> true if has entity type associated
			//entity -> true if specific entity is associated
		
		List<AbstractQueryColumn> columns = new ArrayList<AbstractQueryColumn>();
		
		for (FixedQueryColumn.Column c : FixedQueryColumn.Column.values()){
			columns.add(new FixedQueryColumn(c));
		}
		try{
			IQueryFilter queryFilter = IntelRecordQuery.parseQuery(query.getQueryString()).getFilter();
			
			
			queryFilter.accept(new IFilterVisitor() {
				
				@Override
				public void visitElement(IQueryFilter filter) {
					if (filter instanceof EntityFilter){
						//TODO: name
						EntityColumn ec = new EntityColumn(((EntityFilter) filter).getEntityUuid().toString(),  ((EntityFilter) filter).getEntityUuid());
						if (!columns.contains(ec)){
							columns.add(ec);
						}
					}else if (filter instanceof EntityTypeFilter){
						//TODO: name
						EntityColumn ec = new EntityColumn(((EntityTypeFilter) filter).getTypeKey(), ((EntityTypeFilter) filter).getTypeKey());
						if (!columns.contains(ec)){
							columns.add(ec);
						}
					}else if (filter instanceof IntelAttributeFilter){
						//TODO: do the same thing for intel entity attributes
					}
					//
				}
			});
			
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error loading query columns.  Unable to parse query: " + ex.getMessage(), ex);
		}
		

		//data model columns
		//categories
		SQLQuery sq = session.createSQLQuery("SELECT max(smart.hkeylength(hkey)) from smart.dm_category WHERE ca_uuid = :ca");
		sq.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid());
		Long maxCategory = (Long) sq.uniqueResult();
		
		for (int i = 0; i < maxCategory; i ++){
			columns.add(new DataModelColumn(i));
		}
		
		//attributes
		List<Attribute> attributes = session.createCriteria(Attribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.add(Restrictions.eq("isActive", true))
				.list();
		for (Attribute attribute : attributes){
			columns.add(new DataModelColumn(attribute));
		}
		
		return columns;
	}
}
