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
import org.wcs.smart.intelligence.informant.aes.InformantAesManager;
import org.wcs.smart.intelligence.informant.editor.InformantDataEditor.InfromantColumn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.InformantDataKey;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Editor for particular informant record.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantEditor extends AbstractPropertyJHeaderDialog {
	
	private Informant informant;
	
	private Text txtId;
	private Button btnActive;

	private Map<InfromantColumn, Text> col2Text;

	public InformantEditor(Shell parent, Informant informant) {
		this(parent, informant, Messages.InformantEditor_Title);
	}

	protected InformantEditor(Shell parent, Informant informant, String title) {
		super(parent, title);
		this.informant = informant;
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lblId = new Label(main, SWT.NONE);
		lblId.setText(Messages.InformantEditor_Id);

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
		lblActive.setText(Messages.InformantEditor_Active);

		btnActive = new Button(main, SWT.CHECK);
		btnActive.setSelection(informant.getIsActive());
		btnActive.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				inputChanged();
			}
		});

		if (canEditSecureData()) {
			createSecureContent(main);
		} else {
			Label lblInfo = new Label(main, SWT.NONE);
			lblInfo.setText(Messages.InformantEditor_LoginNotice);
			lblInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		}
		
		setChangesMade(false);
		setTitle(Messages.InformantEditor_Title);
		setMessage(Messages.InformantEditor_Message);
		
		return main;
	}

	private void createSecureContent(Composite main) {
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
	}

	protected void inputChanged() {
		setChangesMade(true);
	}

	private boolean canEditSecureData() {
		if (InformantAesManager.getInstance().isPasswordSet()) {
			if (InformantAesManager.getInstance().isDecrypted(informant)) {
				return true;
			}
			if (informant.getEncryptedData() == null || informant.getEncryptedData().isEmpty()) {
				return !InformantAesManager.getInstance().containsInvalid();
			}
		}
		return false;
	}
	
	@Override
	protected boolean performSave() {
		informant.setId(txtId.getText());
		informant.setIsActive(btnActive.getSelection());

		if (col2Text != null) {
			Map<InformantDataKey, Object> data = new HashMap<>();
			for (InfromantColumn col : InfromantColumn.values()) {
				Text txt = col2Text.get(col);
				data.put(col.getKey(), txt.getText());
			}
			informant.set(data);
		}
		
		IntelligenceHibernateManager.saveInformant(informant);
		setChangesMade(false);
		
		return true;
	}

}
