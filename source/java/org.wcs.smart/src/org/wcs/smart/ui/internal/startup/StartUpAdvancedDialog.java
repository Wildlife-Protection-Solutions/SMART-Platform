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
package org.wcs.smart.ui.internal.startup;

import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.internal.Messages;

/**
 * Displays the advanced dialog associated
 * with the login page. 
 * 
 * @author Emily Gouge
 *
 */
public class StartUpAdvancedDialog extends InitializeDialog {

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public StartUpAdvancedDialog(Shell parent) {
		super(parent);

	}

	/**
	 * @see org.wcs.smart.ui.internal.startup.InitializeDialog#onCancel()
	 */
	@Override
	public void onCancel() {
		
	}

	/**
	 * @see org.wcs.smart.ui.internal.startup.InitializeDialog#getHeaderText()
	 */
	@Override
	public String getHeaderText() {
		return Messages.StartUpAdvancedDialog_DialogHeader;
	}

	/**
	 * @see org.wcs.smart.ui.internal.startup.InitializeDialog#getMessageText()
	 */
	@Override
	public String getMessageText() {
		return Messages.StartUpAdvancedDialog_DialogContent_Label;
	}

	/**
	 * @see org.wcs.smart.ui.internal.startup.InitializeDialog#getDialogText()
	 */
	@Override
	public String getDialogText() {
		return Messages.StartUpAdvancedDialog_DialogTitle;
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
}