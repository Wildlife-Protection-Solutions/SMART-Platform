package org.wcs.smart.i2.ui.handler;

import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.wcs.smart.i2.ui.dialogs.EntityTypeListDialog;

public class ShowEntityTypeListDialogHandler extends ShowDialogHandler {

	public ShowEntityTypeListDialogHandler(){
		super(EntityTypeListDialog.class);
	}
	
	// E3
	public static class ShowEntityTypeListDialogHandlerWrapper extends DIHandler<ShowEntityTypeListDialogHandler> {
		public ShowEntityTypeListDialogHandlerWrapper() {
			super(ShowEntityTypeListDialogHandler.class);
		}
	}
	
}
