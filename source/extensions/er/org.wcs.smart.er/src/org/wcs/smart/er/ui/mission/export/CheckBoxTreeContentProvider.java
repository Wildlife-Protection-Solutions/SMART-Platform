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
package org.wcs.smart.er.ui.mission.export;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
/**
 * A content provider for showing missions under their associated surveys 
 * 
 * 
 * @author Jeff
 *
 */
public class CheckBoxTreeContentProvider implements ITreeContentProvider{

	private Object[] items;

	private String loadingLabel = null;
	
	/**
	 * Creates a new content provider.
	 * 
	 */
	public CheckBoxTreeContentProvider(){

	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null){
			return;
		}
		if (newInput instanceof String){
			loadingLabel = (String) newInput;
			return;
		}
		loadingLabel = null;
		items = (Object[])newInput;

	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (loadingLabel != null){
			return new Object[]{loadingLabel};
		}
		return items;
	}
	
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof MissionTreeItem){
			return null;
		}else if (parentElement instanceof SurveyTreeItem){
			List<MissionTreeItem> missions = ((SurveyTreeItem) parentElement).getChildren();
			Object[] newlist = new Object[missions.size()];
			int x=0;
			for(MissionTreeItem m : missions){
				newlist[x] = m;
				x++;
			}
			return newlist;
		}
		return null;
	}

	 /* (non-Javadoc)
     * Method declared on ITreeContentProvider.
     */
	@Override
    public Object getParent(Object element) {
		if (element instanceof SurveyTreeItem){
			return null;
		}else if (element instanceof MissionTreeItem){
			return ((MissionTreeItem)element).getParent();
		}
		return null;
    }

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof SurveyTreeItem ){
			return true;
		}
		return false;
	}
	
	
	
	
}


