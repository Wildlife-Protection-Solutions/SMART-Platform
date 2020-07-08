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
package org.wcs.smart.common.control;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
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
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Dialog for selecting file to import
 * 
 * @author Emily
 * @since 1.0.0
 */
public class XmlImportDialog  extends SmartStyledTitleDialog {

	private static final String[] FILTER_EXTENSIONS = new String[] { "*.zip;*.xml", "*.zip", "*.xml", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	private static final String[] FILTER_NAMES = new String[] { Messages.XmlImportDialog_Filter_Supported, Messages.XmlImportDialog_Filter_Xml, Messages.XmlImportDialog_Filter_Zip, Messages.XmlImportDialog_Filter_All };

	private ListViewer lstFiles;
	private Button btnAdd;
	private Button btnRemove;
	
	private ArrayList<String> files = new ArrayList<String>();
	
	private String dialogTitle = ""; //$NON-NLS-1$
	private String dialogText = ""; //$NON-NLS-1$
	private String dialogMessage = ""; //$NON-NLS-1$	
	
	/**
	 * Creates a new dialog
	 * @param parentShell parent shell
	 */
	public XmlImportDialog(Shell parentShell) {
		super(parentShell);

	}

	/**
	 * Creates a new dialog
	 * @param parentShell parent shell
	 */
	public XmlImportDialog(Shell parentShell, String dialogTitle, String dialogText, String dialogMessage) {
		super(parentShell);
		this.dialogTitle = dialogTitle;
		this.dialogText = dialogText;
		this.dialogMessage = dialogMessage;
	}
	

	/**
	 * @return the filename selected by user or 
	 * a list of files
	 */
	public List<String> getFileNames() {
		return files;
	}


	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button b = createButton(parent, IDialogConstants.OK_ID, Messages.XmlImportDialog_ImportButton, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

		b.setEnabled(false);
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.XmlImportDialog_Files_Label);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
		
		lstFiles = new ListViewer(main, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		lstFiles.setContentProvider(ArrayContentProvider.getInstance());
		lstFiles.setLabelProvider(new LabelProvider());
		lstFiles.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstFiles.setInput(files);
		
		Composite buttons = new Composite(main, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginTop = gl.marginBottom = gl.marginHeight = 0;
		buttons.setLayout(gl);		
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
		
		btnAdd = new Button(buttons, SWT.PUSH);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setBackground(buttons.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setText(Messages.XmlImportDialog_AddButton);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.MULTI | SWT.OPEN );
				
				fd.setFilterExtensions(FILTER_EXTENSIONS);
				fd.setFilterNames(FILTER_NAMES);
				
				String x = fd.open();
				if (x != null){
					for (int i = 0; i < fd.getFileNames().length; i ++){
						String file = (Paths.get(fd.getFilterPath(),fd.getFileNames()[i])).toString() ;
						if (!files.contains(file)){
							files.add(file);
						}
					}
					lstFiles.refresh();
				}
				getButton(IDialogConstants.OK_ID).setEnabled(files.size() > 0);
			}		
		});
		
		btnRemove = new Button(buttons, SWT.PUSH);
		btnRemove.setText(Messages.XmlImportDialog_RemoveButton);
		btnRemove.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnRemove.setBackground(buttons.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = ((IStructuredSelection)lstFiles.getSelection());
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();
					files.remove(x);
				}
				lstFiles.refresh();
				getButton(IDialogConstants.OK_ID).setEnabled(files.size() > 0);
			}	
		});
		
		setMessage(dialogMessage);
		setTitle(dialogTitle);
		getShell().setText(dialogText);
		return composite;
	}

}
