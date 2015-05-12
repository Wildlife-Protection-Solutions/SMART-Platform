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
package org.wcs.smart.conversion.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;

public class AttributeTreeKeyLookup {

	private Map<String, TreeNodeType> key2Node = new HashMap<String, TreeNodeType>();
	private Map<TreeNodeType, String> node2Key = new HashMap<TreeNodeType, String>();
	
	public AttributeTreeKeyLookup(AttributeType a) {
		rebuildTreeMaps("", a.getTrees()); //$NON-NLS-1$
	}
	
	private void rebuildTreeMaps(String parentKey, List<TreeNodeType> children) {
		for (TreeNodeType child : children) {
			String key = parentKey.isEmpty() ? "" : parentKey + ".";  //$NON-NLS-1$//$NON-NLS-2$
			key += child.getKey();
			key2Node.put(key, child);
			node2Key.put(child, key);
			rebuildTreeMaps(key, child.getChildrens());
		}
	}
	
	public String getFullKey(TreeNodeType node) {
		return node2Key.get(node);
	}

	public TreeNodeType getTreeNode(String fullKey) {
		return key2Node.get(fullKey);
	}
	
	public Set<String> getKeysSet() {
		return key2Node.keySet();
	}
}
