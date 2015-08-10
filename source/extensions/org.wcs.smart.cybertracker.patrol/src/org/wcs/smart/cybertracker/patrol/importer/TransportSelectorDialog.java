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

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
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
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.patrol.model.PatrolTransportType;


/**
 * Dialog for selecting patrol transport
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class TransportSelectorDialog extends TitleAreaDialog {

	private ComboViewer patrolTypeViewer;
	private PatrolTransportType transportType;
	
	private List<PatrolTransportType> types;
	private String errMessage;
	
	public TransportSelectorDialog(Shell parentShell, List<PatrolTransportType> trTypes, String errMessage) {
		super(parentShell);
		this.types = trTypes;
		this.errMessage = errMessage;
		Collections.sort(types, new Comparator<PatrolTransportType>(){
			@Override
			public int compare(PatrolTransportType o1, PatrolTransportType o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
		}});
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		if (errMessage != null) {
			Composite labelsCmp = new Composite(composite, SWT.NONE);
			labelsCmp.setLayout(new GridLayout(2, false));
			labelsCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			Label imgLabel = new Label(labelsCmp, SWT.NONE);
			imgLabel.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			Label msgLabel = new Label(labelsCmp, SWT.NONE);
	        msgLabel.setText(errMessage);
	        new Label(labelsCmp, SWT.NONE);
			Label lbl = new Label(labelsCmp, SWT.NONE);
			lbl.setText(Messages.TransportSelectorDialog_SpecifyTransport_Label);
		}

		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
        Label patrolLabel = new Label(main, SWT.NONE);
        patrolLabel.setText(Messages.TransportSelectorDialog_TransportType);
        patrolLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		patrolTypeViewer = new ComboViewer(main, SWT.READ_ONLY);
		patrolTypeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		patrolTypeViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolTypeViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof PatrolTransportType){
					return ((PatrolTransportType)element).getName();
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
        
 
		setTitle(Messages.TransportSelectorDialog_Title);
		setMessage(Messages.TransportSelectorDialog_Message);
		super.getShell().setText(Messages.TransportSelectorDialog_Title);
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
			transportType = (PatrolTransportType) ((IStructuredSelection)patrolTypeViewer.getSelection()).getFirstElement();
			setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}

	public PatrolTransportType getSelectedTransportType() {
		return transportType;
	}
	
}
