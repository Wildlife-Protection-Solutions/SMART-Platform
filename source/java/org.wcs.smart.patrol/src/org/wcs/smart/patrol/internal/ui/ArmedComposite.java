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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.LabelConstants;

/**
 * Simple composite that asks if
 * a patrol is armed.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ArmedComposite  extends PatrolItemComposite{

	private Button btnYes;
	private Button btnNo;

	/**
	 * 
	 */
	public ArmedComposite() {

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));

		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.ArmedComposite_Message2);

		Composite buttonPanel = new Composite(center, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(1, false));
		buttonPanel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
				false));

		btnYes = new Button(buttonPanel, SWT.RADIO);
		btnYes.setText(Messages.ArmedComposite_OpYes);
		btnYes.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnYes.setSelection(true);
		btnYes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fireChangeListeners();
			}
		});
		
		btnNo = new Button(buttonPanel, SWT.RADIO);
		btnNo.setText(Messages.ArmedComposite_OpNo);
		btnNo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnNo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fireChangeListeners();
			}
		});
		return main;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(Patrol p, Session session) {
		btnYes.setSelection(p.isArmed());
		btnNo.setSelection(!p.isArmed());
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public boolean updatePatrol(Patrol p, Session session) {
		p.setArmed(btnYes.getSelection());
		return true;
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return LabelConstants.ARMED;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_ARMED;
	}
}
