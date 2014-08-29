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
package org.wcs.smart.er.ui;

import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.ui.SurveyListTreeNode.Type;

/**
 * Lazy tree content provider for survey list view.  The roots
 * are the survey designs, then surveys, then associated missions.
 * 
 * @author Emily
 *
 */
public class LazySurveyDesignTreeContentProvider extends
		BaseWorkbenchContentProvider {

	private DeferredTreeContentManager manager;
	
	private List<SurveyListTreeNode> designs;

	
	public LazySurveyDesignTreeContentProvider() {
	}

	/**
	 * @see DeferredTreeContentManager#addUpdateCompleteListener(IJobChangeListener)
	 * @param listsner
	 */
	public void addUpdateCompleteListener(IJobChangeListener listsner) {
		manager.addUpdateCompleteListener(listsner);
	}

	/**
	 * @see DeferredTreeContentManager#removeUpdateCompleteListener(IJobChangeListener)
	 * @param listsner
	 */
	public void removeUpdateCompleteListener(IJobChangeListener listsner) {
		manager.removeUpdateCompleteListener(listsner);
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (designs == null){
			return new Object[]{Messages.LazySurveyDesignTreeContentProvider_LoadingLabel};
		}
		return designs.toArray();
	}

	/**
	 * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.designs = null;
		if (newInput instanceof List<?>){
			this.designs = (List<SurveyListTreeNode>) newInput;
		}
		if (viewer instanceof AbstractTreeViewer) {
			manager = new DeferredTreeContentManager(
					(AbstractTreeViewer) viewer);
		}
		super.inputChanged(viewer, oldInput, newInput);
	}

	/**
	 * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof SurveyListTreeNode 
				&& ((SurveyListTreeNode) element).getType() == Type.MISSION) {
			return false;
		}else if (element instanceof String){
			return false;
		}else{
			return true;
		}
	}

	/*
	 * (non-Javadoc) Method declared on ITreeContentProvider.
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof SurveyListTreeNode) {
			((SurveyListTreeNode)element).getParent();
		}
		return null;
	}

	/**
	 * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof SurveyListTreeNode){
			if (manager != null) {
				Object[] kids = manager.getChildren(parent);
				if (kids != null) {
					return kids;
				}
			}
			Object[] kids = super.getChildren(parent);
			return kids;
		}
		return null;
	}


}