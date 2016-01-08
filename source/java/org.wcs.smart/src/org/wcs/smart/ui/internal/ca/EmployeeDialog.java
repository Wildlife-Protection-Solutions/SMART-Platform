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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.PermissionManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;

/**
 * Dialog for creating new employees or
 * editing existing employees.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EmployeeDialog extends Dialog {


	private Employee toUpdate;
	private ConservationArea ca;
	
	private EmployeeComposite eComposite; 
	private List<Agency> agencies;
	
	private String title = null;
	
	public static String AUTO_GENERATE = "system-generated"; //$NON-NLS-1$
	
	/**
	 * Create the dialog.
	 * 
	 * 
	 * 
	 * @param parent
	 * @param style
	 */
	public EmployeeDialog(Shell parent,  
			Employee toUpdate, ConservationArea ca,
			List<Agency> agencies, Session session) {
		
		super(parent);
		if (toUpdate == null){
			title = Messages.EmployeeDialog_Create_DialogTitle;
		}else{
			title = Messages.EmployeeDialog_Edit_DialogTitle + toUpdate.getId();
		}
		this.ca = (ConservationArea) session.load(ConservationArea.class, ca.getUuid());
		this.toUpdate = toUpdate;
		this.agencies = agencies;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override 
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		p.x = (int)(p.x * 1.2);
		return p;
	}
	
	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		
		int style = EmployeeComposite.AGENCY_RANK;
		if (PermissionManager.INSTANCE.canConfigureSmartUser()){
			style = style | EmployeeComposite.SMART_USER | EmployeeComposite.SMART_USER_LEVEL;
		}
		if (toUpdate != null){
			style = style | EmployeeComposite.END_DATE;
		}
		eComposite = new EmployeeComposite(composite, style, agencies){
			@Override
			public boolean validate(){
				boolean valid = super.validate();
				Button x = getButton(OK);
				if (x != null){
					x.setEnabled(valid);
				}
				return valid;
			}
		};
		eComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (toUpdate != null){
			eComposite.initFields(toUpdate);
		}		
		
		return parent;
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btn = createButton(parent, IDialogConstants.OK_ID, Messages.EmployeeDialog_SaveButton, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (performSave()){
				setReturnCode(OK);
				close();
			}
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
			close();
		}
	}
	private boolean isSmartIdUnique(){
		if (eComposite.getSmartUserSelected()){
			
			String smartUser = eComposite.getSmartUser();
			
			if (toUpdate != null){
				//if the user id has not changed we don't want to check it
				if (toUpdate.getSmartUserId() != null && toUpdate.getSmartUserId().equals(eComposite.getSmartUser())){
					return true;
				}					
			}
			Session tmp = HibernateManager.openSession();
			try{
			if (!HibernateManager.validateUserIdUnique(smartUser, ca, tmp)){
				MessageDialog.openError(this.getShell(), Messages.EmployeeDialog_Error_InvalidUserId_DialogTitle1, 
						MessageFormat.format(Messages.EmployeeDialog_Error_InvalidUserId_DialogMessage1, new Object[]{smartUser}));
				return false;
			}
			}finally{
				tmp.close();
			}
		}
		return true;
	}
	
	private boolean performSave(){
		if (eComposite.validate()){
			if (!isSmartIdUnique()){
				return false;
			}

			//update employee values 
			if (toUpdate == null){
				toUpdate = new Employee();
				toUpdate.setConservationArea(ca);
			}
			eComposite.updateEmploye(toUpdate);
			
			Session session = HibernateManager.openSession();
			try{
				session.beginTransaction();
				if (toUpdate.getId().equals(AUTO_GENERATE)){
					//if they left the default "automatic", auto-generate an id for them
					HibernateManager.generateEmployeeId(toUpdate, session);
				}else if (toUpdate.getUuid() != null){
					//validate that there will always be
					//one employee with admin privileges 
					//in the database
					String error = HibernateManager.validateSmartUserChanges(session, toUpdate);
					if (error != null){
						session.getTransaction().rollback();
						session.refresh(toUpdate);
						SmartPlugIn.displayLog(error, null);
						return false;
					}
				}
				//save results to database
				session.saveOrUpdate(toUpdate);
				session.getTransaction().commit();
				return true;
			}catch (RuntimeException ex){
				session.getTransaction().rollback();
				session.close();
				SmartPlugIn.displayLog(Messages.EmployeeDialog_Error_SaveError + ex.getLocalizedMessage(), ex);
				return false;
			}finally{
				session.close();
			}
		}
		return false;
	}

}
