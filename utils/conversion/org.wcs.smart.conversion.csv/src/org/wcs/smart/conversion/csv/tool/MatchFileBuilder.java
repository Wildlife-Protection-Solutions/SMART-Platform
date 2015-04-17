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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeType;
import org.wcs.smart.conversion.model.MappedAttributeValue;
import org.wcs.smart.conversion.model.SmartMapping;

/**
 * Class is responsible for creating a mapping file guess for current DB data.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class MatchFileBuilder {

	private static final Map<String, MappedAttributeType> KNOWN_ATTRIBUTES;
	static {
		KNOWN_ATTRIBUTES = new HashMap<String, MappedAttributeType>();
		KNOWN_ATTRIBUTES.put("Id",		 MappedAttributeType.IGNORE); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("DeviceId", MappedAttributeType.IGNORE); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Date",	 MappedAttributeType.META_DATE); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Time",	 MappedAttributeType.META_TIME); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Latitude", MappedAttributeType.META_LAT); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Longitude",MappedAttributeType.META_LON); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Altitude", MappedAttributeType.IGNORE); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Accuracy", MappedAttributeType.IGNORE); //$NON-NLS-1$
		KNOWN_ATTRIBUTES.put("Unit_ID",  MappedAttributeType.META_OBJECT_ID); //$NON-NLS-1$
	}

	private static Pattern CT_ID_PATTERN = Pattern.compile("\\{[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}\\}"); //$NON-NLS-1$
	private static Pattern BOOLEAN_PATTERN = Pattern.compile("True|False"); //$NON-NLS-1$
	private static Pattern NUMERIC_PATTERN = Pattern.compile("\\d+(.\\d)?\\d?"); //$NON-NLS-1$
	
	public static boolean isCtId(String value) {
		if (value != null) {
			return CT_ID_PATTERN.matcher(value).matches();
		}
		return false;
	}
	
	public static MappedAttributeType getKnownType(String attrName) {
		return KNOWN_ATTRIBUTES.get(attrName);
	}
	
	public AttributeTypeGuess getAttributeType(ResultSet valRs) throws SQLException {
		AttributeTypeGuess guess = new AttributeTypeGuess();
		MappedAttributeType type = MappedAttributeType.IGNORE;
		while (valRs.next()) {
			String value = valRs.getString(1);
			if (value != null && !value.isEmpty()) {
				guess.valueSet.add(value);
				switch (type) {
					case IGNORE:
					case BOOL:
						if (BOOLEAN_PATTERN.matcher(value).matches()) {
							type = MappedAttributeType.BOOL;
							break;
						}
					case NUMERIC:
						if (NUMERIC_PATTERN.matcher(value).matches()) {
							type = MappedAttributeType.NUMERIC;
							break;
						}
						type = MappedAttributeType.TEXT;
					case TEXT:
						if (CT_ID_PATTERN.matcher(value).matches()) {
							type = MappedAttributeType.REF;
							break;
						}
					default:
						break;
				}
			}
		}
		guess.type = type;
		
		if (MappedAttributeType.TEXT.equals(type) && guess.valueSet.size() < 20) {
			guess.type = MappedAttributeType.REF;
		}
		
		valRs.close();
		return guess;
	}

	public SmartMapping create(Connection c) throws SQLException {
		ResultSet attrRs = c.createStatement().executeQuery("select n, id from CSV_TO_SMART.ATTRIBUTES"); //$NON-NLS-1$
		SmartMapping csv2Smart = new SmartMapping();
		while (attrRs.next()) {
			String attrName = attrRs.getString(1);
			System.out.println("Processing: " + attrName);
			MappedAttribute ctAttr = new MappedAttribute();
			ctAttr.setI(attrName);
			ctAttr.setN(attrName);
			csv2Smart.getMappedAttribute().add(ctAttr);
			MappedAttributeType type = getKnownType(attrName);
			if (type != null) {
				ctAttr.setType(type);
				continue;
			}
			String valuesSql = "select distinct a"+attrRs.getString(2)+" from CSV_TO_SMART.CSV"; //$NON-NLS-1$ //$NON-NLS-2$
			ResultSet valRs = c.createStatement().executeQuery(valuesSql);
			AttributeTypeGuess guess = getAttributeType(valRs);

			ctAttr.setType(guess.type);
			if (MappedAttributeType.REF.equals(guess.type)) {
				for (String s : guess.valueSet) {
					MappedAttributeValue ctAttrValue = new MappedAttributeValue();
					ctAttrValue.setI(s);
					ctAttrValue.setN(s);
					ctAttrValue.setMapTo("???");
					ctAttr.getMappedAttributeValue().add(ctAttrValue);
				}
			}
			
		}
		attrRs.close();
		return csv2Smart;
	}	
	
	private class AttributeTypeGuess {
		public Set<String> valueSet = new HashSet<String>();
		public MappedAttributeType type = MappedAttributeType.IGNORE;
	}
}
