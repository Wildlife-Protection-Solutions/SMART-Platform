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
package org.wcs.smart.hibernate;

import java.sql.DriverManager;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

/**
 * Database specific extensions for the derby database.
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbyHibernateExtensions {

	/**
	 * Correctly shuts down the derby database.
	 * 
	 * @param reconnect should be <code>true</code> if there is any change
	 * the application is going to reconnect to the database.
	 */
	public static void shutDown(boolean reconnect){
		try {
			DriverManager.getConnection("jdbc:derby:;shutdown=true"); //$NON-NLS-1$
		} catch (Exception e) {
			//e.printStackTrace();
			//eatme - this will always throw an exception
			//e.printStackTrace();
		}
		
		if (reconnect){
			try{
				//without this hibernate will not re-connect to the database propery
				Class.forName("org.apache.derby.jdbc.EmbeddedDriver").getDeclaredConstructor().newInstance(); //$NON-NLS-1$
			}catch (Exception ex){
			}
		}

	}

	/**
	 * Checks if given table exists in Derby database.
	 * 
	 * Assumes the table schema is SMART.
	 * 
	 * @param session
	 * @param tableName the name of the table without the schema
	 * @return
	 */
	public static boolean tableExists(Session session, String tableName) {
		String sql = "select count(*) from SYS.SYSTABLES tbl inner join SYS.SYSSCHEMAS sch on tbl.SCHEMAID = sch.SCHEMAID AND sch.SCHEMANAME = 'SMART' WHERE tbl.TABLETYPE = 'T' AND tbl.TABLENAME = UPPER('" + tableName +"')"; //$NON-NLS-1$ //$NON-NLS-2$
		NativeQuery<Integer> q = session.createNativeQuery(sql, Integer.class);
		Integer result = q.uniqueResult();
		return result != null && result > 0;
	}
	
}
