package org.wcs.smart.i2.model;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;

public enum IntelWorkingSetCategory {
	ENTITY("Entities"),
	RECORD("Records"),
	QUERIES("Queries");
	
	private String guiName;
	
	private IntelWorkingSetCategory(String guiName) {
		this.guiName = guiName;
	}
	
	public String getGuiName(){
		return this.guiName;
	}
	
	public Image getImage(){
		if (this == RECORD){
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD);
		}else if (this == ENTITY){
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY);
		}
		return null;
	}

}
