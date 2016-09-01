package org.wcs.smart.i2.ui.handler;

import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.wcs.smart.i2.ui.dialogs.AttributeListDialog;
import org.wcs.smart.i2.ui.dialogs.EntityTypeListDialog;

public class ShowAttributeListDialogHandler extends ShowDialogHandler {

	public ShowAttributeListDialogHandler(){
		super(AttributeListDialog.class);
	}
	
	// E3
	public static class ShowAttributeListDialogHandlerWrapper extends DIHandler<ShowAttributeListDialogHandler> {
		public ShowAttributeListDialogHandlerWrapper() {
			super(ShowAttributeListDialogHandler.class);
		}
	}
	
}
