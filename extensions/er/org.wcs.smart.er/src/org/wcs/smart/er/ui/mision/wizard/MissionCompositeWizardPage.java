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
package org.wcs.smart.er.ui.mision.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.mision.MissionComposite;
import org.wcs.smart.er.ui.mision.SurveyComposite;

/**
 * Wizard page that is comprised of a survey design composite.
 * 
 * @author Emily
 *
 */
public class MissionCompositeWizardPage extends WizardPage {

	private MissionComposite composite;
	/**
	 * Creates a new wizard page
	 * @param composite the composite represented by the wizard page
	 */
	public MissionCompositeWizardPage(MissionComposite composite){
		super(composite.getClass().getName());
		this.composite = composite;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		composite.createControl(center);
		
		super.setControl( center );
		super.setTitle(composite.getTitle());
		super.setDescription(composite.getDescription());
	}

	/**
	 * Updates the survey design with the 
	 * elements from the page.
	 * 
	 * @param design
	 */
	public void updateModel(Mission mission){
		composite.updateDesign(mission);
	}
	
	/**
	 * Updates the GUI widgets with the
	 * survey design values.
	 * 
	 * @param design
	 */
	public void initPage(Mission mission, SurveyDesign survey, Session session){
		if (composite instanceof SurveyComposite){
			((SurveyComposite)composite).init(mission, survey, session);
		}else{
			composite.init(mission, session);	
		}
		
		
		if (mission.getSurvey() != null){
			super.setTitle(mission.getSurvey().getSurveyDesign().getName() + " - " + mission.getSurvey().getId() + ""); //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			super.setTitle(composite.getTitle());	
		}
	}
	
	public MissionComposite getComposite(){
		return this.composite;
	}
	
	@Override
	public boolean isPageComplete() {
		return composite.isValid();
	}
}
