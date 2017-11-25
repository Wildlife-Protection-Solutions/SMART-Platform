package org.wcs.smart.asset.ui.views.data;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.data.importer.FileProxy;

public interface ImportAction {

	public boolean preformAction(FileProcessor processor, FileProxy selectedItem);
	
	public String getMenuLabel();
	
	public default Image getMenuImage() {
		return null;
	}
}
