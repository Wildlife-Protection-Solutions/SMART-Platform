
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
package org.wcs.smart.patrol.query.ui.itempanel;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.patrol.model.PatrolAttributeTreeNode;
import org.wcs.smart.patrol.query.model.PatrolAttributeQueryOption;

/**
 * Tree content provider for patrol options.  This simply takes
 * an array of objects as input and returns them as the
 * root elements.  There are no children.
 * 
 * @author Emily
 *
 */
public class PatrolOptionContentProvider implements ITreeContentProvider {

	private Object[] ops;
	private boolean isGroupBy;
	
	public PatrolOptionContentProvider() {
		this(false);		
	}
	
	public PatrolOptionContentProvider(boolean isGroupBy) {
		this.isGroupBy = isGroupBy;
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.ops = (Object[])newInput;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return ops;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (!isGroupBy) return null;
		if (parentElement instanceof PatrolAttributeQueryOption pa) {
			if (pa.getPatrolAttribute().getType() == Attribute.AttributeType.TREE) {
				return pa.getPatrolAttribute().getAttributeTree().toArray();
			}
		}
		if (parentElement instanceof PatrolAttributeTreeNode node) {
			if (node.getChildren() != null) return node.getChildren().toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (!isGroupBy) return null;
		if (element instanceof PatrolAttributeTreeNode node) {
			if (node.getParent() != null) return node.getParent();
			return node.getAttribute();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (!isGroupBy) return false;
		if (element instanceof PatrolAttributeQueryOption pa) {
			if (pa.getPatrolAttribute().getType() == Attribute.AttributeType.TREE) {
				return true;
			}
		}
		if (element instanceof PatrolAttributeTreeNode node) {
			return node.getChildren() != null && !node.getChildren().isEmpty();
		}
		return false;
	}

	
	
}
