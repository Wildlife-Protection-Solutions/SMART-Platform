package org.wcs.smart.i2.ui;

import java.util.regex.Pattern;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.wcs.smart.ca.NamedItem;

public class NamedItemViewerFilter extends ViewerFilter{

	private String filter;
	private Viewer viewer;
	
	public NamedItemViewerFilter(Viewer viewer){
		this.viewer = viewer;
	}
	
	public void setFilterString(String filter){
		this.filter = filter;
		viewer.refresh();
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (filter == null || filter.length() == 0) {
			return true;
		}
		String search = ".*" + Pattern.quote(filter.toLowerCase()) + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
		NamedItem item = (NamedItem) element;
		if (item.getName().toLowerCase().matches(search)){
			return true;
		}
		return false;
	}
}
