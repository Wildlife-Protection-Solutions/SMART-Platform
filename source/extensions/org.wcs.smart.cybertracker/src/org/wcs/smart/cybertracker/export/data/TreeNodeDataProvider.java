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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.cybertracker.util.LanguageUtil;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.DisplayMode;

/**
 * Class provides data related to tree attributes nodes for CT export engine
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class TreeNodeDataProvider {
	
	List<IAttributeTreeNodeProxy> data;	
	private Language language;
	
	public TreeNodeDataProvider(CmAttribute attribute, ConfigurableModel configurableModel, Language language, Session session) {
		this.language = language;
		data = wrapTreeNodes(session, attribute.getCurrentTree(), configurableModel);
	}

	private List<IAttributeTreeNodeProxy> wrapTreeNodes(Session session, List<CmAttributeTreeNode> nodesList, ConfigurableModel configurableModel) {
		List<IAttributeTreeNodeProxy> result = new ArrayList<IAttributeTreeNodeProxy>();
		for (CmAttributeTreeNode cmItem : nodesList) {
			if (cmItem != null && cmItem.getIsActive()) {
				CommonTreeNodeProxy proxy = new CmTreeNodeProxy(cmItem);
				List<IAttributeTreeNodeProxy> children = wrapChildTreeNodes(session, cmItem, configurableModel);
				proxy.setActiveChildren(children);
				result.add(proxy);
			}
		}
		return result;
	}
	
	private List<IAttributeTreeNodeProxy> wrapChildTreeNodes(Session session, CmAttributeTreeNode node, ConfigurableModel configurableModel) {
		List<CmAttributeTreeNode> children = node.getChildren();
		if (children == null || children.isEmpty())
			return null;
		return wrapTreeNodes(session, children, configurableModel);
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
	
	private class CmTreeNodeProxy extends CommonTreeNodeProxy {
		private CmAttributeTreeNode item;

		public CmTreeNodeProxy(CmAttributeTreeNode item) {
			this.item = item;
		}

		@Override
		public String getName() {
			String name = LanguageUtil.getName(item, language);
			if (name == null || name.isEmpty()) {
				return LanguageUtil.getName(item.getDmTreeNode(), language);
			}
			return name;
		}

		@Override
		public UUID getUuid() {
			AttributeTreeNode dmNode = item.getDmTreeNode();
			return dmNode != null ? dmNode.getUuid() : null;
		}

		@Override
		public DisplayMode getDisplayMode() {
			return item.getDisplayMode();
		}

		@Override
		public File getImageFile() {
			return item.getImageFile();
		}
	}
	
}
