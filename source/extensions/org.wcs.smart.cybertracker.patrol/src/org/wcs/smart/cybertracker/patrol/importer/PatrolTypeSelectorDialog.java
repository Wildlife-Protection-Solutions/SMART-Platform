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

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolType;

/**
 * Dialog for selecting patrol type
 * 
 * @author elitvin
 * @since 3.3.0
 */
public class PatrolTypeSelectorDialog extends TitleAreaDialog {

	private ComboViewer patrolTypeViewer;
	private PatrolType type;
	
	private List<PatrolType> types;
	
	public PatrolTypeSelectorDialog(Shell parentShell, List<PatrolType> types) {
		super(parentShell);
		this.types = types;
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
        Label patrolLabel = new Label(main, SWT.NONE);
        patrolLabel.setText(Messages.PatrolTypeSelectorDialog_PatrolType);
        patrolLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		patrolTypeViewer = new ComboViewer(main, SWT.READ_ONLY);
		patrolTypeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		patrolTypeViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolTypeViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof PatrolType){
					return ((PatrolType)element).getType().getGuiName();
				}
				return super.getText(element);
			}
		});
		patrolTypeViewer.setInput(types.toArray());
		patrolTypeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (getButton(IDialogConstants.OK_ID) != null) {
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
        
 
		setTitle(Messages.PatrolTypeSelectorDialog_Title);
		setMessage(Messages.PatrolTypeSelectorDialog_Message);
		super.getShell().setText(Messages.PatrolTypeSelectorDialog_Title);
		return composite;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setFocus();
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			type = (PatrolType) ((IStructuredSelection)patrolTypeViewer.getSelection()).getFirstElement();
			setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}

	public PatrolType getSelectedType() {
		return type;
	}
}
