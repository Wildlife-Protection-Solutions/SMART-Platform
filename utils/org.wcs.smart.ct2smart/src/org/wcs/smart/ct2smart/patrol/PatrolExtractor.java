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

	public ResultSet getUniqueGroups(Connection c, String... n) throws SQLException {
		StringBuilder sql = new StringBuilder("select distinct "); //$NON-NLS-1$
		
		StringBuilder what = new StringBuilder("s0.v"); //$NON-NLS-1$
		StringBuilder from = new StringBuilder(" from CT_TO_SMART.SIGHTING s0"); //$NON-NLS-1$
		StringBuilder where = new StringBuilder(" where s0.n='"); //$NON-NLS-1$
		where.append(n[0]).append("'"); //$NON-NLS-1$
		
		for (int i = 1; i < n.length; i++) {
			what.append(",s").append(i).append(".v"); //$NON-NLS-1$ //$NON-NLS-2$
			from.append(" join CT_TO_SMART.SIGHTING s").append(i).append(" on s").append(i).append(".s_uuid=s0.s_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			where.append(" and s").append(i).append(".n='").append(n[i]).append("'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		sql.append(what).append(from).append(where);
		return c.createStatement().executeQuery(sql.toString());
	}
	
	public ResultSet getUniqueDateUnitPairs(Connection c) throws SQLException {
		return c.createStatement().executeQuery("select distinct sd.v, su.v from CT_TO_SMART.SIGHTING sd join CT_TO_SMART.SIGHTING su on sd.s_uuid = su.s_uuid where sd.n='Date' and su.n='Unit_ID'"); //$NON-NLS-1$
	}
	

	public List<TagS> extract(Connection c, String[] n, String[] v) throws SQLException {
		StringBuilder inSql = new StringBuilder("select s0.S_UUID from CT_TO_SMART.SIGHTING s0"); //$NON-NLS-1$
		StringBuilder where = new StringBuilder(" where s0.n='"); //$NON-NLS-1$
		where.append(n[0]).append("' and s0.v=?"); //$NON-NLS-1$
		
		for (int i = 1; i < n.length; i++) {
			inSql.append(" join CT_TO_SMART.SIGHTING s").append(i).append(" on s").append(i).append(".s_uuid=s0.s_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			where.append(" and s").append(i).append(".n='").append(n[i]).append("' and s").append(i).append(".v=?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		inSql.append(where);
		
		PreparedStatement ps = c.prepareStatement("select s_uuid, i, n, v  from CT_TO_SMART.SIGHTING where s_uuid in ("+inSql+") order by uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < n.length; i++) {
			ps.setString(i+1, v[i]);
		}
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
