package org.wcs.smart.patrol;

import org.wcs.smart.ILoginHandler;

/**
 * This is a window handler that is run before the window is displayed.
 * It allows the plugin to update the menu names as required.
 * 
 */
public class MenuHandler implements ILoginHandler {

	public MenuHandler() {
	}

	@Override
	public void onLogin() throws Exception {		
		PatrolDynamicMenuManager.INSTANCE.updateMenu();
	}

}
