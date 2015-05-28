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
package org.wcs.smart.conversion.csv.tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.SmartMapping;
import org.wcs.smart.conversion.tag.TagA;
import org.wcs.smart.conversion.tag.TagE;
import org.wcs.smart.conversion.tag.TagS;
import org.wcs.smart.conversion.tool.MatchSession;

/**
 * Common logic for extracting data from CSV files.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public abstract class AbstractCsvExtractor {

	private Connection c;
	private Map<String, TagE> col2Attr;
	private Map<String, String> n2Col;
	
	public AbstractCsvExtractor(Connection c) throws SQLException {
		this.c = c;
		col2Attr = new HashMap<String, TagE>();
		n2Col = new HashMap<String, String>();
		PreparedStatement ps = c.prepareStatement("select id, n from csv_to_smart.attributes"); //$NON-NLS-1$
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			String col = "a" + rs.getString(1); //$NON-NLS-1$
			TagE e = new TagE();
			e.setI(rs.getString(2));
			e.setN(e.getI());
			col2Attr.put(col, e);
			n2Col.put(e.getN(), col);
		}
		rs.close();
	}

	public abstract void extract(String folder, MatchSession session, DataModelLookup dmLookup) throws Exception;

	public String buildId(String[] uniqueValues) {
		StringBuilder sb = new StringBuilder();
		for (int i = uniqueValues.length-1; i >= 0; i--) {
			sb.append(uniqueValues[i].replace('/', '-'));
			if (i > 0) {
				sb.append('-');
			}
		}
		return sb.toString();
	}


	public String[] getUniqueIds(SmartMapping mapping) {
		List<String> result = new ArrayList<>();
		for (MappedAttribute a : mapping.getMappedAttribute()) {
			switch (a.getType()) {
			case META_OBJECT_ID:
				result.add(a.getI());
				break;
			default:
				break;
			}
		}
		return result.toArray(new String[result.size()]);
	}
	
	//Transforms from ["Data", "Unit"] to ["a2", "a15"].
	public String[] getCsvColumns(String... n) {
		String[] columns = new String[n.length];
		for (int i = 0; i < n.length; i++) {
			columns[i] = n2Col.get(n[i]);
		}
		return columns;
	}
	
	public ResultSet getUniqueGroups(String[] columns) throws SQLException {
		StringBuilder sql = new StringBuilder("select distinct "); //$NON-NLS-1$
		for (int i = 0; i < columns.length; i++) {
			sql.append(columns[i]);
			if (i+1 < columns.length)
				sql.append(", "); //$NON-NLS-1$
		}
		sql.append(" from csv_to_smart.csv"); //$NON-NLS-1$
		return c.createStatement().executeQuery(sql.toString());
	}
	
	public List<TagS> extractS(String[] columns, String[] v) throws SQLException {
		StringBuilder sql = new StringBuilder("select * from csv_to_smart.csv where "); //$NON-NLS-1$
		for (int i = 0; i < columns.length; i++) {
			sql.append(columns[i]).append("=?"); //$NON-NLS-1$
			if (i+1 < columns.length)
				sql.append(" and "); //$NON-NLS-1$
		}
		PreparedStatement ps = c.prepareStatement(sql.toString());
		for (int i = 0; i < columns.length; i++) {
			ps.setString(i+1, v[i]);
		}
		return extractS(ps);
	}
	
	protected List<TagS> extractS(PreparedStatement ps) throws SQLException {
		List<TagS> result = new ArrayList<TagS>();
		ResultSet rs = ps.executeQuery();
		Set<String> columns = col2Attr.keySet();
		while (rs.next()) {
			TagS s = new TagS();
			for (String column : columns) {
				String v = rs.getString(column);
				if (v != null && !v.isEmpty()) {
					TagA a = new TagA();
					TagE e = col2Attr.get(column);
					a.setI(e.getI());
					a.setN(e.getN());
					a.setV(v);
					s.add(a);
				}
			}
			result.add(s);		
		}
		rs.close();
		return result;
	}
/*	
	public List<TagT> extractT(String deviceId, String date) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select time, latitude, longitude from CT_TO_SMART.TIMERTRACK where device_id=? and date=?"); //$NON-NLS-1$
		ps.setString(1, deviceId);
		ps.setString(2, date);

		ResultSet rs = ps.executeQuery();

		List<TagT> result = new ArrayList<TagT>();
		TagT t = null;
		while (rs.next()) {
			t = new TagT();
			t.setDeviceId(deviceId);
			t.setDate(date);
			t.setTime(rs.getString(1));
			t.setLatitude(rs.getString(2));
			t.setLongitude(rs.getString(3));
			result.add(t);
		}
		rs.close();
		return result;
	}
*/
	
}
