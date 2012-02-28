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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Patrol item composite for selecting patrol objective and
 * how well object was met.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ObjectiveComposite extends PatrolItemComposite{
	private Text txtObjective;
	private Scale objectiveMet;

	public ObjectiveComposite() {

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
		lbl.setText("Patrol Objective:");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		txtObjective = new Text(center, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.widthHint = 150;
		gd.heightHint = 80;
		txtObjective.setLayoutData(gd);
		txtObjective.addModifyListener(new ModifyListener() {			
			@Override
			public void modifyText(ModifyEvent e) {
				fireChangeListeners();
			}
		});
		lbl = new Label(center, SWT.NONE);
		lbl.setText("Objective Achieved Rating:");
		lbl.setToolTipText("Identify how well the objective was met on a rating scale");
		
		Composite scale = new Composite(center, SWT.NONE);
		scale.setLayout(new GridLayout(3, false));
		lbl = new Label(scale, SWT.NONE);
		lbl.setText("Low");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		objectiveMet = new Scale(scale, SWT.HORIZONTAL);
		objectiveMet.setMinimum(1);
		objectiveMet.setMaximum(5);
		objectiveMet.setPageIncrement(1);
		objectiveMet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fireChangeListeners();
			}
		});
		
		objectiveMet.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lbl = new Label(scale, SWT.NONE);
		lbl.setText("High");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		return main;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(Patrol p, Session session) {
    	if (p.getObjective() != null){
    		txtObjective.setText(p.getObjective());
    	}
    	if (p.getObjectiveRating() != null){
    		objectiveMet.setSelection(p.getObjectiveRating());
    	}
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public void updatePatrol(Patrol p) {
		p.setObjective(txtObjective.getText());
		p.setObjectiveRating(objectiveMet.getSelection());
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Patrol Objective";
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_OBJECTIVE;
	}
}
