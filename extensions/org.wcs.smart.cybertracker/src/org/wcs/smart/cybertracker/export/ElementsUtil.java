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

import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.model.elements.Elements;

/**
 * Util for manipulations with {@link Elements} object
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ElementsUtil {

	public static Elements buildEmptyElements() {
		Elements elements = new Elements();
		Elements.List list = new Elements.List();
		elements.setList(list);
		Elements.List.Items items = new Elements.List.Items();
		list.setItems(items);
		return elements;
	}
	
	/**
	 * Simply add all from the map to elements
	 * @param elements
	 * @param map
	 */
	public static void addElements(Elements elements, Map<? extends DmObject, CyberTrackerId> map) {
		Set<? extends DmObject> keys = map.keySet();
		for (DmObject dmObject : keys) {
			addElementsItem(elements, dmObject.getName(), map.get(dmObject).getItemId());
		}
	}
	
	/**
	 * For given labels function:
	 *  - creates items
	 *	- adds them to elements
	 *  - returns the list of item ids
	 * @param elements
	 * @return
	 */
	public static List<CyberTrackerId> addCustomElements(Elements elements, String... labels) {
		List<CyberTrackerId> idList = new ArrayList<CyberTrackerId>();
		for (String string : labels) {
			CyberTrackerId id = new CyberTrackerId();
			addElementsItem(elements, string, id.getItemId());
			idList.add(id);
		}
		return idList;
	}

	public static List<CyberTrackerId> addCustomElements(Elements elements, List<String> labels, List<String> tag0Values) {
		List<CyberTrackerId> idList = new ArrayList<CyberTrackerId>();
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
		addElementsItem(elements, name, id, null);
	}

	public static void addElementsItem(Elements elements, String name, String id, String tag0) {
		Elements.List.Items.Item item = new Elements.List.Items.Item();
		item.setName(name);
		item.setId(id);
		item.setTag0(tag0);
		elements.getList().getItems().getItem().add(item);
	}
	
}
