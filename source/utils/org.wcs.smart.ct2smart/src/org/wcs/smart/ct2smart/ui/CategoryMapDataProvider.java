package org.wcs.smart.ct2smart.ui;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wcs.smart.ct2smart.dao.ConnectionUtil;
import org.wcs.smart.ct2smart.matcher.FileUtil;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.ct2smart.matcher.model.CtCategory;
import org.wcs.smart.ct2smart.matcher.model.CtCategoryMap;

public class CategoryMapDataProvider {

	public static void main(String[] args) throws Exception {
		CategoryMapDataProvider test = new CategoryMapDataProvider();
		Connection c = ConnectionUtil.getConnection();
		Ct2Smart ct2Smart = FileUtil.loadCt2Smart(new File("d:\\dev\\data\\mist\\match_super_x.xml"));
		List<Ct2Attribute> items = new ArrayList<Ct2Attribute>();
		for (Ct2Attribute a : ct2Smart.getCt2Attribute()) {
			if (Ct2AttributeType.CATEGORY.equals(a.getType()))
				items.add(a);
		}
		
		test.extractCategoryValues(items, c);
		c.close();
	}

	private List<Ct2Attribute> categoryItems;
	
	public void setSource(Ct2Smart source) {
		List<Ct2Attribute> items = new ArrayList<Ct2Attribute>();
		for (Ct2Attribute a : source.getCt2Attribute()) {
			if (Ct2AttributeType.CATEGORY.equals(a.getType()))
				items.add(a);
		}
		
		if (!isEqual(categoryItems, items)) {
			categoryItems = items;
			
		}
	}

	public List<CtCategory> extractCategoryValues(List<Ct2Attribute> attributes, Connection c) throws SQLException {
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
					cmap.setVn(getN(value));
					ct.getCtCategoryMap().add(cmap);
				}
			}
		}
		rs.close();
		return catValues;
	}

	public String getN(String i) {
		return i; //TODO: need proper element name
	}
	
	private boolean isEqual(List<Ct2Attribute> source, List<Ct2Attribute> dest) {
		if (source == null)
			return dest == null;
		if (dest == null)
			return source == null;
		
		if (source.size() != dest.size())
			return false;
		
		for (int i = 0; i < source.size(); i++) {
			if (!source.get(i).equals(dest.get(i)))
				return false;
		}
		
		return true;
	}
	
}
