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
package org.wcs.smart.er.ui.survey.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.ui.component.DatesComponent;

/**
 * Survey dates wizard page.
 * 
 * @author Emily
 *
 */
public class SurveyDatePage extends WizardPage implements INewSurveyWizardPage{
	
	private DatesComponent dates;
	
	protected SurveyDatePage() {
		super("DATEPAGE"); //$NON-NLS-1$
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		dates = new DatesComponent(false);
		dates.createComposite(center);
		dates.setStartDate(null);
		dates.setEndDate(null);
		dates.addModifiedListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getWizard().getContainer().updateButtons();
			}
		});

		setTitle(Messages.SurveyDatePage_Title);
		setMessage(Messages.SurveyDatePage_Message);
		setControl(main);
	}
	
	@Override
	public boolean isPageComplete(){
		return (dates.getError() == null);
	}
	
	@Override
	public void initControls(Survey survey, Session session) {
		if (survey.getStartDate() != null){
			dates.setStartDate(survey.getStartDate());
		}
		if (survey.getEndDate() != null){
			dates.setEndDate(survey.getEndDate());
		}
	}

	@Override
	public boolean updateSurvey(Survey survey, Session session) {
		survey.setStartDate(dates.getStartDate());
		survey.setEndDate(dates.getEndDate());
		return true;
	}
}
