package org.wcs.smart.conversion.ui.support;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;

public class AttributeTreeContentProvider implements ITreeContentProvider {

	private List<TreeNodeType> rootNodes;
	private boolean active;

	private Map<TreeNodeType, TreeNodeType> parentLookup;

	/**
	 * @param active <code>true</code> if only active elements to be included; otherwise <code>false</code> includes all
	 */
	public AttributeTreeContentProvider(boolean active) {
		this.active = active;
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
			this.rootNodes = (List<TreeNodeType>) newInput;
			parentLookup = new HashMap<TreeNodeType, TreeNodeType>();
			mapParent(parentLookup, null, rootNodes);
		}
	}

	private void mapParent(Map<TreeNodeType, TreeNodeType> map, TreeNodeType parent, List<TreeNodeType> children) {
		for (TreeNodeType child : children) {
			map.put(child, parent);
			mapParent(map, child, child.getChildrens());
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
		List<TreeNodeType> kids = null;
		if (parentElement == null) {
			kids = rootNodes;
		} else if (parentElement instanceof TreeNodeType) {
			kids = ((TreeNodeType)parentElement).getChildrens();
			if (active) {
				kids = ((TreeNodeType)parentElement).getChildrens();
				for (Iterator<TreeNodeType> iterator = kids.iterator(); iterator.hasNext();) {
					TreeNodeType nt = iterator.next();
					if (Boolean.FALSE.equals(nt.isIsactive()))
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
		if (element instanceof TreeNodeType) {
			return parentLookup.get((TreeNodeType)element);
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		Object[] children = getChildren(element);
		if (children == null) {
			return false;
		}
		return children.length > 0;
	}
	
}
