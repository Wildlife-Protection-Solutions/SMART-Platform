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
package org.wcs.smart.ui.properties;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;

/**
 * Content provided for an attribute tree 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTreeContentProvider implements ITreeContentProvider {

	private static RootNode root ;
	private Attribute attribute;

	private boolean active;
	private boolean showRoot;

	/**
	 * 
	 * @param active <code>true</code> if only active elements to be included; otherwise <code>false</code> includes all
	 *
	 */
	public AttributeTreeContentProvider(){
		this(false, true);
	}
	
	/**
	 * 
	 * @param active <code>true</code> if only active elements to be included; otherwise <code>false</code> includes all
	 * @param showRoot <code>true</code> if tree should start with a "Root" element, false if it should just
	 * start with children
	 *
	 */
	public AttributeTreeContentProvider(boolean active, boolean showRoot){
		root = new RootNode();
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
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.attribute = (Attribute)newInput;
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (showRoot){
			return new RootNode[]{root};
		}else{
			return getChildren(root);
		}
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RootNode){
			if (attribute.getTree() != null && attribute.getTree().size() > 0){
				ArrayList<AttributeTreeNode> nodes = new ArrayList<AttributeTreeNode>();
				if (!active){
					nodes.addAll(attribute.getTree());
				}else{
					for (AttributeTreeNode nd : attribute.getTree()){
						if (nd.getIsActive()){
							nodes.add(nd);
						}
					}
				}
				Collections.sort(nodes, new AttributeTreeNode.NodeComparator());
				return nodes.toArray();
			}
			return null;
		}
		if (parentElement instanceof AttributeTreeNode){
			if (((AttributeTreeNode)parentElement).getChildren() != null && ((AttributeTreeNode)parentElement).getChildren().size() > 0){
				
				ArrayList<AttributeTreeNode> nodes = new ArrayList<AttributeTreeNode>();
				if (!active){
					nodes.addAll( ((AttributeTreeNode)parentElement).getChildren()  );
				}else{
					for (AttributeTreeNode nd : ((AttributeTreeNode)parentElement).getChildren()){
						if (nd.getIsActive()){
							nodes.add(nd);
						}
					}
				}
				
				return nodes.toArray();
			}
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof AttributeTreeNode){
			if (((AttributeTreeNode)element).getParent() == null){
				return root;
			}
			return ((AttributeTreeNode)element).getParent();
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
		
//		if (element instanceof AttributeTreeNode){
//			return ((AttributeTreeNode)element).getChildren() != null && ((AttributeTreeNode)element).getChildren().size() > 0; 
//		}else if (element instanceof RootNode){
//			return attribute.getTree() != null && attribute.getTree().size() > 0;
//		}
//		return false;
	}
	
	/**
	 * Empty class to represent root node of data tree.
	 * 
	 */
	public class RootNode{}
}