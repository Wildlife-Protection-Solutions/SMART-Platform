package org.wcs.smart.mist.dataReader;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * General class for managing the MIST database.
 * 
 * For this to work you MUST have the firebird embedded database
 * package on your system PATH variable.
 * 
 * 
 * @author Emily
 *
 */
public class MistDatabase {

	/**
	 * Connect to the given MIST database.
	 * 
	 * @param databaseFileName
	 * @return
	 * @throws Exception
	 */
	public static Connection getConnection(String databaseFileName) throws Exception{
		
		Class.forName("org.firebirdsql.jdbc.FBDriver");

		Connection c = DriverManager.getConnection(
				"jdbc:firebirdsql:embedded:" + databaseFileName,
				"sysdba", "masterkey");
		return c;
		
	}
}
