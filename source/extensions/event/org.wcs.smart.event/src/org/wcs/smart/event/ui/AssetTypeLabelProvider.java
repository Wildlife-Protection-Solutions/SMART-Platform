package org.wcs.smart.event.ui;

import java.util.Locale;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.model.IActionType;

public class AssetTypeLabelProvider extends ColumnLabelProvider{

	public String getText(Object element) {
		if (element instanceof IActionType) return ((IActionType) element).getName(Locale.getDefault());
		return super.getText(element);
	}
	
	public Image getImage(Object element) {
		if (element instanceof IActionType) return EventPlugIn.getDefault().getImageRegistry().get(EventPlugIn.ICON_ACTION_TYPE);
		return null;
	}
}
