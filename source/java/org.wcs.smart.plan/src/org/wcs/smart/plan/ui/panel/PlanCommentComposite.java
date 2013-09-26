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
package org.wcs.smart.plan.ui.panel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;

/**
 * Composite for editing plan comments.
 * 
 * @author Emily
 *
 */
public class PlanCommentComposite extends PlanComposite {

	private Text txtComment;

	public PlanCommentComposite(Composite parent, int style) {
		super(parent, style);

		setMessage(Messages.PlanCommentComposite_DialogMessage);
		
		this.setLayout(new GridLayout(2, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		Label descLabel = new Label(this, SWT.NONE);
		descLabel.setText(Messages.PlanCommentComposite_CommentLabel);
		descLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
				false));

		txtComment = new Text(this, SWT.BORDER | SWT.LEFT | SWT.WRAP
				| SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd.heightHint = 80;
		gd.widthHint = 100;
		gd.horizontalIndent = 8;
		txtComment.setTextLimit(Plan.MAX_COMMENT_LENGTH);

		txtComment.setLayoutData(gd);
		txtComment.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				fireInputChangeListeners();
			}
		});
	}

	@Override
	public void initFromModel(Plan plan) {
		if (plan.getComment() != null) {
			txtComment.setText(plan.getComment());
		} else {
			txtComment.setText(""); //$NON-NLS-1$
		}
	}

	@Override
	protected boolean updateModelInternal(Plan plan) {
		plan.setComment(txtComment.getText());
		return false;
	}

	@Override
	public String getTitle() {
		return Messages.PlanCommentComposite_DialogTitle;
	}

}
