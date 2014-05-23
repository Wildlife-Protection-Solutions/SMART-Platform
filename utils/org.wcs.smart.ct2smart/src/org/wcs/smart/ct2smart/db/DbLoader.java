package org.wcs.smart.ct2smart.db;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import org.wcs.smart.ct2smart.dao.ConnectionUtil;
import org.wcs.smart.ct2smart.xml.parser.CTParser;

public class DbLoader {

	public void load(File file, Connection c) throws SQLException {
		cleanDb();
		CTParser.getInstance().parseFile(file, c);
	}

	private void cleanDb() throws SQLException {
		Connection c = ConnectionUtil.getConnection();
		c.createStatement().executeUpdate("TRUNCATE TABLE CT_TO_SMART.ELEMENT"); //$NON-NLS-1$
		c.createStatement().executeUpdate("TRUNCATE TABLE CT_TO_SMART.SIGHTING"); //$NON-NLS-1$
		c.createStatement().executeUpdate("TRUNCATE TABLE CT_TO_SMART.TIMERTRACK"); //$NON-NLS-1$
	}
}
