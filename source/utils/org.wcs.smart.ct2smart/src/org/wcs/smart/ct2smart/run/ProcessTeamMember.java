package org.wcs.smart.ct2smart.run;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.wcs.smart.ct2smart.dao.ConnectionUtil;
import org.wcs.smart.ct2smart.parser.TeamMembersParser;

public class ProcessTeamMember {

	/**
	 * @param args
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws SQLException {
		long start = System.currentTimeMillis();

		Connection c = ConnectionUtil.getConnection();
		ResultSet rs = c.createStatement().executeQuery("select distinct V from CT_TO_SMART.SIGHTING where N = 'team_members'"); //$NON-NLS-1$
		Set<String> rawMembers = new HashSet<String>();
		while (rs.next()) {
			rawMembers.add(rs.getString(1));
		}
		rs.close();
		c.close();
		TeamMembersParser tmp = new TeamMembersParser();
		Set<String> result = tmp.parse(rawMembers);
//		Set<String> result = tmp.parseMembers("eLVIS  obi and amos mKPE");
		System.out.println(Arrays.toString(result.toArray()));
		
		System.out.println("Done in "+ (double)(System.currentTimeMillis()-start)/1000 +" seconds!!!");
	}

}
