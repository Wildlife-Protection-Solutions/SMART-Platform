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
package org.wcs.smart.er.ui.surveydesign.editor;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.ISurveyListener;
import org.wcs.smart.er.ui.surveydesign.SurveyDesignComposite;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignCompositeFactory.PanelType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * EditSurveyDesignItemDialog
 * @author elitvin
 * @since 3.0.0
 */
public class EditSurveyDesignItemDialog extends AbstractPropertyJHeaderDialog {

	private SurveyDesignComposite content;
	
	private SurveyDesign surveyDesign;

	ISurveyListener changeListener = new ISurveyListener() {			
		@Override
		public void compositeModified() {
			setChangesMade(true);
			EditSurveyDesignItemDialog.this.setErrorMessage(!content.isValid() ? Messages.EditSurveyDesignItemDialog_ErrorMsg : null); 
			if (getButton(IDialogConstants.OK_ID) != null) {
				getButton(IDialogConstants.OK_ID).setEnabled(content.isValid());
			}
		}
	};
	
	public EditSurveyDesignItemDialog(Shell shell, PanelType panelType, SurveyDesign surveyDesign) {
		super(shell, ""); //$NON-NLS-1$
		this.surveyDesign = surveyDesign;
		
		this.content = SurveyDesignCompositeFactory.getInstance().createComposite(panelType);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(content.getTitle());
	}
	
	@Override
	protected Composite createContent(Composite parent) {
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		
		content.createControl(center);
		Session s = HibernateManager.openSession();
		try{
			content.init(surveyDesign, s);
		}finally{
			s.close();
		}
		content.addChangeListener(changeListener);
		
		setChangesMade(false);
		setTitle(content.getTitle());
		setMessage(content.getDescription());
		return center;
	}

	@Override
	protected boolean performSave() {
		content.updateDesign(surveyDesign);
		
		SaveSurveyDesignJob saveJob = new SaveSurveyDesignJob(surveyDesign);    
    	saveJob.schedule();
    	try {
			saveJob.join(); //we don't want to close editor if save failed
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

    	
    	if (Status.OK_STATUS.equals(saveJob.getResult())) {
			setChangesMade(false);
			SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, surveyDesign);
			return true;
    	}
		return false;
	}

}
