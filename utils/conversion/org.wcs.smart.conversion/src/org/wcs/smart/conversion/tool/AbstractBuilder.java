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
package org.wcs.smart.conversion.tool;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.wcs.smart.conversion.lookup.Ct2SmartLookup;
import org.wcs.smart.conversion.lookup.Ct2SmartLookup.Ct2AttributeValuePair;
import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeType;
import org.wcs.smart.conversion.model.MappedCategory;
import org.wcs.smart.conversion.model.Param;
import org.wcs.smart.conversion.model.SmartMapping;
import org.wcs.smart.conversion.tag.TagA;
import org.wcs.smart.conversion.tag.TagS;

/**
 * Generic logic for builders.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public abstract class AbstractBuilder {

	private static final String LANGUAGE_CODE = "en"; //$NON-NLS-1$
	
	public static final int MAX_DURATION = 40;

	private Ct2SmartLookup lookup;
	private DataModelLookup dmLookup;
	private TeamMembersParser membersParser = new TeamMembersParser();
	private DateTimeParser dateTimeParser = new DateTimeParser();
	private Map<String, String> params;

	public AbstractBuilder(MatchSession session, DataModelLookup dmLookup) throws SQLException {
		this.dmLookup = dmLookup;
		lookup = new Ct2SmartLookup(session.getSmartMapping());
		params = extractParams(session.getSmartMapping());
	}

	protected Ct2SmartLookup getLookup() {
		return lookup;
	}
	
	protected DataModelLookup getDmLookup() {
		return dmLookup;
	}
	
	public static String getLanguageCode() {
		return LANGUAGE_CODE;
	}
	
	public DateTimeParser getDateTimeParser() {
		return dateTimeParser;
	}
	
	public TeamMembersParser getMembersParser() {
		return membersParser;
	}
	
	public String getParam(String key) {
		return params != null ? params.get(key) : null;
	}

	public String getParam(String key, String defaultValue) {
		String value = getParam(key);
		return value != null ? value : defaultValue;
	}
	
	protected MappedCategory getDefaultCategory(TagS s) {
		List<Ct2AttributeValuePair> data = new ArrayList<Ct2AttributeValuePair>();
		for (TagA a : s) {
			MappedAttribute cta = lookup.findAttribute(a.getI());
			if (cta != null && MappedAttributeType.CATEGORY.equals(cta.getType())) {
				Ct2AttributeValuePair pair = new Ct2AttributeValuePair();
				pair.attribute = cta;
				pair.value = a.getV();
				data.add(pair);
			}
		}
		MappedCategory c = lookup.findCategory(data);
		if (c == null) {
			System.err.println(MessageFormat.format("ERROR: Because no category mapping is specified for items {0} it is imposible to fetch category for row {1}", Arrays.toString(data.toArray()), s));
		}
		return c;
	}

	protected boolean isSame(Object o1, Object o2) {
		if (o1 == null) 
			return o2 == null;
		return o1.equals(o2);
	}

	private Map<String, String> extractParams(SmartMapping smartMapping) {
		Map<String, String> map = new HashMap<>();
		for (Param p : smartMapping.getParam()) {
			map.put(p.getKey(), p.getVal());
		}
		return map;
	}

	public boolean isValidDateRange(XMLGregorianCalendar from, XMLGregorianCalendar to) {
		if (from == null || to == null) {
			return false;
		}
		long diff = Math.abs(to.toGregorianCalendar().getTime().getTime() - from.toGregorianCalendar().getTime().getTime());
		diff = diff / (1000 * 60 * 60 * 24); //now diff is in days
		return diff < MAX_DURATION;
	}
}
