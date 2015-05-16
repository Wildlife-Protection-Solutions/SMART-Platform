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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.wcs.smart.conversion.csv.lookup.AttributeLookup;
import org.wcs.smart.conversion.lookup.AttributeTreeKeyLookup;
import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.model.CategoryMap;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeType;
import org.wcs.smart.conversion.model.MappedAttributeValue;
import org.wcs.smart.conversion.model.MappedCategory;
import org.wcs.smart.conversion.model.SmartMapping;
import org.wcs.smart.conversion.util.ConnectionUtil;
import org.wcs.smart.conversion.util.Ct2AttributeTypeUtil;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryAttributeLink;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.ListNode;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class MappingValidator {
	
	public List<String> validate(SmartMapping ct2Smart, DataModelLookup lookup) {
		List<String> errors = new ArrayList<String>();
		
		errors.addAll(validateAllMapped(ct2Smart));
		
		for (MappedAttribute cta : ct2Smart.getMappedAttribute()) {
			if (!Ct2AttributeTypeUtil.canMap(cta.getType()))
				continue;
			
			String key = cta.getMapTo();
			if (key == null || key.isEmpty()) {
				errors.add(MessageFormat.format("No mapping is specified for attribute \"{0}\"", Ct2AttributeTypeUtil.getN(cta)));
				continue;
			}

			AttributeType a = lookup.getAttribute(key);
			if (a == null) {
				errors.add(MessageFormat.format("Attribute \"{0}\" is mapped to key \"{1}\" which does not exist in datamodel", Ct2AttributeTypeUtil.getN(cta), key));
				continue;
			}
			
			Set<String> valueKeysSet = buildValueKeysSet(a);
			for (MappedAttributeValue ctv : cta.getMappedAttributeValue()) {
				if (Boolean.TRUE.equals(ctv.isIgnore())) {
					continue;
				}
				String vkey = ctv.getMapTo();
				if (vkey == null || vkey.isEmpty()) {
					errors.add(MessageFormat.format("No mapping is specified for attribute value \"{0}\" in attribute \"{1}\"", Ct2AttributeTypeUtil.getN(ctv), Ct2AttributeTypeUtil.getN(cta)));
					continue;
				}
				
				if (!valueKeysSet.contains(vkey)) {
					errors.add(MessageFormat.format("Attribute value \"{0}\" in attribute \"{1}\" has mapped to key \"{2}\" which does not exist in datamodel", Ct2AttributeTypeUtil.getN(ctv), Ct2AttributeTypeUtil.getN(cta), vkey));
					continue;
				}
			}
			
			String catKey = cta.getCategoryKey();
			if (catKey != null) {
				//attributed is mapped to specific category -> check if it exists in datamodel in this category
				CategoryType c = lookup.getCategory(catKey);
				if (c == null) {
					errors.add(MessageFormat.format("Attribute \"{0}\" with key \"{1}\" is mapped to category \"{2}\" which does not exist in datamodel", Ct2AttributeTypeUtil.getN(cta), cta.getMapTo(), catKey));
					continue;
				}
				Set<String> dmKeys = getInnerAttributeKeys(c, lookup);
				if (!dmKeys.contains(cta.getMapTo())) {
					errors.add(MessageFormat.format("Attribute \"{0}\" with key \"{1}\" is mapped to category \"{2}\" but this category has no reference for this attribute in datamodel", Ct2AttributeTypeUtil.getN(cta), cta.getMapTo(), catKey));
					continue;
				}

			}
		}

		AttributeLookup aLookup = new AttributeLookup(ConnectionUtil.getConnection());
		for (MappedCategory ctc : ct2Smart.getMappedCategory()) {
			if (Boolean.TRUE.equals(ctc.isIgnore())) {
				continue;
			}
			String key = ctc.getCategoryKey();
			if (key == null || key.isEmpty()) {
				errors.add(MessageFormat.format("No mapping is specified for category set: {0}", pairsToString(ctc)));
				continue;
			}
			
			CategoryType c = lookup.getCategory(key);
			if (c == null) {
				errors.add(MessageFormat.format("Category \"{0}\" is mapped to key \"{1}\" which does not exist in datamodel", pairsToString(ctc), key));
				continue;
			}
			
			//validating structure
			List<String> catIs = new ArrayList<String>();
			List<MappedAttribute> attrToMap = new ArrayList<MappedAttribute>();
			for (MappedAttribute a : ct2Smart.getMappedAttribute()) {
				if (Ct2AttributeTypeUtil.canMap(a.getType()) && a.getMapTo() != null && a.getCategoryKey() == null) {
					attrToMap.add(a);
				} else if (MappedAttributeType.CATEGORY.equals(a.getType())) {
					catIs.add(a.getI());
				}
			}
			Map<String, MappedAttribute> rqAttrsMap = getRequiredAttributes(ctc, catIs, attrToMap, aLookup);
			Set<String> dmKeys = rqAttrsMap.keySet();
			dmKeys.removeAll(getInnerAttributeKeys(c, lookup));
			for (String dmKey : dmKeys) {
				MappedAttribute a = rqAttrsMap.get(dmKey);
				errors.add(MessageFormat.format("Category \"{0}\" in datamodel do not contain attribute with key \"{1}\" while correspondent data exist in mapping file", ctc.getCategoryKey(), a.getMapTo()));
			}
			
		}
		return errors;
	}

	private List<String> validateAllMapped(SmartMapping ct2Smart) {
		List<String> errors = new ArrayList<String>();
		try {
			Set<String> aSet = new TreeSet<>();
			for (MappedAttribute attr : ct2Smart.getMappedAttribute()) {
				aSet.add(attr.getI());
			}
			Connection c = ConnectionUtil.getConnection();
			ResultSet rs = c.createStatement().executeQuery("select n from csv_to_smart.attributes");
			while (rs.next()) {
				String a = rs.getString(1);
				if (!aSet.contains(a)) {
					errors.add(MessageFormat.format("Attribute ''{0}'' present in loaded csv file but do not have correspodence in mapping file", a));
				}
			}
		} catch (SQLException e) {
			errors.add("Unable to validate that all attributes are mapped due to SQLException. See console or log for details");
			e.printStackTrace();
		}
		return errors;
	}

	private Map<String, MappedAttribute> getRequiredAttributes(MappedCategory ctc, List<String> catIs, List<MappedAttribute> attrToMap, AttributeLookup aLookup) {
		Connection c = ConnectionUtil.getConnection();
		Map<String, MappedAttribute> rqAttrsMap = new HashMap<String, MappedAttribute>();
		Map<String, CategoryMap> iCatMap = new HashMap<String, CategoryMap>();
		for (CategoryMap cmap : ctc.getCategoryMap()) {
			iCatMap.put(cmap.getAi(), cmap);
		}
		StringBuilder catClause = new StringBuilder();
		for (String catI : catIs) {
			catClause.append(" and a").append(aLookup.getColumn(catI));  //$NON-NLS-1$
			CategoryMap cmap = iCatMap.get(catI);
			if (cmap != null) {
				catClause.append("='").append(cmap.getVi()).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				catClause.append(" is null"); //$NON-NLS-1$
			}
		}
		
		for (MappedAttribute cta : attrToMap) {
			try {
				Integer colId = aLookup.getColumn(cta.getI());
				if (colId != null) {
					ResultSet rs = c.createStatement().executeQuery("select count (distinct a"+colId+") from csv_to_smart.csv where a"+colId+ " is not null and a"+colId+" <> ''" + catClause); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					rs.next();
					if (rs.getInt(1) > 0) {
						rqAttrsMap.put(cta.getMapTo(), cta);
					}
					rs.close();
				} else {
					System.out.println(MessageFormat.format("WARN: Mapping file contains attribute i=''{0}'' that do not present in database", cta.getI()));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return rqAttrsMap;
	}

	private Set<String> getInnerAttributeKeys(CategoryType c, DataModelLookup lookup) {
		Set<String> keys = new HashSet<String>();
		while (c != null) {
			for (CategoryAttributeLink link : c.getAttributes()) {
				keys.add(link.getAttributekey());
			}
			c = lookup.getParent(c); //as some attributes might be assigned to parent
		}
		return keys;
	}
	
	private Set<String> buildValueKeysSet(AttributeType a) {
		Set<String> result = new HashSet<String>();
		for (ListNode node : a.getValues()) {
			result.add(node.getKey());
		}
		AttributeTreeKeyLookup treeNodeLookup = new AttributeTreeKeyLookup(a);
		result.addAll(treeNodeLookup.getKeysSet());
		if ("BOOLEAN".equals(a.getType())) { //$NON-NLS-1$
			result.add("True"); //$NON-NLS-1$
			result.add("False"); //$NON-NLS-1$
		}
		return result;
	}

	private String pairsToString(MappedCategory ctc) {
		StringBuilder sb = new StringBuilder();
		for (CategoryMap m : ctc.getCategoryMap()) {
			sb.append(Ct2AttributeTypeUtil.getAn(m)).append("=").append(Ct2AttributeTypeUtil.getVn(m)).append(" | "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return sb.toString();
	}
	
}
