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

import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
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
//		fakeRoot.setChildren(dataModel.getCategories());
		//TODO: switch back to original full datamodel
		List<Category> cats = new ArrayList<Category>();
		cats.add(dataModel.getCategories().get(0));
		fakeRoot.setChildren(cats);
		return fakeRoot;
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

	public static Node createRadioNode(Category category, Map<Category, CyberTrackerId> keyMap) {
		String id = keyMap.get(category).getNodeId();
		String name = category.getName();
		List<String> values = listNodeValues(category, keyMap);
		String trElements = translateElements(category, keyMap);
		String trLinks = translateLinks(category, keyMap);
		return ScreensObjectFactory.createNodeRadio(id, name, values, trElements, trLinks);
	}

	private static List<String> listNodeValues(Category category, Map<Category, CyberTrackerId> keyMap) {
		if (category.getChildren() == null || category.getChildren().isEmpty())
			return null;
		List<String> result = new ArrayList<String>();
		for (Category child : category.getChildren()) {
			result.add(keyMap.get(child).getItemId());
		}
		return result;
	}

	private static String translateElements(Category category, Map<Category, CyberTrackerId> keyMap) {
		StringBuilder elements = new StringBuilder(); 
		for (Category child : category.getChildren()) {
			CyberTrackerId id = keyMap.get(child);
			elements.append(id.getItemTranslatedId());
		}
		return elements.toString();
	}

	private static String translateLinks(Category category, Map<Category, CyberTrackerId> keyMap) {
		StringBuilder links = new StringBuilder(); 
		for (Category child : category.getChildren()) {
			CyberTrackerId id = keyMap.get(child);
			links.append(id.getItemTranslatedId()).append(id.getNodeTranslatedId());
		}
		return links.toString();
	}
	
}
