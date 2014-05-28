package org.wcs.smart.ct2smart.patrol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.ct2smart.xml.parser.TagA;
import org.wcs.smart.ct2smart.xml.parser.TagS;

public class PatrolExtractor {

	public List<String> getDates(Connection c) throws SQLException {
		List<String> dates = new ArrayList<String>();
		ResultSet rs = c.createStatement().executeQuery("select distinct v from CT_TO_SMART.SIGHTING where n='Date'"); //$NON-NLS-1$
		while (rs.next()) {
			dates.add(rs.getString(1));
		}
		rs.close();
		return dates;
	}
	
	public List<TagS> extract(Connection c, String date) throws SQLException {
		List<TagS> result = new ArrayList<TagS>();
		PreparedStatement ps = c.prepareStatement("select s_uuid, i, n, v  from CT_TO_SMART.SIGHTING where s_uuid in (select S_UUID from CT_TO_SMART.SIGHTING where n='Date' and v=?) order by uuid"); //$NON-NLS-1$
		ps.setString(1, date);
		ResultSet rs = ps.executeQuery();
		int last_s_uuid = -1;
		TagS s = null;
		int s_uuid;
		while (rs.next()) {
			s_uuid = rs.getInt(1);
			if (last_s_uuid == s_uuid) {
				TagA a = new TagA();
				a.setI(rs.getString(2));
				a.setN(rs.getString(3));
				a.setV(rs.getString(4));
				s.add(a);
			} else {
				if (s != null)
					result.add(s);
				s = new TagS();
				last_s_uuid = s_uuid;
			}
		}
		if (s != null)
			result.add(s);
		rs.close();
		return result;
	}
}
