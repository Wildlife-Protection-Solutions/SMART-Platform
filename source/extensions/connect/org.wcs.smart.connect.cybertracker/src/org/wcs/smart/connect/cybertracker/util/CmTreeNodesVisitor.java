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
package org.wcs.smart.connect.cybertracker.util;

import java.util.List;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Utility class that allows to quickly go through {@link ConfigurableModel}
 * using its tree representation and visit each node including tree nodes and list items
 * in a way they are displayed in GUI.
 * This mean that based on attribute configuration it may go through default list/tree or custom.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CmTreeNodesVisitor {

	public void visit(ConfigurableModel model, INodeVisitHandler handler) {
		visitNodes(handler, model.getNodes());
	}

	private void visitNodes(INodeVisitHandler handler, List<CmNode> nodes) {
		for (CmNode cmNode : nodes) {
			handler.handle(cmNode, null);
			for (CmAttribute attr : cmNode.getCmAttributes()) {
				handler.handle(attr, null);
				visitList(handler, attr, attr.getCurrentList());
				visitTree(handler, attr, attr.getCurrentTree());
			}
			visitNodes(handler, cmNode.getChildren());
		}
	}
	
	private void visitList(INodeVisitHandler handler, CmAttribute attr, List<CmAttributeListItem> lists) {
		for (CmAttributeListItem item : lists) {
			handler.handle(item, attr);
		}
	}
	
	private void visitTree(INodeVisitHandler handler, CmAttribute attr, List<CmAttributeTreeNode> trees) {
		for (CmAttributeTreeNode node : trees) {
			handler.handle(node, attr);
			visitTree(handler, attr, node.getChildren());
		}
	}
	
	public static interface INodeVisitHandler {
		public void handle(UuidItem item, CmAttribute attribute);
	}

}
