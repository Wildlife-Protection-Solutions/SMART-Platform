/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.connect.dataqueue.cybertracker;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.connect.dataqueue.cybertracker.internal.Messages;

/**
 * Simple dialog with not buttons, not title to inform user they
 * must close dialogs 
 * 
 * @author Emily
 *
 */
public class CloseMsgDialog extends Dialog {

	protected CloseMsgDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.MODELESS | SWT.ON_TOP);
		setBlockOnOpen(false);
	}

	protected Control createDialogArea(Composite parent) {
		 Composite composite = (Composite) super.createDialogArea(parent);
		 composite.setLayout(new GridLayout());
		 Label l = new Label(composite, SWT.NONE);
		 l.setText(Messages.CloseMsgDialog_Msg);
		 return composite;
	 }
	
	protected void createButtonsForButtonBar(Composite parent) {
	}
}
