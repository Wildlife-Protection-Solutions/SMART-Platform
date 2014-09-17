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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;

/**
 * Wizard page that asks the use if they want
 * to start from a blank design or use
 * an existing survey design as a template.
 *  
 * @author Emily
 *
 */
public class TemplateWizardPage extends WizardPage implements SelectionListener {

	private Button opBlank;
	private Button opTemplate;
	private ComboViewer cmbDesigns ;
	private List<SurveyDesign> templates;
	
	public TemplateWizardPage(List<SurveyDesign> templates){
		super("TEMPLATE"); //$NON-NLS-1$
		this.templates = templates;
	}
	
	@Override
	public void createControl(Composite parent) {
		
		super.setTitle(Messages.TemplateWizardPage_Title);
		super.setDescription(Messages.TemplateWizardPage_Description);
		
		Composite part2 = new Composite(parent, SWT.NONE);
		part2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		part2.setLayout(new GridLayout(1, false));
		
		Composite part = new Composite(part2, SWT.NONE);
		part.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		part.setLayout(new GridLayout(1, false));
		
		opBlank = new Button(part, SWT.RADIO);
		opBlank.setText(Messages.TemplateWizardPage_BlankDesign);
		opBlank.setSelection(true);
		
		opTemplate = new Button(part, SWT.RADIO);
		opTemplate.setText(Messages.TemplateWizardPage_TemplateDesign);
		opTemplate.setSelection(false);
		
		cmbDesigns = new ComboViewer(part, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbDesigns.setContentProvider(ArrayContentProvider.getInstance());
		cmbDesigns.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		cmbDesigns.setInput(templates);
		cmbDesigns.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				widgetSelected(null);
			}
		});
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = 20;
		gd.widthHint = 200;
		cmbDesigns.getControl().setLayoutData(gd);
		cmbDesigns.getControl().setEnabled(false);
		
		opTemplate.addSelectionListener(this);
		opBlank.addSelectionListener(this);
		
		super.setControl(part2);
	}

	@Override
	public boolean isPageComplete() {
		if (opTemplate.getSelection() && cmbDesigns.getSelection().isEmpty()){
			return false;
		}
		return true;
	}
	
	public void updateModel(SurveyDesign design){
		if (opTemplate.getSelection()){
			if (((StructuredSelection)cmbDesigns.getSelection()).isEmpty()){
				return;
			}
			//copy of design elements
			SurveyDesign copy = (SurveyDesign) ((StructuredSelection)cmbDesigns.getSelection()).getFirstElement();
			design.setStartDate(copy.getStartDate());
			design.setEndDate(copy.getEndDate());
			design.setDescription(copy.getDescription());
			design.setConfigurableModel(copy.getConfigurableModel());
			design.setTrackDistanceDirection(copy.getTrackDistanceDirection());
			design.setConservationArea(copy.getConservationArea());
			
			design.setMissionProperties(new ArrayList<MissionProperty>());
			if (copy.getMissionProperties() != null){
				for (MissionProperty mp : copy.getMissionProperties()){
					MissionProperty clone = new MissionProperty();
					clone.setSurveyDesign(design);
					clone.setOrder(mp.getOrder());
					clone.setAttribute(mp.getAttribute());
					
					design.getMissionProperties().add(clone);
				}
			}
			
		}else{
			//clear
			design.setStartDate(null);
			design.setEndDate(null);
			design.setDescription(null);
			design.setConfigurableModel(null);
			design.setTrackDistanceDirection(false);
			design.setMissionProperties(null);
		}
	}
	

	@Override
	public void widgetSelected(SelectionEvent e) {
		cmbDesigns.getControl().setEnabled(opTemplate.getSelection());
		getContainer().updateButtons();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
}
