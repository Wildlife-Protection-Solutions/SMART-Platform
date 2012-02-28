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
package org.wcs.smart.patrol.internal.ui.editpatrol;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.ui.IPatrolItemChangeListener;
import org.wcs.smart.patrol.internal.ui.PatrolItemComposite;
import org.wcs.smart.patrol.internal.ui.PatrolSaveException;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Dialog box for editing patrol items.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EditPatrolItemDialog extends AbstractPropertyJHeaderDialog{

	private PatrolItemComposite item;
	private Patrol patrol;
	
	/**
	 * 
	 * @param parent parent shell
	 * @param item the patrol item composite to display for editing 
	 * @param patrol patrol being edited
	 * @param session active session
	 */
	public EditPatrolItemDialog(Shell parent, 
			PatrolItemComposite item, 
			Patrol patrol){
		super(parent, item.getTitle());
		this.item = item;
		this.patrol = patrol;
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		Composite comp = item.createComponent(parent, SWT.NONE);
		item.addChangeListener(new IPatrolItemChangeListener() {			
			@Override
			public void itemChanged() {
				setChangesMade(true);
				setErrorMessage(item.getErrorMessage());
				if (getButton(IDialogConstants.OK_ID) != null){
					getButton(IDialogConstants.OK_ID).setEnabled(item.getErrorMessage() == null);
				}
			}
		});
		
		Session s = getSession();
		try{
		//	s.saveOrUpdate(patrol);
			item.setValues(patrol, s);
		}finally{
			if (s.isOpen()){
				s.close();
			}
		}
		setMessage(item.getTitle());
		setChangesMade(false);
		return comp;
	}
	
	public Session getSession(){
		if (session == null || !session.isOpen()){
			session = HibernateManager.openSession(new WaypointAttachmentInterceptor());
			session.refresh(ca);
		}
		return session;
	}
	
	/**
	 * Saves the updates to he database.
	 * 
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		
		Session s = getSession();
		try{
			try{
				item.updatePatrol(patrol);
			}catch (PatrolSaveException ex){
				MessageDialog.openError(getShell(), "Error", ex.getMessage());
				return false;
			}
			if (PatrolHibernateManager.savePatrol(patrol, s, null)){
				setChangesMade(false);
				return true;
			}
			//s.saveOrUpdate(patrol);
			return false;
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog("Could not save changed to patrol. " + ex.getMessage(), ex);
		}finally{
			s.close();
		}
		
		return false;
	}

}
