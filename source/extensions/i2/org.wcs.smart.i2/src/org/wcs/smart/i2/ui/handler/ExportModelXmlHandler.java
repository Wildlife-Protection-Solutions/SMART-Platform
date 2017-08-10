package org.wcs.smart.i2.ui.handler;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.i2.ui.dialogs.ExportModelElementsDialog;

public class ExportModelXmlHandler {

	@Execute
	public void execute(IEclipseContext context){
		ExportModelElementsDialog dialog = new ExportModelElementsDialog(context.get(Shell.class));
		dialog.open();
	}
	
	// E3
	public static class ExportModelXmlHandlerWrapper extends DIHandler<ExportModelXmlHandler> {
		public ExportModelXmlHandlerWrapper() {
			super(ExportModelXmlHandler.class);
		}
	}

}
