package org.wcs.smart.r.ui;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.r.RPlugIn;


public class ShowRScriptHandler {

	public ShowRScriptHandler() {
	}
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell, IEclipseContext context) {
		try{
			Dialog d = (Dialog)RScriptListDialog.class.getDeclaredConstructor(Shell.class).newInstance(activeShell);
			ContextInjectionFactory.inject(d, context);
			d.open();
		}catch (Exception ex){
			RPlugIn.displayLog(ex.getMessage(), ex);
		}
	}

	
	// E3
	public static class ShowRScriptHandlerWrapper extends DIHandler<ShowRScriptHandler> {
		public ShowRScriptHandlerWrapper() {
			super(ShowRScriptHandler.class);
		}
	}
}
