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
import org.wcs.smart.util.SmartUtils;

/**
 * Track distance direction survey design property.
 * 
 * @author Emily
 *
 */
public class DistanceDirectionComposite extends SurveyDesignComposite {

	private Button chDistance;
	private Button chObserver;
	
	@Override
	public Control createControl(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		
		part.setLayout(new GridLayout(1, false));
		
		Composite inner = new Composite(part, SWT.NONE);
		inner.setLayout(new GridLayout(1, false));
			
		chDistance = new Button(inner, SWT.CHECK);
		chDistance.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fireChangeListeners();	
			}
		});
		chDistance.setText(SmartUtils.formatStringForLabel(Messages.DistanceDirectionComposite_Label));
		
		chObserver = new Button(inner, SWT.CHECK);
		chObserver.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fireChangeListeners();	
			}
		});
		chObserver.setText(SmartUtils.formatStringForLabel(Messages.DistanceDirectionComposite_RecordObserverOption));

		inner.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return part;
	}

	@Override
	public void init(SurveyDesign design, Session session) {
		chDistance.setSelection(design.getTrackDistanceDirection());
		chObserver.setSelection(design.getTrackObserver());
	}

	@Override
	public void updateDesign(SurveyDesign design) {
		design.setTrackDistanceDirection(chDistance.getSelection());
		design.setTrackObserver(chObserver.getSelection());
	}


	@Override
	public boolean isValid() {
		return true;
	}
	
	@Override
	public String getTitle(){
		return Messages.DistanceDirectionComposite_Title;
	}
	
	@Override
	public String getDescription(){
		return Messages.DistanceDirectionComposite_Description;
	}

}
