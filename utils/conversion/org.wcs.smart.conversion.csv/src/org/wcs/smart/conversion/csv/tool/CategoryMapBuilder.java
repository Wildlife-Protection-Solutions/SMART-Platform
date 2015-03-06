package org.wcs.smart.conversion.csv.tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wcs.smart.conversion.model.CategoryMap;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedCategory;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class CategoryMapBuilder {
	
	private Connection c;
//	private ElementsLookup elLookup;
	
	public CategoryMapBuilder(Connection c) throws SQLException {
		this.c = c;
//		elLookup = new ElementsLookup(c);
	}	

	public List<MappedCategory> extractCategoryValues(List<MappedAttribute> attributes) throws SQLException {
		StringBuilder sql = new StringBuilder();
		int size = attributes.size();
		for (int i = 0; i < size; i++) {
			sql.append("?"); //$NON-NLS-1$
			if (i+1 != size)
				sql.append(", "); //$NON-NLS-1$
		}
		PreparedStatement ps = c.prepareStatement("select id, n from csv_to_smart.attributes where n in ("+sql+")"); //$NON-NLS-1$ //$NON-NLS-2$
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
		ps = c.prepareStatement("select distinct "+sql+" from csv_to_smart.csv"); //$NON-NLS-1$ //$NON-NLS-2$
		rs = ps.executeQuery();
		List<MappedCategory> catValues = new ArrayList<MappedCategory>(); 
		while (rs.next()) {
			MappedCategory ct = new MappedCategory();
			catValues.add(ct);
			for (int i = 0; i < size; i++) {
				String value = rs.getString(i+1);
				if (value != null) {
					MappedAttribute a = attributes.get(i);
					CategoryMap cmap = new CategoryMap();
					cmap.setAi(a.getI());
//					cmap.setAn(Ct2AttributeTypeUtil.getN(a));
					cmap.setVi(value);
//					cmap.setVn(value);
//					cmap.setVn(elLookup.getN(value));
					ct.getCategoryMap().add(cmap);
				}
			}
		}
		rs.close();
		return catValues;
	}

}
