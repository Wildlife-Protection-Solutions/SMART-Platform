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
package org.wcs.smart.cybertracker.patrol.importer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.PatrolFilteredComboViewer;

/**
 * Dialog for selecting {@link Patrol}
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolSelectorDialog extends TitleAreaDialog {

	private PatrolFilteredComboViewer patrolId;
	private Patrol patrol;
	
	private boolean isNew;
	
	private Button btnNew;
	private Button btnAppend;

	public PatrolSelectorDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		
		
		btnNew = new Button(main, SWT.RADIO);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER,true, false);
		gd.horizontalIndent = 10;
		btnNew.setLayoutData(gd);
		
		btnNew.setSelection(true);
		btnNew.setText("Add as new patrol");
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChanged();
			}
		});
		
		btnAppend = new Button(main, SWT.RADIO);
		btnAppend.setSelection(false);
		gd = new GridData(SWT.FILL, SWT.CENTER,true, false);
		gd.horizontalIndent = 10;
		btnAppend.setLayoutData(gd);
		btnAppend.setText("Add as leg to existing patrol");
		btnAppend.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChanged();
			}
		});

		Composite appendCmp = new Composite(main, SWT.NONE);
		GridLayout appendCmpLayout = new GridLayout(1, false);
		appendCmpLayout.marginLeft = 25;
		appendCmp.setLayout(appendCmpLayout);
		appendCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		patrolId = new PatrolFilteredComboViewer(appendCmp);
		patrolId.setControlsEnabled(false);
		patrolId.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (getButton(IDialogConstants.OK_ID) != null) {
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});

		setTitle(Messages.PatrolSelectorDialog_Title);
		setMessage(Messages.PatrolSelectorDialog_Message);
		super.getShell().setText(Messages.PatrolSelectorDialog_Title);
		return composite;
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		getShell().setSize(getShell().computeSize(440, 200));
		return control;
	}	
	
	protected void optionChanged() {
		boolean enable = btnNew.getSelection() || patrolId.getSelection() != null;
		getButton(IDialogConstants.OK_ID).setEnabled(enable);
		patrolId.setControlsEnabled(btnAppend.getSelection());
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setFocus();
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			isNew = btnNew.getSelection();
			patrol = isNew ? null : patrolId.getSelection();
			setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}

	public boolean isNew() {
		return isNew;
	}

	public Patrol getSelectedPatrol() {
		return patrol;

	}
}
