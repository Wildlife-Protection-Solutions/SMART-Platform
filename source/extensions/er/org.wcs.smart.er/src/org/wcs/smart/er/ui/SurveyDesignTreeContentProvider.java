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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.er.hibernate.SurveyMissionProxy;
import org.wcs.smart.er.hibernate.SurveyMissionProxy.Type;
import org.wcs.smart.er.internal.Messages;

/**
 * Lazy tree content provider for survey list view.  The roots
 * are the survey designs, then surveys, then associated missions.
 * 
 * @author Emily
 *
 */
public class SurveyDesignTreeContentProvider implements ITreeContentProvider {
	
	private List<SurveyMissionProxy> surveys;

	
	public SurveyDesignTreeContentProvider() {
	}

	
	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (surveys == null){
			return new Object[]{Messages.LazySurveyDesignTreeContentProvider_LoadingLabel};
		}
		return surveys.toArray();
	}

	/**
	 * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.surveys = null;
		if (newInput instanceof List<?>){
			this.surveys = (List<SurveyMissionProxy>) newInput;
		}
	}

	/**
	 * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof SurveyMissionProxy) 
			return !((SurveyMissionProxy)element).getMissions().isEmpty();
		return false;
	
	}

	/*
	 * (non-Javadoc) Method declared on ITreeContentProvider.
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof SurveyMissionProxy){
			SurveyMissionProxy p = (SurveyMissionProxy) element;
//			if (p.getType() == Type.SURVEY) return false;
			if (p.getType() == Type.SURVEY) return null;
			return p.getParent();
		}
		return null;
	}

	/**
	 * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof SurveyMissionProxy){
			return ((SurveyMissionProxy)parent).getMissions().toArray();
		}
		return null;
	}


}