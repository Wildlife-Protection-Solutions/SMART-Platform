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
package org.wcs.smart.patrol.xml.export;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Dialog box for exporting patrol data as xml.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolExportDialog extends SmartStyledTitleDialog {

	private Patrol patrol;
	private Text txtFile;
	private Button btnIncludeAttachments;

	private String fileName;
	private boolean includeAttachements;

	/**
	 * Creates a new dialog
	 * @param parentShell parent shell
	 * @param patrol patrol to export
	 */
	public PatrolExportDialog(Shell parentShell, Patrol patrol) {
		super(parentShell);
		this.patrol = patrol;

	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		fileName = txtFile.getText();
		includeAttachements = btnIncludeAttachments.getSelection();

		super.buttonPressed(buttonId);
	}

	/**
	 * @return the filename selected by user
	 */
	public String getFileName() {
		return this.fileName;
	}

	/**
	 * @return if attachments should be included
	 */
	public boolean getIncludeAttachments() {
		return this.includeAttachements;
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button b = createButton(parent, IDialogConstants.OK_ID, Messages.PatrolExportDialog_Export_Button, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);

		b.setEnabled(false);
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.PatrolExportDialog_FileLabel);
		txtFile = new Text(main, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (txtFile.getText().length() > 0) {
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText(Messages.PatrolExportDialog_BrowseButton);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(PatrolExportDialog.this
						.getShell(), SWT.SAVE);
				if (!btnIncludeAttachments.getSelection()) {
					fd.setFilterExtensions(new String[] { "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
					fd.setFilterNames(new String[] { Messages.PatrolExportDialog_XMLFilterName,
							Messages.PatrolExportDialog_AllFileFilterName });
				} else {
					fd.setFilterExtensions(new String[] { "*.zip", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
					fd.setFilterNames(new String[] { Messages.PatrolExportDialog_ZipFileFilterName,
							Messages.PatrolExportDialog_AllFileFilterName });
				}
				fd.setFilterPath(txtFile.getText());
				if (txtFile.getText().length() == 0) {
					if (btnIncludeAttachments.getSelection()) {
						fd.setFileName(patrol.getId() + ".zip"); //$NON-NLS-1$
					} else {
						fd.setFileName(patrol.getId() + ".xml"); //$NON-NLS-1$
					}
				} else {
					fd.setFileName(txtFile.getText());
				}
				String f = fd.open();
				if (f != null) {
					txtFile.setText(f);
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});

		lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.PatrolExportDialog_IncludeAttachmentsLabel + "*:");  //$NON-NLS-1$
		btnIncludeAttachments = new Button(main, SWT.CHECK);
		btnIncludeAttachments.setSelection(true);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
		btnIncludeAttachments.setLayoutData(gd);
		btnIncludeAttachments.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (txtFile.getText().length() > 0) {
					int index = txtFile.getText().lastIndexOf('.');
					String extension = ".xml"; //$NON-NLS-1$
					if (btnIncludeAttachments.getSelection()) {
						extension = ".zip"; //$NON-NLS-1$
					}
					if (index > 0) {
						txtFile.setText(txtFile.getText().substring(0, index)
								+ extension);
					}else{
						txtFile.setText(txtFile.getText() + extension);
					}
				}
			}
		});
		lbl = new Label(main, SWT.WRAP);
		lbl = new Label(main, SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd.widthHint = lbl.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		lbl.setLayoutData(gd);
		lbl.setText("*" + Messages.PatrolExportDialog_AttachmentInfoLabel); //$NON-NLS-1$

		setMessage(MessageFormat.format(Messages.PatrolExportDialog_DialogMessage, new Object[]{patrol.getId()}));
		getShell().setText(Messages.PatrolExportDialog_DialogTitle);
		return composite;

	}

}
