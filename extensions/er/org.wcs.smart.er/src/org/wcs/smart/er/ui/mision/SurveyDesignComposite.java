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
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;

/**
 * Composite for users to select the survey design.
 * 
 * @author Emily
 *
 */
public class SurveyDesignComposite extends MissionComposite{

	private ComboViewer cmbDesigns;
	
	private List<SurveyDesignEditorInput> surveys;
	
	public SurveyDesignComposite(List<SurveyDesignEditorInput> surveys){
		this.surveys = surveys;
	}
	
	
	@Override
	public Control createControl(Composite parent) {
		
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(2, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Label l = new Label(part, SWT.NONE);
		l.setText(Messages.SurveyDesignComposite_SdLabel);
		
		cmbDesigns = new ComboViewer(part, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbDesigns.setContentProvider(ArrayContentProvider.getInstance());
		cmbDesigns.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		cmbDesigns.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbDesigns.getControl().getLayoutData()).widthHint = 100;
		cmbDesigns.setInput(surveys);
		
		cmbDesigns.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();
			}
		});
		
		return part;
	}
	
	/**
	 * Does nothing
	 */
	@Override
	public void init(Mission design, Session session) {

	}

	/**
	 * Does nothing
	 */
	@Override
	public void updateDesign(Mission design) {
	}

	/**
	 * 
	 * @return the selected survey design.
	 */
	public SurveyDesign getSurveyDesign(Session session){
		Object x = ((IStructuredSelection)cmbDesigns.getSelection()).getFirstElement();
		if (x != null && x instanceof SurveyDesignEditorInput ){
			return (SurveyDesign) session.load(SurveyDesign.class, ((SurveyDesignEditorInput)x).getUuid());
		}
		return null;
	}
	
	
	@Override
	public boolean isValid() {
		if (cmbDesigns.getSelection().isEmpty()){
			return false;
		}
		if (((IStructuredSelection)cmbDesigns.getSelection()).getFirstElement() instanceof SurveyDesignEditorInput ){
			return true;
		}
		return false;
	}

	@Override
	public String getTitle() {
		return Messages.SurveyDesignComposite_Title;
	}

	@Override
	public String getDescription() {
		return Messages.SurveyDesignComposite_Description;
	}

	@Override
	public String getErrorMessage() {
		if (surveys.size() == 0){
			return Messages.SurveyDesignComposite_NeedsSurveyDesign;
		}
		return null;
	}
}
