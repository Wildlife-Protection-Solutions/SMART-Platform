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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.dataentry.CmAttributeOptionLabelProvider;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeOption.EnterOnceType;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Info composite for {@link CmAttribute}
 *
 * @author elitvin
 * @since 2.0.0
 */
public abstract class CmAttributeInfoComposite extends AbstractInfoComposite {

	private CmAttribute attribute;

	private Label lblAttribute;
	private Label lblKey;
	
	public CmAttributeInfoComposite(Composite parent, ConfigurableModel model, Session session) {
		super(parent, model, session);
		createControls();
	}
	
	/**
	 * attribute composites have no controls
	 */
	public boolean isButtonValid(ConfigurableModelEditDialog.ControlButton button){
		return false;
	}
	
	private void createControls() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite container = createContentContainer(this);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createDisplayNameControls(container);

		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Attribute);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		lblAttribute = new Label(container, SWT.WRAP);
		lblAttribute.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		((GridData)lblAttribute.getLayoutData()).widthHint = 100;
		lblAttribute.setText(""); //$NON-NLS-1$

		label = new Label(container, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Key);
		lblKey = new Label(container, SWT.NONE);
		lblKey.setText(""); //$NON-NLS-1$
		
		final ComboViewer enterOncesComboViewer = createEnterOnceControl(container);
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttribute attr = getSourceObject();
				if (attr != null) {
					if (lblAttribute != null) {
						String text = attr.getAttribute().findNameNull(language);
						if (text == null){
							text = attr.getAttribute().findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
						}
						text += " (" + attr.getNode().getCategory().getFullCategoryName(language) + ")";  //$NON-NLS-1$//$NON-NLS-2$
						lblAttribute.setText(text);
					}
					
					if (lblKey != null)
						lblKey.setText(attr.getAttribute().getKeyId());
					
					if (enterOncesComboViewer != null) {
						CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_ENTER_ONCES);
						if (op != null && op.getStringValue() != null){
							enterOncesComboViewer.setSelection(new StructuredSelection(EnterOnceType.valueOf(op.getStringValue())));
						}else{
							enterOncesComboViewer.setSelection(new StructuredSelection(EnterOnceType.NONE));
						}
					}
					CmAttributeInfoComposite.this.layout(true, true);
				}
			}
		});

		createTypeSpecificControls(container);
	}

	protected abstract void createTypeSpecificControls(Composite container);

	protected ComboViewer createEnterOnceControl(Composite parent) {
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Option_EnableOnce);
		label.setToolTipText(Messages.CmAttributeInfoComposite_EnableOnce_Tooltip);
		
		final ComboViewer enterOncesCombo = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		enterOncesCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		enterOncesCombo.setContentProvider(ArrayContentProvider.getInstance());
		enterOncesCombo.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof EnterOnceType){
					return CmAttributeOptionLabelProvider.INSTANCE.getGuiName(((EnterOnceType)element));
				}
				return ""; //$NON-NLS-1$
			}
		});
		enterOncesCombo.setInput(EnterOnceType.values());
		
		enterOncesCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)enterOncesCombo.getSelection()).getFirstElement();
				CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_ENTER_ONCES);
				if (op == null) {
					op = CmAttributeOptionFactory.createEnterOnceOption(getSourceObject());
					getSourceObject().getCmAttributeOptions().put(op.getOptionId(),op);
				}
				boolean fire = true;
				if (x != null && x instanceof EnterOnceType) {
					fire = !x.toString().equals(op.getStringValue());
					op.setStringValue(x.toString());
				}else{
					getSourceObject().getCmAttributeOptions().remove(op.getOptionId());
					op.setStringValue(null);
				}
				if (fire) fireModelChanged();
			}
		});
		return enterOncesCombo;
		
	}
	
	protected Button createIsVisibleControl(Composite container) {
		return createBooleanControl(container, CmAttributeOption.ID_IS_VISIBLE, 
				Messages.CmAttributeInfoComposite_Option_IsVisible, "", Messages.CmAttributeInfoComposite_enabledTooltip); //$NON-NLS-1$
	}
	
	protected Button createBooleanControl(Composite parent, final String optionId, String text, String cbText, String tooltip) {
		final Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		label.setToolTipText(tooltip);
		
		final Button btnBool = new Button(parent, SWT.CHECK);
		btnBool.setText(cbText);
		btnBool.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().getCmAttributeOptions().get(optionId).setBooleanValue(btnBool.getSelection());
				fireModelChanged();
			}
		});
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(optionId);
				btnBool.setVisible(option != null);
				label.setVisible(option != null);
				if (option != null) {
					btnBool.setSelection(option.getBooleanValue());
				}
			}
		});
		return btnBool;
	}

	@Override
	public CmAttribute getSourceObject() {
		return attribute;
	}
	
	public void setSourceObject(CmAttribute attribute, Language language) {
		this.attribute = attribute;
		fireSourceObjectChanged(attribute, language);
	}

}
