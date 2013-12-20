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
package org.wcs.smart.entity.ui.typelist.editor;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for editing the an attribute associated with
 * an entity type.  Users can modify the name, the
 * is required and is primary attributes.
 *  
 * @author Emily
 *
 */
public class EntityTypeEditDmAttributeDialog extends TranslateSimpleListItemDialog  {

	private Button btnIsRequired; 
	private Button btnIsPrimary; 
	
	
	public EntityTypeEditDmAttributeDialog(Shell parentShell, EntityAttribute attribute) {
		super(parentShell, attribute);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite translateComp = (Composite) super.createDialogArea(parent);
		
		Composite additionalComp = new Composite(parent, SWT.NONE);
		additionalComp.setLayout(new GridLayout());
		btnIsRequired = new Button(additionalComp, SWT.CHECK);
		btnIsRequired.setText(Messages.EntityTypeEditDmAttributeDialog_IsRequiredFieldName);
		btnIsRequired.setSelection(  ((EntityAttribute)item).getIsRequired());
		btnIsRequired.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
			}
		});
		btnIsRequired.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnIsPrimary = new Button(additionalComp, SWT.CHECK);
		btnIsPrimary.setText(Messages.EntityTypeEditDmAttributeDialog_IsPrimaryFieldName);
		btnIsPrimary.setSelection(  ((EntityAttribute)item).getIsPrimary());
		btnIsPrimary.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
			}
		});
		btnIsPrimary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		super.getShell().setText(Messages.EntityTypeEditDmAttributeDialog_EditTypeTitle);
		super.setMessage(Messages.EntityTypeEditDmAttributeDialog_EditTypeMessage);
		setTitle(Messages.EntityTypeEditDmAttributeDialog_EditTypeTitle);
		
		return translateComp;
	}
	
	@Override
	protected boolean validate(){
		boolean ok = true;
		setErrorMessage(null);
		for (org.wcs.smart.ca.Label lbl : input){
			if (!SmartUtils.isSimpleString(lbl.getValue(),
					SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX,
					org.wcs.smart.ca.Label.MAX_LENGTH, 0)) {

				setErrorMessage(MessageFormat
						.format(Messages.EntityTypeEditDmAttributeDialog_InvalidLabel,
								new Object[] {
										SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc,
										org.wcs.smart.ca.Label.MAX_LENGTH }));
				ok = false;
				break;
			
			}
		}
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null){
			if (isDirty){
				btn.setEnabled(ok);
			}else{
				btn.setEnabled(false);
			}
		}
		return ok;
	}
	
	@Override
	protected boolean save(){
		if (!validate()){
			return false;
		}
		
		if (!super.save()){
			return false;
		}
		
		((EntityAttribute)super.item).setIsRequired(btnIsRequired.getSelection());
		((EntityAttribute)super.item).setIsPrimary(btnIsPrimary.getSelection());
		
		return true;
	}
}