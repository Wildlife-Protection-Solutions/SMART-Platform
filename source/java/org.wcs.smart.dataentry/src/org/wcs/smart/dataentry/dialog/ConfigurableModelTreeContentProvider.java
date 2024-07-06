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
	
	private Map<CmNode, MatrixNode> matrixNodes;
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
			matrixNodes = new HashMap<>();
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

	/**
	 * 
	 * @param attribute
	 * @return true if attribue can be grouped, false otherwise
	 */
	public boolean canAddToGroup(CmAttribute attribute) {
		if (attribute.getAttribute().getType() == Attribute.AttributeType.DATE ||
				attribute.getAttribute().getType() == Attribute.AttributeType.TIME ||
				attribute.getAttribute().getType() == Attribute.AttributeType.TREE ||
				attribute.getAttribute().getType() == Attribute.AttributeType.MLIST) {
			return false;
		}
		return true;		
	}

	/**
	 * Determines if the matrix node associated with the given parent node is still
	 * valid after the specified attributes are added or removed from the node.
	 * Valid means that there is at least one list and one non-list node.
	 * @param parentNode
	 * @param toAdd
	 * @param toRemove
	 * @return
	 */
	public boolean isGroupValid(Object parentNode, List<CmAttribute> toAdd, List<CmAttribute> toRemove) {

		if (toAdd == null) toAdd = new ArrayList<>();
		if (toRemove == null) toRemove = new ArrayList<>();
		
		Object[] kids = getChildren(parentNode);
		MatrixNode mn = null;
		for (Object k : kids) {
			if (k instanceof MatrixNode) {
				mn = (MatrixNode) k;
				break;
			}
		}
		
		List<CmAttribute> items = null;
		if (mn == null) {
			items = new ArrayList<>(toAdd);
			items.removeAll(toRemove);
		}else {
			items = new ArrayList<>();
			for (Object k : getChildren(mn)) {
				if (k instanceof CmAttribute) items.add((CmAttribute) k);
			}
			
			items.addAll(toAdd);
			items.removeAll(toRemove);
		}
		
		
		if (items.isEmpty()) return true;
		//otherwise we need at least one list and one non-list
		int listcnt = 0;
		int othercnt = 0;
		for (CmAttribute i : items) {
			if (i.getAttribute().getType() == Attribute.AttributeType.LIST) {
				listcnt++;
			}else {
				othercnt++;
			}
		}
		return listcnt >= 1 && othercnt >= 1;
	}
	/**
	 * Adds the set of attributes to the current attribute group
	 * 
	 * @param attributes
	 */
	public void addToGroup(List<CmAttribute> attributes, boolean reorder) {
		CmNode parent = null;
		
		for (CmAttribute item : attributes) {
			if (parent == null) {
				parent = item.getNode();
			}else {
				if (parent != item.getNode()) return;
			}
			if (!canAddToGroup(item)) return;
		}
		
		//reorder
		if (reorder) {
			int min = -1;
			List<CmAttribute> existingGroups = new ArrayList<>();
			for (int i = 0; i < parent.getCmAttributes().size(); i ++) {
				CmAttribute x = parent.getCmAttributes().get(i);
				if (x.isGrouped()) {
					existingGroups.add(x);
					if (min == -1) min = i;
				}
			}
			
			if (min == -1) {
				//no existing group
				min = attributes.get(0).getOrder();
				for (CmAttribute x : attributes) {
					if (x.getOrder() < min) min = x.getOrder();
				}
				min = min - 1;
			}
			if (min <= 0) min = 0;
			
			existingGroups.addAll(attributes);

			//sort existing groups to list appears first
			int lastindex = 0;
			for (int i = 0; i < existingGroups.size(); i ++) {
				if (existingGroups.get(i).getAttribute().getType() != Attribute.AttributeType.LIST) {
					lastindex = i;
					break;
				}
			}
			List<CmAttribute> tomove = new ArrayList<>();
			for (int i = lastindex; i < existingGroups.size(); i ++) {
				if (existingGroups.get(i).getAttribute().getType() == Attribute.AttributeType.LIST) {
					tomove.add(existingGroups.get(i));
				}
			}
			existingGroups.removeAll(tomove);
			existingGroups.addAll(lastindex, tomove);
			

			//remove and re-insert the attribute groups in the correct location 
			parent.getCmAttributes().removeAll(existingGroups);
			parent.getCmAttributes().addAll(min, existingGroups);
		}

		for (CmAttribute x : attributes) {
			x.setGrouped(Boolean.TRUE);
		}
		reorder(parent);
	}
	
	private void reorder(CmNode parent) {
		int cnt = 1;
		for (CmAttribute kid : parent.getCmAttributes()) kid.setOrder(cnt++);
	}
	
	/**
	 * remove the attributes from the current group
	 * @param attributes
	 */
	public void removeFromGroup(List<CmAttribute> attributes, boolean reorder) {
		//ensure they all have the same parent
		CmNode parent = null;
		for (CmAttribute item : attributes) {
			if (parent == null) {
				parent = item.getNode();
			}else {
				if (parent != item.getNode()) return;
			}
		}
		

		//reorder
		if (reorder) {
			int max = -1;
			for (int i = 0; i < parent.getCmAttributes().size(); i ++) {
				CmAttribute x = parent.getCmAttributes().get(i);
				if (x.isGrouped() && i > max) {
					max = i;
				}
			}
			if (max == -1) {
				//add these at end
				parent.getCmAttributes().removeAll(attributes);
				parent.getCmAttributes().addAll(attributes);
			}else {
				parent.getCmAttributes().removeAll(attributes);
				max = max - attributes.size()+1;
				parent.getCmAttributes().addAll(max, attributes);
			}
		}
				
		for (CmAttribute x : attributes) {
			x.setGrouped(Boolean.FALSE);
		}
		reorder(parent);
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
				List<CmAttribute> attributes = n.getCmAttributes();
				if (attributes == null || attributes.isEmpty()) return new Object[] {};
				
				List<Object> lkids = new ArrayList<>();
				MatrixNode mNode = null;
				for (CmAttribute attribute : n.getCmAttributes()) {
					if (attribute.isGrouped()) {
						if (mNode == null) {
							mNode = matrixNodes.get((CmNode)parentElement);
							if (mNode == null) {
								mNode = new MatrixNode((CmNode)parentElement);
								matrixNodes.put(((CmNode)parentElement), mNode);
							}
							mNode.getKids().clear();
							lkids.add(mNode);
						}
						mNode.addKid(attribute);
						
					}else {
						lkids.add(attribute);
					}
				}
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
			
			if (cma.isGrouped()) return matrixNodes.get(cma.getNode());
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
		public CmNode getParent() {
			return this.parent;
		}
	}
}
