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
package org.wcs.smart.query.common.ui.itempanel;

/**
 * Node for the ItemTreeNodeTree which wraps a sub-item with it's parent item.
 * This is so we know what content provider and label provider to use to get the
 * children and labe.
 * 
 * @author Emily
 * 
 */
public class WrappedTreeNode {

	private IItemTreeNode parent;
	private Object item;

	public WrappedTreeNode(IItemTreeNode parent, Object item) {
		this.parent = parent;
		this.item = item;
	}

	public boolean equals(Object other) {
		if (other instanceof WrappedTreeNode) {
			return ((WrappedTreeNode) other).item.equals(item);
		}
		return false;
	}

	public int hashCode() {
		return item.hashCode();
	}
	
	public Object getItem(){
		return this.item;
	}
	
	public IItemTreeNode getParent(){
		return this.parent;
	}
}

