/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.db;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.derby.iapi.services.io.FileUtil;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class CreateSmartDb {

	
	private static String[] tableFiles = new String[]{
		"org/wcs/smart/db/smart-tables.sql",
		"org/wcs/smart/db/smart-tables-dm.sql",
		"org/wcs/smart/db/smart-tables-patrol.sql",
		"org/wcs/smart/db/smart-permissions.sql"
	};
	private static String userFile = "org/wcs/smart/db/smart-users.sql";
	

	private static String SMART_ADMIN_USER = "smart_admin";
	private static String SMART_ADMIN_PASS = "smart_derby";

	private static void createSmartDb(String databaseLocation) throws Exception{
		Connection c = DatabaseCmdRunner.createConnection(databaseLocation, SMART_ADMIN_USER, null);
	
		InputStreamReader reader = new InputStreamReader(CreateSmartDb.class.getClassLoader().getResourceAsStream(userFile));
		DatabaseCmdRunner.processFile(c, reader);
		
		//shut down 
		 try {
			 DriverManager.getConnection("jdbc:derby:" + databaseLocation + ";shutdown=true;user=smart_admin;password=smart_derby;");
         } catch (SQLException se) {
             if ( !se.getSQLState().equals("08006") ) {
                 throw se;
             }
         }

		//re-connect as correct user
		c = DatabaseCmdRunner.createConnection(databaseLocation, SMART_ADMIN_USER, SMART_ADMIN_PASS);
		for (int i = 0; i < tableFiles.length; i ++){
			reader = new InputStreamReader(CreateSmartDb.class.getClassLoader().getResourceAsStream(tableFiles[i]));
			DatabaseCmdRunner.processFile(c, reader);	
		}
		
		DatabaseCmdRunner.spatialize(c,"SMART", "AREA_GEOMETRIES", "GEOM");
		c.close();
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if (args.length <1){
			System.err.println("Invalid usage.  Usage: <databaseLocation>");
			return;
		}
		String filename = args[0];
		if (filename.charAt(0) == '"' && filename.charAt(filename.length()-1) == '"'){
			filename = filename.substring(1, filename.length() - 1);
		}
		
		File ff = new File(filename);
		if (ff.exists()){
			System.out.println("removing directory : " + ff.getAbsolutePath());
			//delete ff and all subdirectories
			FileUtil.removeDirectory(ff);
		}
		createSmartDb(filename);
	}

}
