/*
 * Copyright (C) 2021 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.incident.birt.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.incident.birt.IncidentBirtManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Handler to revert changes to incient template.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class RevertTemplateHandler {

	@Execute
	public void execute(Shell activeShell, EModelService modelService, MWindow activeWindow) {
		boolean revert = MessageDialog.openConfirm(activeShell, 
				Messages.RevertTemplateHandler_ConfirmTitle, 
				Messages.RevertTemplateHandler_ConfirmMessage);
		if (!revert) return;

		Path customTemplate = IncidentBirtManager.INSTANCE.getCustomIncidentTemplateLocation();
		if (customTemplate == null) return;
		
		boolean open = IncidentBirtManager.INSTANCE.closeTemplateEditor();
		//delete
		try {
			Files.delete(IncidentBirtManager.INSTANCE.getCustomIncidentTemplateLocation());
		}catch (IOException ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			MessageDialog.openError(activeShell, 
					DialogConstants.ERROR_STRING, 
					MessageFormat.format(Messages.RevertTemplateHandler_ErrorMessage, ex.getMessage()));
		}				
		//re-open
		if (open && 
				modelService.getActivePerspective(activeWindow).getElementId().equals(IncidentBirtPerspective.ID)){
			IncidentBirtManager.INSTANCE.editTemplate();
		}
	}

	
	public static class RevertTemplateHandlerWrapper extends DIHandler<RevertTemplateHandler>{
		public RevertTemplateHandlerWrapper(){
			super(RevertTemplateHandler.class);
		}
	}
}
