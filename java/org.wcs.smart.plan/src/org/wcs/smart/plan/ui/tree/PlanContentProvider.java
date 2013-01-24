/**
 * 
 */
package org.wcs.smart.plan.ui.tree;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.plan.model.Plan;

/**
 * @author Emily
 *
 */
public class PlanContentProvider implements ITreeContentProvider {

	private Object[] rootNodes;
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.rootNodes = (Object[])newInput;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		return rootNodes;
	}
	
	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (!(parentElement instanceof Plan)){
			return null;
		}
		Plan p = (Plan)parentElement;
		List<Plan> kids = p.getChildren();
		if (kids == null || kids.size() == 0){
			return null;
		}
		return kids.toArray(new Plan[kids.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (!(element instanceof Plan)){
			return null;
		}
		return ((Plan)element).getParent();
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		if (!(element instanceof Plan)){
			return false;
		}
		Plan p = (Plan)element;
		if (p.getChildren() == null || p.getChildren().size() == 0){
			return false;
		}
		return true;
	}

}
