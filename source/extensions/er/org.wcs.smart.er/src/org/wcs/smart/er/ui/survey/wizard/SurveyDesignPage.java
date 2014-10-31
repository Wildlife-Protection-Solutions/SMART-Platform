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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.er.hibernate.SurveyDesignFilter;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesign.State;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;
import org.wcs.smart.er.ui.handlers.NewSurveyDesignHandler;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Survey design wizard page.
 * 
 * @author Emily
 *
 */
public class SurveyDesignPage extends WizardPage implements INewSurveyWizardPage{

	private ComboViewer cmbViewer;
	
	protected SurveyDesignPage() {
		super("DESIGN_PAGE"); //$NON-NLS-1$
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		Label l = new Label(center, SWT.NONE);
		l.setText(Messages.SurveyDesignPage_Label);
		
		cmbViewer = new ComboViewer(center, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbViewer.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)cmbViewer.getSelection()).getFirstElement();
				if (x instanceof String){
					createSurveyDesign();
				}
			}
		});
		
		setTitle(Messages.SurveyDesignPage_Title);
		setMessage(Messages.SurveyDesignPage_Message);
		
		setControl(main);
	}
	
	@Override
	public void initControls(Survey survey, Session session) {
		loadDesigns(session, survey.getSurveyDesign());
	}
	
	private void loadDesigns(Session session, SurveyDesign init){
		SurveyDesignFilter filter = new SurveyDesignFilter();
		filter.setSurveyStates(new State[]{SurveyDesign.State.ACTIVE});
		List<Object> items = new ArrayList<Object>();
		items.addAll(SurveyHibernateManager.getInstance().getSurveyDesignEditorInputs(session, filter));
		items.add(Messages.SurveyDesignPage_NewDesignItem);
		cmbViewer.setInput(items);
		
		if (init != null){
			SurveyDesignEditorInput sdei = new SurveyDesignEditorInput(init.getName(), init.getUuid(), init.getKeyId(), init.getState());
			if (!items.contains(sdei)){
				items.add(sdei);
			}
			cmbViewer.refresh();
			cmbViewer.setSelection(new StructuredSelection(sdei));
		}
	}

	private void createSurveyDesign(){
		//New Survey Design Wizard...
		//this will close the hibernate current hibernate session
		SurveyDesign sd = NewSurveyDesignHandler.showNewDesignWizard(getShell());
		if (sd == null || sd.getUuid() == null){
			//new design not created
			return;
		}			
		
		Session session = HibernateManager.openSession();
		loadDesigns(session, sd);
	}
	
	
	@Override
	public boolean updateSurvey(Survey survey, Session session) {
		Object x = ((StructuredSelection)cmbViewer.getSelection()).getFirstElement();
		if (x instanceof SurveyDesignEditorInput){
			SurveyDesign sd = (SurveyDesign) session.load(SurveyDesign.class, ((SurveyDesignEditorInput) x).getUuid());
			survey.setSurveyDesign(sd);
			return true;
		}
		return false;
	}

}
