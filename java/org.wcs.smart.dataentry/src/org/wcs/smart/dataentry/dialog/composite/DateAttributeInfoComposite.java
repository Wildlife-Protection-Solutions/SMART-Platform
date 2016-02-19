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
package org.wcs.smart.dataentry.dialog.composite;

import java.util.Date;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeValidator;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab.ChangeTracker;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.util.SmartUtils;

public class DateAttributeInfoComposite extends CmAttributeInfoComposite {

	private Button chSet;
	private DateTime dtime;
	private ControlDecoration cd;
	
	private Attribute validationAttribute ;
	
	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public DateAttributeInfoComposite(Composite parent, ConfigurableModel model, ChangeTracker tracker) {
		super(parent, model, tracker);
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.dataentry.dialog.composite.CmAttributeInfoComposite#createTypeSpecificControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);
		createDateControl(container, Messages.CmAttributeInfoComposite_Option_DefaultValue);

	}

	private void createDateControl(Composite parent, String labelText) {

		//create a temporary attribute for validation purpose; we don't want
		//to validate the is required so this attribute is never required.
		validationAttribute = new Attribute();
		validationAttribute.setIsRequired(false);
		
		final Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		
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
				updateValue(true);
			}

		});
		
		dtime = new DateTime(dtComp, SWT.DROP_DOWN | SWT.DATE | SWT.MEDIUM);
		dtime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		
		final boolean[] internalChange = {false}; //indicate if text was changed by user or by calling setter
		
		cd = createControlDecoration(dtime);
		cd.hide();
		
		
		dtime.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				updateValue(true);
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				
			}
		});
		dtime.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateValue(false);
			}
		});
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);

				if (option != null) {
					internalChange[0] = true;
					if (option.getDateValue() == null){
						chSet.setSelection(false);
						dtime.setEnabled(false);
						SmartUtils.initDateDateTimeWidget(dtime, new Date());
					}else{
						chSet.setSelection(true);
						dtime.setEnabled(true);
						SmartUtils.initDateDateTimeWidget(dtime, option.getDateValue());
					}					
					internalChange[0] = false;
				}else{
					chSet.setSelection(false);
					dtime.setEnabled(false);
					SmartUtils.initDateDateTimeWidget(dtime, new Date());
				}
				validationAttribute.setRegex(getSourceObject().getAttribute().getRegex());
				validationAttribute.setName(getSourceObject().getName());
			}
		});
	}

	private void updateValue(boolean showError){
		Date defaultDate = SmartUtils.getDate(dtime);
		String error = AttributeValidator.validateDate(validationAttribute, defaultDate);
		if (error != null){
			//display error
			if (showError){
				MessageDialog.openError(getShell(), Messages.NumericAttributeInfoComposite_ErrorDialogTitle, error);
			}
			cd.setDescriptionText(error);
			cd.show();
			
			//reset to original value
			if (showError){
				CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
				if (op == null){
					chSet.setSelection(false);
					dtime.setEnabled(false);
					SmartUtils.initDateDateTimeWidget(dtime, new Date());
				}else{
					chSet.setSelection(true);
					dtime.setEnabled(true);
					SmartUtils.initDateDateTimeWidget(dtime, op.getDateValue());
				}
			}
		}else{
			CmAttributeOption op = getSourceObject().getCmAttributeOptions().get( CmAttributeOption.ID_DEFAULT_VALUE);
			if (!chSet.getSelection()){
				if (op != null){
					getSourceObject().getCmAttributeOptions().remove(op.getOptionId());
					op.setCmAttribute(null);
				}
			}else{
				if (op == null){
					op = CmAttributeOptionFactory.createDefaultValueOption(getSourceObject());
					getSourceObject().getCmAttributeOptions().put(op.getOptionId(), op);
				}
				op.setDateValue(defaultDate);
			}
			cd.hide();
			fireModelChanged();
			tracker.saveOrUpdate(getSourceObject());
		}
		
	}
}

