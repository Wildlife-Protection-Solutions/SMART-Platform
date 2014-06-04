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

//	public List<String> getDates(Connection c) throws SQLException {
//		List<String> dates = new ArrayList<String>();
//		ResultSet rs = c.createStatement().executeQuery("select distinct v from CT_TO_SMART.SIGHTING where n='Date'"); //$NON-NLS-1$
//		while (rs.next()) {
//			dates.add(rs.getString(1));
//		}
//		rs.close();
//		return dates;
//	}

//	public List<TagS> extract(Connection c, String date) throws SQLException {
//		PreparedStatement ps = c.prepareStatement("select s_uuid, i, n, v  from CT_TO_SMART.SIGHTING where s_uuid in (select S_UUID from CT_TO_SMART.SIGHTING where n='Date' and v=?) order by uuid"); //$NON-NLS-1$
//		ps.setString(1, date);
//		return extract(ps);
//	}
	
	public ResultSet getUniqueDateUnitPairs(Connection c) throws SQLException {
		return c.createStatement().executeQuery("select distinct sd.v, su.v from CT_TO_SMART.SIGHTING sd join CT_TO_SMART.SIGHTING su on sd.s_uuid = su.s_uuid where sd.n='Date' and su.n='Unit_ID'"); //$NON-NLS-1$
	}
	

	public List<TagS> extract(Connection c, String date, String unitId) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select s_uuid, i, n, v  from CT_TO_SMART.SIGHTING where s_uuid in (select sd.S_UUID from CT_TO_SMART.SIGHTING sd join CT_TO_SMART.SIGHTING su on sd.s_uuid = su.s_uuid where sd.n='Date' and sd.v=? and su.n='Unit_ID' and su.v=?) order by uuid"); //$NON-NLS-1$
		ps.setString(1, date);
		ps.setString(2, unitId);
		return extract(ps);
	}

	public List<TagS> extract(PreparedStatement ps) throws SQLException {
		List<TagS> result = new ArrayList<TagS>();
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
