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
package org.wcs.smart.ui.internal.ca.properties;

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.DmObject;

/**
 * Dialog box for adding/modifing an attribute list
 * item.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeItemDialog  extends TitleAreaDialog{

	private DmObject toUpdate = null;
	private Language lang = null;
	private List<? extends DmObject> siblings;
	
	private NameKeyComposite comp;
	
	/**
	 * Creates anew dialog.
	 * 
	 * @param parent
	 * @param toUpdate the attribute list item to update
	 * @param siblings list of sibling attribute list items
	 * @param defaultLang current language
	 */
	protected AttributeItemDialog(Shell parent, DmObject toUpdate, List<? extends DmObject> siblings, Language defaultLang) {
		super(parent);
		this.toUpdate = toUpdate;
		this.lang = defaultLang;
		this.siblings = siblings;
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Attribute List Element");
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}


	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Control createDialogArea(Composite parent){
		Composite container = new Composite(parent, SWT.NONE);
		
		container.setLayout(new GridLayout(3, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		comp = new NameKeyComposite(parent, SWT.NONE) {			
			@Override
			protected Collection<? extends DmObject> getSiblings() {
				return siblings;
			}
		};
		
		comp.createNameKeyFields(container, true, toUpdate.getKeyId() == null);
		comp.initFields(toUpdate, lang);
		setMessage("Create a attribute list element");
		return container;
		
	}

	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "Save", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			setReturnCode(OK);
			comp.updateFields(toUpdate, lang);
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
		}
		
		close();
	}

}
