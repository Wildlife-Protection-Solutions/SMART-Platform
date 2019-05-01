/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.ctpackage.ui;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICtPackage;

/**
 * Manager for exporting cybertracker packages
 * 
 * @author Emily
 *
 */
public class ExportCtPackageManager {

	private Shell shell;
	
	public ExportCtPackageManager(Shell parent) {
		this.shell = parent;
	}
	
	public boolean doExport(List<ICtPackage> toExport, IEclipseContext context) throws IOException {
		
		//if any one of these already has packages then first ask the user if they want to
		//update/create the packages with SMART data or export the existing package data
		boolean requireCreateOp = false;
		for (ICtPackage p : toExport) {
			if (p.getLocalFile() != null) requireCreateOp = true;
		}
		
		List<ICtExportAction> actions = CtPackageExtensionPointManager.INSTANCE.getPackageActions();
		
		boolean create = true;
		List<ICtExportAction> doActions = new ArrayList<>(actions);
		if (actions.size() == 1) {
			if (requireCreateOp) {
				if (MessageDialog.openQuestion(shell, Messages.ExportCtPackageManager_ShellTitle,
						Messages.ExportCtPackageManager_RegenQuestion)) {
					create = true;
				}else {
					create = false;
				}
			}
		}else {
			CtPackageExportDialog dialog = new CtPackageExportDialog(shell, requireCreateOp, actions);
			if (dialog.open() != Window.OK) return false;
			create = dialog.getDoGenerate();
			doActions = dialog.getSelectedActions();
		}
		
		List<ICtPackage> towrite;
		if (create) {
			towrite = createPackages(toExport, context);
		}else {
			towrite = new ArrayList<>(toExport);
		}
		if (towrite.isEmpty()) return false;
		for (ICtExportAction a : doActions) {
			a.doAction(towrite, context);
		}
		return true;
	}
	
	private List<ICtPackage> createPackages(List<ICtPackage> items, IEclipseContext context){
		List<ICtPackage> towrite = new ArrayList<>(items);
		//create all the packages
		for (ICtPackage p : items) {
			try {
				CtPackageExtensionPointManager.INSTANCE.createPackage(p, context);
			}catch (Throwable ex) {
				towrite.remove(p);
				CyberTrackerPlugIn.displayError(Messages.ExportCtPackageManager_ErrorTitle, MessageFormat.format(Messages.ExportCtPackageManager_ErrorMsg, p.getName(), ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()), ex);
			}
		}
		return towrite;
	}
}
