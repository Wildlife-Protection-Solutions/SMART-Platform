package org.wcs.smart.ct2smart.ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.CtCategory;
import org.wcs.smart.ct2smart.matcher.model.CtCategoryMap;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class CategoryMapBuilder {
	
	private Connection c;
	private ElementsLookup elLookup;
	
	public CategoryMapBuilder(Connection c) throws SQLException {
		this.c = c;
		elLookup = new ElementsLookup(c);
	}	

	public List<CtCategory> extractCategoryValues(List<Ct2Attribute> attributes) throws SQLException {
		StringBuilder sql = new StringBuilder();
		int size = attributes.size();
		for (int i = 0; i < size; i++) {
			sql.append("?"); //$NON-NLS-1$
			if (i+1 != size)
				sql.append(", "); //$NON-NLS-1$
		}
		PreparedStatement ps = c.prepareStatement("select id, i from ct_to_smart.attributes where i in ("+sql+")"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < size; i++) {
			ps.setString(i+1, attributes.get(i).getI());
		}
		
		Map<String, Integer> map = new HashMap<String, Integer>();
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			map.put(rs.getString(2), rs.getInt(1));
		}
		rs.close();

		sql = new StringBuilder();
		for (int i = 0; i < size; i++) {
			Integer column = map.get(attributes.get(i).getI());
			sql.append("a").append(column); //$NON-NLS-1$
			if (i+1 != size)
				sql.append(", "); //$NON-NLS-1$
		}
		ps = c.prepareStatement("select distinct "+sql+" from ct_to_smart.csv"); //$NON-NLS-1$ //$NON-NLS-2$
		rs = ps.executeQuery();
		List<CtCategory> catValues = new ArrayList<CtCategory>(); 
		while (rs.next()) {
			CtCategory ct = new CtCategory();
			catValues.add(ct);
			for (int i = 0; i < size; i++) {
				String value = rs.getString(i+1);
				if (value != null) {
					Ct2Attribute a = attributes.get(i);
					CtCategoryMap cmap = new CtCategoryMap();
					cmap.setAi(a.getI());
					cmap.setAn(a.getN());
					cmap.setVi(value);
					cmap.setVn(elLookup.getN(value));
					ct.getCtCategoryMap().add(cmap);
				}
			}
		}
		rs.close();
		return catValues;
	}

}
