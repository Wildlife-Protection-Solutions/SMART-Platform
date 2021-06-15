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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.birt.IncidentBirtManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Handler for importing a birt incident template.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class ImportTemplateHandler {

	@Execute
	public void execute(Shell activeShell, EModelService modelService, MWindow activeWindow){
		if (! MessageDialog.openConfirm(activeShell, 
				Messages.ImportTemplateHandler_ConfirmTitle,
				Messages.ImportTemplateHandler_ConfirmMessage)){
			return ;
		}
		
	
		FileDialog fd = new FileDialog(activeShell, SWT.OPEN);
		fd.setFilterNames(new String[]{Messages.ImportTemplateHandler_designFileName, Messages.ImportTemplateHandler_allFiles});
		fd.setFilterExtensions(new String[]{"*.rptdesign", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setText(Messages.ImportTemplateHandler_ImportTitle);
		fd.setFileName(SmartDB.getCurrentConservationArea().getId() + "_incidenttemplate.rptdesign"); //$NON-NLS-1$
		fd.setOverwrite(true);
		String exportFile = fd.open();
		if (exportFile == null){
			return ;
		}
		final Path inFile = Paths.get(exportFile);
		if (!Files.exists(inFile)){
			MessageDialog.openError(activeShell, DialogConstants.ERROR_STRING, 
					MessageFormat.format(Messages.ImportTemplateHandler_FileNotFound, new Object[]{exportFile}));
			return ;
		}
		try{
			boolean open = IncidentBirtManager.INSTANCE.closeTemplateEditor();
			IncidentBirtManager.INSTANCE.importIncidentTemplate(inFile);
			//re-open
			if (open && 
					modelService.getActivePerspective(activeWindow).getElementId().equals(IncidentBirtPerspective.ID)){
				IncidentBirtManager.INSTANCE.editTemplate();
			}
		}catch (Exception ex){
			IncidentPlugIn.displayLog(Messages.ImportTemplateHandler_ImportError + ex.getMessage(), ex);
		}
	}

	public static class ImportTemplateHandlerWrapper extends DIHandler<ImportTemplateHandler>{
		public ImportTemplateHandlerWrapper(){
			super(ImportTemplateHandler.class);
		}
	}
}
