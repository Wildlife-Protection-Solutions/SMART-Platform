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
package org.wcs.smart.er.ui.mision;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Allow users to select a survey for the mission.
 * 
 * 
 * @author Emily
 *
 */
public class SurveyComposite extends MissionComposite{

	private ComboViewer cmbSurveys;
	private SurveyDesign parentSurvey;
	private Session session;
	
	@Override
	public Control createControl(Composite parent) {
		
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(2, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Label l = new Label(part, SWT.NONE);
		l.setText(Messages.SurveyComposite_SurveyLabel);
		
		cmbSurveys = new ComboViewer(part, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbSurveys.setContentProvider(ArrayContentProvider.getInstance());
//		cmbSurveys.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		cmbSurveys.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Survey){
					return ((Survey) element).getId() + " [" + ((Survey)element).getSurveyDesign().getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return super.getText(element);
			}
		});
		cmbSurveys.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbSurveys.getControl().getLayoutData()).widthHint = 100;
		cmbSurveys.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();
			}
		});
		
		return part;
	}
	
	private void refreshSurveys(Session s){
		List<Survey> kids = null;
		if (parentSurvey == null){
			kids = SurveyHibernateManager.getInstance().getActiveSurveys(s);
		}else{
			kids = SurveyHibernateManager.getInstance().getActiveSurveys(parentSurvey, s);
		}
		cmbSurveys.setInput(kids);
	}
	
	/**
	 * Initializes the controls
	 * @param mission the current mission
	 * @param parentSurvey the parent survey
	 * @param s current session
	 */
	public void init(Mission mission, SurveyDesign parentSurvey, Session s){
		init(mission, s);
		
		this.parentSurvey = parentSurvey;
		this.session = s;
		
		refreshSurveys(session);
		if (mission.getSurvey() != null){
			cmbSurveys.setSelection(new StructuredSelection(mission.getSurvey()));
		}
	}
	
	/**
	 * Does nothing; users should call init(mission, parentSurvey, s)
	 */
	@Override
	public void init(Mission design, Session session) {
	}

	@Override
	public void updateDesign(Mission mission) {
		Object first = ((IStructuredSelection)cmbSurveys.getSelection()).getFirstElement() ;
		if (first instanceof Survey){
			Survey survey = (Survey)first;
			mission.setSurvey(survey);
			mission.setStartDate(survey.getStartDate());
			mission.setEndDate(survey.getEndDate());
		}else{
			mission.setSurvey(null);
			mission.setStartDate(null);
			mission.setEndDate(null);
		}
	}

	@Override
	public boolean isValid() {
		if (cmbSurveys.getSelection().isEmpty()){
			return false;
		}
		if (((IStructuredSelection)cmbSurveys.getSelection()).getFirstElement() instanceof Survey ){
			return true;
		}
		return false;
	}

	@Override
	public String getTitle() {
		return Messages.SurveyComposite_Title;
	}

	@Override
	public String getDescription() {
		return Messages.SurveyComposite_Description;
	}

}
