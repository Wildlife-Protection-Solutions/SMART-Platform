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
package org.wcs.smart.incident.ui.newwizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Incident comment field
 * @author Emily
 *
 */
public class CommentComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.comment"; //$NON-NLS-1$
	
	private Text txtComment;
	
	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout(1, false));
		
		Label l = new Label(item, SWT.NONE);
		l.setText(Messages.CommentComposite_Label);
		
		txtComment = new Text(item, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
		txtComment.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)txtComment.getLayoutData()).widthHint = 150;
		((GridData)txtComment.getLayoutData()).heightHint = 150;
		txtComment.setTextLimit(Waypoint.COMMENT_MAX_LENGTH);
		return item;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		if (txtComment.getText().trim().isEmpty()){
			incident.setComment(null);
		}else{
			incident.setComment(txtComment.getText().trim());
		}
		
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		if (incident.getComment()== null){
			txtComment.setText(""); //$NON-NLS-1$
		}else{
			txtComment.setText(incident.getComment());
		}
	}

	@Override
	public String getName() {
		return Messages.CommentComposite_Name;
	}

	@Override
	public String getDescription() {
		return Messages.CommentComposite_Description;
	}

}