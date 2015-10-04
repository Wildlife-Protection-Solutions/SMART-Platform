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
package org.wcs.smart.cybertracker.survey.importer;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Dialog for selecting the target {@link Survey} when importing mission from CyberTracker.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class SurveySelectorDialog extends TitleAreaDialog {

	private SurveyDesign surveyDesign;

	private Text surveyNewId;
	private ComboViewer surveyComboViewer;
	private String newId;
	private Survey survey;
	private boolean isNew;
	
	private Button btnNew;
	private Button btnAppend;
	
	public SurveySelectorDialog(Shell parentShell, SurveyDesign surveyDesign) {
		super(parentShell);
		this.surveyDesign = surveyDesign;
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		
		
		btnNew = new Button(main, SWT.RADIO);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER,true, false);
		gd.horizontalIndent = 10;
		btnNew.setLayoutData(gd);
		
		btnNew.setSelection(true);
		btnNew.setText("Create a new survey");
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stateChanged();
			}
		});

		Composite newCmp = new Composite(main, SWT.NONE);
		GridLayout newCmpLayout = new GridLayout(2, false);
		newCmpLayout.marginLeft = 25;
		newCmp.setLayout(newCmpLayout);
		newCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(newCmp, SWT.NONE);
		lbl.setText("Survey name:");
		surveyNewId = new Text(newCmp, SWT.BORDER);
		surveyNewId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		surveyNewId.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				stateChanged();
			}
		});

		
		btnAppend = new Button(main, SWT.RADIO);
		btnAppend.setSelection(false);
		gd = new GridData(SWT.FILL, SWT.CENTER,true, false);
		gd.horizontalIndent = 10;
		btnAppend.setLayoutData(gd);
		btnAppend.setText("Add to existing survey");
		btnAppend.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stateChanged();
			}
		});

		Composite appendCmp = new Composite(main, SWT.NONE);
		GridLayout appendCmpLayout = new GridLayout(1, false);
		appendCmpLayout.marginLeft = 25;
		appendCmp.setLayout(appendCmpLayout);
		appendCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
        surveyComboViewer = new ComboViewer(appendCmp);
        surveyComboViewer.getControl().setEnabled(false);
        surveyComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				stateChanged();
			}
		});
        surveyComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        surveyComboViewer.setLabelProvider(new LabelProvider() {
        	@Override
        	public String getText(Object element) {
        		if (element instanceof Survey) {
        			return ((Survey)element).getId();
        		}
        		return super.getText(element);
        	}
        	
        });
        surveyComboViewer.setInput(loadSurveys());

		setTitle("survey selection");
		setMessage("Select a target survey to which selected data will be added");
		super.getShell().setText("survey selection");
		return composite;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		getShell().setSize(getShell().computeSize(440, 230));
		return control;
	}	
	
	protected void stateChanged() {
		boolean enable = (btnNew.getSelection() && !surveyNewId.getText().isEmpty()) || (btnAppend.getSelection() && !surveyComboViewer.getSelection().isEmpty());
		getButton(IDialogConstants.OK_ID).setEnabled(enable);
		surveyComboViewer.getControl().setEnabled(btnAppend.getSelection());
		surveyNewId.setEnabled(btnNew.getSelection());
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setFocus();
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			isNew = btnNew.getSelection();
			survey = isNew ? null : (Survey) ((IStructuredSelection) surveyComboViewer.getSelection()).getFirstElement();
			newId = isNew ? surveyNewId.getText() : null;
			setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}

    private List<Survey> loadSurveys() {
    	Session s = HibernateManager.openSession();
    	try {
			@SuppressWarnings("unchecked")
			List<Survey> surveys = s.createCriteria(Survey.class)
					.add(Restrictions.eq("surveyDesign", surveyDesign)) //$NON-NLS-1$
					.list();
    		return surveys;
    	} finally {
    		s.close();
    	}
    }
	
	public boolean isNew() {
		return isNew;
	}
	
	public Survey getSelectedSurvey() {
		return survey;

	}
	
	public String getNewId() {
		return newId;
	}
}
