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
package org.wcs.smart.er.query.ui.panels.item.configmodel;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
/**
 * Content provider for providing data model item for query filters.
 * 
 * @author Emily
 *
 */
public class FiltersConfigurableModelContentProvider implements ITreeContentProvider{

	//data model 
	private ConfigurableModel configModel = null;
	private ConfigurableModelTreeContentProvider provider;
	private ConfigurableModelLabelProvider dmLabelProvider =  new ConfigurableModelLabelProvider();
	
	/**
	 * Creates a new content provider 
	 */
	public FiltersConfigurableModelContentProvider(){
		provider = new ConfigurableModelTreeContentProvider(false, true);
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		provider.dispose();
	}

	
	/**
	 * 
	 * @param newInput must be a map that contains the keys
	 * QueryFilterContentProvider.ROOT_NODES - array of RootNodeType which should appear as 
	 * the root nodes in the tree
	 * RootNodeType.DATA_MODEL_FILTERS whose value is the current data model
	 * and 
	 * RootNodeType.PATROL_FILTERS whose value is the patrol filter options
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null){
			provider.inputChanged(viewer, oldInput, null);
		}else if (newInput instanceof String){
			configModel = null;
		}else{
			this.configModel = (ConfigurableModel)newInput;
			provider.inputChanged(viewer, oldInput, this.configModel);
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (configModel == null ){
			return new String[]{"Loading..."};
		}
		return provider.getElements(inputElement);
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		return provider.getChildren(parentElement);
	}


	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		return provider.getParent(element);
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		return provider.hasChildren(element);
	}

	
	
	public LabelProvider getLabelProvider(){
		return dmLabelProvider;
	}
}