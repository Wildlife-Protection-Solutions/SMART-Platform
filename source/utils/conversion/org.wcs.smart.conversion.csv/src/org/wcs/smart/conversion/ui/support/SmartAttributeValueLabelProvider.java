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
package org.wcs.smart.conversion.ui.support;

import org.wcs.smart.conversion.lookup.AttributeTreeKeyLookup;
import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.model.ExtraAttribute;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.ListNode;
import org.wcs.smart.internal.ca.datamodel.xml.generate.NameType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class SmartAttributeValueLabelProvider extends LangColumnLabelProvider {

	private DataModelLookup lookup;

	public SmartAttributeValueLabelProvider(DataModelLookup lookup) {
		super();
		this.lookup = lookup;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof ListNode) {
			return getName((ListNode) element);

		} else if (element instanceof TreeNodeType) {
			return getName((TreeNodeType) element);
			
		} else if (element instanceof ExtraAttribute) {
			ExtraAttribute a = (ExtraAttribute) element;
			return getNameForKey(a.getAttributeKey(), a.getValueKey());
		}
		return super.getText(element);
	}
	
	protected String getNameForKey(String attrKey, String valueKey) {
		AttributeType a = lookup.getAttribute(attrKey);
		if (a == null || valueKey == null)
			return ""; //$NON-NLS-1$
		String type = a.getType();
		if ("LIST".equals(type)) { //$NON-NLS-1$
			for (ListNode node : a.getValues()) {
				if (valueKey.equals(node.getKey()))
					return getName(node);
			}
			
		} else if ("TREE".equals(type)) { //$NON-NLS-1$
			AttributeTreeKeyLookup treeNodeLookup = new AttributeTreeKeyLookup(a);
			return getName(treeNodeLookup.getTreeNode(valueKey));

		} else if ("NUMERIC".equals(type) || "TEXT".equals(type)) { //$NON-NLS-1$ //$NON-NLS-2$
			return valueKey != null ? valueKey : ""; //$NON-NLS-1$
			
		} else if ("BOOLEAN".equals(type)) { //$NON-NLS-1$
			return valueKey != null ? valueKey : ""; //$NON-NLS-1$;
		}
		return "???"; //should never happen //$NON-NLS-1$
	}

	private String getName(ListNode node) {
		if (node == null)
			return "?"; //TODO: empty string?
		String langCode = getLanguageCode();
		for (NameType nameType : node.getNames()) {
			if (langCode.equals(nameType.getLanguageCode()))
				return nameType.getValue();
		}
		if (!node.getNames().isEmpty())
			return node.getNames().get(0).getValue();
		return "???"; //should never happen //$NON-NLS-1$
	}

	private String getName(TreeNodeType node) {
		if (node == null)
			return "?"; //TODO: empty string?
		String langCode = getLanguageCode();
		for (NameType nameType : node.getNames()) {
			if (langCode.equals(nameType.getLanguageCode()))
				return nameType.getValue();
		}
		if (!node.getNames().isEmpty())
			return node.getNames().get(0).getValue();
		return "???"; //should never happen //$NON-NLS-1$
	}
	
}
