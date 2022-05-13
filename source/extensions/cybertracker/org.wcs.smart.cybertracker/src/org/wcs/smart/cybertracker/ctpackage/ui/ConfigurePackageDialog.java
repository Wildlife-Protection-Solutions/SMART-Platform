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

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for managing a single ct package
 * @author Emily
 *
 */
public class ConfigurePackageDialog extends SmartStyledTitleDialog{
	
	private ICtPackage toEdit;
	private ICtPackageConfigurator config;

	@Inject
	private IEclipseContext context;
	
	private boolean isModified = false;
	
	public ConfigurePackageDialog(Shell parentShell, ICtPackage toEdit) {
		super(parentShell);
		this.toEdit = toEdit;
	}

	@Override
	protected void okPressed() {
		try {
			save();
		}catch (Exception ex) {
			CyberTrackerPlugIn.displayError(Messages.ConfigurePackageDialog_ErrorTitle, Messages.ConfigurePackageDialog_ErrorMessage +ex.getMessage(), ex);
		}
	}

	private boolean save() throws Exception{
		if (getErrorMessage() != null) {
			MessageDialog.openInformation(getShell(), Messages.ConfigurePackageDialog_SaveTitle, Messages.ConfigurePackageDialog_ErrorsExistMsg);
			return false;
		}
			
		config.save();
		isModified = false;
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		return true;
	}

	@Override
	protected void cancelPressed() {
		if (isModified) {
			if (MessageDialog.openQuestion(getShell(), Messages.ConfigurePackageDialog_SaveChangesTitle,Messages.ConfigurePackageDialog_UnSavedChangesMSg)) {
				try {
					if (!save()) return;
				}catch (Exception ex) {
					CyberTrackerPlugIn.displayError(Messages.ConfigurePackageDialog_ErrorTitle, Messages.ConfigurePackageDialog_ErrorMessage +ex.getMessage(), ex);
				}
			}
		}
		super.cancelPressed();
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		Button btn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btn.setEnabled(isModified);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		config = CtPackageExtensionPointManager.INSTANCE.findManager(toEdit).createConfigurator();
		ContextInjectionFactory.inject(config, context.createChild());
		if (config == null) {
			throw new IllegalStateException(Messages.ConfigurePackageDialog_ManagerNotFound + toEdit.getTypeIdentifier());
		}
		
		config.createGui(parent, toEdit, e -> {
			if (getButton(IDialogConstants.OK_ID) == null)
				return;
			if (e == null) {
				setErrorMessage(null);
				getButton(IDialogConstants.OK_ID).setEnabled(isModified);
			} else {
				setErrorMessage(e);
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			}
		}, e -> {
			isModified = e;
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		});
		
		if (toEdit.getUuid() == null) isModified = true;
		setMessage(Messages.ConfigurePackageDialog_ShellMessage);
		setTitle(Messages.ConfigurePackageDialog_ShellTitle);
		getShell().setText(Messages.ConfigurePackageDialog_ShellTitle);
		
		return parent;
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
}