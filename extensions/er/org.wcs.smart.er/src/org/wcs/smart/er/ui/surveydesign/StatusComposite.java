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
package org.wcs.smart.er.ui.surveydesign;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.hibernate.Session;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesign.State;

/**
 * Survey design composite that status fields.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class StatusComposite extends SurveyDesignComposite {

	private Button btnActive;
	private Button btnInactive;
	
	@Override
	public Control createControl(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(1, false));

		btnActive = new Button(part, SWT.RADIO);
		btnActive.setText(Messages.StatusComposite_Active);
		btnActive.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnActive.setSelection(true);
		btnActive.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fireChangeListeners();
			}
		});
		
		btnInactive = new Button(part, SWT.RADIO);
		btnInactive.setText(Messages.StatusComposite_Inactive);
		btnInactive.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnInactive.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fireChangeListeners();
			}
		});
		
		part.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		return part;
	}

	@Override
	public void init(SurveyDesign design, Session session) {
		if (design.getState() == null)
			return;
		switch (design.getState()) {
			case ACTIVE:
				btnActive.setSelection(true);
				break;
			case INACTIVE:
				btnInactive.setSelection(true);
				break;
		}
	}

	@Override
	public void updateDesign(SurveyDesign design) {
		if (btnActive.getSelection()) {
			design.setState(State.ACTIVE);
		} else {
			design.setState(State.INACTIVE);
		}
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public String getTitle() {
		return Messages.StatusComposite_Title;
	}

	@Override
	public String getDescription() {
		return Messages.StatusComposite_Description;
	}

}
