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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Survey design description composite.
 * 
 * @author Emily
 *
 */
public class DescriptionComposite extends SurveyDesignComposite {

	private Text txtDescription;
	
	@Override
	public Control createControl(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		
		part.setLayout(new GridLayout(1, false));
		
		Label l = new Label(part, SWT.NONE);
		l.setText("Description");
		
		txtDescription = new Text(part, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtDescription.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				fireChangeListeners();
			}
		});
		
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return part;
	}

	@Override
	public void init(SurveyDesign design) {
		if (design.getDescription() == null){
			txtDescription.setText(""); //$NON-NLS-1$
		}else{
			txtDescription.setText(design.getDescription());
		}
	}

	@Override
	public void updateDesign(SurveyDesign design) {
		if (txtDescription.getText().trim().length() > 0){
			design.setDescription(txtDescription.getText().trim());
		}else{
			design.setDescription(null);
		}
	}


	@Override
	public boolean isValid() {
		return true;
	}
	
	@Override
	public String getTitle(){
		return "Description";
	}
	
	@Override
	public String getDescription(){
		return "Enter a brief description for the survey design.";
	}

}
