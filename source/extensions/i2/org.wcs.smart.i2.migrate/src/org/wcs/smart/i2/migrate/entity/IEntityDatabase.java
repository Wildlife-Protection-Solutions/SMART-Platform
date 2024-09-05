package org.wcs.smart.i2.migrate.entity;

import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;

public interface IEntityDatabase {

	public Collection<EntityItem> getEntities(UUID entityType) throws SQLException;

}
