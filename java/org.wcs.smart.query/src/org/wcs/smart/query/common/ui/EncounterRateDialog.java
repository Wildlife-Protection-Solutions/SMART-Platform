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
package org.wcs.smart.query.common.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IValueDropItem;

/**
 * Dialog for selecting encourter rate variables.
 * @author egouge
 * @since 1.0.0
 */
public class EncounterRateDialog extends TitleAreaDialog{

	private static final String ENCOUNTER_RATE = Messages.EncounterRateDialog_EncounterRateLabel;
	private Composite main = null;
	private ComboViewer viewer;
	
	private IValueDropItem selectedRate;
	
	private IValueDropItem[] encounterRateOptions;
	
	/**
	 * Creates new dialog
	 * @param parent
	 */
	public EncounterRateDialog(Shell parent, IValueDropItem[] encounterRateOptions) {
		super(parent);
		this.encounterRateOptions  = encounterRateOptions;
	}
	
	/**
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if(buttonId == IDialogConstants.OK_ID){
			if (viewer.getSelection().isEmpty()){
				MessageDialog.openError(getShell(), Messages.EncounterRateDialog_ErrorDialogTitle, Messages.EncounterRateDialog_Error_NoSelection);
				return;
			}
			selectedRate = (IValueDropItem) ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		}
		
		super.buttonPressed(buttonId);
	}
	
	/**
	 * @return the patrol value option selected for the encounter rate
	 * 
	 */
	public IValueDropItem getSelectedItems(){
		return this.selectedRate;
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		
		main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = gl.marginWidth = 20;
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Label lbl2 = new Label(main, SWT.WRAP);
		lbl2.setText(Messages.EncounterRateDialog_RateValueLabel + "*"); //$NON-NLS-1$
		
		viewer = new ComboViewer(main, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		viewer.setLabelProvider(new LabelProvider(){
			/**
			 * The <code>LabelProvider</code> implementation of this
			 * <code>ILabelProvider</code> method returns the element's
			 * <code>toString</code> string. Subclasses may override.
			 */
			public String getText(Object element) {
				if (element instanceof DropItem){
					return ((DropItem) element).getText();
				}
				return super.getText(element);
			}
		});
				
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(encounterRateOptions);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		viewer.getCombo().select(0);
		
		new Label(main, SWT.NONE);
		
		Label lbl = new Label(main, SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false,2,1);
		gd.widthHint = main.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		lbl.setText("*" + Messages.EncounterRateDialog_RateValueInfo); //$NON-NLS-1$
		lbl.setLayoutData(gd);
		
		setMessage(Messages.EncounterRateDialog_DialogMessage);
		setTitle(ENCOUNTER_RATE);
		getShell().setText(ENCOUNTER_RATE);
		return main;
		
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

}