package org.wcs.smart.ct2smart.validate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class AttributeLookup {
	
	private Map<String, Integer> i2Column;
//	private List<Ct2Attribute> catAttributes;
//	private List<Ct2Attribute> attrToMap;
	
	
	public AttributeLookup(Connection c)  {
		try {
			i2Column = new HashMap<String, Integer>();
			ResultSet rs = c.createStatement().executeQuery(
					"select i, id from ct_to_smart.attributes"); //$NON-NLS-1$
			while (rs.next()) {
				i2Column.put(rs.getString(1), rs.getInt(2));
			}
			rs.close();
		} catch (Exception e) {
			i2Column = null;
			e.printStackTrace();
		}
	}

	public Integer getColumn(String i) {
		return i2Column.get(i);
	}
	
}
