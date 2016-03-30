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
package org.wcs.smart.cybertracker.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.cybertracker.export.data.IAttributeTreeNodeProxy;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.util.LanguageUtil;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.DisplayMode;

/**
 * Util for CyberTracker xml data creation
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerUtil {
	public static class CyberTrackerId {
		private static String ITEM_PREFIX = "1C640427-4F44-4796-97A6-"; //$NON-NLS-1$
		private static String TRNS_PREFIX = "2704641C444F964797A6"; //$NON-NLS-1$

		private static long generatedId = System.currentTimeMillis();
		
		private String itemId;
		private String itemTranslatedId;
		private String nodeId;
		private String nodeTranslatedId;

		public CyberTrackerId() {
			generatedId++;
			String gidStr = String.valueOf(generatedId);
			String back =gidStr.substring(gidStr.length()-12);
			this.itemId = "{" + ITEM_PREFIX + back + "}";  //$NON-NLS-1$//$NON-NLS-2$
			this.itemTranslatedId = TRNS_PREFIX + back;
			
			generatedId++;
			gidStr = String.valueOf(generatedId);
			back =gidStr.substring(gidStr.length()-12);
			this.nodeId = "{" + ITEM_PREFIX + back + "}";  //$NON-NLS-1$//$NON-NLS-2$
			this.nodeTranslatedId = TRNS_PREFIX + back;

		}
		public String getItemId() {
			return itemId;
		}
		public String getItemTranslatedId() {
			return itemTranslatedId;
		}
		public String getNodeId() {
			return nodeId;
		}
		public String getNodeTranslatedId() {
			return nodeTranslatedId;
		}
	}

	private ScreensObjectFactory screensFactory;
	private Language currentLanguage;
	
	public CyberTrackerUtil(ScreensObjectFactory screensFactory, Language language) {
		this.screensFactory = screensFactory;
		currentLanguage = language;
	}
	
	public ScreensObjectFactory getScreensFactory() {
		return screensFactory;
	}

	public CyberTrackerPropertiesProfile getCtProperties() {
		return screensFactory.getCtProperties();
	}
	
	public Map<Category, CyberTrackerId> buildMap(Category category) {
		Map<Category, CyberTrackerId> map = new HashMap<Category, CyberTrackerId>();
		map.put(category, new CyberTrackerId());
		mapCategories(category.getActiveChildren(), map);
		return map;
	}

	private void mapCategories(List<Category> categories, Map<Category, CyberTrackerId> map) {
		for (Category category : categories) {
			map.put(category, new CyberTrackerId());
			if (category.getActiveChildren() != null) {
				mapCategories(category.getActiveChildren(), map);
			}
		}
	}

	public Map<IAttributeTreeNodeProxy, CyberTrackerId> buildTreeNodeMap(List<IAttributeTreeNodeProxy> treeNodes) {
		Map<IAttributeTreeNodeProxy, CyberTrackerId> map = new HashMap<IAttributeTreeNodeProxy, CyberTrackerId>();
		mapTreeNodes(treeNodes, map);
		return map;
	}

	private void mapTreeNodes(List<IAttributeTreeNodeProxy> treeNodes, Map<IAttributeTreeNodeProxy, CyberTrackerId> map) {
		if (treeNodes == null)
			return;
		for (IAttributeTreeNodeProxy attrTreeNode : treeNodes) {
			map.put(attrTreeNode, new CyberTrackerId());
			if (attrTreeNode.getActiveChildren() != null && !attrTreeNode.getActiveChildren().isEmpty()) {
				mapTreeNodes(attrTreeNode.getActiveChildren(), map);
			}
		}
	}
	
	/**
	 * Creates radio node with options from childIds.
	 * NOTE: If resultElement is not null this will be treated as final radio list (not as part of navigation in some tree)
	 * 
	 * @param id
	 * @param name
	 * @param childIds
	 * @param resultElement
	 * @return
	 */
	public Node createRadioNode(String id, String name, List<CyberTrackerId> childIds, String resultElement) {
		return createRadioNode(id, name, childIds, resultElement, resultElement == null);
	}

	public Node createRadioNode(String id, String name, List<CyberTrackerId> childIds, String resultElement, boolean linkToNode) {
		return createRadioNode(id, name, childIds, resultElement, linkToNode, DisplayMode.TEXT);
	}
	
	public Node createRadioNode(String id, String name, List<CyberTrackerId> childIds, String resultElement, boolean linkToNode, DisplayMode mode) {
		List<String> values = listItemIds(childIds);
		String trElements = translateElements(childIds);
		String trLinks = translateLinks(childIds, linkToNode);
		return screensFactory.createNodeRadio(id, name, values, trElements, trLinks, resultElement, mode);
	}
	
	public Node createRadioNode(String id, String name, List<CyberTrackerId> childIds, Collection<CyberTrackerId> childToLinkToNodeIds, String resultElement, DisplayMode mode) {
		List<String> values = listItemIds(childIds);
		String trElements = translateElements(childIds);
		String trLinks = translateLinks(childIds, childToLinkToNodeIds);
		return screensFactory.createNodeRadio(id, name, values, trElements, trLinks, resultElement, mode);
	}

	public List<CyberTrackerId> getChildrenIds(List<?> objects, Map<?, CyberTrackerId> keyMap) {
		List<CyberTrackerId> result = new ArrayList<CyberTrackerId>();
		if (objects == null)
			return result;
		for (Object child : objects) {
			result.add(keyMap.get(child));
		}
		return result;
	}
	
	public List<String> listItemIds(List<CyberTrackerId> ids) {
		List<String> result = new ArrayList<String>();
		for (CyberTrackerId id : ids) {
			result.add(id.getItemId());
		}
		return result;
	}
	
	public String translateElements(List<CyberTrackerId> ids) {
		StringBuilder elements = new StringBuilder(); 
		for (CyberTrackerId id : ids) {
			elements.append(id.getItemTranslatedId());
		}
		return elements.toString();
	}

	public String translateLinks(List<CyberTrackerId> ids, boolean linkToNode) {
		StringBuilder links = new StringBuilder(); 
		for (CyberTrackerId id : ids) {
			links.append(id.getItemTranslatedId());
			if (linkToNode) {
				links.append(id.getNodeTranslatedId());
			} else {
				links.append("00000000000000000000000000000000"); //$NON-NLS-1$
			}
		}
		return links.toString();
	}

	public String translateLinks(List<CyberTrackerId> ids, Collection<CyberTrackerId> childToLinkToNodeIds) {
		StringBuilder links = new StringBuilder(); 
		for (CyberTrackerId id : ids) {
			links.append(id.getItemTranslatedId());
			if (childToLinkToNodeIds.contains(id)) {
				links.append(id.getNodeTranslatedId());
			} else {
				links.append("00000000000000000000000000000000"); //$NON-NLS-1$
			}
		}
		return links.toString();
	}

