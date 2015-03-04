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
