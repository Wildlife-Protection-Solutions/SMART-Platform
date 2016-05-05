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
import java.util.List;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Util class for configurable model custom trees manipulations
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CmCustomTreesUtil {

	public static List<CmAttributeTreeNode> buildCustomTree(ConfigurableModel model, CmAttribute cmAttr, Attribute dmAttr) {
		return buildCustomTree(model, cmAttr, dmAttr, null, null);
	}
	
	private static List<CmAttributeTreeNode> buildCustomTree(ConfigurableModel model, CmAttribute cmAttr, Attribute dmAttr, CmAttributeTreeNode cmParent, AttributeTreeNode dmParent) {
		List<AttributeTreeNode> source = dmParent != null ? dmParent.getActiveChildren() : dmAttr.getActiveTreeNodes();
		List<CmAttributeTreeNode> result = new ArrayList<>(source.size());
		for (AttributeTreeNode dmNode : source) {
			CmAttributeTreeNode cmNode = new CmAttributeTreeNode();
			cmNode.setConfigurableModel(model);
			cmNode.setDmTreeNode(dmNode);
			cmNode.setIsActive(dmNode.getIsActive());
			cmNode.setParent(cmParent);
			cmNode.setNodeOrder(dmNode.getNodeOrder());
			cmNode.setAttribute(cmAttr);
			cmNode.setChildren(buildCustomTree(model, cmAttr, dmAttr, cmNode, dmNode));
			result.add(cmNode);
		}
		return result;
	}
	
}
