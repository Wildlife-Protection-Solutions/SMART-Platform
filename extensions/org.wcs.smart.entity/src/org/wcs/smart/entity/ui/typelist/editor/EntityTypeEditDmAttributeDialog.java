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
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for editing the attribute which represents the entity
 * type in the data model.
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
		btnIsRequired.setText("Is Required");
		btnIsRequired.setSelection(  ((EntityAttribute)item).getIsRequired());
		btnIsRequired.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
			}
		});
		btnIsRequired.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnIsPrimary = new Button(additionalComp, SWT.CHECK);
		btnIsPrimary.setText("Is Primary");
		btnIsPrimary.setSelection(  ((EntityAttribute)item).getIsPrimary());
		btnIsPrimary.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
			}
		});
		btnIsPrimary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		super.getShell().setText("Edit Entity Type Attribute");
		super.setMessage("Edit the entity attribute");
		setTitle("Edit Entity Type Attribute");
		
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
						.format("Label must only contain {0} and must be less than {1, number, integer} characters in length.",
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