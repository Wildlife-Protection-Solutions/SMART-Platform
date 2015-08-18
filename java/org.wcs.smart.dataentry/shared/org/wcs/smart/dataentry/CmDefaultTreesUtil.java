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
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Util class for configurable model default trees manipulations
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class CmDefaultTreesUtil {

	public static Set<Attribute> getPresentedTreeAttributes(CmNode node) {
		Queue<CmNode> toCheck = new LinkedList<CmNode>();
		toCheck.add(node);
		return getPresentedTreeAttributes(toCheck);
	}
	
	public static Set<Attribute> getPresentedTreeAttributes(ConfigurableModel model) {
		Queue<CmNode> toCheck = new LinkedList<CmNode>();
		toCheck.addAll(model.getNodes());
		return getPresentedTreeAttributes(toCheck);
	}

	private static Set<Attribute> getPresentedTreeAttributes(Queue<CmNode> toCheck) {
		Set<Attribute> result = new HashSet<Attribute>();
		while(!toCheck.isEmpty()) {
			CmNode node = toCheck.remove();
			if (node.getCmAttributes() != null) {
				for (final CmAttribute a : node.getCmAttributes()) {
					if (AttributeType.TREE.equals(a.getAttribute().getType())) {
						result.add(a.getAttribute());
					}
				}
			}
			toCheck.addAll(node.getChildren());
		}
		return result;
	}
	
	public static List<CmAttributeTreeNode> buildDefaultTree(ConfigurableModel model, Attribute a) {
		return buildDefaultTree(model, a, null, null, null);
	}
	
//	private static List<CmAttributeTreeNode> buildDefaultTree(ConfigurableModel model, Attribute a, CmAttributeTreeNode cmParent, AttributeTreeNode dmParent) {
//		List<CmAttributeTreeNode> result = new ArrayList<CmAttributeTreeNode>();
//		List<AttributeTreeNode> source = dmParent != null ? dmParent.getActiveChildren() : a.getActiveTreeNodes();
//		for (AttributeTreeNode dmNode : source) {
//			CmAttributeTreeNode cmNode = new CmAttributeTreeNode();
//			cmNode.setConfigurableModel(model);
//			cmNode.setDmTreeNode(dmNode);
//			cmNode.setIsActive(dmNode.getIsActive());
//			cmNode.setParent(cmParent);
//			cmNode.setNodeOrder(dmNode.getNodeOrder());
//			cmNode.setDmAttribute(a);
//			cmNode.setChildren(buildDefaultTree(model, a, cmNode, dmNode));
//			result.add(cmNode);
//		}
//		return result;
//	}

	/**
	 * Upgrades tree mapping used in 3.1.0 and previous versions to 3.2.0
	 * @param oldNodes
	 * @return
	 */
	public static List<CmAttributeTreeNode> upgradeDefaultTrees(ConfigurableModel m, List<CmAttributeTreeNode> oldNodes) {
		List<CmAttributeTreeNode> result = new ArrayList<CmAttributeTreeNode>();
		Map<AttributeTreeNode, CmAttributeTreeNode> preMapping = new HashMap<AttributeTreeNode, CmAttributeTreeNode>();
		for (CmAttributeTreeNode cmNode : oldNodes) {
			preMapping.put(cmNode.getDmTreeNode(), cmNode);
		}
		Set<Attribute> existingTrees = CmDefaultTreesUtil.getPresentedTreeAttributes(m);
		
		for (Attribute a : existingTrees) {
			List<CmAttributeTreeNode> defTree = buildDefaultTree(m, a, null, null, preMapping);
			result.addAll(defTree);
		}
		
		return result;
	}

	private static List<CmAttributeTreeNode> buildDefaultTree(ConfigurableModel model, Attribute a, CmAttributeTreeNode cmParent, AttributeTreeNode dmParent, Map<AttributeTreeNode, CmAttributeTreeNode> preMapping) {
		List<CmAttributeTreeNode> result = new ArrayList<CmAttributeTreeNode>();
		List<AttributeTreeNode> source = dmParent != null ? dmParent.getActiveChildren() : a.getActiveTreeNodes();
		for (AttributeTreeNode dmNode : source) {
			CmAttributeTreeNode cmNode = preMapping == null ? null : preMapping.get(dmNode);
			if (cmNode == null) {
				cmNode = new CmAttributeTreeNode();
				cmNode.setConfigurableModel(model);
				cmNode.setDmTreeNode(dmNode);
				cmNode.setIsActive(dmNode.getIsActive());
			}
			cmNode.setParent(cmParent);
			cmNode.setNodeOrder(dmNode.getNodeOrder());
			cmNode.setDmAttribute(a);
			cmNode.setChildren(buildDefaultTree(model, a, cmNode, dmNode, preMapping));
			result.add(cmNode);
		}
		return result;
	}
	
}
