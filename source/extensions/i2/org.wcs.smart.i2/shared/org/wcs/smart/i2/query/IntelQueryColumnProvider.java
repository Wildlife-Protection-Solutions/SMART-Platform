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
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.observation.filter.EntityFilter;
import org.wcs.smart.i2.query.observation.filter.EntityTypeFilter;
import org.wcs.smart.i2.query.observation.filter.IFilterVisitor;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.IntelAttributeFilter;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItemFactory;

public class IntelQueryColumnProvider {

	private static IntelQueryColumnProvider instance;
	
	public synchronized static IntelQueryColumnProvider getInstance(){
		if (instance == null){
			instance = new IntelQueryColumnProvider();
		}
		return instance;
		
	}
	
	
	//TODO: cache data model ???
	public List<IQueryColumn> getQueryColumns (IntelRecordObservationQuery query, Locale l, Session session) {
			
			//add one column for each filter item that is true or false depending on column
			//entity types -> true if has entity type associated
			//entity -> true if specific entity is associated
		
		List<IQueryColumn> columns = new ArrayList<>();
		
		for (FixedQueryColumn.Column c : FixedQueryColumn.Column.values()){
			columns.add(new FixedQueryColumn(c, l));
		}
		try{
			IQueryFilter queryFilter = IntelRecordObservationQuery.parseQuery(query.getQueryString()).getFilter();
			if (queryFilter != null){
				queryFilter.accept(new IFilterVisitor() {
					@Override
					public void visitElement(IQueryFilter filter) {
						if (filter instanceof EntityFilter){
							IntelEntity entity = (IntelEntity) session.get(IntelEntity.class, ((EntityFilter) filter).getEntityUuid());
							String name = null;
							if (entity != null){
								name = DropItemFactory.generateName(entity);
							}else{
								name= ((EntityFilter) filter).getEntityUuid().toString();
							}
							EntityColumn ec = new EntityColumn(name,  ((EntityFilter) filter).getEntityUuid());
							if (!columns.contains(ec)){
								columns.add(ec);
							}
						}else if (filter instanceof EntityTypeFilter){
							IntelEntityType entity = (IntelEntityType) session.createCriteria(IntelEntityType.class)
									.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
									.add(Restrictions.eq("keyId", ((EntityTypeFilter) filter).getTypeKey()))
									.uniqueResult();
							String name = null;
							if (entity != null){
								name = entity.getName();
							}else{
								name= ((EntityTypeFilter) filter).getTypeKey();
							}
							EntityColumn ec = new EntityColumn(name, ((EntityTypeFilter) filter).getTypeKey());
							if (!columns.contains(ec)){
								columns.add(ec);
							}
						}else if (filter instanceof IntelAttributeFilter){
							//TODO: do the same thing for intel entity attributes
						}
						//
					}
				});
			}
			
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error loading query columns.  Unable to parse query: " + ex.getMessage(), ex);
		}
		

		//data model columns
		//categories
		SQLQuery sq = session.createSQLQuery("SELECT max(smart.hkeylength(hkey)) from smart.dm_category WHERE ca_uuid = :ca");
		sq.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid());
		Integer maxCategory = (Integer) sq.uniqueResult();
		
		for (int i = 0; i < maxCategory; i ++){
			columns.add(new DataModelColumn(i));
		}
		
		//attributes
		List<Attribute> attributes = session.createCriteria(Attribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
//				.add(Restrictions.eq("isActive", true))
				.list();
		for (Attribute attribute : attributes){
			columns.add(new DataModelColumn(attribute));
		}
		
		return columns;
	}
}
