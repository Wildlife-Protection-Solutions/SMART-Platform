/* --------- NOT USED --------------*/
//TODO: delete me
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
package org.wcs.smart.ui.properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public abstract class AbstractPropertyJDialog extends Dialog {

	protected ConservationArea ca;
	protected Session session;
	
	protected boolean changesMade;
	
	private String title;
	
	/**
	 * @param parentShell
	 */
	protected AbstractPropertyJDialog(Shell parent, String title) {
		super(parent);
		this.title = title;
		session = HibernateManager.openSession();
		getConservationArea();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
	}

	
	@Override 
	protected Point getInitialSize() {
		Rectangle r = getShell().getMonitor().getBounds();
		return new Point(r.width / 2, r.height / 2);
	}
	
	protected void getConservationArea(){
		ca = SmartDB.getCurrentConservationArea();
		session.beginTransaction();
		session.refresh(ca);		//attach the ca to the session
		session.getTransaction().commit();
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		 
		
		Composite c = createContent(composite);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return composite;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "Save", false);
//		createButton(parent, IDialogConstants.CANCEL_ID, "Undo", true);
		createButton(parent, IDialogConstants.CLOSE_ID,IDialogConstants.CLOSE_LABEL, true);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			performSave();
		} else if (IDialogConstants.CLOSE_ID == buttonId) {
			close();
		}
	}
	
	protected void setChangesMade(boolean ischanged){
		this.changesMade = ischanged;
		getButton(IDialogConstants.OK_ID).setEnabled(ischanged);
	}
	
	@Override
	public boolean close(){
		if (changesMade){
			//TODO: warning about unsaved changes
			
		}
		return super.close();
	}
	
	protected abstract Composite createContent (Composite parent);
	protected abstract void performSave();
//	protected abstract void preformRevert();
	
	@Override
	protected boolean isResizable() {
		return true;
	}
}
