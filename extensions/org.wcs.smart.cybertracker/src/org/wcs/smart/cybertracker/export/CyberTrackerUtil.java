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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.screens.Node;

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

	public static Category buildRoot(DataModel dataModel) {
		Category fakeRoot = new Category();
		fakeRoot.setName("Data Model"); //$NON-NLS-1$
		fakeRoot.setChildren(dataModel.getCategories());
		return fakeRoot;
		//TODO: switch back to original full datamodel
//		List<Category> cats = new ArrayList<Category>();
//		cats.add(dataModel.getCategories().get(0));
//		fakeRoot.setChildren(cats);
//		return fakeRoot;
	}

	public static Map<Category, CyberTrackerId> buildMap(Category category) {
		Map<Category, CyberTrackerId> map = new HashMap<Category, CyberTrackerId>();
		map.put(category, new CyberTrackerId());
		mapCategories(category.getChildren(), map);
		return map;
	}

	private static void mapCategories(List<Category> categories, Map<Category, CyberTrackerId> map) {
		for (Category category : categories) {
			map.put(category, new CyberTrackerId());
			if (category.getChildren() != null) {
				mapCategories(category.getChildren(), map);
			}
		}
	}

	public static Map<AttributeTreeNode, CyberTrackerId> buildTreeNodeMap(List<AttributeTreeNode> treeNodes) {
		Map<AttributeTreeNode, CyberTrackerId> map = new HashMap<AttributeTreeNode, CyberTrackerId>();
		mapTreeNodes(treeNodes, map);
		return map;
	}

	private static void mapTreeNodes(List<AttributeTreeNode> treeNodes, Map<AttributeTreeNode, CyberTrackerId> map) {
		if (treeNodes == null)
			return;
		for (AttributeTreeNode attrTreeNode : treeNodes) {
			map.put(attrTreeNode, new CyberTrackerId());
			if (attrTreeNode.getActiveChildren() != null && !attrTreeNode.getActiveChildren().isEmpty()) {
				mapTreeNodes(attrTreeNode.getChildren(), map);
			}
		}
	}
	
	public static Node createRadioNode(Category category, Map<Category, CyberTrackerId> keyMap) {
		String id = keyMap.get(category).getNodeId();
		String name = category.getName();
		List<CyberTrackerId> childIds = getChildrenIds(category.getChildren(), keyMap);
		List<String> values = listItemIds(childIds);
		String trElements = translateElements(childIds);
		String trLinks = translateLinks(childIds, true);
		return ScreensObjectFactory.createNodeRadio(id, name, values, trElements, trLinks, null);
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
	public static Node createRadioNode(String id, String name, List<CyberTrackerId> childIds, String resultElement) {
		List<String> values = listItemIds(childIds);
		String trElements = translateElements(childIds);
		String trLinks = translateLinks(childIds, resultElement == null);
		return ScreensObjectFactory.createNodeRadio(id, name, values, trElements, trLinks, resultElement);
	}
	
	public static List<CyberTrackerId> getChildrenIds(List<?> objects, Map<?, CyberTrackerId> keyMap) {
		List<CyberTrackerId> result = new ArrayList<CyberTrackerId>();
		if (objects == null)
			return result;
		for (Object child : objects) {
			result.add(keyMap.get(child));
		}
		return result;
	}
	
	public static List<String> listItemIds(List<CyberTrackerId> ids) {
		List<String> result = new ArrayList<String>();
		for (CyberTrackerId id : ids) {
			result.add(id.getItemId());
		}
		return result;
	}
	
	public static String translateElements(List<CyberTrackerId> ids) {
		StringBuilder elements = new StringBuilder(); 
		for (CyberTrackerId id : ids) {
			elements.append(id.getItemTranslatedId());
		}
		return elements.toString();
	}

	public static String translateLinks(List<CyberTrackerId> ids, boolean linkToNode) {
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
	
}