//-------------------------------------------------------------	
	public CmNode buildRoot(ConfigurableModel model) {
		CmNode fakeRoot = new CmNode();
		fakeRoot.setModel(model);
		fakeRoot.setName(model.getName());
		fakeRoot.setNames(model.getNames());
		fakeRoot.setChildren(model.getNodes());
		return fakeRoot;
	}

	public Map<CmNode, CyberTrackerId> buildMap(CmNode node) {
		Map<CmNode, CyberTrackerId> map = new HashMap<CmNode, CyberTrackerId>();
		map.put(node, new CyberTrackerId());
		mapNodes(node.getChildren(), map);
		return map;
	}

	private void mapNodes(List<CmNode> nodes, Map<CmNode, CyberTrackerId> map) {
		for (CmNode node : nodes) {
			map.put(node, new CyberTrackerId());
			if (node.getChildren() != null) {
				mapNodes(node.getChildren(), map);
			}
		}
	}
	
	public Node createRadioNode(CmNode node, Map<CmNode, CyberTrackerId> keyMap, String resultElementId) {
		String id = keyMap.get(node).getNodeId();
		String name = getName(node);
		List<CyberTrackerId> childIds = getChildrenIds(node.getChildren(), keyMap);
		List<String> values = listItemIds(childIds);
		String trElements = translateElements(childIds);
		String trLinks = translateLinks(childIds, true);
		return screensFactory.createNodeRadio(id, name, values, trElements, trLinks, resultElementId, node.getDisplayMode());
	}
	
	public String getName(NamedItem i) {
		return LanguageUtil.getName(i, currentLanguage);
	}
	
	public Node createSaveNode(CyberTrackerId id, CyberTrackerId saveTargetId, String title, String msg, boolean takeGpsReading) {
		Node saveNode = screensFactory.createNodeMsgText(id.getNodeId(), title, msg);
		//disable next button, enable save button, navigate on save to target point
		Control control2 = ScreensObjectFactory.getNavigationControl(saveNode);
		control2.setShowNext("False"); //$NON-NLS-1$
		control2.setShowMajor("True"); //$NON-NLS-1$
		control2.setTranslateMajorScreenId(saveTargetId.getNodeId());
		control2.setTakeGPS(takeGpsReading ? "True" : "False"); //$NON-NLS-1$ //$NON-NLS-2$
		return saveNode;
	}
	
}
