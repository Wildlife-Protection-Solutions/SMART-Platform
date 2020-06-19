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
package org.wcs.smart.ui.properties;

import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledInputDialog;

/**
 * Dialog for editing key
 * @author Emily
 *
 */
public class KeyInputDialog extends SmartStyledInputDialog{

	public KeyInputDialog(Shell parentShell, final String initialValue, final Collection<? extends NamedKeyItem> siblings) {

		super(	parentShell,
		 Messages.NameKeyComposite_ChangeKey_Dialog_Title,
			Messages.NameKeyComposite_ChangeKey_Dialog_Message,
			initialValue, new IInputValidator() {

				@Override
				public String isValid(String newText) {
					if (initialValue != null && initialValue.equals(newText)){
						//same key
						return ""; //$NON-NLS-1$
					}
					String error = DataModelManager.INSTANCE.validateKey(newText, siblings);
					return error;

				}
			});
			

	}
	/**
	 * Opens the dialog without displaying
	 * a warning about modifying keys.
	 * @return
	 */
	public int openNoWarning(){
		return super.open();
	}
	
	/**
	 * Opens the dialog, but first display a warning
	 * that modifying keys may invalidate queries,reports.
	 */
	@Override
	public int open(){
		int ret = MessageDialog.open(MessageDialog.INFORMATION, getShell(), Messages.KeyInputDialog_DialogTitle, Messages.KeyInputDialog_EditKeyWarning, SWT.NONE, IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL);
		if (ret == 1) { return Window.CANCEL; }
		return super.open();
	}

}
