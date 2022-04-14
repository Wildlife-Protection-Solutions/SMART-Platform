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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
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
	
	
	public static CmAttributeConfig buildDefaultTreeConfig(ConfigurableModel model, Attribute a) {
		CmAttributeConfig cfg = CmAttributeConfig.createConfig(model, a, true);
		List<CmAttributeTreeNode> tree = buildDefaultTree(cfg, a, null, null, null);
		cfg.setTree(tree);
		return cfg;
	}

	public static List<CmAttributeTreeNode> buildDefaultTree(CmAttributeConfig cfg, Attribute a) {
		return buildDefaultTree(cfg, a, null, null, null);
	}
	
	private static List<CmAttributeTreeNode> buildDefaultTree(CmAttributeConfig cfg, Attribute a, CmAttributeTreeNode cmParent, AttributeTreeNode dmParent, Map<AttributeTreeNode, CmAttributeTreeNode> preMapping) {
		List<CmAttributeTreeNode> result = new ArrayList<CmAttributeTreeNode>();
		List<AttributeTreeNode> source = null;
		
		//we don't use the getActiveChildren function here because it is not properly configured when
		//it's a newly created attribute.
		if (dmParent != null) {
			source = dmParent.getChildren().stream().filter(e->e.getIsActive()).collect(Collectors.toList());
		}else {
			source = a.getTree().stream().filter(e->e.getIsActive()).collect(Collectors.toList());
		}
		
		for (AttributeTreeNode dmNode : source) {
			CmAttributeTreeNode cmNode = preMapping == null ? null : preMapping.get(dmNode);
			if (cmNode == null) {
				cmNode = new CmAttributeTreeNode();
				cmNode.setDmTreeNode(dmNode);
				cmNode.setIsActive(dmNode.getIsActive());
			}
			cmNode.setParent(cmParent);
			cmNode.setNodeOrder(dmNode.getNodeOrder());
			cmNode.setConfig(cfg);
			cmNode.setChildren(buildDefaultTree(cfg, a, cmNode, dmNode, preMapping));
			result.add(cmNode);
		}
		return result;
	}
	
}
