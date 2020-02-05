package org.wcs.smart.i2.ui;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelProfile;

public class ProfileLabelProvider extends ColumnLabelProvider {
	
	public String getText(Object other) {
		if (other instanceof IntelProfile) {
			return ((IntelProfile)other).getName();
		}
		return super.getText(other);
	}
	
	@Override
	public Image getImage(Object item) {
		if (!(item instanceof IntelProfile)) return null;
		return Resources.INSTANCE.getImage((IntelProfile)item);
	}
}
