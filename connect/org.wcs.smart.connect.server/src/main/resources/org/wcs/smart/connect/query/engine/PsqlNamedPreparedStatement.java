package org.wcs.smart.connect.query.engine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.wcs.smart.query.common.engine.NamedPreparedStatement;

public class PsqlNamedPreparedStatement extends NamedPreparedStatement{
	
	public PsqlNamedPreparedStatement(Connection connection, String query)
			throws SQLException {
		super(connection, query);
	}

	@Override
    public void setObject(String name, Object value) throws SQLException {
        for(int i: getIndexes(name)) {
       		statement.setObject(i, value);
        }
    }
}
