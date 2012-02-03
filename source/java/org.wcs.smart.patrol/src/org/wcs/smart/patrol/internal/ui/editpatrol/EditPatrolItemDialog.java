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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.ui.IPatrolItemChangeListener;
import org.wcs.smart.patrol.internal.ui.PatrolItemComposite;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

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
public class EditPatrolItemDialog extends AbstractPropertyJHeaderDialog{

	private PatrolItemComposite item;
	private Patrol patrol;
	private Session session;
	
	public EditPatrolItemDialog(Shell parent, 
			PatrolItemComposite item, 
			Patrol patrol, Session session){
		super(parent, item.getTitle());
		this.item = item;
		this.patrol = patrol;
		this.session = session;
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		Composite comp = item.createComponent(parent, SWT.NONE);
		item.addChangeListener(new IPatrolItemChangeListener() {			
			@Override
			public void itemChanged() {
				setChangesMade(true);
				
			}
		});
		item.setValues(patrol, session);
		setChangesMade(false);
		return comp;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		item.updatePatrol(patrol);
		
		session.beginTransaction();
		try{
			session.save(patrol);
			session.getTransaction().commit();
			setChangesMade(false);
			return true;
		}catch (Exception ex){
			session.getTransaction().rollback();
			session.close();
			SmartPatrolPlugIn.displayLog("Could not save changed to patrol. " + ex.getMessage(), ex);
		}
		
		return false;
	}

}
