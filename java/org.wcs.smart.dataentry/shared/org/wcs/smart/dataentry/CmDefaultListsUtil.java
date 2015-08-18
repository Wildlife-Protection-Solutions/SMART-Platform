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
package org.wcs.smart.dataentry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Util class for configurable model default lists manipulations
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class CmDefaultListsUtil {

	public static Set<Attribute> getPresentedListAttributes(CmNode node) {
		Queue<CmNode> toCheck = new LinkedList<CmNode>();
		toCheck.add(node);
		return getPresentedListAttributes(toCheck);
	}
	
	public static Set<Attribute> getPresentedListAttributes(ConfigurableModel model) {
		Queue<CmNode> toCheck = new LinkedList<CmNode>();
		toCheck.addAll(model.getNodes());
		return getPresentedListAttributes(toCheck);
	}

	private static Set<Attribute> getPresentedListAttributes(Queue<CmNode> toCheck) {
		Set<Attribute> result = new HashSet<Attribute>();
		while(!toCheck.isEmpty()) {
			CmNode node = toCheck.remove();
			if (node.getCmAttributes() != null) {
				for (final CmAttribute a : node.getCmAttributes()) {
					if (AttributeType.LIST.equals(a.getAttribute().getType())) {
						result.add(a.getAttribute());
					}
				}
			}
			toCheck.addAll(node.getChildren());
		}
		return result;
	}

	public static List<CmAttributeListItem> buildDefaultList(ConfigurableModel model, Attribute a) {
		return buildDefaultList(model, a, null);
	}
	
	private static List<CmAttributeListItem> buildDefaultList(ConfigurableModel model, Attribute a, Map<AttributeListItem, CmAttributeListItem> preMapping) {
		List<CmAttributeListItem> result = new ArrayList<CmAttributeListItem>();
		List<AttributeListItem> source = a.getActiveListItems();
		for (AttributeListItem dmNode : source) {
			CmAttributeListItem cmNode = preMapping == null ? null : preMapping.get(dmNode);
			if (cmNode == null) {
				cmNode = new CmAttributeListItem();
				cmNode.setConfigurableModel(model);
				cmNode.setListItem(dmNode);
				cmNode.setIsActive(dmNode.getIsActive());
			}
			cmNode.setListOrder(dmNode.getListOrder());
			cmNode.setDmAttribute(a);
			result.add(cmNode);
		}
		return result;
	}

	public static List<CmAttributeListItem> buildCustomList(ConfigurableModel model, CmAttribute cmAttribute) {
		List<CmAttributeListItem> result = new ArrayList<CmAttributeListItem>();
		List<AttributeListItem> source = cmAttribute.getAttribute().getActiveListItems();
		for (AttributeListItem dmNode : source) {
			CmAttributeListItem cmNode = new CmAttributeListItem();
			cmNode.setConfigurableModel(model);
			cmNode.setListItem(dmNode);
			cmNode.setIsActive(dmNode.getIsActive());
			cmNode.setListOrder(dmNode.getListOrder());
			cmNode.setAttribute(cmAttribute);
			result.add(cmNode);
		}
		return result;
	}
	
	/**
	 * Upgrades list mapping used in 3.1.0 and previous versions to 3.2.1
	 * @param oldNodes
	 * @return
	 */
	public static List<CmAttributeListItem> upgradeDefaultLists(ConfigurableModel m, List<CmAttributeListItem> oldNodes) {
		List<CmAttributeListItem> result = new ArrayList<CmAttributeListItem>();
		Map<AttributeListItem, CmAttributeListItem> preMapping = new HashMap<AttributeListItem, CmAttributeListItem>();
		for (CmAttributeListItem cmNode : oldNodes) {
			preMapping.put(cmNode.getListItem(), cmNode);
		}
		Set<Attribute> existingLists = CmDefaultListsUtil.getPresentedListAttributes(m);
		
		for (Attribute a : existingLists) {
			List<CmAttributeListItem> defList = buildDefaultList(m, a, preMapping);
			result.addAll(defList);
		}
		
		return result;
	}
}
