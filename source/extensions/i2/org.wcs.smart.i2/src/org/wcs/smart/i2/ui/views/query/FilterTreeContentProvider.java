/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views.query;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Content provider for tree that contains FilterTreeItems
 * 
 * @author Emily
 *
 */
public class FilterTreeContentProvider implements ITreeContentProvider{

	private List<FilterTreeItem> items;
	private Viewer viewer;
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		if (newInput == null){
			items = null;
			return;
		}
		if (newInput instanceof List){
			items = (List<FilterTreeItem>) newInput;
		}
		
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (items == null) return new Object[]{DialogConstants.LOADING_TEXT};
		return items.toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		
		if (parentElement instanceof DeferredTreeFilterItem){
			DeferredTreeFilterItem di = (DeferredTreeFilterItem)parentElement;
			if (di.requiresLoad()){
				
				Job j = new Job("loading children"){
	
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						((DeferredTreeFilterItem)parentElement).getChildren();
						Display.getDefault().asyncExec(()->viewer.refresh());
						
						return Status.OK_STATUS;
					}
					
				};
				j.schedule();
				
				return new Object[]{"Loading"};
			}else if (di.getChildren().isEmpty()){
				return null;
			}else{
				return di.getChildren().toArray();
			}
		}else if (parentElement instanceof FilterTreeItem){
			return ((FilterTreeItem) parentElement).getChildren().toArray();
		}
			
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof FilterTreeItem){
			return ((FilterTreeItem) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof DeferredTreeFilterItem){
			return ((DeferredTreeFilterItem)element).hasChildren();
		}
		if (element instanceof FilterTreeItem){
			FilterTreeItem i = (FilterTreeItem)element;
			return ! (i.getChildren() == null || i.getChildren().isEmpty());
		}
		return false;
	}

}
