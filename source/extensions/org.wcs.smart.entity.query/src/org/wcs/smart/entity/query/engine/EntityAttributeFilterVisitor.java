package org.wcs.smart.entity.query.engine;

import java.util.HashSet;

import org.wcs.smart.ca.Area;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

public class EntityAttributeFilterVisitor  implements IFilterVisitor{

	private HashSet<String> addedTableNames = new HashSet<String>();
	private StringBuilder sql;
	private DerbyObservationQueryEngine engine;
	
	/**
	 * Creates a new visitor
	 * @param sql sql to append to
	 * @param engine query engine
	 * @param usedTables list of tables already added to sql (Track.class)
	 * only needs to be added once
	 */
	public EntityAttributeFilterVisitor(StringBuilder sql, DerbyObservationQueryEngine engine){
		this.sql = sql;
		this.engine = engine;
	}
	
	
	@Override
	public void visit(IFilter filter) {
		if (filter instanceof EntityAttributeFilterVisitor){
			EntityAttributeFilterVisitor ff = (EntityAttributeFilterVisitor)filter;
			String areaTableName = engine.tableName(EntityAttribute.class);
			
			if (!addedTableNames.contains(areaTableName)) {
				addedTableNames.add(areaTableName);
				
				// TODO: escape special characters from the key
				sql.append(" left join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(EntityAttribute.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".list_element_uuid");
				sql.append (" = ");
				sql.append(engine.tablePrefix(EntityAttribute.class) + ".dm_attribute_uuid");
				sql.append(" left join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(EntityAttributeValue.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(EntityAttributeValue.class) + ".entity_attribute_uuid");
				sql.append (" = ");
				sql.append(engine.tablePrefix(EntityAttribute.class) + ".uuid");
			}
		}
	}

}
