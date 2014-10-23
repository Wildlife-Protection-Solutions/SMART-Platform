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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.MultipleSelectComposite;
import org.wcs.smart.common.control.MultipleSelectComposite.IListChanged;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.missionattribute.AttributeLabelProvider;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Mission properties composite.
 * @author Emily
 *
 */
public class MissionPropertiesComposite extends SurveyDesignComposite {

	private MultipleSelectComposite<MissionAttribute> attributesComposite; 
	
	private Composite warn;
	
	
	public MissionPropertiesComposite(){
		super();
	}
	
	@Override
	public Control createControl(final Composite parent) {
		warn = new Composite(parent, SWT.NONE);
		warn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		warn.setLayout(new GridLayout(2, false));
		
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(3, false));
		
		Label lblWarnImage = new Label(warn, SWT.NONE);
		lblWarnImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		
		Label lblWarn = new Label(warn, SWT.WRAP);
		lblWarn.setText(Messages.MissionPropertiesComposite_DeleteWarning );
		lblWarn.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		
		attributesComposite = new MultipleSelectComposite<MissionAttribute>(part, SWT.NONE);
		attributesComposite.setLabelAllText(Messages.MissionPropertiesComposite_AllMissionAttributes);
		attributesComposite.setLabelSelectedText(Messages.MissionPropertiesComposite_SelectedMissionAttributes);

		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		return part;
	}

	@Override
	public void init(SurveyDesign design, Session session) {
		warn.setVisible(design.getUuid() != null);
		
		@SuppressWarnings("unchecked")
		List<MissionAttribute> allAttributes = session.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.list(); 
		List<MissionAttribute> selectedAttributes = new ArrayList<MissionAttribute>();
		
		if (design.getMissionProperties() != null){
			for (MissionProperty mp : design.getMissionProperties()){
				selectedAttributes.add(mp.getAttribute());
				allAttributes.remove(mp.getAttribute());
			}
		}
		attributesComposite.setLabelProvider(new AttributeLabelProvider());
		attributesComposite.setItemComparator(new Comparator<MissionAttribute>() {
			@Override
			public int compare(MissionAttribute arg0, MissionAttribute arg1) {
				return Collator.getInstance().compare(arg0.getName(), arg1.getName());
			}
		});
		attributesComposite.setItemsData(allAttributes, selectedAttributes);
		attributesComposite.addSelectionChangedListener(new IListChanged<MissionAttribute>() {
			@Override
			public void listChanged(List<MissionAttribute> items) {
				fireChangeListeners();
			}
		});
		
	}

	@Override
	public void updateDesign(SurveyDesign design) {
		if (design.getMissionProperties() == null){
			design.setMissionProperties(new ArrayList<MissionProperty>());
		}
		
		List<MissionProperty> toDelete = new ArrayList<MissionProperty>();
		
		List<MissionAttribute> items = new ArrayList<MissionAttribute>();
		items.addAll(attributesComposite.getSelectedItemsAsList());
		
		for (MissionProperty mp : design.getMissionProperties()){
			if (!items.contains(mp.getAttribute())){
				toDelete.add(mp);
			}else{
				items.remove(mp.getAttribute());
			}
		}
		
		for (MissionProperty mp : toDelete){
			design.getMissionProperties().remove(mp);
			mp.setSurveyDesign(null);
			mp.setAttribute(null);
		}
		
		//new items
		for (MissionAttribute ma : items){
			//new
			MissionProperty mp = new MissionProperty();
			mp.setAttribute(ma);
			mp.setSurveyDesign(design);
			design.getMissionProperties().add(mp);
		}
		//update order
		int i = 1;
		for (MissionProperty mp : design.getMissionProperties()){
			mp.setOrder(i++);
		}
		
	}


	@Override
	public boolean isValid() {
		return true;
	}
	
	@Override
	public String getTitle(){
		return Messages.MissionPropertiesComposite_Title;
	}
	
	@Override
	public String getDescription(){
		return Messages.MissionPropertiesComposite_Description;
	}
}

