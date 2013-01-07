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
package org.wcs.smart.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for changing the name of
 * any SimpleListItem.
 * <p>Displays to the user a dialog with a language combo
 * and text box.</p>
 * 
 * @author egouge
 *
 */
public class TranslateSimpleListItemDialog extends TitleAreaDialog {

	private LanguageViewer langViewer;
	private SimpleListItem item;
	private Text txtName;
	private Language currentLang;
	private HashMap<Language, String> values;
	

	/**
	 * @param parentShell parent shell
	 * @param item item to update
	 * @param initLang initial language to display value for
	 */
	public TranslateSimpleListItemDialog(Shell parentShell, SimpleListItem item, Language initLang) {
		super(parentShell);

		this.item = item;
		values = new HashMap<Language, String>();
		for (org.wcs.smart.ca.Label l : this.item.getNames()) {
			values.put(l.getLanguage(), l.getValue());
		}
		this.currentLang = initLang;
	}

	private boolean validate(){
		boolean ok = true;
		setErrorMessage(null);
		if (values.get(SmartDB.getCurrentConservationArea().getDefaultLanguage()) == null){
			setErrorMessage(MessageFormat.format(Messages.TranslateSimpleListItemDialog_Error_LabelRequired, new Object[]{SmartDB.getCurrentConservationArea().getDefaultLanguage().getDisplayName()}));
			ok = false;
		}
		if (ok) {
			for (Entry<Language, String> value : values.entrySet()) {
				if (value.getKey().isDefault()
						&& value.getValue().trim().length() == 0) {
					setErrorMessage(MessageFormat
							.format(Messages.TranslateSimpleListItemDialog_Error_LabelRequired,
									new Object[] { value.getKey()
											.getDisplayName() }));
					ok = false;
				}
				if (!SmartUtils.isSimpleString(value.getValue(),
						SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX,
						org.wcs.smart.ca.Label.MAX_LENGTH, 0)) {

					setErrorMessage(MessageFormat
							.format(Messages.TranslateSimpleListItemDialog_Error_InvalidLabel,
									new Object[] {
											SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc,
											org.wcs.smart.ca.Label.MAX_LENGTH }));
					ok = false;
				}
			}
		}
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null){
			btn.setEnabled(ok);
		}
		return ok;
	}
	protected void okPressed() {
		if (!validate()){
			return ;
		}
		if (currentLang != null) {
			if (txtName.getText().trim().isEmpty()) {
				values.remove(currentLang);
			} else {
				values.put(currentLang, txtName.getText().trim());
			}
		}
		
		
		// update item object 
		for (Entry<Language, String> e : values.entrySet()) {
			org.wcs.smart.ca.Label lbl = null;
			for (org.wcs.smart.ca.Label tmp : this.item.getNames()){
				if (tmp.getLanguage().equals(e.getKey())){
					lbl = tmp;
					break;
				}
			}
			if (lbl != null){
				lbl.setValue(e.getValue());
			}else{
				org.wcs.smart.ca.Label l = new org.wcs.smart.ca.Label();
				l.setElement(item);
				l.setLanguage(e.getKey());
				l.setValue(e.getValue());
				this.item.getNames().add(l);
				lbl = l;
			}
			if (lbl.getLanguage().equals(SmartDB.getCurrentLanguage())){
				this.item.setName(lbl.getValue());
			}
		}
		
		List<org.wcs.smart.ca.Label> toRemove = new ArrayList<org.wcs.smart.ca.Label>();
		for (org.wcs.smart.ca.Label l : this.item.getNames()){
			String key = values.get(l.getLanguage());
			if (key == null){
				toRemove.add(l);
			}
		}
		this.item.getNames().removeAll(toRemove);
		
		super.okPressed();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		super.getShell().setText(Messages.TranslateSimpleListItemDialog_DialogTitle);
		super.setMessage(Messages.TranslateSimpleListItemDialog_DialogMessage);
		
		Composite composite = (Composite) super.createDialogArea(parent);
		composite = new Composite(composite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(2, false));

		Label lbl = new Label(composite, SWT.NONE);
		lbl.setText(Messages.TranslateSimpleListItemDialog_LanguageLabel);

		langViewer = new LanguageViewer(composite, SWT.DEFAULT,
				SmartDB.getCurrentConservationArea());
		langViewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, false));
		langViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (currentLang != null) {
					if (txtName.getText().trim().isEmpty()) {
						values.remove(currentLang);
					} else {
						values.put(currentLang, txtName.getText().trim());
					}
				}
				currentLang = langViewer.getCurrentSelection();
				String newValue = values.get(currentLang);
				if (newValue == null) {
					newValue = ""; //$NON-NLS-1$
				}
				txtName.setText(newValue);
			}
		});
		
		
		lbl = new Label(composite, SWT.NONE);
		lbl.setText(Messages.TranslateSimpleListItemDialog_NameLabel);
		txtName = new Text(composite, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addModifyListener(new ModifyListener() {			
			@Override
			public void modifyText(ModifyEvent e) {
				values.put(currentLang, txtName.getText());
				validate();	
			}
		});

		Language defaultLang = currentLang;
		currentLang = null;
		langViewer.setSelection(new StructuredSelection(defaultLang));
		
		return composite;
	}

	protected boolean isResizable() {
		return true;
	}
}
