package org.wcs.smart.asset.ui.handler;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.asset.ui.metadata.MetadataMappingDialog;

@SuppressWarnings("restriction")
public class OpenMappingDialogHandler  extends ShowDialogHandler {

	public OpenMappingDialogHandler(){
		super(MetadataMappingDialog.class);
	}
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell, IEclipseContext context) {
		super.execute(activeShell, context);
	}
	
	// E3
	public static class OpenMappingDialogHandlerWrapper extends DIHandler<OpenMappingDialogHandler> {

		public OpenMappingDialogHandlerWrapper() {
			super(OpenMappingDialogHandler.class);
		}
	}
	
}
