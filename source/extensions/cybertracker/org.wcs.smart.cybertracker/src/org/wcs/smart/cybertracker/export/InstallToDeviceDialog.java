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
package org.wcs.smart.cybertracker.export;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.MobileDeviceUtils;
import org.wcs.smart.cybertracker.internal.Messages;

/**
 * Dialog for installing apk to device.
 * 
 * @author Emily
 *
 */
public class InstallToDeviceDialog extends TitleAreaDialog{

	private static final String ERROR_MSG = Messages.InstallToDeviceDialog_InstallFailed;
	private static final String FILE_KEY = "org.wcs.smart.cybertracker.export.InstallToDeviceDialog.file";  //$NON-NLS-1$
	
	private Text txtApk;
	
	
	
	public InstallToDeviceDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, Messages.InstallToDeviceDialog_InstallButtonText, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setFocus();		
		super.setReturnCode(IDialogConstants.CANCEL_ID);
		
		updateButtons();
	}
	
	@Override
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		if (p.x > 600) return new Point(600, p.y);
		return p;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		parent = (Composite) super.createDialogArea(parent);
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(3, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(c, SWT.NONE);
		l.setText(Messages.InstallToDeviceDialog_APKFileLabel);
		
		txtApk = new Text(c, SWT.BORDER);
		txtApk.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (CyberTrackerPlugIn.getDefault().getPreferenceStore().contains(FILE_KEY)) {
			txtApk.setText(CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(FILE_KEY));
		}
		txtApk.addListener(SWT.Modify, e->updateButtons());
		
		Button btnBrowse = new Button(c, SWT.PUSH);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.addListener(SWT.Selection, e->selectFile());
		
		l = new Label(c, SWT.WRAP);
		l.setText(Messages.InstallToDeviceDialog_APKInstallInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		((GridData)l.getLayoutData()).widthHint = 300; 
		
		getShell().setText(Messages.InstallToDeviceDialog_ShellTitle);
		setTitle(Messages.InstallToDeviceDialog_MessageTitle);
		setMessage(Messages.InstallToDeviceDialog_Message);

		if (!CyberTrackerPlugIn.getDefault().isWindows()) {
			setErrorMessage(Messages.InstallToDeviceDialog_NotSupportedMessage);
			l.setEnabled(false);
			txtApk.setEnabled(false);
			btnBrowse.setEnabled(false);
		}
		
		
		return parent;
	}

	
	@Override
	public void okPressed() {
		Path p = Paths.get(txtApk.getText());
		
		CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(FILE_KEY, p.toString());
		
		if (!Files.exists(p)) {
			MessageDialog.openError(getShell(), Messages.InstallToDeviceDialog_ErrorTitle, MessageFormat.format(Messages.InstallToDeviceDialog_FileNotFound, p.toString()));
			return;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						MobileDeviceUtils.exportApkToDevice(p);
					}catch (Exception ex) {
						throw new InvocationTargetException(ex, ex.getMessage()); 
					}
				}
			});
		}catch (Exception ex) {
			String temp = ERROR_MSG + "\n\n{0}"; //$NON-NLS-1$
			CyberTrackerPlugIn.displayError(Messages.InstallToDeviceDialog_ErrorTitle, MessageFormat.format(temp, ex.getMessage()), ex);
			return;
		}
		
		MessageDialog.openInformation(getShell(), Messages.InstallToDeviceDialog_CopyComplete, MessageFormat.format(Messages.InstallToDeviceDialog_CopyCompleteMsg, p.getFileName().toString()));
		super.okPressed();
		
	}
	private void updateButtons() {
		if (txtApk.getText().isEmpty()) {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}else {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}
	}
	
	private void selectFile() {
		
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
		
		fd.setFilterExtensions(new String[] {"*.apk", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[] {Messages.InstallToDeviceDialog_APKFiles, Messages.InstallToDeviceDialog_AllFiles});
		
		String init = txtApk.getText().trim();
		if (!init.isEmpty()) {
			fd.setFilterPath(init);
		}
		
		String file = fd.open();
		if (file != null) {
			txtApk.setText(file);
		}
	}
	
	
	@Override
	protected boolean isResizable() {
		return true;
	}
}
