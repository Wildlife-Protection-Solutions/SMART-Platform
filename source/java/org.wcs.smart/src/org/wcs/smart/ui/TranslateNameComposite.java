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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Composite with text field and "Translate" button.
 * Contains all related functionality for translating {@link NamedItem}
 * 
 * @author elitvin
 * @since 6.0.0
 */
public class TranslateNameComposite extends Composite {

	private Text text;
	private Button button;
	
	private NamedItem item;
	private Language currentLanguage = null;
	
	private String error;
	
	public TranslateNameComposite(Composite parent, NamedItem item) {
		super(parent, SWT.NONE);
		this.item = item;
		createControls();
	}

	private ControlDecoration createControlDecoration(Control widget){
		ControlDecoration cd = new ControlDecoration(widget, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	private void createControls() {
		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		this.setLayout(gd);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		text = new Text(this, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (item != null) {
			updateText(item);
		}
		
		final ControlDecoration cd = createControlDecoration(text);
		cd.hide();
		
		text.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				if (!isInnerChange() && item != null) {
					boolean changed = false;
					error = validateDisplayName(text.getText());
					if (error != null) {
						MessageDialog.openError(getShell(), Messages.TranslateNameComposite_ErrorDialog_Title, error);
						text.setText(item.getName() != null ? item.getName() : ""); //$NON-NLS-1$
					}else{
						changed = !text.getText().equals(item.getName());
						item.setName(text.getText());
						item.updateName(currentLanguage, item.getName());
						
					}
					if (changed){
						//only fire if name actually changed
						handleChanged();
					}
				}
			}
			
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!isInnerChange() && item != null) {
					error = validateDisplayName(text.getText());
					if (error != null) {
						cd.setDescriptionText(error);
						cd.show();
					}else{
						cd.hide();
					}
					handleChanged();
				}
			}
		});
		
		button = new Button(this, SWT.PUSH);
		button.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		button.setText(Messages.TranslateNameComposite_Button_Translate);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (item != null){
					TranslateSimpleListItemDialog translateDialog = new TranslateSimpleListItemDialog(getShell(), item);
					if (translateDialog.open() == Window.OK){
						updateText(item);
						handleChanged();
					}
					
				}
			}
		});
	}

	private String validateDisplayName(String name) {
		if (name.length() > org.wcs.smart.ca.Label.MAX_LENGTH) {
			return MessageFormat.format(Messages.TranslateNameComposite_MaxLengthError, org.wcs.smart.ca.Label.MAX_LENGTH);
		}
		if (name.trim().isEmpty()) {
			return Messages.TranslateNameComposite_EmptyError;
		}
		return null;
	}
	
	protected boolean isInnerChange() {
		return false;
	}

	protected void handleChanged() {
		//empty
	}

	protected void updateText(NamedItem item){
		String l = item.findNameNull(currentLanguage);
		if (l == null){
			l = item.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		}
		text.setText(l);
	}
	
	public Text getText() {
		return text;
	}
	
	public Button getButton() {
		return button;
	}

	protected NamedItem getItem() {
		return item;
	}
	protected void setItem(NamedItem item) {
		this.item = item;
	}
	
	protected Language getCurrentLanguage() {
		return currentLanguage;
	}
	public void setCurrentLanguage(Language currentLanguage) {
		this.currentLanguage = currentLanguage;
	}

	public String getError() {
		return error;
	}
}
