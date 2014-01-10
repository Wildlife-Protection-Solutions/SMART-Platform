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
package org.wcs.smart.entity.ui.editor;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.newwizard.AbstractEntityComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing properties of entity types.
 * 
 * @author Emily
 *
 */
public class EntityTypeEditPropertyDialog extends TitleAreaDialog{

	private AbstractEntityComposite editComponent;
	private EntityType toEdit;
	private Session session;
	
	public EntityTypeEditPropertyDialog(Shell parentShell, 
			AbstractEntityComposite editComponent, EntityType toEdit) {
		super(parentShell);
		
		this.editComponent = editComponent;
		this.toEdit = toEdit;
		session = HibernateManager.openSession();
		session.beginTransaction();
		session.update(toEdit);
		
	}
	
	
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btnSave = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,
				true);
		btnSave.setEnabled(false);
		
		createButton(parent, IDialogConstants.CANCEL_ID, 
				IDialogConstants.CANCEL_LABEL, false);
	}
	
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		getShell().setText(editComponent.getName());
		setTitle(editComponent.getName());
		setMessage(editComponent.getDescription());
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite c = editComponent.createComposite(main);
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,true, true));
		((GridLayout)c.getLayout()).marginWidth = 30;
		
		editComponent.initFields(toEdit, session);
		
		editComponent.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		});
		
		return parent;
	}
	@Override
	public boolean isResizable(){
		return true;
	}

	@Override
	protected void okPressed() {
		editComponent.updateEntityType(toEdit);
		session.getTransaction().commit();
		session.close();
		
		try{
			EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_MODIFIED, toEdit);
		}catch (Exception ex){
			EntityPlugIn.displayLog(ex.getMessage(), ex);
		}
		super.okPressed();
	}
	
	@Override
	protected void cancelPressed() {
		session.getTransaction().rollback();
		session.close();
		
		super.cancelPressed();
	}

}
