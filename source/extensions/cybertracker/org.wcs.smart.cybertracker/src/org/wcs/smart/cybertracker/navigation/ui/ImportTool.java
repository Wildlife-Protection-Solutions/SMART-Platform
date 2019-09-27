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
package org.wcs.smart.cybertracker.navigation.ui;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.locationtech.udig.project.ui.tool.AbstractActionTool;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.ui.SmartWizardDialog;

/**
 * Import targets from shapefile or gpx file.
 * 
 * @author Emily
 *
 */
public class ImportTool  extends AbstractActionTool {

	public static final String ID = "org.wcs.smart.ui.map.navigation.import"; //$NON-NLS-1$

	
	@Override
	public void run() {
		
		Object x = super.getContext().getMap().getBlackboard().get(ITargetEditor.ID);
		if ( x == null || !(x instanceof ITargetEditor) ) return;
		ITargetEditor target = (ITargetEditor)x;
		
		Display.getDefault().syncExec(()->{
			TargetImportWizard wd =  new TargetImportWizard(target);
			WizardDialog dialog = new SmartWizardDialog(Display.getDefault().getActiveShell(), wd);
			dialog.setTitle(Messages.ImportTool_wizardtitle);
			dialog.open();
		});
		
	}


	@Override
	public void dispose() {
	}
}
