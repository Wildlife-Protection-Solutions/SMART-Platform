/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui.config;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ui.properties.DialogConstants;

public class DataModelContentProvider implements ITreeContentProvider {

	protected DataModel model;
	
	/**
	 * Creates a new content provider that provides categories, tree and list
	 * attributes
	 */
	public DataModelContentProvider(){
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
		if (newInput instanceof DataModel) {
			this.model = (DataModel)newInput;
		}else {
			this.model = null;
		}
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (model == null) return new Object[] {DialogConstants.LOADING_TEXT};
		return  model.getCategories().toArray();
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		
		if (parentElement instanceof CategoryAttribute) {
			CategoryAttribute ca = (CategoryAttribute)parentElement;
			if (ca.getAttribute().getType() == Attribute.AttributeType.LIST) {
				List<CategoryItemWrapper> kids = new ArrayList<>();
				for (AttributeListItem li : ca.getAttribute().getActiveListItems()) {
					kids.add(new CategoryItemWrapper(ca.getCategory(), li));
				}
				return kids.toArray();
			}
			if (ca.getAttribute().getType() == Attribute.AttributeType.TREE) {
				List<CategoryItemWrapper> kids = new ArrayList<>();
				for (AttributeTreeNode li : ca.getAttribute().getActiveTreeNodes()) {
					kids.add(new CategoryItemWrapper(ca.getCategory(), li));
				}
				return kids.toArray();
			}
		
		}else if (parentElement instanceof CategoryItemWrapper && ((CategoryItemWrapper)parentElement).node != null) {
			AttributeTreeNode tn = (AttributeTreeNode)((CategoryItemWrapper)parentElement).node;
			if (tn.getActiveChildren() != null) {
				List<CategoryItemWrapper> kids = new ArrayList<>();
				for (AttributeTreeNode li : tn.getActiveChildren()) {
					kids.add(new CategoryItemWrapper(((CategoryItemWrapper)parentElement).c, li));
				}
				return kids.toArray();
			}
			return null;
		
		}else if (parentElement instanceof Category) {
			
			ArrayList<Object> children = new ArrayList<Object>();
			Category category = ((Category)parentElement);
			
			if (category.getActiveChildren() != null) children.addAll(category.getActiveChildren());
			//add attributes
			List<CategoryAttribute> all = new ArrayList<>();
			category.getAllCategoryAttribute(all, true);
			for (CategoryAttribute ca : all) {
				if (ca.getAttribute().getType() == Attribute.AttributeType.LIST || ca.getAttribute().getType() == Attribute.AttributeType.TREE) {
					children.add(ca);
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
		}else if (element instanceof AttributeListItem) {
			//TODO:
		}else if (element instanceof AttributeTreeNode) {
			//TODO:
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		
		if (element instanceof AttributeTreeNode) {
			if (((AttributeTreeNode)element).getActiveChildren() != null) return true;
			return false;
			
		}else if (element instanceof CategoryAttribute) {
			Attribute.AttributeType t = ((CategoryAttribute)element).getAttribute().getType();
			return t == Attribute.AttributeType.LIST || t == Attribute.AttributeType.TREE;
			
		}else if (element instanceof Category){
			Category category = (Category)element;
			if (category.getActiveChildren() != null && category.getActiveChildren().size() > 0) return true;
			if (category.getAttributes(true).size() > 0) return true;
		}
		return false;
	}

	public class CategoryItemWrapper {
		Category c;
		AttributeListItem li;
		AttributeTreeNode node;
		
		public CategoryItemWrapper(Category c, AttributeListItem li) {
			this.c = c;
			this.li = li;
		}
		
		public CategoryItemWrapper(Category c, AttributeTreeNode node) {
			this.c = c;
			this.node = node;
		}
	}
}

