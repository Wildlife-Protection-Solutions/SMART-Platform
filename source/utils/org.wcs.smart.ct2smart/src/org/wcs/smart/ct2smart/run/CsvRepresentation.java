package org.wcs.smart.ct2smart.run;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.ct2smart.dao.ConnectionUtil;

public class CsvRepresentation {

	public static void main(String[] args) throws SQLException {
		CsvRepresentation rep = new CsvRepresentation();
		rep.process();
	}

	
	public void process() throws SQLException {
		Connection c = ConnectionUtil.getConnection();
//		c.createStatement().executeUpdate("TRUNCATE TABLE CT_TO_SMART.attributes"); //$NON-NLS-1$
		try {
			c.createStatement().executeUpdate("DROP TABLE CT_TO_SMART.CSV"); //$NON-NLS-1$
		} catch (SQLException e) {
			System.out.print("No need to remove CSV table");
			//ignore, table doesn't exist
		}
//		c.createStatement().executeUpdate("insert into ct_to_smart.attributes (n, i) select distinct e.n, e.i from CT_TO_SMART.ELEMENT e join CT_TO_SMART.SIGHTING s on e.i = s.i"); //$NON-NLS-1$
		
		Map<String, String> attr2Column = new HashMap<String, String>();
		StringBuilder sql = new StringBuilder("create table ct_to_smart.csv ( id integer not null GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), ");
		ResultSet attrRs = c.createStatement().executeQuery("select id, n, i from ct_to_smart.attributes"); //$NON-NLS-1$
		while (attrRs.next()) {
			String id = attrRs.getString(1);
			String n = attrRs.getString(2);
			String i = attrRs.getString(3);
			
			attr2Column.put(i, id);
			
			sql.append("a_").append(id).append(" varchar(255), ");
		}
		attrRs.close();
		sql.append("primary key (id))");
		c.createStatement().executeUpdate(sql.toString());
		System.out.println("CSV Table created");
		
		
		c.close();
	}
}
