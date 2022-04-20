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

import java.text.MessageFormat;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.dialog.TranslateConfigurableModelItemDialog;
import org.wcs.smart.dataentry.dialog.composite.AbstractInfoComposite.ISourceObjectChangedListener;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Composite with text field and "Translate" button.
 * Contains all related functionality for translating {@link NamedItem}
 * 
 * @author elitvin
 * @since 6.0.0
 */
public class CmTranslateNameComposite extends Composite implements ISourceObjectChangedListener {

	private Text text;
	private Button button;
	
	private NamedItem item;
	private Language currentLanguage = null;
	
	private boolean internalChange = false; //indicate if text was changed by user or by calling setter
	
	public CmTranslateNameComposite(Composite parent, NamedItem item) {
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
	
	protected Language getCurrentLanguage() {
		return currentLanguage;
	}
	
	public void setCurrentLanguage(Language currentLanguage) {
		this.currentLanguage = currentLanguage;
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
		initText();
		
		final ControlDecoration cd = createControlDecoration(text);
		cd.hide();
		
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!isInnerChange() && item != null) {
					text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_BLACK));

					boolean changed = false;
					String error = validate();
					if (error != null) return;
					
					changed = !text.getText().equals(item.findName(currentLanguage));
					//item.setName(text.getText());
					item.updateName(currentLanguage, text.getText().isBlank() ? null : text.getText());
					if (changed) handleChanged();
				}
			}
		});
		
		text.addListener(SWT.FocusOut, e->{
			if (text.getText().isBlank()) {
				item.setName(null);
				item.updateName(currentLanguage, null);
				this.internalChange = true;
				initText();
				this.internalChange = false;
			}
		});
		
		button = new Button(this, SWT.PUSH);
		button.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		button.setText(Messages.CmTranslateNameComposite_TranslateButton);
		button.addListener(SWT.Selection, e->{
			if (item == null) return;
			TranslateConfigurableModelItemDialog translateDialog = new TranslateConfigurableModelItemDialog(getShell(), item);
			if (translateDialog.open() == Window.OK){
				initText();
				handleChanged();
			}
		});
	}

	protected String validate() {
		String name = text.getText();
		if (name.length() > org.wcs.smart.ca.Label.MAX_LENGTH) {
			return MessageFormat.format(Messages.CmTranslateNameComposite_InvalidName, 
					org.wcs.smart.ca.Label.MAX_LENGTH);
		}
		return null;
	}

	
	protected void handleChanged() {
	}
	
	protected boolean isInnerChange() {
		return internalChange;
	}
	
	protected void initText(){
		if (item == null) return;
		String l = item.findNameNull(getCurrentLanguage());
		if (l != null) {
			text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_BLACK));
			text.setText(l);
			return;
		}
		
		if (item instanceof CmNode && ((CmNode)item).getCategory() != null) {
			l = ((CmNode)item).getCategory().findNameNull(getCurrentLanguage());
			if (l != null) {
				text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
				text.setText(l);
				return;
			}
		}else if (item instanceof CmAttribute && ((CmAttribute)item).getAttribute() != null) {
			l = ((CmAttribute)item).getAttribute().findNameNull(getCurrentLanguage());
			if (l != null) {
				text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
				text.setText(l);
				return;
			}
		}
			
		l = item.findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		if (l != null) {
			text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_BLACK));
			text.setText(l);
			return;
		}
		
		
		if (item instanceof CmNode && ((CmNode)item).getCategory() != null) {
			l = ((CmNode)item).getCategory().findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			if (l != null) {
				text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
				text.setText(l);
				return;
			}
		}else if (item instanceof CmAttribute && ((CmAttribute)item).getAttribute() != null) {
			l = ((CmAttribute)item).getAttribute().findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			if (l != null) {
				text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
				text.setText(l);
				return;
			}
		}
		
		text.setText(""); //$NON-NLS-1$
	}
	
	@Override
	public void sourceObjectChanged(Object newObject, Language language) {
		setCurrentLanguage(language);
		if (newObject instanceof NamedItem) {
			internalChange = true;
			this.item = ((NamedItem)newObject);
			initText();
			internalChange = false;
		} else if (newObject instanceof CmRootNode) {
			CmRootNode root = (CmRootNode) newObject;
			sourceObjectChanged(root.model, language);
		}
	}
}
