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
package org.wcs.smart.conversion.csv.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.conversion.ui.ReportDialog;

/**
 * Dialog to select columns that specify unique record when merging csv file into database.
 *  
 * @author elitvin
 * @since 3.2.0
 */
public class CsvMergeMatchDialog extends TitleAreaDialog {
	
	private List<String> matched;
	private List<String> unmatched;

	private List<String> result = new ArrayList<>();
	private boolean isCreateOption = true;
	
	private CheckboxTableViewer tableViewer;
	private Button btnCreateRow;
	private Button btnMergeRow;

	public CsvMergeMatchDialog(Shell parentShell, List<String> matched, List<String> unmatched) {
		super(parentShell);
		this.matched = matched;
		this.unmatched = unmatched;
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label noteExist = new Label(main, SWT.NONE);
		noteExist.setText("Following columns already exist in loaded data:\n"+ReportDialog.join(matched, "\n"));
		
		Label separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label noteAdded = new Label(main, SWT.NONE);
		noteAdded.setText("Following columns exist only in merged file and will be added to loaded data:\n"+ReportDialog.join(unmatched, "\n"));

		separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnCreateRow = new Button(main, SWT.RADIO);
		btnCreateRow.setText("Create new row for each merged record");
		btnCreateRow.setSelection(true);
		btnCreateRow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				tableViewer.getTable().setEnabled(false);
			}
		});
		
		btnMergeRow = new Button(main, SWT.RADIO);
		btnMergeRow.setText("Merge with existing rows if specific column values are identical (this colunms exist in both csv files)");
		btnMergeRow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				tableViewer.getTable().setEnabled(true);
			}
		});
		
		tableViewer = CheckboxTableViewer.newCheckList(main, SWT.BORDER | SWT.MULTI);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		tableViewer.getControl().setLayoutData(gd);
		tableViewer.getTable().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (tableViewer.getSelection().isEmpty()) {
					return;
				}
				if (e.keyCode == SWT.SPACE) {
					IStructuredSelection selection = ((IStructuredSelection)tableViewer.getSelection());
					selection.getFirstElement();
					boolean value = tableViewer.getChecked(selection.getFirstElement());
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = iterator.next();
						tableViewer.setChecked(tp, !value);
					}
					e.doit = false;
				}
			}
		});
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(matched);
		tableViewer.getTable().setEnabled(false);

		getShell().setText("Merge options");
		setTitle("Merge options");
		setMessage("Select an option and parameters for merge process");
		
		return composite;
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	protected void buttonPressed(int buttonId) {
		result.clear();
		if (buttonId == IDialogConstants.OK_ID) {
			isCreateOption = btnCreateRow.getSelection();
			for (Object s : tableViewer.getCheckedElements()) {
				result.add((String) s);
			}
		}
		super.buttonPressed(buttonId);
	}

	public List<String> getSelection() {
		return result;
	}
	
	public boolean isCreateOption() {
		return isCreateOption;
	}
	
}
