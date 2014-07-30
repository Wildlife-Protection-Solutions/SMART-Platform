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
package org.wcs.smart.ui.internal.ca;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.internal.ca.properties.CategoryInfoPanel;

/**
 * Dialog page for creating/editing category.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CategoryDialogPage  extends TitleAreaDialog {

	private Language defaultLang;
	private Category toUpdate;
	private List<Category> sibilings;
	private Button okBtn; 
	
	private CategoryInfoPanel cip;
	/**
	 * Creates new category dialog page
	 * @param parentShell
	 * @param toUpdate category to update
	 * @param sibilings sibling categories
	 * @param defaultLang current language 
	 */
	public CategoryDialogPage(Shell parentShell, Category toUpdate, List<Category> sibilings, Language defaultLang) {
		super(parentShell);
		this.defaultLang = defaultLang;
		this.toUpdate = toUpdate;
		this.sibilings = sibilings;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(Messages.CategoryDialogPage_DialogTitle);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	public Point getInitialSize(){
		Point p = super.getInitialSize();
		p.x = Math.min(p.x, 550);
		return p;
	}
	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		
		cip = new CategoryInfoPanel(composite, SWT.NONE, true, toUpdate.getKeyId() == null) {	
			@Override
			public boolean validate(){
				boolean valid = super.validate();
				if(okBtn != null)okBtn.setEnabled(!valid);
				return valid;
			}
			
		};
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 300;
		cip.setLayoutData(gd);
		cip.setCategory(toUpdate, sibilings, this.defaultLang);
		
		if (toUpdate.getKeyId() == null){
			setMessage(Messages.CategoryDialogPage_New_DialogMessage);
			setTitle(Messages.CategoryDialogPage_CreatePageTitle);
		}else{
			setMessage(MessageFormat.format(Messages.CategoryDialogPage_Edit_DialogTitle2, new Object[]{toUpdate.getName()}));
			setTitle(Messages.CategoryDialogPage_EditPageTitle);
		}
		return parent;
		
	}
	
	public void updateCategory(){
		cip.updateCategory(toUpdate);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okBtn = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		okBtn.setEnabled(false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			setReturnCode(OK);
			updateCategory();
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
		}
		
		close();
	}
}
