package org.wcs.smart.i2.ui.handler;

import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.wcs.smart.i2.ui.dialogs.ProfilesListDialog;


@SuppressWarnings("restriction")
public class ShowProfilesHandler extends ShowDialogHandler {

	public ShowProfilesHandler(){
		super(ProfilesListDialog.class);
	}
	
	// E3
	public static class ShowProfilesHandlerWrapper extends DIHandler<ShowProfilesHandler> {
		public ShowProfilesHandlerWrapper() {
			super(ShowProfilesHandler.class);
		}
	}
	
}