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
package org.wcs.smart.patrol.internal.ui;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.LabelConstants;

/**
 * Composite for collecting patrol comments.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CommentComposite extends PatrolItemComposite implements ModifyListener{
	private Text txtComment;

	public CommentComposite() {

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(LabelConstants.COMMENT + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		txtComment = new Text(center, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.widthHint = 150;
		gd.heightHint = 80;
		txtComment.setLayoutData(gd);
		txtComment.addModifyListener(this);

		return main;
	}

	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void modifyText(ModifyEvent e) {
		String error = null;
		if (txtComment.getText().length() > Patrol.MAX_COMMENT_LENGTH){
			error = MessageFormat.format( Messages.CommentComposite_CommentTooLong
					, new Object[]{Patrol.MAX_COMMENT_LENGTH});
		}
		setErrorMessage(error);
		fireChangeListeners();
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(Patrol p, Session session) {
    	if (p.getComment() != null){
    		txtComment.setText(p.getComment());
    	}
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public boolean updatePatrol(Patrol p, Session session) {
		p.setComment(txtComment.getText());
		return true;
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return LabelConstants.COMMENT;		
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_COMMENT;
	}
}
