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

public class FilterTreeContentProvider implements ITreeContentProvider{

	private List<FilterItem> items;
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
			items = (List<FilterItem>) newInput;
		}
		
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (items == null) return new Object[]{DialogConstants.LOADING_TEXT};
		return items.toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		
		if (parentElement instanceof DeferredFilterItem){
			DeferredFilterItem di = (DeferredFilterItem)parentElement;
			if (di.requiresLoad()){
				
				Job j = new Job("loading children"){
	
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						((DeferredFilterItem)parentElement).getChildren();
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
		}else if (parentElement instanceof FilterItem){
			return ((FilterItem) parentElement).getChildren().toArray();
		}
			
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof FilterItem){
			return ((FilterItem) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof DeferredFilterItem){
			return ((DeferredFilterItem)element).hasChildren();
		}
		if (element instanceof FilterItem){
			FilterItem i = (FilterItem)element;
			return ! (i.getChildren() == null || i.getChildren().isEmpty());
		}
		return false;
	}

}
