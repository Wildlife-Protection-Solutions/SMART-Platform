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
package org.wcs.smart.ui.internal.ca.properties;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.wcs.smart.ca.Area;
import org.wcs.smart.internal.Messages;

public class AreaIdDialog extends TitleAreaDialog {
	
	private SimpleFeatureType schema;
	private ComboViewer cmbAttributes;
	private Button opField;
	private Button opGenerated;
	
	private boolean blnGenerate;
	private AttributeDescriptor selectedAttribute;
	
	private Listener validateListener =  new Listener() {
		@Override
		public void handleEvent(Event event) {
			validate();
		}};
	
	
	
	public AreaIdDialog(Shell parentShell, SimpleFeatureType schema) {
		super(parentShell);
		
		this.schema = schema;
	}

	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		opField = new Button(main, SWT.RADIO);
		opField.setText(Messages.AreaIdDialog_Op_DefinedIDField);
		opField.addListener(SWT.Selection,validateListener);
		opField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		cmbAttributes = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbAttributes.setContentProvider(ArrayContentProvider.getInstance());
		cmbAttributes.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof AttributeDescriptor){
					return ((AttributeDescriptor)element).getLocalName();
				}
				return super.getText(element);
			}
		});
		ArrayList<AttributeDescriptor> atts = new ArrayList<AttributeDescriptor>();
		for (AttributeDescriptor att : schema.getAttributeDescriptors()){
			atts.add(att);
		}
		cmbAttributes.setInput(atts.toArray(new AttributeDescriptor[atts.size()]));
		cmbAttributes.getCombo().addListener(SWT.Modify, validateListener);
		cmbAttributes.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbAttributes.getCombo().getLayoutData()).horizontalIndent = 20;
		
		Label lblInfo = new Label(main, SWT.WRAP);
		lblInfo.setText( MessageFormat.format(Messages.AreaIdDialog_Error_IdToLong, new Object[]{ Area.ID_MAX_LENGTH }));
		lblInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblInfo.getLayoutData()).horizontalIndent = 20;
		
		opGenerated = new Button(main, SWT.RADIO);
		opGenerated.setText(Messages.AreaIdDialog_Op_UserSystemId);
		opGenerated.addListener(SWT.Selection,validateListener);
		opGenerated.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		opField.setSelection(true);
		
		setMessage(Messages.AreaIdDialog_DialogMessage);
		getShell().setText(Messages.AreaIdDialog_DialogTitle);
		return composite; 
	}
	
	private void validate(){
		if (opGenerated.getSelection()){
			getButton(IDialogConstants.OK_ID).setEnabled(true);
			cmbAttributes.getControl().setEnabled(false);
		}else if (opField.getSelection()){
			cmbAttributes.getControl().setEnabled(true);
			if (cmbAttributes.getSelection().isEmpty()){
				getButton(IDialogConstants.OK_ID).setEnabled(false);	
			}else{
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btn = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		blnGenerate = opGenerated.getSelection();
		selectedAttribute = null;
		if (!cmbAttributes.getSelection().isEmpty()){
			selectedAttribute = (AttributeDescriptor) ((IStructuredSelection)cmbAttributes.getSelection()).getFirstElement();
		}
		super.buttonPressed(buttonId);
	}
	
	public boolean useGenerated(){
		return this.blnGenerate;
	}
	public AttributeDescriptor getSelectedAttribute(){
		return this.selectedAttribute;
	}
	
	/** dialog is resizable
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}
}
