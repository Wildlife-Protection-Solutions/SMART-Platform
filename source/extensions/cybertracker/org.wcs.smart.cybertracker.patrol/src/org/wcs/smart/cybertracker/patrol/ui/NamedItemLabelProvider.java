package org.wcs.smart.cybertracker.patrol.ui;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.NamedItem;

public class NamedItemLabelProvider extends LabelProvider{

	@Override
	public String getText(Object element) {
		if (element instanceof NamedItem) return ((NamedItem) element).getName();
		return super.getText(element);
	}

}
