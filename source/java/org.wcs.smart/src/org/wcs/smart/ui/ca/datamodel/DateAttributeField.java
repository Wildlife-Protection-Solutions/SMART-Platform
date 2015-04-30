package org.wcs.smart.ui.ca.datamodel;

import java.util.Date;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeValidator;
import org.wcs.smart.util.SmartUtils;

public class DateAttributeField implements IAttributeField<Date>{

	private Attribute attribute;
	private boolean isModified = false;
	private Date originalValue = null;
	
	private Button chSet;
	private DateTime dtime;
	private ControlDecoration cd;
	
	/**
	 * creates a new string attribute field.
	 * @param attribute
	 */
	public DateAttributeField(Attribute attribute){
		this.attribute = attribute;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#getValue()
	 * @return the string value entered or null if value entered
	 */
	@Override
	public Date getValue() {
		if (!chSet.getSelection()){
			return null;
		}
		return SmartUtils.getDate(dtime);
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(SmartUtils.formatStringForLabel(attribute.getName()) + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		Composite dtComp = new Composite(parent, SWT.NONE);
		dtComp.setLayout(new GridLayout(2, false));
		((GridLayout)dtComp.getLayout()).marginHeight = 0;
		((GridLayout)dtComp.getLayout()).marginWidth = 0;
		dtComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)dtComp.getLayoutData()).horizontalIndent = 5;
		
		chSet = new Button(dtComp,SWT.CHECK);
		chSet.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				dtime.setEnabled(chSet.getSelection());
				if (!chSet.getSelection() && originalValue == null){
					isModified = false;
				}else if (chSet.getSelection() && originalValue != null){
					Date newValue = SmartUtils.getDate(dtime);
					isModified = !originalValue.equals(newValue);
				}else{
					isModified = true;
				}
				
				validate();
			}

		});
		
		dtime = new DateTime(dtComp, SWT.DROP_DOWN | SWT.DATE | SWT.MEDIUM);
		dtime.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		
		dtime.addListener(SWT.Modify, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (!chSet.getSelection() && originalValue == null){
					isModified = false;
				}else if (chSet.getSelection() && originalValue != null){
					Date newValue = SmartUtils.getDate(dtime);
					isModified = !originalValue.equals(newValue);
				}else{
					isModified = true;
				}
				validate();
			}});
		
		cd = new ControlDecoration(dtComp, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());

		validate();
		chSet.setSelection(false);
		dtime.setEnabled(chSet.getSelection());
		this.isModified = false;
		this.originalValue = null; 
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#validate()
	 */
	@Override
	public String validate() {
		String error = AttributeValidator.validateAttribute(attribute, getValue());
		if (error != null){
			cd.setDescriptionText(error);
			cd.show();
		}else{
			cd.hide();
		}
		return error;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#getAttribute()
	 */
	@Override
	public Attribute getAttribute() {
		return this.attribute;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#clear()
	 */
	@Override
	public void clear() {
		chSet.setSelection(false);
		dtime.setEnabled(false);
		SmartUtils.initDateDateTimeWidget(dtime, new Date());
		validate();
		this.isModified = false;
		this.originalValue = null; 
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#isModified()
	 */
	@Override
	public boolean isModified(){
		return this.isModified;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setValue(java.lang.Object)
	 * @param x the initial string value
	 */
	@Override
	public void setValue(Object x){
		if (x != null & !(x instanceof Date)){
			throw new IllegalStateException("Invalid value"); //$NON-NLS-1$
		}
		this.originalValue = (Date)x;
		if (originalValue == null){
			chSet.setSelection(false);
			dtime.setEnabled(false);
		}else{
			chSet.setSelection(true);
			dtime.setEnabled(true);
			SmartUtils.initDateDateTimeWidget(dtime, originalValue);
		}
		validate();
		this.isModified = false;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setFocus()
	 */
	@Override
	public void setFocus(){
		dtime.setFocus();
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#dispose()
	 */
	@Override
	public void dispose(){
	}
}
