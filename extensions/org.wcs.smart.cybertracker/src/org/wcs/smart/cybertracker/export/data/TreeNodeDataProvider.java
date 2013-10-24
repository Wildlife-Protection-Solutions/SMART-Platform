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
package org.wcs.smart.cybertracker.export.data;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Class provides data related to tree attributes nodes for CT export engine
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class TreeNodeDataProvider {
	
	List<IAttributeTreeNodeProxy> data;	
	
	public TreeNodeDataProvider(Attribute attribute, ConfigurableModel configurableModel, Session session) {
		List<AttributeTreeNode> activeTreeNodes = attribute.getActiveTreeNodes();
		if (activeTreeNodes != null) {
			data = wrapTreeNodes(session, activeTreeNodes, configurableModel);
		}
	}

	private List<IAttributeTreeNodeProxy> wrapTreeNodes(Session session, List<AttributeTreeNode> nodesList, ConfigurableModel configurableModel) {
		List<IAttributeTreeNodeProxy> result = new ArrayList<IAttributeTreeNodeProxy>();
		CommonTreeNodeProxy proxy = null;
		for (AttributeTreeNode dmItem : nodesList) {
			CmAttributeTreeNode cmItem = getCmTreeNode(session, dmItem, configurableModel);
			proxy = null;
			if (cmItem != null) {
				if (cmItem.getIsActive()) {
					proxy = new CmTreeNodeProxy(cmItem);
				}
			} else {
				proxy = new DmTreeNodeProxy(dmItem);
			}
			if (proxy != null) {
				List<IAttributeTreeNodeProxy> children = wrapChildTreeNodes(session, dmItem, configurableModel);
				proxy.setActiveChildren(children);
				result.add(proxy);
			}
		}
		return result;
	}
	
	private List<IAttributeTreeNodeProxy> wrapChildTreeNodes(Session session, AttributeTreeNode node, ConfigurableModel configurableModel) {
		List<AttributeTreeNode> activeChildren = node.getActiveChildren();
		if (activeChildren == null || activeChildren.isEmpty())
			return null;
		return wrapTreeNodes(session, activeChildren, configurableModel);
	}
	
	private CmAttributeTreeNode getCmTreeNode(Session session, AttributeTreeNode element, ConfigurableModel configurableModel) {
		if (configurableModel.getUuid() == null)
			return null;
		List<?> items = session.createCriteria(CmAttributeTreeNode.class)
				.add(Restrictions.eq("dmTreeNode", element))  //$NON-NLS-1$
				.add(Restrictions.eq("configurableModel", configurableModel)).list();  //$NON-NLS-1$
		if (items.size() > 0) {
			return (CmAttributeTreeNode) items.get(0);
		}
		return null;
	}
	
	public List<IAttributeTreeNodeProxy> getActiveTreeNodes() {
		return data;
	}

	private abstract class CommonTreeNodeProxy implements IAttributeTreeNodeProxy {
		private List<IAttributeTreeNodeProxy> children;
		@Override
		public List<IAttributeTreeNodeProxy> getActiveChildren() {
			return children;
		}
		
		public void setActiveChildren(List<IAttributeTreeNodeProxy> children) {
			this.children = children;
		}
	}
	
	
	private class DmTreeNodeProxy extends CommonTreeNodeProxy {
		private AttributeTreeNode item;

		public DmTreeNodeProxy(AttributeTreeNode item) {
			this.item = item;
		}

		@Override
		public String getName() {
			return item.getName();
		}

		@Override
		public byte[] getUuid() {
			return item.getUuid();
		}
	}
	
	private class CmTreeNodeProxy extends CommonTreeNodeProxy {
		private CmAttributeTreeNode item;

		public CmTreeNodeProxy(CmAttributeTreeNode item) {
			this.item = item;
		}

		@Override
		public String getName() {
			String name = item.getName();
			if (name == null || name.isEmpty()) {
				return item.getDmTreeNode().getName();
			}
			return name;
		}

		@Override
		public byte[] getUuid() {
			return item.getDmTreeNode().getUuid();
		}
	}
	
}
