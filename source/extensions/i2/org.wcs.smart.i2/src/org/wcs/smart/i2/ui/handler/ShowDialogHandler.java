package org.wcs.smart.i2.ui.handler;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.i2.Intelligence2PlugIn;

public class ShowDialogHandler{

	private Class<? extends Dialog> dialogClass;
	
	public ShowDialogHandler(Class<? extends Dialog> dialogClass){
		this.dialogClass = dialogClass;
	}
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) {
		try{
			Dialog d = (Dialog)dialogClass.getDeclaredConstructor(Shell.class).newInstance(activeShell);
			d.open();
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
		}
	}
}
