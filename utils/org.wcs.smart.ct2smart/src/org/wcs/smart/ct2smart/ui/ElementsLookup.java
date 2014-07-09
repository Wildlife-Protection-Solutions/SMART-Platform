package org.wcs.smart.ct2smart.ui;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class ElementsLookup {
	
	private Map<String, String> i2n;

	public ElementsLookup(Connection c) throws SQLException {
		i2n = new HashMap<String, String>();
		ResultSet rs = c.createStatement().executeQuery("select i, n from ct_to_smart.element"); //$NON-NLS-1$
		while (rs.next()) {
			i2n.put(rs.getString(1), rs.getString(2));
		}
		rs.close();
	}

	public String getN(String i) {
		String n = i2n.get(i);
		return n != null ? n : i;
	}
	
}
