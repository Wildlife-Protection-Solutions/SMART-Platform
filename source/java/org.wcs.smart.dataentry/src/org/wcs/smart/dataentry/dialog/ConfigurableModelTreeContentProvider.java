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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;


/**
 * Content provided for configurable data model tree.
 * <p>Also provides ability to display a single string
 * as a single tree node by setting the input to a string. Useful
 * if you want to disply "loading..." while loading data model in 
 * background.</p>
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ConfigurableModelTreeContentProvider implements ITreeContentProvider {

	private CmRootNode rootNode = new CmRootNode();
	private boolean showRoot = false;
	private boolean showAttributes = true;
	private String message = null;
	
	private Map<CmNode, List<?>> kids;
	/**
	 * Creates a new content provider
	 * 
	 * @param showRoot if the root is to be displayed
	 */
	public ConfigurableModelTreeContentProvider(boolean showRoot) {
		this(showRoot, true);
	}
	
	/**
	 * Creates a new content provider 
	 * @param showRoot if the root is to be displayed
	 * @param showAttributes if attributes are to be included
	 */
	public ConfigurableModelTreeContentProvider(boolean showRoot, boolean showAttributes) {
		super();
		this.showRoot = showRoot;
		this.showAttributes = showAttributes;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		rootNode.model = null;
		message = null;
		if (newInput instanceof ConfigurableModel){
			rootNode.model = (ConfigurableModel) newInput;
			
			kids = new HashMap<>();
		}else if (newInput instanceof String){
			message = (String) newInput;
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ConfigurableModel) {
			return showRoot ? new Object[]{rootNode} : getChildren(inputElement);
		}else if (inputElement instanceof String){
			return new String[]{message};
		}
		return showRoot ? new Object[]{rootNode} : getChildren(rootNode.model);
	}

	public void addToGroup(List<CmAttribute> attributes) {
		CmNode parent = null;
		
		for (CmAttribute item : attributes) {
			if (parent == null) {
				parent = item.getNode();
			}else {
				if (parent != item.getNode()) return;
			}
			if (item.getAttribute().getType() == Attribute.AttributeType.DATE ||
					item.getAttribute().getType() == Attribute.AttributeType.TREE ||
					item.getAttribute().getType() == Attribute.AttributeType.MLIST) {
				return;
			}
			
		}

		for (CmAttribute x : attributes) {
			x.setGrouped(Boolean.TRUE);
		}
		kids.remove(parent);
		reorder(parent);
	}
	

	
	public void reset(CmNode parent) {
		kids.remove(parent);
	}
	
	private void reorder(CmNode parent) {
		getChildren(parent);
		//re-order nodes
		int cnt = 1;
		for (Object kid : kids.get(parent)) {
			if (kid instanceof CmAttribute) {
				((CmAttribute)kid).setOrder(cnt++);
			}else {
				for (CmAttribute kkid : ((MatrixNode)kid).getKids()) {
					kkid.setOrder(cnt++);
				}
			}
		}
	}
	
	public void removeFromGroup(List<CmAttribute> attributes) {
		CmNode parent = null;
	
		for (CmAttribute item : attributes) {
			if (parent == null) {
				parent = item.getNode();
			}else {
				if (parent != item.getNode()) return;
			}
		}

		for (CmAttribute x : attributes) {
			x.setGrouped(Boolean.FALSE);
		}
		kids.remove(parent);
		reorder(parent);
	}
	
	public MatrixNode findGroupNode(CmNode parent) {
		if (!kids.containsKey(parent)) getChildren(parent);
		for (Object x : kids.get(parent)) {
			if (x instanceof MatrixNode) return (MatrixNode) x;
		}
		return null;
	}
	
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof CmNode) {
			CmNode n = (CmNode) parentElement;
			List<CmNode> nodes = n.getChildren();
			if (nodes != null && !nodes.isEmpty()) {
				return nodes.toArray();
			}
			if (showAttributes){
				if (kids.get(parentElement) != null) return kids.get(parentElement).toArray();
				
				List<CmAttribute> attributes = n.getCmAttributes();
				if (attributes == null || attributes.isEmpty()) return new Object[] {};
				
				List<Object> lkids = new ArrayList<>();
				MatrixNode mNode = null;
				for (CmAttribute attribute : n.getCmAttributes()) {
					if (attribute.isGrouped()) {
						if (mNode == null) {
							mNode = new MatrixNode((CmNode)parentElement);
							lkids.add(mNode);
						}
						mNode.addKid(attribute);
						
					}else {
						lkids.add(attribute);
					}
				}
				kids.put((CmNode)parentElement, lkids);
				return lkids.toArray();
			}
		}
		if (parentElement instanceof MatrixNode) {
			return ((MatrixNode)parentElement).getKids().toArray();
		}
		if (parentElement instanceof ConfigurableModel) {
			ConfigurableModel cm = (ConfigurableModel) parentElement;
			List<CmNode> nodes = cm.getNodes();
			return nodes == null ? new Object[]{} : nodes.toArray();
		}
		if (parentElement instanceof CmRootNode) {
			return getChildren(((CmRootNode) parentElement).model);
		}
		return new Object[]{};
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof CmNode) {
			CmNode n = (CmNode) element;
			return n.getParent();
		} else if (element instanceof CmAttribute) {
			CmAttribute cma = (CmAttribute) element;
			if (!kids.containsKey(cma.getNode())) getChildren(cma.getNode());
				
			if (kids.get(cma.getNode()).contains(cma)) return cma.getNode();
			for (Object x : kids.get(cma.getNode())) {
				if (x instanceof MatrixNode) return x;
			}
			
			return cma.getNode();
		} else if (element instanceof MatrixNode) {
			return ((MatrixNode)element).parent;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof CmNode) {
			CmNode n = (CmNode) element;
			List<CmNode> nodes = n.getChildren();
			List<CmAttribute> attributes = null;
			if (showAttributes){
				attributes = n.getCmAttributes();
			}
			return (nodes != null && !nodes.isEmpty()) || (attributes != null && !attributes.isEmpty());
		}
		if (element instanceof MatrixNode) {
			return true;
		}
		if (element instanceof ConfigurableModel) {
			ConfigurableModel cm = (ConfigurableModel) element;
			List<CmNode> nodes = cm.getNodes();
			return (nodes != null && !nodes.isEmpty());
		}
		if (element instanceof CmRootNode) {
			return hasChildren(((CmRootNode) element).model);
		}
		return false;
	}

	@Override
	public void dispose() {
		//nothing
	}
	
	public class CmRootNode {
		public ConfigurableModel model;
		
		public ConfigurableModel getModel() {
			return model;
		}
	}
	
	public class MatrixNode{
		
		private CmNode parent;
		private List<CmAttribute> kids;
		
		public MatrixNode(CmNode parent) {
			this.parent = parent;
			kids = new ArrayList<>();
		}
		public List<CmAttribute> getKids(){
			return this.kids;
		}
		public void addKid(CmAttribute kid) {
			this.kids.add(kid);
		}
	}
}
