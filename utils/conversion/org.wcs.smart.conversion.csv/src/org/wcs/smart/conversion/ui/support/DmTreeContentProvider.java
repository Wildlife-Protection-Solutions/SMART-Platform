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
package org.wcs.smart.conversion.ui.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;

/**
 * @author elivin
 * @since 3.0.0
 */
public class DmTreeContentProvider implements ITreeContentProvider {

	private List<CategoryType> rootNodes;
	private boolean active;

	private DataModelLookup lookup;
	
	public static final CategoryType NO_TYPE_CATEGORY = new NoCategoryType();

	/**
	 * @param active <code>true</code> if only active elements to be included; otherwise <code>false</code> includes all
	 */
	public DmTreeContentProvider(boolean active, DataModelLookup lookup) {
		this.active = active;
		this.lookup = lookup;
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
		if (newInput instanceof List){
			this.rootNodes = new ArrayList<CategoryType>();
			rootNodes.add(NO_TYPE_CATEGORY);
			this.rootNodes.addAll((List<CategoryType>) newInput);
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(null);
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		List<CategoryType> kids = null;
		if (parentElement == null) {
			kids = rootNodes;
		} else if (parentElement instanceof CategoryType) {
			kids = ((CategoryType)parentElement).getCategories();
			if (active) {
				kids = ((CategoryType)parentElement).getCategories();
				for (Iterator<CategoryType> iterator = kids.iterator(); iterator.hasNext();) {
					CategoryType ct = iterator.next();
					if (Boolean.FALSE.equals(ct.isIsactive()))
						iterator.remove();
				}
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
		if (element instanceof CategoryType) {
			return lookup.getParent((CategoryType)element);
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
	 * Class used to represent "--default--" category if category dropdown.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	public static final class NoCategoryType extends CategoryType {}
}