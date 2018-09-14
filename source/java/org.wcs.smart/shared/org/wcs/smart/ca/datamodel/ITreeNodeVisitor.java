package org.wcs.smart.ca.datamodel;

public interface ITreeNodeVisitor {

	/**
	 * return false if children should not be visited. Parent is visited first
	 * followed by all children
	 * @param node
	 * @return
	 */
	public boolean visit(AttributeTreeNode node);
}
