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
package org.wcs.smart.connect.dataentry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Map that allows to quickly find any element in {@link ConfigurableModel} based on its {@link UUID}.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CmElementsMap {

	private Map<UUID, UuidItem> map;

	public CmElementsMap(ConfigurableModel model) {
		map = new HashMap<>();
		if (model.getUuid() == null) {
			return;
		}
		map.put(model.getUuid(), model);
		mapNodes(model.getNodes());
		mapList(model.getDefaultLists());
		mapTree(model.getDefaultTrees());
	}

	private void mapNodes(List<CmNode> nodes) {
		for (CmNode cmNode : nodes) {
			map.put(cmNode.getUuid(), cmNode);
			if (cmNode.getCategory() != null) {
				map.put(cmNode.getCategory().getUuid(), cmNode.getCategory());
			}
			for (CmAttribute attr : cmNode.getCmAttributes()) {
				map.put(attr.getUuid(), attr);
				mapList(attr.getList());
				mapTree(attr.getTree());
			}
			mapNodes(cmNode.getChildren());
		}
	}
	
	private void mapList(List<CmAttributeListItem> lists) {
		for (CmAttributeListItem item : lists) {
			map.put(item.getUuid(), item);
		}
	}
	
	private void mapTree(List<CmAttributeTreeNode> trees) {
		for (CmAttributeTreeNode node : trees) {
			map.put(node.getUuid(), node);
			mapTree(node.getChildren());
		}
	}
	
	public UuidItem getUuidItem(UUID uuid) {
		if (uuid == null) {
			return null;
		}
		UuidItem obj = map.get(uuid);
		if (obj == null) {
			//just some development validation
			SmartPlugIn.log("Unexpected item requested from a configurable model map: " + uuid, null); //$NON-NLS-1$
		}
		return obj;
	}
	
	public CmAttribute getCmAttribute(UUID uuid) {
		UuidItem obj = getUuidItem(uuid);
		return obj instanceof CmAttribute ? (CmAttribute) obj : null;
	}

}
