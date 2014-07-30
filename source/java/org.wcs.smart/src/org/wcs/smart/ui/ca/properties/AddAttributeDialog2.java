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
package org.wcs.smart.ui.ca.properties;

import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.internal.Messages;


/**
 * Dialog to create a new attribute or update an existing
 * attribute.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AddAttributeDialog2 extends TitleAreaDialog {
	
	
	private Language defaultLang;			//current lang being processed
	private Collection<Attribute> siblings;	//attributes that are siblings to the current attribute being updated/created
	private Attribute toUpdate;	//attribute to update
	
	private AttributeInfoPanel attributePanel;  //attribute panel
	private Session currentSession;
	
	/**
	 * creates a new dialog
	 * 
	 * @param parentShell
	 * @param toUpdate Attribute to update
	 * @param siblings Sibling attributes to the attribute being updated
	 * @param defaultLang the current language being modified
	 * @param currentSession active hibernate session
	 */
	public AddAttributeDialog2(Shell parentShell,  
			Attribute toUpdate,  Collection<Attribute> siblings, 
			Language defaultLang, Session currentSession) {
		super(parentShell);
		this.toUpdate = toUpdate;
		this.siblings = siblings;
		this.defaultLang = defaultLang;
		this.currentSession = currentSession;
	}

	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		Composite myparent = (Composite) super.createDialogArea(parent);
		myparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//Create an outer composite for spacing
		ScrolledComposite scrolled = new ScrolledComposite(myparent, SWT.V_SCROLL | SWT.H_SCROLL );
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));				
		// always show the focus control
		scrolled.setShowFocusedControl(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite composite = new Composite(scrolled, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(1, false));


		attributePanel = new AttributeInfoPanel(composite, SWT.NONE, 
				true, toUpdate.getKeyId() == null, currentSession);
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true,true);
		gd.widthHint = 200;
		attributePanel.setLayoutData(gd);
		attributePanel.addValidationListener(new AttributeInfoPanel.IValidationListener() {
			@Override
			public void validated(boolean hasError) {
						Button x = getButton(IDialogConstants.OK_ID);
						if (x != null) {
							if (hasError) {
								getButton(IDialogConstants.OK_ID).setEnabled(
										false);
							} else {
								getButton(IDialogConstants.OK_ID).setEnabled(
										true);
							}
						}
				
			}
		});
		
		attributePanel.setAttribute(toUpdate, siblings, defaultLang);
		
		scrolled.setContent(composite);
		scrolled.setMinSize(scrolled.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		if (toUpdate.getKeyId() == null){
			getShell().setText(Messages.AddAttributeDialog2_DialogTitle);
			setMessage(Messages.AddAttributeDialog2_New_DialogMessage);
			setTitle(Messages.AddAttributeDialog2_DialogTitle);
		}else{
			getShell().setText(Messages.AddAttributeDialog2_EditAttribute_DialogTitle);
			setMessage(MessageFormat.format(Messages.AddAttributeDialog2_Edit_DialogMessage2, new Object[]{toUpdate.getName()}));
			setTitle(Messages.AddAttributeDialog2_EditAttribute_DialogTitle);
		}
		return composite;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.FINISH_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control r = super.createContents(parent);
		
		attributePanel.validate();
		return r;
	}
	
	@Override
	public Point getInitialSize(){
		Point p = super.getInitialSize();
		p.x = Math.min(p.x, 550);
		return p;
	}
	/*
	 * updates the attribute to update with the
	 * information in the attribute panel
	 */
	private void updateAttribute(){
		attributePanel.updateAttribute(toUpdate, currentSession);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			setReturnCode(OK);
			updateAttribute();
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
		}
		
		close();
	}
}
