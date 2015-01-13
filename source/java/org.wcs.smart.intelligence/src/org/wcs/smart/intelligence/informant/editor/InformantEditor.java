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
package org.wcs.smart.intelligence.informant.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.informant.editor.InformantDataEditor.InfromantColumn;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.ui.panel.IntelligenceCompositeFactory;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author elitvin
 * @since 3.2.0
 */
public final class InformantEditor extends AbstractPropertyJHeaderDialog {
	
	private Informant informant;
	
	private Text txtId;
	private Button btnActive;

	private Map<InfromantColumn, Text> col2Text;
	
	protected InformantEditor(Shell parent, Informant informant) {
		super(parent, ""); //$NON-NLS-1$
		this.informant = informant;
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lblId = new Label(main, SWT.NONE);
		lblId.setText("Id:");

		txtId = new Text(main, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtId.setText(informant.getId());
		txtId.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				inputChanged();
			}
		});
		
		Label lblActive = new Label(main, SWT.NONE);
		lblActive.setText("Active:");

		btnActive = new Button(main, SWT.CHECK);
		btnActive.setSelection(informant.getIsActive());
		btnActive.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				inputChanged();
			}
		});

		//TODO: custom/different editors with validation?
		col2Text = new HashMap<InformantDataEditor.InfromantColumn, Text>();
		for (InfromantColumn col : InfromantColumn.values()) {
			Label lbl = new Label(main, SWT.NONE);
			lbl.setText(col.getGuiName() + ":"); //$NON-NLS-1$
			
			Text txt = new Text(main, SWT.BORDER);
			txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			Object value = informant.get(col.getKey());
			if (value != null) {
				txt.setText(value.toString());
			}
			txt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					inputChanged();
				}
			});
			col2Text.put(col, txt);
		}
		
		setChangesMade(false);
		setTitle("Edit Informant");
		setMessage("Edit informant related informantion.");
		
		return main;
	}

	protected void inputChanged() {
		setChangesMade(true);
	}

	@Override
	protected boolean performSave() {
		informant.setId(txtId.getText());
		informant.setIsActive(btnActive.getSelection());

		for (InfromantColumn col : InfromantColumn.values()) {
			Text txt = col2Text.get(col);
			informant.set(col.getKey(), txt.getText());
		}
		
		IntelligenceHibernateManager.saveInformant(informant);
		setChangesMade(false);
		
		return true;
	}

}
