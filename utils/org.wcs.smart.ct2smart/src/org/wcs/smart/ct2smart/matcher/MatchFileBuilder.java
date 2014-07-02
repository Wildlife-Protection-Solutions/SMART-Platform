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
package org.wcs.smart.ct2smart.matcher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeValue;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.ct2smart.matcher.model.CtCategory;
import org.wcs.smart.ct2smart.matcher.model.CtCategoryMap;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class MatchFileBuilder {

	private static final Map<String, Ct2AttributeType> KNOWN_ATTRIBUTES;
	static {
		KNOWN_ATTRIBUTES = new HashMap<String, Ct2AttributeType>();
		KNOWN_ATTRIBUTES.put("Id",		 Ct2AttributeType.IGNORE); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("DeviceId", Ct2AttributeType.IGNORE); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Date",	 Ct2AttributeType.META_DATE); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Time",	 Ct2AttributeType.META_TIME); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Latitude", Ct2AttributeType.META_LAT); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Longitude",Ct2AttributeType.META_LON); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Altitude", Ct2AttributeType.IGNORE); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Accuracy", Ct2AttributeType.IGNORE); //$NON-NLS-1$

//		KNOWN_ATTRIBUTES.put("team_members"); //$NON-NLS-1$
	}

	
	private static Pattern CT_ID_PATTERN = Pattern.compile("\\{[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}\\}"); //$NON-NLS-1$
	private static Pattern BOOLEAN_PATTERN = Pattern.compile("True|False"); //$NON-NLS-1$
	private static Pattern NUMERIC_PATTERN = Pattern.compile("\\d+(.\\d)?\\d?"); //$NON-NLS-1$
	
	public Ct2Smart create(Connection c) throws SQLException {
		ResultSet attrRs = c.createStatement().executeQuery("select distinct e.n, e.i from CT_TO_SMART.ELEMENT e join CT_TO_SMART.SIGHTING s on e.i = s.i"); //$NON-NLS-1$
		PreparedStatement valueSt = c.prepareStatement("select distinct v from CT_TO_SMART.SIGHTING where N = ?"); //$NON-NLS-1$
		PreparedStatement elementsSt = c.prepareStatement("select distinct e.i, e.n from CT_TO_SMART.ELEMENT e join CT_TO_SMART.SIGHTING s on e.i = s.v where s.N = ?"); //$NON-NLS-1$
		Set<String> valueSet = new HashSet<String>();
		Map<String, String> ref2Value = new HashMap<String, String>();
		Ct2Smart ct2Smart = new Ct2Smart();
		while (attrRs.next()) {
			String attrName = attrRs.getString(1);
			System.out.println("Processing: " + attrName);
			Ct2Attribute ctAttr = new Ct2Attribute();
			ctAttr.setN(attrName);
			ctAttr.setI(attrRs.getString(2));
			ct2Smart.getCt2Attribute().add(ctAttr);
			if (KNOWN_ATTRIBUTES.containsKey(attrName)) {
				ctAttr.setType(KNOWN_ATTRIBUTES.get(attrName));
				continue;
			}
			valueSet.clear();
			valueSt.setString(1, attrName);
			ResultSet valRs = valueSt.executeQuery();
			Ct2AttributeType type = Ct2AttributeType.IGNORE;
			while (valRs.next()) {
				String value = valRs.getString(1);
				valueSet.add(value);
				switch (type) {
				case IGNORE:
				case BOOL:
					if (BOOLEAN_PATTERN.matcher(value).matches()) {
						type = Ct2AttributeType.BOOL;
						break;
					}
				case NUMERIC:
					if (NUMERIC_PATTERN.matcher(value).matches()) {
						type = Ct2AttributeType.NUMERIC;
						break;
					}
					type = Ct2AttributeType.TEXT;
				case TEXT:
					if (CT_ID_PATTERN.matcher(value).matches()) {
						type = Ct2AttributeType.REF;
						break;
					}
				case REF:
					break;
				}
			}
			valRs.close();

			ctAttr.setType(type);
			
			if (Ct2AttributeType.REF.equals(type)) {
				ref2Value.clear();
				elementsSt.setString(1, attrName);
				ResultSet elemRs = elementsSt.executeQuery();
				while (elemRs.next()) {
					String key = elemRs.getString(1);
					String name = elemRs.getString(2);
					Ct2AttributeValue ctAttrValue = new Ct2AttributeValue();
					ctAttrValue.setI(key);
					ctAttrValue.setN(name);
					ctAttrValue.setMapTo("???");
					ctAttr.getCt2AttributeValue().add(ctAttrValue);
					valueSet.remove(key);
				}
				for (String s : valueSet) {
					Ct2AttributeValue ctAttrValue = new Ct2AttributeValue();
					ctAttrValue.setI(s);
					ctAttrValue.setN(s);
					ctAttrValue.setMapTo("???");
					ctAttr.getCt2AttributeValue().add(ctAttrValue);
				}
			}
			
		}
		attrRs.close();
		c.close();
		
//		CtCategory cat = new CtCategory();
//		ct2Smart.getCtCategory().add(cat);
//		cat.setCategoryKey("testkey");
//		
//		CtCategoryMap cmap = new CtCategoryMap();
//		cat.getCtCategoryMap().add(cmap);
//		cmap.setAi("ai1");
//		cmap.setAn("an1");
//		cmap.setVi("vi1");
//		cmap.setVn("vn1");
//		
//		cmap = new CtCategoryMap();
//		cat.getCtCategoryMap().add(cmap);
//		cmap.setAi("ai2");
//		cmap.setAn("an2");
//		cmap.setVi("vi2");
//		cmap.setVn("vn2");

		return ct2Smart;
	}

}
