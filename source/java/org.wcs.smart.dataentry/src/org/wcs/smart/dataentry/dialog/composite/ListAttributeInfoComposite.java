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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Info composite for {@link CmAttribute} of list type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ListAttributeInfoComposite extends CmAttributeInfoComposite {

	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public ListAttributeInfoComposite(Composite parent, ConfigurableModel model, Session session) {
		super(parent, model, session);
	}

	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);
		createMultiselectControl(container);
	}

	private void createMultiselectControl(Composite parent) {
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Option_Multiselect);
		final Button btnBool = new Button(parent, SWT.CHECK);
		btnBool.setText(Messages.CmAttributeInfoComposite_Option_Multi_Checkbox);
		btnBool.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_MULTISELECT).setBooleanValue(btnBool.getSelection());
				fireModelChanged();
			}
		});
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject) {
				CmAttribute cmAttr = getSourceObject();
				boolean isEnabled = cmAttr.getOrder() == 0; //must be the first element for this option to be enabled
				CmAttributeOption option = cmAttr.getCmAttributeOptions().get(CmAttributeOption.ID_MULTISELECT);
				btnBool.setVisible(option != null);
				label.setVisible(option != null);
				btnBool.setEnabled(isEnabled);
				if (option != null && isEnabled) {
					btnBool.setSelection(option.getBooleanValue());
				} else {
					btnBool.setSelection(false);
				}
			}
		});
	}

}
