package org.wcs.smart.query.qimport;

import java.util.ArrayList;
import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFactory;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.filter.FilterValidator;
import org.wcs.smart.query.parser.internal.summary.GridQueryDefinition;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Query importer for importing gridded queries
 * @author Emily
 *
 */
public class GriddedQueryDefinitionImporter implements IQueryImporter{

	/*
	 * list of warnings generated during import process
	 */
	private ArrayList<String> warnings = new ArrayList<String>();
	
	
	/**
	 * Imports the given definition file.
	 * 
	 * <p>
	 * The returned query does not have the folder or shared value
	 * set.  This must be set by the calling function based
	 * on where the query is to be saved.
	 * </p> 
	 * 
	 * @param file the query definition xml file to import
	 * @return the imported query
	 * @throws Exception if the file cannot be converted to a query.
	 * 
	 */
	@Override
	public Query importQuery(QueryType qt) throws Exception{
		warnings.clear();
		
		String langCode = qt.getLanguage();
		GriddedQuery griddedQuery = QueryFactory.createGriddedQuery();
		
		QueryImporter.importNames(griddedQuery, qt);
		
		HashMap<String, UuidItemType> uuidLookup = new HashMap<String, UuidItemType>();
		for (UuidItemType type : qt.getUuiditem()){
			uuidLookup.put(type.getUuid(), type);
		}
		
		for (QueryPart part : qt.getQueryPart()) {
			
			if (part.getKey().equalsIgnoreCase("definition")) { //$NON-NLS-1$
				if (part.getValue() != null && part.getValue().length() > 0) {
					
					griddedQuery.setQuery(part.getValue());
					Session session = HibernateManager.openSession();
					session.beginTransaction();
					try {
						GridQueryDefinition def = griddedQuery.getQueryDefinition();
						FilterValidator filter = new FilterValidator();
						if (def.getValueFilter() != null){
							filter.validateFilterPart(def.getValueFilter(), langCode, uuidLookup, session);
							this.warnings.addAll(filter.getWarnings());
						}
						
						if (def.getRateFilter() != null){
							filter.validateFilterPart(def.getRateFilter(), langCode, uuidLookup, session);
							this.warnings.addAll(filter.getWarnings());
						}
						
						//process value items
						validateValuePart(def.getValuePart(), session);
						
						griddedQuery.setQuery(def.asQuery(), def);
					} finally {
						session.getTransaction().rollback();
						session.close();
					}
				}
			}else if (part.getKey().equalsIgnoreCase("crs")){ //$NON-NLS-1$
				griddedQuery.setCrsDefinition(part.getValue());
			}
		}
		
		
		griddedQuery.setConservationArea(SmartDB.getCurrentConservationArea());
		griddedQuery.setOwner(SmartDB.getCurrentEmployee());
		
		ConservationAreaFilter caFilter = new ConservationAreaFilter();
		caFilter.addConservationArea(SmartDB.getCurrentConservationArea());
		griddedQuery.setConservationAreaFilter(caFilter);
		
		return griddedQuery;
	}
	
	/**
	 * @return warnings generated during import process
	 */
	@Override
	public ArrayList<String> getWarnings(){
		return this.warnings;
	}
	
	private void validateValuePart(IValueItem value, Session session) throws Exception{
		value.validateDatabase(session);
	}
	
	
}