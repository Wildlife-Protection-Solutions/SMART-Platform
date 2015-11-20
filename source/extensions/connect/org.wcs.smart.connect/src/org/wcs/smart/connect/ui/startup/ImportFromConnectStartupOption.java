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
package org.wcs.smart.connect.ui.startup;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.ui.IAdvancedStartupOption;

/**
 * Import ca from connect start up option
 * @author Emily
 *
 */
public class ImportFromConnectStartupOption implements IAdvancedStartupOption {
	public static final int ORDER = 40;
	@Override
	public String getLabel() {
		return Messages.ImportFromConnectStartupOption_name;
	}

	@Override
	public Status performTask(Shell activeShell) throws Exception {
		WizardDialog wd = new WizardDialog(activeShell, new DownloadConnectWizard());
		wd.open();
		return Status.OK;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

}
