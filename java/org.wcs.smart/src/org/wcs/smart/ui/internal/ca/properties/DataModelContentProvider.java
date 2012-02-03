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
package org.wcs.smart.ui.internal.ca.properties;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;

/**
 * Content provided for data model tree.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DataModelContentProvider implements ITreeContentProvider {

	private static RootNode root = new RootNode();
	private DataModel model;

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
		this.model = (DataModel)newInput;
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		return new RootNode[]{root};
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RootNode){
			if ((model.getCategories() != null && model.getCategories().size() > 0)){
				return model.getCategories().toArray();
				
			}
			return null;
		}
		if (parentElement instanceof Category){
			ArrayList<Object> children = new ArrayList<Object>();
			Category category = ((Category)parentElement);
			if ((category.getChildren() != null && category.getChildren().size() > 0)){
				children.addAll(category.getChildren());
				
			}
			if (category.getAttributes() != null && category.getAttributes().size() > 0){
				children.addAll(category.getAttributes());
			}
			return children.toArray();
		}else if (parentElement instanceof Attribute){
			
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof Category){
			return ((Category)element).getParent();
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		
		if (element instanceof Category){
			if (((Category)element).getChildren() != null && ((Category)element).getChildren().size() > 0){
				return true;
			}
			if (((Category)element).getAttributes() != null && ((Category)element).getAttributes().size() > 0){
				return true;
			}
		}else if (element instanceof RootNode){
			if (model.getCategories() != null && model.getCategories().size() > 0){
				return true;
			}
		}
		return false;
	}
	
}

/**
 * Empty class to represent root node of data tree.
 * 
 */
class RootNode{}