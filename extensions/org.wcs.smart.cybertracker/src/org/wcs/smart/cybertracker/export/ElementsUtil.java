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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wcs.smart.ca.Language;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.util.LanguageUtil;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.util.SmartUtils;

/**
 * Util for manipulations with {@link Elements} object
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ElementsUtil {

	public static final String BOOL_TRUE = "true"; //$NON-NLS-1$
	public static final String BOOL_FALSE = "false"; //$NON-NLS-1$
	
	public static final String NULL_VALUE = "null"; //$NON-NLS-1$

	public static final String ATTRIBUTE_ELEMENT_TAG = "a"; //$NON-NLS-1$
	public static final String CATEGORY_ELEMENT_TAG = "c"; //$NON-NLS-1$
	public static final String MEMBER_ELEMENT_TAG = "m"; //$NON-NLS-1$
	public static final String MULISELECT_ELEMENT_TAG = "s"; //$NON-NLS-1$
	public static final String DEFAULT_VALUES_ELEMENT_TAG = "d"; //$NON-NLS-1$
	
	public static Elements buildEmptyElements() {
		Elements elements = new Elements();
		Elements.List list = new Elements.List();
		elements.setList(list);
		Elements.List.Items items = new Elements.List.Items();
		list.setItems(items);
		return elements;
	}
	
	public static List<CyberTrackerId> addCustomElements(Elements elements, String... labels) {
		List<CyberTrackerId> idList = new ArrayList<CyberTrackerId>();
		for (String string : labels) {
			CyberTrackerId id = new CyberTrackerId();
			addElementsItem(elements, string, id.getItemId());
			idList.add(id);
		}
		return idList;
	}
	
	/**
	 * For given labels function:
	 *  - creates items
	 *	- adds them to elements
	 *  - returns the list of item ids
	 * @param elements
	 * @return
	 */
	public static List<CyberTrackerId> addCustomElements(Elements elements, List<String> labels, List<String> tag0Values) {
		List<CyberTrackerId> idList = new ArrayList<CyberTrackerId>();
		if (labels.size() != tag0Values.size()) {
			//development validation
			throw new IllegalArgumentException("Lables and Tag0 lists are expected to be of an equal size."); //$NON-NLS-1$
		}
		int size = labels.size() > tag0Values.size() ? tag0Values.size() : labels.size(); //size of smallest array (it is expected that arrays are of same size!!!)
		for (int i = 0; i < size; i++) {
			String label = labels.get(i);
			String tag0 = tag0Values.get(i);
			CyberTrackerId id = new CyberTrackerId();
			addElementsItem(elements, label, id.getItemId(), tag0);
			idList.add(id);
		}
		return idList;
	}
	
	public static void addElementsItem(Elements elements, String name, String id) {
		addElementsItem(elements, name, id, null, null, null, null, null);
	}

	public static void addElementsItem(Elements elements, String name, String id, String tag0) {
		addElementsItem(elements, name, id, tag0, null, null, null, null);
	}

	public static void addElementsItem(Elements elements, String name, String id, String tag0, String tag1) {
		addElementsItem(elements, name, id, tag0, tag1, null, null, null);
	}

	public static void addElementsItem(Elements elements, String name, String id, String tag0, String tag1, String tag2) {
		addElementsItem(elements, name, id, tag0, tag1, tag2, null, null);
	}
	
	public static void addElementsItem(Elements elements, String name, String id, String tag0, String tag1, String tag2, String tag3, String tag4) {
		Elements.List.Items.Item item = new Elements.List.Items.Item();
		item.setName(name);
		item.setId(id);
		item.setTag0(tag0);
		item.setTag1(tag1);
		item.setTag2(tag2);
		item.setTag3(tag3);
		item.setTag4(tag4);
		elements.getList().getItems().getItem().add(item);
	}

	public static CyberTrackerId buildAttributeNullElement(Elements elements, String name) {
		CyberTrackerId id = new CyberTrackerId();
		addElementsItem(elements, name, id.getItemId(), NULL_VALUE);
		return id;
	}
	
	public static List<CyberTrackerId> buildBooleanElements(Elements elements) {
		List<String> labels = new ArrayList<String>();
		labels.add(Messages.Elements_BooleanAttribute_Yes);
		labels.add(Messages.Elements_BooleanAttribute_No);
		List<String> tag0Values = new ArrayList<String>();
		tag0Values.add(BOOL_TRUE);
		tag0Values.add(BOOL_FALSE);
		return ElementsUtil.addCustomElements(elements, labels, tag0Values);
	}
	
	//-------------------------------------------------------
	public static void addNodeElements(Elements elements, Map<CmNode, CyberTrackerId> map, Language language) {
		Set<CmNode> keys = map.keySet();
		for (CmNode node : keys) {
			//no need for tag0 in case of group cause we should not relay on any uuids from configurable model
			String tag0 = node.isGroup() ? null : SmartUtils.encodeHex(node.getCategory().getUuid());
			addElementsItem(elements, LanguageUtil.getName(node, language), map.get(node).getItemId(), tag0);
		}
	}
	
}
