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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeValidator;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab.ChangeTracker;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Info composite for {@link CmAttribute} of numeric type
 *
 * @author elitvin
 * @since 2.0.0
 */
public class NumericAttributeInfoComposite extends CmAttributeInfoComposite {

	public NumericAttributeInfoComposite(Composite parent, ConfigurableModel model, ChangeTracker tracker) {
		super(parent, model, tracker);
	}

	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);
		createTextNumberControl(container, CmAttributeOption.ID_DEFAULT_VALUE, Messages.CmAttributeInfoComposite_Option_DefaultValue);
		createNumericListControl(container);
	}

	private Text createTextNumberControl(Composite parent, final String optionId, String labelText) {
		final Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		label.setToolTipText(Messages.NumericAttributeInfoComposite_defaultTooltip);
		
		final Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final boolean[] internalChange = {false}; //indicate if text was changed by user or by calling setter
		
		final ControlDecoration cd = createControlDecoration(text);
		cd.hide();
		
		//create a temporary attribute for validation purpose; we don't want
		//to validate the is required so this attribute is never required.
		final Attribute validationAttribute = new Attribute();
		validationAttribute.setIsRequired(false);
		
		
		text.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				validationAttribute.setMaxValue(getSourceObject().getAttribute().getMaxValue());
				validationAttribute.setMinValue(getSourceObject().getAttribute().getMinValue());
				validationAttribute.setName(getSourceObject().getName());
				
				String error = AttributeValidator.validateNumeric(validationAttribute, text.getText());
				if (error != null){
					//display error
					MessageDialog.openError(getShell(), Messages.NumericAttributeInfoComposite_ErrorDialogTitle, error);
					//reset to original value
					CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
					Double defaultv = null;
					if (op != null){
						defaultv = op.getDoubleValue();
					}
					text.setText(defaultv == null ? "" : defaultv.toString()); //$NON-NLS-1$
				}else{
					Double value = text.getText() == null || text.getText().isEmpty() ? null : Double.valueOf(text.getText());
					CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(optionId);
					if (value == null ){
						if (op != null){
							//remove 
							getSourceObject().getCmAttributeOptions().remove(op.getOptionId());
							op.setCmAttribute(null);
						}
					}else{
						if (op == null){
							op = CmAttributeOptionFactory.createDefaultValueOption(getSourceObject());
							getSourceObject().getCmAttributeOptions().put(op.getOptionId(), op);
						}
						op.setDoubleValue(value);
					}
					tracker.saveOrUpdate(getSourceObject());
					fireModelChanged();
				}
				cd.hide();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				
			}
		});
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (internalChange[0])
					return;
				validationAttribute.setMaxValue(getSourceObject().getAttribute().getMaxValue());
				validationAttribute.setMinValue(getSourceObject().getAttribute().getMinValue());
				validationAttribute.setName(getSourceObject().getName());
				
				String error = AttributeValidator.validateNumeric(validationAttribute, text.getText());
				if (error != null){
					//display error
					cd.setDescriptionText(error);
					cd.show();
				}else{
					cd.hide();
				}
				tracker.saveOrUpdate(getSourceObject());
				fireModelChanged();
			}
		});
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(optionId);
//				text.setVisible(option != null);
//				label.setVisible(option != null);
				if (option != null) {
					Double value = option.getDoubleValue();
					internalChange[0] = true;
					text.setText(value != null ? value.toString() : ""); //$NON-NLS-1$
					internalChange[0] = false;
				}
			}
		});
		return text;
	}

	private void createNumericListControl(Composite parent) {
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Option_Numeric);
		label.setToolTipText(Messages.NumericAttributeInfoComposite_numericOpTooltip);
		
		final Button btnBool = new Button(parent, SWT.CHECK);
		btnBool.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_NUMERIC).setBooleanValue(btnBool.getSelection());
				tracker.saveOrUpdate(getSourceObject());
				fireModelChanged();
			}
		});
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttribute cmAttr = getSourceObject();
				String disabledText = getNumericDisableText(cmAttr);
				boolean isEnabled = disabledText.isEmpty();
				CmAttributeOption option = cmAttr.getCmAttributeOptions().get(CmAttributeOption.ID_NUMERIC);
				btnBool.setVisible(option != null);
				label.setVisible(option != null);
				
				btnBool.setEnabled(isEnabled);
				if (option != null && isEnabled) {
					btnBool.setSelection(option.getBooleanValue());
				} else {
					btnBool.setSelection(false);
				}
				btnBool.setText(disabledText);
				btnBool.getParent().layout();
			}
		});
	}

	private String getNumericDisableText(CmAttribute cmAttr) {
		if (cmAttr.getNode().isCollectMultipleObservations()) {
			return Messages.CmAttributeInfoComposite_NotAllowedInMultiObservationMode;
		}
		if (cmAttr.getOrder() > 0) {
			CmAttribute prevAttr = cmAttr.getNode().getCmAttributes().get(cmAttr.getOrder()-1);
			if (!prevAttr.isMultiselect() || !prevAttr.isVisible()) {
				return Messages.NumericAttributeInfoComposite_previousInfo;
			}
		}
		return ""; //$NON-NLS-1$
	}
	
}
