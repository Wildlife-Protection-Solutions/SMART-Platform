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
package org.wcs.smart.patrol.internal.ui.createpatrol;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.CommentComposite;
import org.wcs.smart.patrol.internal.ui.IPatrolItemChangeListener;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;

/**
 * Wizard page for collecting the patrol comment
 * @author egouge
 * @since 1.0.0
 */
public class CommentWizardPage extends NewPatrolWizardPage {

	
	private CommentComposite commentComp = null;


	/**
	 * 
	 */
	public CommentWizardPage() {
		super("PatrolComment"); //$NON-NLS-1$
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		commentComp = new CommentComposite();
		commentComp.addChangeListener(new IPatrolItemChangeListener() {			
			@Override
			public void itemChanged() {
				setErrorMessage(commentComp.getErrorMessage());
				setPageComplete(commentComp.getErrorMessage() == null);
			}
		});
		setTitle(Messages.CommentWizardPage_Title);
		setMessage(Messages.CommentWizardPage_PageMessage2);
		super.setControl(commentComp.createComponent(parent, SWT.NONE));
	}
	
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#updateModel()
	 */
	@Override
	public boolean updateModel(Patrol p, Session session) {
		return commentComp.updatePatrol(p, session);
	}
	
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public void initModel(Patrol p, Session session) {
		commentComp.setValues(p, session);
	}
	
}