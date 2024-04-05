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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeValidator;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Info composite for {@link CmAttribute} of text type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class TextAttributeInfoComposite extends CmAttributeInfoComposite {

	private static final String QAKEY = "UPDATE"; //$NON-NLS-1$

	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public TextAttributeInfoComposite(Composite parent, ConfigurableModel model, Session session) {
		super(parent, model, session);
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.dataentry.dialog.composite.CmAttributeInfoComposite#createTypeSpecificControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);
		createTextStringControl(container, CmAttributeOption.ID_DEFAULT_VALUE, Messages.CmAttributeInfoComposite_Option_DefaultValue);

		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.TextAttributeInfoComposite_QRCodeOp);
		label.setToolTipText(Messages.TextAttributeInfoComposite_QRCodeTooltip);
		
		Button btnQa = new Button(container, SWT.CHECK);
		btnQa.setData(QAKEY, false);
		btnQa.addListener(SWT.Selection, e->{
			if (!(Boolean)btnQa.getData(QAKEY)){
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_QR_CODE);
				if (option == null) {
					option = new CmAttributeOption();
					option.setCmAttribute(getSourceObject());
					option.setOptionId(CmAttributeOption.ID_QR_CODE);
					getSourceObject().getCmAttributeOptions().put(CmAttributeOption.ID_QR_CODE, option);
				}
				option.setBooleanValue(btnQa.getSelection());
				fireModelChanged();
			}
		});
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_QR_CODE);
				Boolean value = Boolean.FALSE;
				if (option != null && option.getBooleanValue() != null) value = option.getBooleanValue();
				
				btnQa.setData(QAKEY, true);
				try {
					btnQa.setSelection(value);
				}finally {
					btnQa.setData(QAKEY, false);
				}
			}
		});
		
		label = new Label(container, SWT.NONE);
		label.setText(Messages.TextAttributeInfoComposite_NumPadOp);
		label.setToolTipText(Messages.TextAttributeInfoComposite_NumPadTooltip);
		
		Button btnNumpad = new Button(container, SWT.CHECK);
		btnNumpad.setData(QAKEY, false);
		btnNumpad.addListener(SWT.Selection, e->{
			if (!(Boolean)btnNumpad.getData(QAKEY)){
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_USENUMPAD);
				if (option == null) {
					option = new CmAttributeOption();
					option.setCmAttribute(getSourceObject());
					option.setOptionId(CmAttributeOption.ID_USENUMPAD);
					getSourceObject().getCmAttributeOptions().put(CmAttributeOption.ID_USENUMPAD, option);
				}
				option.setBooleanValue(btnNumpad.getSelection());
				fireModelChanged();
			}
		});
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_USENUMPAD);
				Boolean value = Boolean.FALSE;
				if (option != null && option.getBooleanValue() != null) value = option.getBooleanValue();
				
				btnNumpad.setData(QAKEY, true);
				try {
					btnNumpad.setSelection(value);
				}finally {
					btnNumpad.setData(QAKEY, false);
				}
			}
		});
	}

	private Text createTextStringControl(Composite parent, final String optionId, String labelText) {
		final Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
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
				validationAttribute.setRegex(getSourceObject().getAttribute().getRegex());
				validationAttribute.setName(getSourceObject().getName());
				String error = AttributeValidator.validateString(validationAttribute, text.getText());
				if (error != null){
					//display error
					MessageDialog.openError(getShell(), Messages.NumericAttributeInfoComposite_ErrorDialogTitle, error);
					//reset to original value
					String defaultv = null;
					CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
					if (op != null){
						defaultv = op.getStringValue();
					}
					text.setText(defaultv == null ? "" : defaultv.toString()); //$NON-NLS-1$
				}else{
					CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(optionId);
					if (text.getText().trim().length() == 0){
						if (op != null){
							getSourceObject().getCmAttributeOptions().remove(op.getOptionId());
						}
					}else{
						if (op == null){
							op = CmAttributeOptionFactory.createDefaultValueOption(getSourceObject());
							getSourceObject().getCmAttributeOptions().put(op.getOptionId(), op);
						}
						op.setStringValue(text.getText());
					}
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
				validationAttribute.setRegex(getSourceObject().getAttribute().getRegex());
				validationAttribute.setName(getSourceObject().getName());
				String error = AttributeValidator.validateString(validationAttribute, text.getText());
				if (error != null){
					cd.setDescriptionText(error);
					cd.show();
				}else{
					cd.hide();
				}
				fireModelChanged();
			}
		});
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(optionId);
				String value = ""; //$NON-NLS-1$
				if (option != null && option.getStringValue() != null) value = option.getStringValue();
				internalChange[0] = true;
				text.setText(value);
				internalChange[0] = false;
			}
		});
		return text;
	}
	
}
