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

import java.text.MessageFormat;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.WizardPage;
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
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

/**
 * Survey id wizard page.
 * 
 * @author Emily
 *
 */
public class SurveyIdPage extends WizardPage implements INewSurveyWizardPage{
	
	private Text txtId;
	private ControlDecoration cdId;
	
	protected SurveyIdPage() {
		super("ID_PAGE"); //$NON-NLS-1$
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(center, SWT.NONE);
		l.setText(Messages.SurveyIdPage_IdLabel);
		
		txtId = new Text(center, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		cdId = createDecoration(txtId);
		cdId.hide();
		
		txtId.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				if (!SmartUtils.isSimpleString(txtId.getText(), RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Survey.ID_MAX_LENGTH)){
					cdId.setDescriptionText(MessageFormat.format(Messages.SurveyIdPage_IdError, new Object[]{Survey.ID_MAX_LENGTH, RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
					cdId.show();
				}else{
					cdId.hide();
				}
				getWizard().getContainer().updateButtons();
			}
		});
		setTitle(Messages.SurveyIdPage_Title);
		setMessage(Messages.SurveyIdPage_Message);
		setControl(main);
	}

	@Override
	public boolean isPageComplete(){
		return !cdId.isVisible();
	}
	
	@Override
	public void initControls(Survey survey, Session session) {
		if (survey.getId() != null){
			txtId.setText(survey.getId());
		}else{
			txtId.setText(""); //$NON-NLS-1$
		}
		
	}

	@Override
	public boolean updateSurvey(Survey survey, Session session) {
		survey.setId(txtId.getText().trim());
		return true;
	}
	
	private ControlDecoration createDecoration(Control parent){
		ControlDecoration cd = new ControlDecoration(parent, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
}
