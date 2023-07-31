package org.wcs.smart.i2.migrate.entity;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.migrate.SmartSQLDatabase;

public abstract class EntityDatabase extends SmartSQLDatabase {

	public EntityDatabase(Path dir) throws SQLException {
		super(dir);
	}
	
	public abstract boolean validateEntityVersion() throws SQLException ;
	
	public abstract List<ConservationArea> getConservationAreasWithEntity()  throws SQLException;

	public abstract Collection<EntityItem> getEntities(UUID entityType) throws SQLException;

	public abstract Collection<EntityTypeItem> getTypes(ConservationArea ca) throws SQLException;

}
