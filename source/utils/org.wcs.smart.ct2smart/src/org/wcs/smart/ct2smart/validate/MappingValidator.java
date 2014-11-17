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
package org.wcs.smart.ct2smart.validate;

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

import org.wcs.smart.ct2smart.dao.ConnectionUtil;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeValue;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.ct2smart.matcher.model.CtCategory;
import org.wcs.smart.ct2smart.matcher.model.CtCategoryMap;
import org.wcs.smart.ct2smart.ui.DataModelLookup;
import org.wcs.smart.ct2smart.ui.support.AttributeTreeKeyLookup;
import org.wcs.smart.ct2smart.util.Ct2AttributeTypeUtil;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryAttributeLink;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.ListNode;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class MappingValidator {
	
	public List<String> validate(Ct2Smart ct2Smart, DataModelLookup lookup) {
		List<String> errors = new ArrayList<String>();
		for (Ct2Attribute cta : ct2Smart.getCt2Attribute()) {
			if (!Ct2AttributeTypeUtil.canMap(cta.getType()))
				continue;
			
			String key = cta.getMapTo();
			if (key == null || key.isEmpty()) {
				errors.add(MessageFormat.format("No mapping is specified for attribute \"{0}\"", cta.getN()));
				continue;
			}

			AttributeType a = lookup.getAttribute(key);
			if (a == null) {
				errors.add(MessageFormat.format("Attribute \"{0}\" is mapped to key \"{1}\" which do not exist in datamodel", cta.getN(), key));
				continue;
			}
			
			Set<String> valueKeysSet = buildValueKeysSet(a);
			for (Ct2AttributeValue ctv : cta.getCt2AttributeValue()) {
				if (Boolean.TRUE.equals(ctv.isIgnore())) {
					continue;
				}
				String vkey = ctv.getMapTo();
				if (vkey == null || vkey.isEmpty()) {
					errors.add(MessageFormat.format("No mapping is specified for attribute value \"{0}\" in attribute \"{1}\"", ctv.getN(), cta.getN()));
					continue;
				}
				
				if (!valueKeysSet.contains(vkey)) {
					errors.add(MessageFormat.format("Attribute value \"{0}\" in attribute \"{1}\" has mapped to key \"{2}\" which do not exist in datamodel", ctv.getN(), cta.getN(), vkey));
					continue;
				}
			}
			
			String catKey = cta.getCategoryKey();
			if (catKey != null) {
				//attributed is mapped to specific category -> check if it exists in datamodel in this category
				CategoryType c = lookup.getCategory(catKey);
				if (c == null) {
					errors.add(MessageFormat.format("Attribute \"{0}\" with key \"{1}\" is mapped to category \"{2}\" which do not exist in datamodel", cta.getN(), cta.getMapTo(), catKey));
					continue;
				}
				Set<String> dmKeys = getInnerAttributeKeys(c, lookup);
				if (!dmKeys.contains(cta.getMapTo())) {
					errors.add(MessageFormat.format("Attribute \"{0}\" with key \"{1}\" is mapped to category \"{2}\" but this category has no reference for this attribute in datamodel", cta.getN(), cta.getMapTo(), catKey));
					continue;
				}

			}
		}

		AttributeLookup aLookup = new AttributeLookup(ConnectionUtil.getConnection());
		for (CtCategory ctc : ct2Smart.getCtCategory()) {
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
				errors.add(MessageFormat.format("Category \"{0}\" is mapped to key \"{1}\" which do not exist in datamodel", pairsToString(ctc), key));
				continue;
			}
			
			//validating structure
			List<String> catIs = new ArrayList<String>();
			List<Ct2Attribute> attrToMap = new ArrayList<Ct2Attribute>();
			for (Ct2Attribute a : ct2Smart.getCt2Attribute()) {
				if (Ct2AttributeTypeUtil.canMap(a.getType()) && a.getMapTo() != null && a.getCategoryKey() == null) {
					attrToMap.add(a);
				} else if (Ct2AttributeType.CATEGORY.equals(a.getType())) {
					catIs.add(a.getI());
				}
			}
			Map<String, Ct2Attribute> rqAttrsMap = getRequiredAttributes(ctc, catIs, attrToMap, aLookup);
			Set<String> dmKeys = rqAttrsMap.keySet();
			dmKeys.removeAll(getInnerAttributeKeys(c, lookup));
			for (String dmKey : dmKeys) {
				Ct2Attribute a = rqAttrsMap.get(dmKey);
				errors.add(MessageFormat.format("Category \"{0}\" in datamodel do not contain attribute with key \"{1}\" while correspondent data exist in mapping file", ctc.getCategoryKey(), a.getMapTo()));
			}
			
		}
		return errors;
	}

	private Map<String, Ct2Attribute> getRequiredAttributes(CtCategory ctc, List<String> catIs, List<Ct2Attribute> attrToMap, AttributeLookup aLookup) {
		Connection c = ConnectionUtil.getConnection();
		Map<String, Ct2Attribute> rqAttrsMap = new HashMap<String, Ct2Attribute>();
		Map<String, CtCategoryMap> iCatMap = new HashMap<String, CtCategoryMap>();
		for (CtCategoryMap cmap : ctc.getCtCategoryMap()) {
			iCatMap.put(cmap.getAi(), cmap);
		}
		StringBuilder catClause = new StringBuilder();
		for (String catI : catIs) {
			catClause.append(catClause.length() != 0 ? " and " : "where ");  //$NON-NLS-1$//$NON-NLS-2$
			catClause.append(" a").append(aLookup.getColumn(catI));  //$NON-NLS-1$
			CtCategoryMap cmap = iCatMap.get(catI);
			if (cmap != null) {
				catClause.append("='").append(cmap.getVi()).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				catClause.append(" is null"); //$NON-NLS-1$
			}
		}
		
		for (Ct2Attribute cta : attrToMap) {
			try {
				ResultSet rs = c.createStatement().executeQuery("select count (distinct a"+aLookup.getColumn(cta.getI())+") from ct_to_smart.csv " + catClause); //$NON-NLS-1$ //$NON-NLS-2$
				rs.next();
				if (rs.getInt(1) > 0) {
					rqAttrsMap.put(cta.getMapTo(), cta);
				}
				rs.close();
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

	private String pairsToString(CtCategory ctc) {
		StringBuilder sb = new StringBuilder();
		for (CtCategoryMap m : ctc.getCtCategoryMap()) {
			sb.append(m.getAn()).append("=").append(m.getVn()).append(" | "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return sb.toString();
	}
	
}
