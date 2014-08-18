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
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.common.control.MultipleSelectComposite;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.missionattribute.AttributeLabelProvider;
import org.wcs.smart.hibernate.SmartDB;

public class MissionPropertiesComposite extends SurveyDesignComposite {

	private MultipleSelectComposite<MissionAttribute> attributesComposite; 
	
	
	public MissionPropertiesComposite(){
		super();
	}
	
	@Override
	public Control createControl(final Composite parent) {	
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(3, false));
		
		attributesComposite = new MultipleSelectComposite<MissionAttribute>(part, SWT.NONE);
		attributesComposite.setLabelAllText(Messages.MissionPropertiesComposite_AllMissionAttributes);
		attributesComposite.setLabelSelectedText(Messages.MissionPropertiesComposite_SelectedMissionAttributes);
		
		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		return part;
	}

	@Override
	public void init(SurveyDesign design, Session session) {
		@SuppressWarnings("unchecked")
		List<MissionAttribute> allAttributes = session.createCriteria(MissionAttribute.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
		List<MissionAttribute> selectedAttributes = new ArrayList<MissionAttribute>();
		
		if (design.getMissionProperties() != null){
			for (MissionProperty mp : design.getMissionProperties()){
				selectedAttributes.add(mp.getAttribute());
				allAttributes.remove(mp.getAttribute());
			}
		}
		attributesComposite.setLabelProvider(new AttributeLabelProvider());
		attributesComposite.setItemsData(allAttributes, selectedAttributes);
		attributesComposite.setItemComparator(new Comparator<MissionAttribute>() {
			
			@Override
			public int compare(MissionAttribute arg0, MissionAttribute arg1) {
				return Collator.getInstance().compare(arg0.getName(), arg1.getName());
			}
		});
		
	}

	@Override
	public void updateDesign(SurveyDesign design) {
		if (design.getMissionProperties() != null){
			//clear existing properties
			for (MissionProperty mp : design.getMissionProperties()){
				mp.setAttribute(null);
				mp.setSurveyDesign(null);
			}
			design.getMissionProperties().clear();
		}else{
			design.setMissionProperties(new ArrayList<MissionProperty>());
		}
		//add new properties
		int order = 0;
		for (MissionAttribute ma : attributesComposite.getSelectedItemsAsList()){
			MissionProperty mp = new MissionProperty();
			mp.setAttribute(ma);
			mp.setSurveyDesign(design);
			mp.setOrder(order++);
			design.getMissionProperties().add(mp);
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

