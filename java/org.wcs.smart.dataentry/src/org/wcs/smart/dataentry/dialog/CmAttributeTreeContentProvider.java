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
package org.wcs.smart.dataentry.dialog;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;

/**
 * Content provided for an configurable model attribute tree
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class CmAttributeTreeContentProvider implements ITreeContentProvider {

	private CmTreeRootNode root ;
	private List<CmAttributeTreeNode> rootNodes;
	private boolean active;
	private boolean showRoot;

	/**
	 * 
	 * Creates an attribute tree content provider that provides both
	 * inactive and active nodes and starts with a 
	 * root element.
	 *
	 */  
	public CmAttributeTreeContentProvider(){
		this(false, true);
	}
	
	/**
	 * 
	 * @param active <code>true</code> if only active elements to be included; otherwise <code>false</code> includes all
	 * @param showRoot <code>true</code> if tree should start with a "Root" element, false if it should just
	 * start with children
	 *
	 */
	public CmAttributeTreeContentProvider(boolean active, boolean showRoot){
		root = new CmTreeRootNode();
		this.active = active;
		this.showRoot = showRoot;
	}
	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof CmAttribute){
			if (active){
				rootNodes = ((CmAttribute) newInput).getActiveTreeNodes();
			}else{
				rootNodes = ((CmAttribute) newInput).getCurrentTree();
			}
		}else if (newInput instanceof List){
			this.rootNodes = (List<CmAttributeTreeNode>) newInput;
		}
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (showRoot){
			return new CmTreeRootNode[]{root};
		}else{
			return getChildren(root);
		}
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		List<CmAttributeTreeNode> kids = null;
		if (parentElement instanceof CmTreeRootNode){
			kids = rootNodes;
		}
		if (parentElement instanceof CmAttributeTreeNode){
			if (!active){
				kids = ((CmAttributeTreeNode)parentElement).getChildren();
			}else{
				kids = ((CmAttributeTreeNode)parentElement).getActiveChildren();
			}
		}
		if (kids != null && kids.size() > 0){
			return kids.toArray();
		}
		return new Object[]{};
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof CmAttributeTreeNode){
			if (((CmAttributeTreeNode)element).getParent() == null){
				return root;
			}
			return ((CmAttributeTreeNode)element).getParent();
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		Object[] children = getChildren(element);
		if (children == null){
			return false;
		}
		return children.length > 0;
	}
	
	/**
	 * Empty class to represent root node of data tree.
	 * 
	 */
	public class CmTreeRootNode{}
	
}
