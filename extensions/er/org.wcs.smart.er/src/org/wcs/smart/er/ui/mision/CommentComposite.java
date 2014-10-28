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
package org.wcs.smart.er.ui.mision;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
/**
 * Mission comment composite.
 * 
 * @author Emily
 *
 */
public class CommentComposite extends MissionComposite {

	private Text txtComment;
	
	@Override
	public Control createControl(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		
		part.setLayout(new GridLayout(1, false));
		
		Label l = new Label(part, SWT.NONE);
		l.setText(Messages.CommentComposite_CommentLabel);
		
		txtComment = new Text(part, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
		txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtComment.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				fireChangeListeners();
			}
		});
		txtComment.setTextLimit(Mission.MAX_LENGTH_COMMENT);
		((GridData)txtComment.getLayoutData()).widthHint = 200;
		((GridData)txtComment.getLayoutData()).heightHint = 200;
		
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return part;
	}

	@Override
	public void init(Mission mission, Session session) {
		if (mission.getComment() == null){
			txtComment.setText(""); //$NON-NLS-1$
		}else{
			txtComment.setText(mission.getComment());
		}
	}

	@Override
	public void updateDesign(Mission mission) {
		if (txtComment.getText().trim().length() > 0){
			mission.setComment(txtComment.getText().trim());
		}else{
			mission.setComment(null);
		}
	}


	@Override
	public boolean isValid() {
		return true;
	}
	
	@Override
	public String getTitle(){
		return Messages.CommentComposite_Title;
	}
	
	@Override
	public String getDescription(){
		return Messages.CommentComposite_Description;
	}

}
