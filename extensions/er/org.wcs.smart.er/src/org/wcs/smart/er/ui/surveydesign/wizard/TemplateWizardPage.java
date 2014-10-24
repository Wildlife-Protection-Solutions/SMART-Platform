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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignProperty;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.ConservationAreaLabelProvider;

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
	private ComboViewer cmbCa;
	private Map<ConservationArea, List<SurveyDesign>> ca2designs;
	private ComboViewer cmbDesigns ;
	private Button chCopySu;
	
	private List<SamplingUnit> newSamplingUnits = null;
	
	public TemplateWizardPage(List<SurveyDesign> templates){
		super("TEMPLATE"); //$NON-NLS-1$
		ca2designs = new HashMap<ConservationArea, List<SurveyDesign>>();
		for (SurveyDesign sd : templates) {
			ConservationArea ca = sd.getConservationArea();
			List<SurveyDesign> list = ca2designs.get(ca);
			if (list == null) {
				list = new ArrayList<SurveyDesign>();
				ca2designs.put(ca, list);
			}
			list.add(sd);
		}
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
		
		Composite templeteCmp = new Composite(part, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = 20;
		templeteCmp.setLayoutData(gd);
		templeteCmp.setLayout(new GridLayout(2, false));
		
		Label caLabel = new Label(templeteCmp, SWT.NONE);
		caLabel.setText(Messages.TemplateWizardPage_ConservationArea);

		cmbCa = new ComboViewer(templeteCmp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbCa.setContentProvider(ArrayContentProvider.getInstance());
		cmbCa.setLabelProvider(new ConservationAreaLabelProvider());
		cmbCa.setInput(ca2designs.keySet());
		cmbCa.setSelection(new StructuredSelection(SmartDB.getCurrentConservationArea()));
		cmbCa.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				widgetSelected(null);
				updateDesignInput();
			}
		});
		cmbCa.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbCa.getControl().setEnabled(false);

		Label designLabel = new Label(templeteCmp, SWT.NONE);
		designLabel.setText(Messages.TemplateWizardPage_SurveyDesign);
		
		cmbDesigns = new ComboViewer(templeteCmp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbDesigns.setContentProvider(ArrayContentProvider.getInstance());
		cmbDesigns.setLabelProvider(SurveyDesignLabelProvider.getInstance());
//		cmbDesigns.setInput(templates);
		cmbDesigns.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				widgetSelected(null);
			}
		});
		cmbDesigns.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbDesigns.getControl().setEnabled(false);
	
		chCopySu = new Button(templeteCmp, SWT.CHECK);
		chCopySu.setSelection(false);
		chCopySu.setEnabled(false);
		chCopySu.setText(Messages.TemplateWizardPage_CopySuLabels);
		chCopySu.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		opTemplate.addSelectionListener(this);
		opBlank.addSelectionListener(this);
		
		updateDesignInput();
		
		super.setControl(part2);
	}
	
	protected void updateDesignInput() {
		IStructuredSelection sel = (IStructuredSelection) cmbCa.getSelection();
		List<SurveyDesign> designsList = null;
		if (!sel.isEmpty()) {
			designsList = ca2designs.get(sel.getFirstElement());
		}
		cmbDesigns.setInput(designsList != null ? designsList : new Object[0]);
	}

	@Override
	public boolean isPageComplete() {
		if (opTemplate.getSelection() && cmbDesigns.getSelection().isEmpty()){
			return false;
		}
		return true;
	}
	
	
	
	public void updateModel(SurveyDesign design, Session session){
		if (opTemplate.getSelection()){
			if (((StructuredSelection)cmbCa.getSelection()).isEmpty()){
				return;
			}
			if (((StructuredSelection)cmbDesigns.getSelection()).isEmpty()){
				return;
			}
			ConservationArea copyCa = (ConservationArea) ((StructuredSelection)cmbCa.getSelection()).getFirstElement();
			boolean isSameCa = SmartDB.getCurrentConservationArea().equals(copyCa);
			//copy of design elements
			SurveyDesign copy = (SurveyDesign) ((StructuredSelection)cmbDesigns.getSelection()).getFirstElement();
			design.setStartDate(copy.getStartDate());
			design.setEndDate(copy.getEndDate());
			design.setDescription(copy.getDescription());
			design.setTrackDistanceDirection(copy.getTrackDistanceDirection());
			design.setConservationArea(SmartDB.getCurrentConservationArea());
			
			design.setMissionProperties(new ArrayList<MissionProperty>());
			if (isSameCa) {
				design.setConfigurableModel(copy.getConfigurableModel());
				if (copy.getMissionProperties() != null){
					for (MissionProperty mp : copy.getMissionProperties()){
						MissionProperty clone = new MissionProperty();
						clone.setSurveyDesign(design);
						clone.setOrder(mp.getOrder());
						clone.setAttribute(mp.getAttribute());
						
						design.getMissionProperties().add(clone);
					}
				}
			} else {
				//try to find mission properties with the same key in current CA
				List<String> keysList = new ArrayList<String>();
				for (MissionProperty mp : copy.getMissionProperties()) {
					keysList.add(mp.getAttribute().getKeyId());
				}
				if (!keysList.isEmpty()) {
					@SuppressWarnings("unchecked")
					List<MissionAttribute> attributes = session.createCriteria(MissionAttribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
							.add(Restrictions.in("keyId", keysList)).list(); //$NON-NLS-1$
					
					for (int i = 0; i < attributes.size(); i++) {
						MissionProperty clone = new MissionProperty();
						clone.setSurveyDesign(design);
						clone.setOrder(i);
						clone.setAttribute(attributes.get(i));
						
						design.getMissionProperties().add(clone);
					}
				}
			}
			
			//survey design properties
			design.setProperties(new ArrayList<SurveyDesignProperty>());
			if (copy.getProperties() != null){
				for (SurveyDesignProperty sdp : copy.getProperties()){
					SurveyDesignProperty clone = new SurveyDesignProperty();
					clone.setSurveyDesign(design);
					clone.setName(sdp.getName());
					clone.setValue(sdp.getValue());

					design.getProperties().add(clone);
				}
			}
			
			//copy sampling unit attributes
			design.setSamplingUnitAttributes(new ArrayList<SurveyDesignSamplingUnitAttribute>());
			for (SurveyDesignSamplingUnitAttribute sua : copy.getSamplingUnitAttributes()){
				SurveyDesignSamplingUnitAttribute a2 = new SurveyDesignSamplingUnitAttribute();
				a2.setSamplingUnitAttribute(sua.getSamplingUnitAttribute());
				a2.setSurveyDesign(design);
				
				design.getSamplingUnitAttributes().add(a2);
			}
			
			//copy sampling units & associated attributes
			newSamplingUnits = null;
			if (chCopySu.getSelection()){
				newSamplingUnits = new ArrayList<SamplingUnit>();
				@SuppressWarnings("unchecked")
				List<SamplingUnit> sus = session.createCriteria(SamplingUnit.class).add(Restrictions.eq("surveyDesign", copy)).list(); //$NON-NLS-1$
				for (SamplingUnit s2: sus){
					SamplingUnit newsu = new SamplingUnit();
					newsu.setGeom(s2.getGeom());
					newsu.setId(s2.getId());
					newsu.setState(s2.getState());
					newsu.setSurveyDesign(design);
					newsu.setType(s2.getType());
					newsu.setAttributes(new ArrayList<SamplingUnitAttributeValue>());
					
					for (SamplingUnitAttributeValue suav : s2.getAttributes()){
						SamplingUnitAttributeValue newAv = new SamplingUnitAttributeValue();
						newAv.setNumberValue(suav.getNumberValue());
						newAv.setStringValue(suav.getStringValue());
						newAv.setSamplingUnit(newsu);
						newAv.setSamplingUnitAttribute(suav.getSamplingUnitAttribute());
						newsu.getAttributes().add(newAv);
					}
					newSamplingUnits.add(newsu);
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
	
	public List<SamplingUnit> getClonedSamplingUnits(){
		return this.newSamplingUnits;
	}
	
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		cmbCa.getControl().setEnabled(opTemplate.getSelection());
		cmbDesigns.getControl().setEnabled(opTemplate.getSelection());
		chCopySu.setEnabled(opTemplate.getSelection());
		
		getContainer().updateButtons();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
}
