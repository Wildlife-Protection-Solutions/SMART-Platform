package org.wcs.smart.er.ui.surveydesign.editor;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.common.control.MultipleSelectComposite;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.ui.samplingunit.EditSamplingUnitAttributeDialog;
import org.wcs.smart.er.ui.samplingunit.SamplingUnitAttributeDialog;
import org.wcs.smart.er.ui.samplingunit.SamplingUnitLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public class SurveyDesignSamplingUnitAttributeDialog extends TitleAreaDialog {

	private MultipleSelectComposite<SamplingUnitAttribute> composite;
	
	private Session session;
	private SurveyDesign design;
	
	private Link createNew;
	
	public SurveyDesignSamplingUnitAttributeDialog(Shell parentShell, SurveyDesign design) {
		super(parentShell);
		
		
		session = HibernateManager.openSession();
		this.design = (SurveyDesign) session.load(SurveyDesign.class, design.getUuid());
	}
	
	public boolean close(){
		session.close();
		
		return super.close();
	}
	
	public void okPressed(){
		List<SamplingUnitAttribute> selected = composite.getSelectedItemsAsList();
		List<SurveyDesignSamplingUnitAttribute> toDelete = new ArrayList<SurveyDesignSamplingUnitAttribute>();

		for (SurveyDesignSamplingUnitAttribute a : this.design.getSamplingUnitAttributes()){
			if (!selected.contains(a.getSamplingUnitAttribute())){
				toDelete.add(a);
			}else{
				selected.remove(a.getSamplingUnitAttribute());
			}
		}
		
		if (toDelete.size() > 0){
			String msg = "Are you sure you want to remove the attributes {0}?  This will delete any existing values associated with this attribute.";
			StringBuilder atts = new StringBuilder();
			for (SurveyDesignSamplingUnitAttribute a : toDelete){
				atts.append(a.getSamplingUnitAttribute().getName());
				atts.append(", ");
			}
			atts.deleteCharAt(atts.length() - 1);
			atts.deleteCharAt(atts.length() - 1);
	
			MessageDialog dialog = new MessageDialog(getShell(), 
					"Remove Attributes", null, 
					MessageFormat.format(msg, new Object[]{atts.toString()}),
					MessageDialog.QUESTION, 
					new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},
					2);
			int value = dialog.open();
			if (value == 1 || value == 2){
				//NO / CANCEL
				return;
			}
		}
		
		session.beginTransaction();
		try{
			//	remove anything in delete
			if (toDelete.size() > 0){
				//delete attributes
			
				for (SurveyDesignSamplingUnitAttribute a : toDelete){

					//we need to delete any sampling unit attribute values
					Query q = session.createQuery("SELECT sua FROM SamplingUnitAttributeValue sua JOIN sua.id.samplingUnit su WHERE sua.id.samplingUnitAttribute = :sua AND su.surveyDesign = :sd");
					q.setParameter("sua", a.getSamplingUnitAttribute());
					q.setParameter("sd", this.design);
					List<SamplingUnitAttributeValue> values = q.list();
					
//					List<SamplingUnitAttributeValue> values = session.createCriteria(SamplingUnitAttributeValue.class)
//						.createAlias("id.samplingUnit", "su")
//						.add(Restrictions.eq("id.samplingUnitAttribute", a.getSamplingUnitAttribute()))
//						.add(Restrictions.eq("su.surveyDesign", this.design)).list();
					
					for (SamplingUnitAttributeValue v : values){
						session.delete(v);
					}
					
					this.design.getSamplingUnitAttributes().remove(a);
					session.delete(a);
				}
			}
		
			//add everything left in selected
			for (SamplingUnitAttribute sa : selected){
				SurveyDesignSamplingUnitAttribute newAtt = new SurveyDesignSamplingUnitAttribute();
				newAtt.setSamplingUnitAttribute(sa);
				newAtt.setSurveyDesign(this.design);
				this.design.getSamplingUnitAttributes().add(newAtt);
			}

			session.getTransaction().commit();
			
			//TODO: review this
			//close session
			session.close();
			SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, design);
			
			//open again
			session = HibernateManager.openSession();
			
		}catch (Exception ex){
			session.getTransaction().rollback();
			EcologicalRecordsPlugIn.displayLog("Error saving changes. Please close dialog and try again." + "\n\n" + ex.getMessage(), ex);
			return ;
		}

		super.okPressed();
		
	}

//	@Override
//	public void createButtonsForButtonBar(Composite parent){
//
//	}

	
	private void initValues(boolean keepCurrent){
		List<SamplingUnitAttribute> allAttributes = 
				session.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
		
		List<SamplingUnitAttribute> selectedAttributes = new ArrayList<SamplingUnitAttribute>();
		if (!keepCurrent){
			for (SurveyDesignSamplingUnitAttribute a : design.getSamplingUnitAttributes()){
				selectedAttributes.add(a.getSamplingUnitAttribute());
			}	
		}else{
			selectedAttributes = composite.getSelectedItemsAsList();
		}
		
		composite.setItemsData(allAttributes, selectedAttributes);
	}
	
	
	@Override
	public Composite createDialogArea(Composite parent){
		parent = (Composite) super.createDialogArea(parent);
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		composite = new MultipleSelectComposite<SamplingUnitAttribute>(c, SWT.NONE);
		composite.setLabelAllText("Sampling Unit Attributes");
		composite.setLabelSelectedText("Survey Sampling Unit Attributes");
		composite.setLabelProvider(SamplingUnitLabelProvider.INSTANCE);
		composite.setItemComparator(new Comparator<SamplingUnitAttribute>() {
			@Override
			public int compare(SamplingUnitAttribute su1, SamplingUnitAttribute su2) {
				return Collator.getInstance().compare(su1.getName(), su2.getName());
			}
		});
		
		createNew = new Link(c, SWT.NONE);
		createNew.setText("<a>" + "Create New Attribute..." + "</a>");
		createNew.addListener(SWT.Selection, new  Listener(){
			@Override
			public void handleEvent(Event event) {
				SamplingUnitAttribute sua = new SamplingUnitAttribute();
				sua.setConservationArea(SmartDB.getCurrentConservationArea());
				sua.setType(AttributeType.TEXT);
				
				List<SamplingUnitAttribute> siblings = session.createCriteria(SamplingUnitAttribute.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
				EditSamplingUnitAttributeDialog d = new EditSamplingUnitAttributeDialog(
						getShell(), sua, siblings);
				
				if (d.open() == EditSamplingUnitAttributeDialog.OK){
				
					session.beginTransaction();
					try{
						session.save(sua);
						session.getTransaction().commit();
					}catch (Exception ex){
						session.getTransaction().rollback();
						EcologicalRecordsPlugIn.displayLog("Could not have new attribute." + "\n\n" + ex.getMessage(), ex);
					}
					initValues(true);
					
				}
				
				
			}
			
		});
		
		
		initValues(false);
		
		setTitle("Sampling Unit Attributes");
		getShell().setText("Sampling Unit Attributes");
		setMessage("Select the sampling unit attributes relevant for this survey.");
		
		return parent;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
