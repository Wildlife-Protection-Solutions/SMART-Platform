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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wcs.smart.conversion.csv.lookup.AttributeLookup;
import org.wcs.smart.conversion.csv.ui.CsvMatcherDialog;
import org.wcs.smart.conversion.lookup.AttributeTreeKeyLookup;
import org.wcs.smart.conversion.lookup.Ct2SmartLookup;
import org.wcs.smart.conversion.lookup.Ct2SmartLookup.Ct2AttributeValuePair;
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
	
	private static final Logger logger = LogManager.getLogger(CsvMatcherDialog.class); 
	
	public List<String> validate(SmartMapping ct2Smart, DataModelLookup lookup) {
		List<String> errors = new ArrayList<String>();
		
		errors.addAll(validateRequiredMetaPresent(ct2Smart));

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
			for (CategoryMap ctm : ctc.getCategoryMap()) {
				if (ctm.getVi() == null || ctm.getVi().isEmpty()) {
					errors.add("\"vi\" should not be null or empty. Do not declare <CategoryMap> tag if given attribute value is empty or null.");
					continue;
				}
			}
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
		
		errors.addAll(validateAllMappingsPresent(ct2Smart));
		return errors;
	}

	private List<String> validateRequiredMetaPresent(SmartMapping ct2Smart) {
		boolean date = false;
		boolean objectId = false;
		List<String> errors = new ArrayList<>();
		
		for (MappedAttribute a : ct2Smart.getMappedAttribute()) {
			if (a.getType() == null) {
				errors.add(MessageFormat.format("Type is not set for attribute ''{0}''.", a.getI()));
				continue;
			}
			switch (a.getType()) {
			case META_OBJECT_ID:
				objectId = true;
				break;
			case WP_DATE:
				date = true;
				break;
			default:
				break;
			}
		}
		
		if (!date) {
			errors.add("No 'Date' found in mapping. Mapping must contain a date field.");
		}
		if (!objectId) {
			errors.add("No 'Patrol/Mission ID' found in mapping. Mapping must contain at least one Patrol/Mission ID field.");
		}
		return errors;
	}

	private List<String> validateAllMappingsPresent(SmartMapping ct2Smart) {
		List<String> errors = new ArrayList<>();

		List<MappedAttribute> categoryAttributes = new ArrayList<>();
		for (MappedAttribute attr : ct2Smart.getMappedAttribute()) {
			if (MappedAttributeType.CATEGORY.equals(attr.getType())) {
				categoryAttributes.add(attr);
			}
		}
		
		if (categoryAttributes.isEmpty()) {
			errors.add("No attributes mapped to category are defined. You should have at least one attribute that indicates the category from datamodel which will be used to record each observation.");
			return errors;
		}

		try {
			Connection c = ConnectionUtil.getConnection();
			//get ids for category attributes
			logger.debug("get ids for category attributes"); //$NON-NLS-1$
			HashMap<String, String> n2Col = new HashMap<String, String>();
			PreparedStatement ps = c.prepareStatement("select id, n from csv_to_smart.attributes"); //$NON-NLS-1$
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String col = "a" + rs.getString(1); //$NON-NLS-1$
				n2Col.put(rs.getString(2), col);
			}
			rs.close();
			
			//extract all possible set of category attribute values
			logger.debug("extract all possible set of category attribute values"); //$NON-NLS-1$
			StringBuilder sb = new StringBuilder();
			for (MappedAttribute attr : categoryAttributes) {
				String col = n2Col.get(attr.getI());
				if (col != null) {
					if (sb.length() > 0) {
						sb.append(", "); //$NON-NLS-1$
					}
					sb.append(col);
				} else {
					errors.add(MessageFormat.format("Attribute with key ''{0}'' is mapped to category that do not exist in database", Ct2AttributeTypeUtil.getN(attr)));
				}
			}
			if (!errors.isEmpty()) {
				return errors;
			}
			
			Ct2SmartLookup lookup = new Ct2SmartLookup(ct2Smart);
			List<Ct2AttributeValuePair> pairs = new ArrayList<>();
			String sql = "select distinct "+sb+" from csv_to_smart.csv";  //$NON-NLS-1$ //$NON-NLS-2$
			logger.debug("run SQL: " + sql); //$NON-NLS-1$
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				pairs.clear();
				for (int i = 0; i < categoryAttributes.size(); i++) {
					String v = rs.getString(i+1);
					if (v != null && !v.isEmpty()) {
						Ct2AttributeValuePair pair = new Ct2AttributeValuePair();
						pair.attribute = categoryAttributes.get(i);
						pair.value = v;
						pairs.add(pair);
					}
				}
				MappedCategory mc = lookup.findCategory(pairs);
				if (mc == null) {
					errors.add(MessageFormat.format("No category mapping is specified for items {0}", Arrays.toString(pairs.toArray())));
				}
			}
			rs.close();
		} catch (Exception e) {
			errors.add("Unable to validate that all category mappings present due to SQLException. See console or log for details");
			logger.error("Unable to validate that all category mappings present", e); //$NON-NLS-1$
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
			logger.error("Unable to validate that all attributes are mapped", e); //$NON-NLS-1$
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
		List<String> catClauseValues = new ArrayList<String>();
		for (String catI : catIs) {
			catClause.append(" and a").append(aLookup.getColumn(catI));  //$NON-NLS-1$
			CategoryMap cmap = iCatMap.get(catI);
			if (cmap != null) {
				catClause.append("=?"); //$NON-NLS-1$
				catClauseValues.add(cmap.getVi());
			} else {
				catClause.append(" is null"); //$NON-NLS-1$
			}
		}
		
		for (MappedAttribute cta : attrToMap) {
			try {
				Integer colId = aLookup.getColumn(cta.getI());
				if (colId != null) {
					String sql = "select count (distinct a"+colId+") from csv_to_smart.csv where a"+colId+ " is not null and a"+colId+" <> ''" + catClause; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					logger.debug("run SQL: " + sql); //$NON-NLS-1$
					PreparedStatement pst = c.prepareStatement(sql);
					for (int i = 0; i < catClauseValues.size(); i++) {
						pst.setString(i+1, catClauseValues.get(i));
					}
					ResultSet rs = pst.executeQuery();
					rs.next();
					if (rs.getInt(1) > 0) {
						rqAttrsMap.put(cta.getMapTo(), cta);
					}
					rs.close();
				} else {
					logger.warn(MessageFormat.format("WARN: Mapping file contains attribute i=''{0}'' that do not present in database", cta.getI()));
				}
			} catch (SQLException e) {
				logger.error("Error getting required attributes during validation", e); //$NON-NLS-1$
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
