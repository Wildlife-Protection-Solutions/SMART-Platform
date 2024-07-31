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
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
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

	protected RootNode root = new RootNode();
	protected DataModel model;

	//only categories
	private boolean onlyCategories = false;
	//if only enabled category and attributes are shown
	private boolean onlyEnabled = false;
	//if include parent attributes
	private boolean allAttributes = false;
	
	/**
	 * Creates a new content provider that provides
	 * both enabled and disabled categories and attributes
	 */
	public DataModelContentProvider(){
		this(false, false, false);
	}
	
	/**
	 * Creates a new content provider
	 * @param onlyCategories if true only categories are provided 
	 * otherwise both categories and attributes are provided
	 * @param onlyEnabled if true only enabled categories and attributes
	 * are provided
	 */
	public DataModelContentProvider(boolean onlyCategories, boolean onlyEnabled){
		this(onlyCategories, onlyEnabled, false);
	}
	
	/**
	 * Creates a new content provider
	 * @param onlyCategories if true only categories are provided 
	 * otherwise both categories and attributes are provided
	 * @param onlyEnabled if true only enabled categories and attributes
	 * @param allAttributes - <code>true</code> if all direct & parent attributes are to be included in the categories children
	 * are provided
	 */
	public DataModelContentProvider(boolean onlyCategories, boolean onlyEnabled, boolean allAttributes){
		this.onlyCategories = onlyCategories;
		this.onlyEnabled = onlyEnabled;
		this.allAttributes = allAttributes;
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
			return new Object[]{};
		}else if (parentElement instanceof Category){
			ArrayList<Object> children = new ArrayList<Object>();
			Category category = ((Category)parentElement);
			
			//add children attributes
			if (onlyEnabled){
				if (category.getActiveChildren() != null){
					children.addAll(category.getActiveChildren());
				}
			}else{
				if (category.getChildren() != null ){
					children.addAll(category.getChildren());
				}
			}
				
			if (!onlyCategories){
				//add attributes

				if (allAttributes){
					if (onlyEnabled) {
						children.addAll(category.getAllActiveAttributes());
					}else {
						children.addAll(category.getAllAttributes());
					}
				}else{
					if (onlyEnabled) {
						for(CategoryAttribute ca : category.getRootAttributes()) {
							if (ca.getIsActive()) children.add(ca);
						}
					}else {
						children.addAll(category.getRootAttributes());
					}
				}
			}
			return children.toArray();
			
		}
		return new Object[]{};
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof Category){
			return ((Category)element).getParent();
		}else if (element instanceof CategoryAttribute){
			return ((CategoryAttribute) element).getCategory();
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		
		if (element instanceof Category category){
			List<Category> ckids = null;
			if (onlyEnabled){
				ckids = category.getActiveChildren();
			}else{
				ckids = category.getChildren();
			}
			if (ckids != null && ckids.size() > 0){
				return true;
			}
			
			if (!onlyCategories){
				//also check attributes
				if (onlyEnabled){
					if (allAttributes) {
						return category.getAllActiveAttributes().size() > 0;
					}else {
						for (CategoryAttribute ca: category.getRootAttributes()) {
							if (ca.getIsActive()) return true;
						}
						return false;
					}
				}else {
					if (allAttributes) {
						return category.getAllAttributes().size() > 0;
					}else {
						return category.getRootAttributes().size() > 0;
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

