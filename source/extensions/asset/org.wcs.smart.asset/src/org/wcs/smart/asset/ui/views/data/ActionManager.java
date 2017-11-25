package org.wcs.smart.asset.ui.views.data;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.wcs.smart.asset.data.importer.ActionableWarning;
import org.wcs.smart.asset.data.importer.NewAssetWarning;

public class ActionManager {

	public static ImportAction findAction(ActionableWarning warning, IEclipseContext context) {
		ImportAction importAction = null;
		if (warning.getClass().equals(NewAssetWarning.class)) importAction = new NewAssetAction((NewAssetWarning) warning);
		
		if (importAction != null) {
			ContextInjectionFactory.inject(importAction, context);
		}
		return importAction;
	}
}
