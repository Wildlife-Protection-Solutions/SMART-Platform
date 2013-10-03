package org.wcs.smart.mist.dataReader;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;

public class DataReader {

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length == 0){
			System.err.println("No MIST database file specified. Usage: DataReader PATH_TO_MIST_DB");
			System.exit(1);
		}
		File dbFile = new File(args[0]);
		if (!dbFile.exists()){
			System.err.println("MIST database file not found: '" + dbFile.toString() + "'");
			System.exit(1);
		}
		
		
		/* THIS IS JUST AN EXAMPLE*/
		Connection c = MistDatabase.getConnection(dbFile.getAbsolutePath());
		try{
			ResultSet rs = c
				.createStatement()
				.executeQuery(
						"SELECT D.DEPARTMENT_NAME, DU.UNIT_NAME FROM DEPARTMENT_UNITS DU LEFT JOIN DEPARTMENTS D ON D.DEPARTMENT_ID = DU.DEPARTMENT_ID ");
			while (rs.next()) {
				System.out.println(rs.getString(1));
			}
		}finally{
			c.close();
		}
	}


}
