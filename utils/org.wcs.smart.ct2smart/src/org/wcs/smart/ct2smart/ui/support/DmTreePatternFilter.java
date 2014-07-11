package org.wcs.smart.ct2smart.ui.support;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.PatternFilter;

public class DmTreePatternFilter extends PatternFilter {
	
	protected boolean isChildMatch(Viewer viewer, Object element) {
		Object parent = ((ITreeContentProvider) ((TreeViewer) viewer).getContentProvider()).getParent(element);
		if (parent != null) {
			return (isLeafMatch(viewer, parent) ? true : isChildMatch(viewer, parent));
		}
		return false;
	}

	@Override
	public boolean isLeafMatch(Viewer viewer, Object element) {
		String labelText = ((LabelProvider) ((TreeViewer) viewer).getLabelProvider()).getText(element);
		if (labelText == null) {
			return false;
		}
		return (wordMatches(labelText) ? true : isChildMatch(viewer,element));
	}

}
