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
package org.wcs.smart.er.ui.samplingunit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;

/**
 * Dialog for editing specific sampling unit attribute.
 * 
 * <p>Does not update the database.  If using this dialog you
 * must update the database.</p>
 * 
 * @author Emily
 *
 */
public class EditSamplingUnitAttributeDialog extends TitleAreaDialog {
	
	
	private Collection<? extends NamedKeyItem> siblings;	//attributes that are siblings to the current attribute being updated/created
	private SamplingUnitAttribute toUpdate;	//attribute to update
	
	private NameKeyComposite nameKeyControls;
	
	private ComboViewer cmbType;
	
	private HashMap<Language, String> copyNames;
	

	/**
	 * creates a new dialog
	 * 
	 * @param parentShell
	 * @param toUpdate Attribute to update
	 * @param siblings Sibling attributes to the attribute being updated
	 * @param defaultLang the current language being modified
	 * @param currentSession active hibernate session
	 */
	public EditSamplingUnitAttributeDialog(Shell parentShell,  
			SamplingUnitAttribute toUpdate,  
			Collection<? extends NamedKeyItem> siblings) {
		
		super(parentShell);
		this.toUpdate = toUpdate;
		
		this.siblings = new ArrayList<NamedKeyItem>(siblings);
		this.siblings.remove(toUpdate);

		copyNames = new HashMap<Language, String>();
		for (org.wcs.smart.ca.Label l : toUpdate.getNames()){
			copyNames.put(l.getLanguage(), l.getValue());
		}
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		Composite myparent = (Composite) super.createDialogArea(parent);
			
		Composite composite = new Composite(myparent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(3, false));

		/* Type */
		Label lblNewLabel_2 = new Label(composite, SWT.NONE);
		lblNewLabel_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_2.setText(Messages.EditSamplingUnitAttributeDialog_TypeLabel);
		
		cmbType = new ComboViewer(composite, SWT.SIMPLE | SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				return ((Attribute.AttributeType)element).name();
			}
		});
		cmbType.setInput(new Object[]{
				Attribute.AttributeType.NUMERIC,
				Attribute.AttributeType.TEXT
		});
		
		Combo combo = cmbType.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		if (toUpdate.getUuid() != null){
			combo.setEnabled(false);
		}
		
		combo.addSelectionListener(new SelectionAdapter() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
		});
		
		nameKeyControls = new NameKeyComposite();
		nameKeyControls.createControls(composite, true, toUpdate.getUuid() == null, new IChangeListener() {
			@Override
			public void itemModified() {
				validate();
			}
		});
		
		if (toUpdate.getKeyId() == null){
			getShell().setText(Messages.EditSamplingUnitAttributeDialog_NewTitle);
			setTitle(Messages.EditSamplingUnitAttributeDialog_NewTitle);
			setMessage(Messages.EditSamplingUnitAttributeDialog_NewMessage);
		}else{
			getShell().setText(Messages.EditSamplingUnitAttributeDialog_EditTitle);
			setTitle(toUpdate.getName());
			setMessage(Messages.EditSamplingUnitAttributeDialog_EditMessage);
		}

		//init fields
		nameKeyControls.initFields(toUpdate, siblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());
		cmbType.setSelection(new StructuredSelection(toUpdate.getType()));
		
		return composite;
	}
	
	
	private void validate(){
		boolean error = nameKeyControls.validate();

		if (getButton(OK) != null){
			getButton(OK).setEnabled(!error);
		}
	}
	
	
	private Attribute.AttributeType getType(){
		return (AttributeType) ((IStructuredSelection)cmbType.getSelection()).getFirstElement();
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.FINISH_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
		validate();
	}

	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	/*
	 * updates the attribute to update with the
	 * information in the attribute panel
	 */
	private void updateAttribute(){
		nameKeyControls.updateFields(toUpdate);
		toUpdate.setType(getType());
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			setReturnCode(OK);
			updateAttribute();
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
		}	
		close();
	}
}
