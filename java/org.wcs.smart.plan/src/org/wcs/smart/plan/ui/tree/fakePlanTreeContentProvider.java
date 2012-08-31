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
package org.wcs.smart.plan.ui.tree;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

/**
 * A content provider that provides the 
 * plans for a treeviewer
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class fakePlanTreeContentProvider implements ITreeContentProvider {

	
	private MockModel model;
	
	@Override
	  public void dispose() {
	  }

	  @Override
	  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	    this.model = (MockModel) newInput;
	  }

	  @Override
	  public Object[] getElements(Object inputElement) {
	    return model.getCategories().toArray();
	  }

	  @Override
	  public Object[] getChildren(Object parentElement) {
	    if (parentElement instanceof FakeCategory) {
	      FakeCategory category = (FakeCategory) parentElement;
	      return category.getTodos().toArray();
	    }
	    return null;
	  }

	  @Override
	  public Object getParent(Object element) {
	    return null;
	  }

	  @Override
	  public boolean hasChildren(Object element) {
	    if (element instanceof FakeCategory) {
	      return true;
	    }
	    return false;
	  }

	}
