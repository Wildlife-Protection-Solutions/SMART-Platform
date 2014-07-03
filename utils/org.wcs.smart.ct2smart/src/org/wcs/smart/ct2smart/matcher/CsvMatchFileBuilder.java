package org.wcs.smart.ct2smart.matcher;

import java.sql.Connection;
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

public class CsvMatchFileBuilder {

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
	}

	
	private static Pattern CT_ID_PATTERN = Pattern.compile("\\{[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}\\}"); //$NON-NLS-1$
	private static Pattern BOOLEAN_PATTERN = Pattern.compile("True|False"); //$NON-NLS-1$
	private static Pattern NUMERIC_PATTERN = Pattern.compile("\\d+(.\\d)?\\d?"); //$NON-NLS-1$
	
	public Ct2Smart create(Connection c) throws SQLException {
		ResultSet attrRs = c.createStatement().executeQuery("select n, i, id from CT_TO_SMART.ATTRIBUTES"); //$NON-NLS-1$
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
			String valuesSql = "select distinct a"+attrRs.getString(3)+" from CT_TO_SMART.CSV"; //$NON-NLS-1$ //$NON-NLS-2$
			valueSet.clear();
			ResultSet valRs = c.createStatement().executeQuery(valuesSql);
			Ct2AttributeType type = Ct2AttributeType.IGNORE;
			while (valRs.next()) {
				String value = valRs.getString(1);
				if (value != null) {
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
			}
			valRs.close();

			ctAttr.setType(type);
			
			if (Ct2AttributeType.REF.equals(type)) {
				ref2Value.clear();
				ResultSet elemRs = c.createStatement().executeQuery("select distinct e.i, e.n from CT_TO_SMART.ELEMENT e where e.i in ("+valuesSql+")");  //$NON-NLS-1$//$NON-NLS-2$
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
				//below are elements that do not have matches in ELEMENTS table
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

		return ct2Smart;
	}
	
}
