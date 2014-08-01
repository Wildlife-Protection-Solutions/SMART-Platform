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
package org.wcs.smart.er.ui.surveydesign.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.surveydesign.SurveyDesignComposite;

/**
 * Wizard page that is comprised of a survey design composite.
 * 
 * @author Emily
 *
 */
public class SurveyCompositeWizardPage extends WizardPage {

	private SurveyDesignComposite composite;
	
	/**
	 * Creates a new wizard page
	 * @param composite the composite represened by wizard page
	 */
	public SurveyCompositeWizardPage(SurveyDesignComposite composite){
		super(composite.getClass().getName());
		this.composite = composite;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		
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
	public void updateModel(SurveyDesign design){
		composite.updateDesign(design);
	}
	
	/**
	 * Updates the GUI widgets with the
	 * survey design values.
	 * 
	 * @param design
	 */
	public void initPage(SurveyDesign design){
		composite.init(design, ((NewSurveyDesignWizard)getWizard()).getSession()  );
	}
	
	@Override
	public boolean isPageComplete() {
		return composite.isValid();
	}
}
