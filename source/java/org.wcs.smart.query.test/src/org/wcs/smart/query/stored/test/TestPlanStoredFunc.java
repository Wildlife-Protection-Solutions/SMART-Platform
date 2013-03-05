package org.wcs.smart.query.stored.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.Test;


public class TestPlanStoredFunc {

	@Test
	public void testPatrolInPlanRemoteCall() throws SQLException {
//		byte[] patrolUuid = {1,  10  a0  c4  54  67  45  50  82  4a  92  5b  cf  a0  8e  29  };
		byte[] patrolUuid = {(byte)0xfc,  0x10,  (byte)0xa0,  (byte)0xc4,  0x54,  0x67,  0x45,  0x50,  (byte)0x82,  0x4a,  (byte)0x92,  0x5b,  (byte)0xcf,  (byte)0xa0,  (byte) 0x8e,  0x29  };
		EmbeddedDataSource eds = new EmbeddedDataSource();
		eds.setDatabaseName("data/database/smartdb"); //$NON-NLS-1$
		eds.setUser("smart_admin"); //$NON-NLS-1$
		eds.setPassword("smart_derby"); //$NON-NLS-1$
		Connection c = eds.getConnection();
		String sql = "values smart.patrolInPlan(?, '4346bb54584e4bb7a26db878b06865f3')"; //$NON-NLS-1$
		PreparedStatement s = c.prepareStatement(sql);
		s.setObject(1, patrolUuid);
		ResultSet resultSet = s.executeQuery();
		resultSet.next();
//		System.out.println(resultSet.getInt(1));
	}

}
