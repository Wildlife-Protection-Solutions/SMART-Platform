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
package org.wcs.smart.connect.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.replication.DerbyReplicationManager;

/**
 * Derby utilities to support connect replication.
 * @author Emily
 *
 */
public class DerbyUtil {

	/**
	 * Computes the next revision number for a given conservation area
	 * it.  Should be used for computing the revision number to insert into the
	 * change log table.
	 * 
	 * @param uuid
	 * @return
	 * @throws SQLException
	 */
	/*
	 * I implemented this as a function insert statements must call instead of a trigger
	 * on the change_log_table because the trigger caused deadlocks with two 
	 * threads with two open transactions were trying to insert into the change log table
	 * at the same time.
	 */
	public static Long getNextRevisionId(byte[] cauuid) throws SQLException{
		if (cauuid == null) return -1l;
		String sql = "select (case when max(revision) is null then -1 else max(revision) end) + 1 "  //$NON-NLS-1$
				+ " from smart.connect_change_log where ca_uuid = ?"; //$NON-NLS-1$
		Connection c = DriverManager.getConnection("jdbc:default:connection"); //$NON-NLS-1$
		PreparedStatement ps = c.prepareStatement(sql);
		ps.setBytes(1, cauuid);
		ResultSet rs = ps.executeQuery();
		rs.next();
		return rs.getLong(1);
	}
	
	public static Boolean isReplicationEnabled(byte[] cauuid) throws SQLException{
		Connection c = DriverManager.getConnection("jdbc:default:connection"); //$NON-NLS-1$
		return isReplicationEnabled(cauuid, c);
	}
	
	public static Boolean isReplicationEnabled(byte[] cauuid, Connection c) throws SQLException{
		if (cauuid == null) return Boolean.FALSE;
		
		//check application property
		String sql = "values syscs_util.syscs_get_database_property( '" + DerbyReplicationManager.LOGGING_DB_PROPERTY + "' )"; //$NON-NLS-1$ //$NON-NLS-2$
		try(ResultSet rs = c.createStatement().executeQuery(sql)){
			rs.next();
			if (!rs.getBoolean(1)){
				return Boolean.FALSE;
			}
		}
		
		sql = "SELECT status from smart.connect_status where ca_uuid = ?"; //$NON-NLS-1$
		PreparedStatement ps = c.prepareStatement(sql);
		ps.setBytes(1, cauuid);
		try(ResultSet rs = ps.executeQuery()){
			if (rs.next()){
				ConnectServerStatus.Status status = ConnectServerStatus.Status.valueOf(rs.getString(1));
				if (status == null) return false;
				if (status == ConnectServerStatus.Status.UPLOAD ||
					status == ConnectServerStatus.Status.DONE){
					return true;
				}
			}
		}
		return false;
	}
	

}
