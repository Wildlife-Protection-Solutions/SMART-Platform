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
import java.util.Iterator;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;

/**
 * Content provided for data model tree.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DataModelContentProvider implements ITreeContentProvider {

	private RootNode root = new RootNode();
	private DataModel model;

	private boolean onlyCategories = false;
	private boolean onlyEnabled = false;
	/**
	 * Creates a new content provider that provides
	 * both enabled and disabled categories and attributes
	 */
	public DataModelContentProvider(){
		this(false, true);
	}
	
	/**
	 * Creates a new content provider
	 * @param onlyCategories if true only categories are provided 
	 * otherwise both categories and attributes are provided
	 * @param onlyEnabled if true only enabled categories and attributes
	 * are provided
	 */
	public DataModelContentProvider(boolean onlyCategories, boolean onlyEnabled){
		this.onlyCategories = onlyCategories;
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
				if (onlyEnabled){
					return model.getActiveCategories().toArray();
				}else{
					return model.getCategories().toArray();
				}
				
			}
			return null;
		}else if (parentElement instanceof Category){
			ArrayList<Object> children = new ArrayList<Object>();
			Category category = ((Category)parentElement);
			if (onlyEnabled){
				children.addAll(category.getChildren(true));
			}else{
				if (category.getChildren() != null ){
					children.addAll(category.getChildren());
				}
			}
				
			if (!onlyCategories){
				//add attributes
				if (onlyEnabled){
					children.addAll(category.getChildren(true));
				}else{
					if (category.getAttributes() != null ){
						children.addAll(category.getAttributes());
					}
				}
			}
			return children.toArray();
			
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
			if (onlyEnabled){
				if (((Category)element).getChildren(true).size() > 0){
					return true;
				}
			}else{
				if (((Category)element).getChildren() != null && ((Category)element).getChildren().size() > 0){
					return true;
				}	
			}
			
			if (!onlyCategories){
				//also check attributes
				if (onlyEnabled){
					if (((Category)element).getAttributes(true).size() > 0){
						return true;
					}
				}else{
					if (((Category)element).getAttributes() != null && ((Category)element).getAttributes().size() > 0){
						return true;
					}
				}
			}
			
		}else if (element instanceof RootNode){
			if (onlyEnabled){
				if (model.getActiveCategories().size() > 0){
					return true;
				}
			}else{
				if (model.getCategories() != null && model.getCategories().size() > 0){
					return true;
				}
			}
		}
		return false;
	}
	
	
	/**
	 * Empty class to represent root node of data tree.
	 * 
	 */
	public class RootNode{}
}

