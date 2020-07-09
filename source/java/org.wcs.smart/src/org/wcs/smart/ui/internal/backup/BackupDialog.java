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
package org.wcs.smart.ui.internal.backup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for displaying system
 * backup options to the user.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class BackupDialog extends SmartStyledTitleDialog {

	// only maintain dialog settings within the current application run
	private final static DialogSettings localSettings = new DialogSettings(""); //$NON-NLS-1$
	
	private Text txtBackupFile;
	private Path selectedFile;
	private Boolean excludeFilestore;
	private String title;
	private String message;
	private String buttonText;
	private String defaultFileName;
	
	private Button chExcludeFile;
	
	private String fileNameKey;
	private boolean upgradeInstructions;
	private boolean excludeDatastoreOp;
	/**
	 * @param parentShell
	 * @param title dialog title
	 * @param message dialog message
	 * @param buttonText ok button text
	 * @param fileNameKey unique key for storing & retrieving selected filename
	 * between uses of dialog
	 * @param deafultFileName the default filename to display
	 * @param upgradeInstructions if upgrade instructions should be displayed
	 */
	public BackupDialog(Shell parentShell, String title, String message, 
			String buttonText, String fileNameKey, String defaultFileName,
			boolean upgradeInstructions, boolean exludeDatastoreOp) {
		super(parentShell);
		this.title = title;
		this.message = message;
		this.buttonText = buttonText;
		
		this.fileNameKey = fileNameKey;
		this.defaultFileName = defaultFileName;
		this.upgradeInstructions = upgradeInstructions;
		this.excludeDatastoreOp = exludeDatastoreOp;
	}
	
	/**
	 * @return the backup file selected by the user
	 */
	public Path getSelectedFile(){
		localSettings.put(fileNameKey, selectedFile.getParent().toString());
		return this.selectedFile;
	}
	
	public boolean getExcludeFilestore(){
		if (excludeDatastoreOp) return this.excludeFilestore;
		return false;
	}
	
	@Override
	public Point getInitialSize(){
		return new Point(550, 400);
	}
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.BackupDialog_FileLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtBackupFile = new Text(main, SWT.SINGLE | SWT.BORDER);
		String set = localSettings.get(fileNameKey);
		if (set == null || set.trim().isEmpty()){
			txtBackupFile.setText(this.defaultFileName);
		}else{
			Path tmp = Paths.get(set).resolve(defaultFileName);
			txtBackupFile.setText(tmp.toString());
		}
		
		txtBackupFile.setEditable(true);
		txtBackupFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		int width = txtBackupFile.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		if (width > 400){
			width = 400;
		}
		((GridData)txtBackupFile.getLayoutData()).widthHint = width;
		txtBackupFile.setSelection(txtBackupFile.getText().length());
		
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText(Messages.BackupDialog_BrowseButton);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
				Path f = Paths.get(txtBackupFile.getText());
				fd.setFilterPath(f.getParent().toString());
				fd.setFileName(f.getFileName().toString());
				fd.setFilterNames(new String[]{"zip (*.zip)", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterExtensions(new String[]{"*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				
				String file = fd.open();
				if (file == null){
					return;
				}else{
					if (!file.endsWith(".zip")){ //$NON-NLS-1$
						file = file + ".zip"; //$NON-NLS-1$
					}
					txtBackupFile.setText(file);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		if (excludeDatastoreOp) {
			new Label(main, SWT.NONE);
			chExcludeFile = new Button(main, SWT.CHECK);
			chExcludeFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			chExcludeFile.setText(Messages.BackupDialog_ExcludeFilestoreOp);
			chExcludeFile.setToolTipText(Messages.BackupDialog_ExcludeFilestoreTooltip);
		}
		
		if (upgradeInstructions){
			
			Label l = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
			
			Composite g = new Composite(main, SWT.NONE);
			g.setLayout(new GridLayout());
			((GridLayout)g.getLayout()).marginWidth = 0;
			g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
			
			Link lnk = new Link(g, SWT.NONE);
			lnk.setText(Messages.BackupDialog_UpgradeQuestion);
			
			final Text info = new Text(g, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
			info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridData)info.getLayoutData()).widthHint = 200;
			info.setText(Messages.BackupDialog_InfoMessage);
			info.setVisible(false);
			
			lnk.addListener(SWT.Selection, new org.eclipse.swt.widgets.Listener() {
				@Override
				public void handleEvent(Event event) {
					info.setVisible(true);
				}
			});
		}
		
		setTitle(title);
		setMessage(message); //"Select the file to backup the system to."
		super.getShell().setText(title);
		return composite;
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, buttonText, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.CANCEL_ID).setFocus();
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			excludeFilestore = false;
			if (chExcludeFile != null) {
				if (chExcludeFile.getSelection()){
					MessageDialog md = new MessageDialog(getShell(),
							Messages.BackupDialog_ExcludeMsgTitle, 
							null,
							Messages.BackupDialog_ExcludeMsg,
							MessageDialog.WARNING, 
							new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL},
							1);
					if (md.open() == 1){
						return;
					}
					excludeFilestore = true;
				}
			}
			
			Path file = Paths.get(txtBackupFile.getText());
			
			if (Files.exists(file)){
				if (Files.isDirectory(file)){
					MessageDialog.openError(getShell(), Messages.BackupDialog_ErrorDialogTitle, MessageFormat.format(Messages.BackupDialog_InvalidFile, file.toString()));
					return;
				}
				if (!MessageDialog.openConfirm(getShell(), Messages.BackupDialog_Confirm_DialogTitle, MessageFormat.format(Messages.BackupDialog_Confirm_Message, new Object[]{ file.toAbsolutePath()}) )){
					return;
				}
			}
			if (!Files.exists(file.getParent())){
				if (!MessageDialog.openConfirm(getShell(), 
						Messages.BackupDialog_ConfirmCreateDirTitle, 
						MessageFormat.format(Messages.BackupDialog_ConfirmCreateDirMsg, 
								new Object[]{ file.getParent()}) )){
					return;
				}
				
				if (!SmartUtils.createDirectory(file.getParent())) return;
				
			}
			
			selectedFile = file;
			super.setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

}
