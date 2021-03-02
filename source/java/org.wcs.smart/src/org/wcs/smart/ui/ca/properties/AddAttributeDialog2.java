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
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;


/**
 * Dialog to create a new attribute or update an existing
 * attribute.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AddAttributeDialog2 extends SmartStyledTitleDialog {
	
	
	private Language defaultLang;			//current lang being processed
	private Collection<Attribute> siblings;	//attributes that are siblings to the current attribute being updated/created
	private Attribute toUpdate;	//attribute to update
	
	private AttributeInfoPanel attributePanel;  //attribute panel
	private Session currentSession;
	
	private String message;
	
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

		attributePanel = new AttributeInfoPanel(myparent, SWT.NONE, 
				true, toUpdate.getKeyId() == null, currentSession) {
			
			@Override
			public boolean validate(){
				nameKeyValues.setNameWarning(null);

				AddAttributeDialog2.this.setErrorMessage(null);
				boolean isvalid = super.validate();
				
				Entry<Language, String> duplicate = nameKeyValues.hasDuplicateName(toUpdate);
				if (duplicate != null) {
					String msg = MessageFormat.format(Messages.AddAttributeDialog2_duplicateNameWarning, duplicate.getValue());
					AddAttributeDialog2.this.setMessage(msg, IMessageProvider.WARNING);
					nameKeyValues.setNameWarning(msg);
				}else {
					AddAttributeDialog2.this.setMessage(message);
				}
				
				return isvalid;
			}
			
		};
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true,true);
		gd.widthHint = 200;
		attributePanel.setLayoutData(gd);
		final boolean isadd = toUpdate.getKeyId() == null;
		
		attributePanel.addValidationListener(new AttributeInfoPanel.IValidationListener() {
			@Override
			public void validated(boolean hasError) {
				Button x = getButton(IDialogConstants.OK_ID);
				if (x != null) {
					getButton(IDialogConstants.OK_ID).setEnabled(!hasError);
				}
				if (isadd) {
					switch(attributePanel.getSelectedType()) {
					case BOOLEAN:
						setMessage(Messages.AddAttributeDialog2_NewBooleanMsg);
						break;
					case DATE:
						setMessage(Messages.AddAttributeDialog2_NewDateMsg);
						break;
					case LIST:
						setMessage(Messages.AddAttributeDialog2_NewListMsg);
						break;
					case MLIST:
						setMessage(Messages.AddAttributeDialog2_NewMultiListMsg);
						break;
					case NUMERIC:
						setMessage(Messages.AddAttributeDialog2_NewNumberMsg);
						break;
					case TEXT:
						setMessage(Messages.AddAttributeDialog2_NewTextMsg);
						break;
					case TREE:
						setMessage(Messages.AddAttributeDialog2_NewTreeMsg);
						break;
					default:
						break;
					}
				}
				
			}
		});
		
		attributePanel.setAttribute(toUpdate, siblings, defaultLang);
		
		if (isadd){
			getShell().setText(Messages.AddAttributeDialog2_DialogTitle);
			setMessage(Messages.AddAttributeDialog2_New_DialogMessage);
			setTitle(Messages.AddAttributeDialog2_DialogTitle);
		}else{
			getShell().setText(Messages.AddAttributeDialog2_EditAttribute_DialogTitle);
			setMessage(MessageFormat.format(Messages.AddAttributeDialog2_Edit_DialogMessage2, new Object[]{toUpdate.getName()}));
			setTitle(Messages.AddAttributeDialog2_EditAttribute_DialogTitle);
		}
		message = getMessage();
		
		return myparent;
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
		p.y = Math.min(p.y, 750);
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
