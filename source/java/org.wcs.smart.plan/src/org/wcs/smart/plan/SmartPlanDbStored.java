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
package org.wcs.smart.plan;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


/**
 * Smart Plan Database Stored function:
 * 
 * create function smart.patrolInPlan(patrol_uuid CHAR(16) FOR BIT DATA, uuidStr long varchar)
 *     returns boolean
 *     language java
 *     parameter style java
 *     reads sql data
 *     external name 'org.wcs.smart.plan.SmartPlanDbStored.patrolInPlan';
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class SmartPlanDbStored {

	public static boolean patrolInPlan(byte[] patrolUuid, String planUuidStr) throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:default:connection"); //$NON-NLS-1$
		//TODO: algorithm might be optimised:
		//we do not actually need rebuild plan ids every time
		//list of ids might be saved somewhere but it should be rebuild after changes in plans
		List<Object> planIds = listChildPlanIds(connection, planUuidStr);
		if (planIds.size() == 0)
			return false;
		StringBuilder sql = new StringBuilder("SELECT * FROM smart.patrol_plan pa2pl WHERE pa2pl.patrol_uuid = ? AND pa2pl.plan_uuid in ("); //$NON-NLS-1$
		for (int i = planIds.size(); i > 1 ; i--) {
			sql.append("?,"); //$NON-NLS-1$
		}
		sql.append("?)"); //last item //$NON-NLS-1$
		PreparedStatement statement = connection.prepareStatement(sql.toString());
		statement.setObject(1, patrolUuid);
		for (int i = planIds.size()+1; i > 1 ; i--) { //i is from 2 to planIds.size()+1 (to fill all '?' symbols)
			statement.setObject(i, planIds.get(i-2));
		}
		ResultSet resultSet = statement.executeQuery();
		boolean result = resultSet.next();
		return result;
	}
	
	private static List<Object> listChildPlanIds(Connection connection, Object uuidStr) throws SQLException {
		List<Object> ids = new ArrayList<Object>();
		Statement statement = connection.createStatement();
		String sql = "SELECT p.uuid FROM smart.plan p where p.uuid = x'"+uuidStr+"'"; //$NON-NLS-1$ //$NON-NLS-2$
		ResultSet resultSet = statement.executeQuery(sql);
		if (resultSet.next()) {
			byte[] uuid = (byte[]) resultSet.getObject(1);
			ids.add(uuid); //adding current root plan id to result
			ids.addAll(listChildPlanIds(uuid, connection));
		}
		return ids;
	}

	private static List<Object> listChildPlanIds(byte[] uuid, Connection c) throws SQLException {
		List<Object> ids = new ArrayList<Object>();
		String sql = "SELECT p.uuid FROM smart.plan p where p.parent_uuid = ?"; //$NON-NLS-1$
		PreparedStatement statement = c.prepareStatement(sql);
		statement.setObject(1, uuid);
		ResultSet resultSet = statement.executeQuery();
		while (resultSet.next()) {
			byte[] childUuid = (byte[]) resultSet.getObject(1);
			ids.add(childUuid);
			ids.addAll(listChildPlanIds(childUuid, c));
		}
		resultSet.close();
		return ids;
	}
	
}
