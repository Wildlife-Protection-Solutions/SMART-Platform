/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.ui.server;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.connect.ui.startup.LocalCaListPage;
import org.wcs.smart.connect.ui.startup.SyncMultipleCaWizard;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartWizardDialog;

/**
 * Download change log handler for manually downloading 
 * change log from server.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class SyncMultiChangeLogHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) {
		SyncMultipleCaWizard syncWizard = new SyncMultipleCaWizard();
		((LocalCaListPage)syncWizard.getPage(LocalCaListPage.NAME)).setUsernamePassword(SmartDB.getCurrentEmployee().getSmartUserId(), SmartDB.getPlainTextPassword());
		WizardDialog wd = new SmartWizardDialog(activeShell, syncWizard);
		wd.open();
	}
	
	public static class SyncMultiChangeLogHandlerWrapper extends DIHandler<SyncMultiChangeLogHandler>{
		public SyncMultiChangeLogHandlerWrapper() {
			super(SyncMultiChangeLogHandler.class);
		}
		
	}
}
