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
				if (attributes != null && !attributes.isEmpty()) {
					return attributes.toArray();
				}
			}
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
			return cma.getNode();
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
	}
}
