package org.wcs.smart.ca.datamodel;

public interface ICategoryVisitor {

	/**
	 * return false if children should not be visited. Parent is visited first
	 * followed by all children
	 * @param node
	 * @return
	 */
	public boolean visit(Category category);
}
