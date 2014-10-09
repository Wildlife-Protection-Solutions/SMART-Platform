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
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.ui.samplingunit.EditSamplingUnitAttributeDialog;
import org.wcs.smart.er.ui.samplingunit.SamplingUnitLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Dialog for selecting the sampling unit attributes to use with 
 * the given survey design.  This updates the survey design
 * and fires associated events. 
 * 
 * @author Emily
 *
 */
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
		if (session.isOpen()){
			session.close();
		}
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
			String msg = Messages.SurveyDesignSamplingUnitAttributeDialog_ConfirmMsg;
			StringBuilder atts = new StringBuilder();
			for (SurveyDesignSamplingUnitAttribute a : toDelete){
				atts.append(a.getSamplingUnitAttribute().getName());
				atts.append(", "); //$NON-NLS-1$
			}
			atts.deleteCharAt(atts.length() - 1);
			atts.deleteCharAt(atts.length() - 1);
	
			MessageDialog dialog = new MessageDialog(getShell(), 
					Messages.SurveyDesignSamplingUnitAttributeDialog_DialogTitle, null, 
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
					Query q = session.createQuery("SELECT sua FROM SamplingUnitAttributeValue sua JOIN sua.id.samplingUnit su WHERE sua.id.samplingUnitAttribute = :sua AND su.surveyDesign = :sd"); //$NON-NLS-1$
					q.setParameter("sua", a.getSamplingUnitAttribute()); //$NON-NLS-1$
					q.setParameter("sd", this.design); //$NON-NLS-1$
					List<SamplingUnitAttributeValue> values = q.list();
					
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
			
			//close session so doesn't interfere with handlers
			session.close();
			SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, design);
			
			//open again
			session = HibernateManager.openSession();
			
		}catch (Exception ex){
			session.getTransaction().rollback();
			EcologicalRecordsPlugIn.displayLog(Messages.SurveyDesignSamplingUnitAttributeDialog_ErrorMsg + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			return ;
		}

		super.okPressed();
		
	}
	
	private void initValues(boolean keepCurrent){
		List<SamplingUnitAttribute> allAttributes = 
				session.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
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
		composite.setLabelAllText(Messages.SurveyDesignSamplingUnitAttributeDialog_AllSuLAbel);
		composite.setLabelSelectedText(Messages.SurveyDesignSamplingUnitAttributeDialog_SurveySuLabel);
		composite.setLabelProvider(SamplingUnitLabelProvider.INSTANCE);
		composite.setItemComparator(new Comparator<SamplingUnitAttribute>() {
			@Override
			public int compare(SamplingUnitAttribute su1, SamplingUnitAttribute su2) {
				return Collator.getInstance().compare(su1.getName(), su2.getName());
			}
		});
		
		createNew = new Link(c, SWT.NONE);
		createNew.setText("<a>" + Messages.SurveyDesignSamplingUnitAttributeDialog_CreateAttributeLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		createNew.addListener(SWT.Selection, new  Listener(){
			@Override
			public void handleEvent(Event event) {
				SamplingUnitAttribute sua = new SamplingUnitAttribute();
				sua.setConservationArea(SmartDB.getCurrentConservationArea());
				sua.setType(AttributeType.TEXT);
				
				List<SamplingUnitAttribute> siblings = session
						.createCriteria(SamplingUnitAttribute.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
						.list(); 
				EditSamplingUnitAttributeDialog d = new EditSamplingUnitAttributeDialog(
						getShell(), sua, siblings, session);
				
				if (d.open() == EditSamplingUnitAttributeDialog.OK){
				
					session.beginTransaction();
					try{
						session.save(sua);
						session.getTransaction().commit();
					}catch (Exception ex){
						session.getTransaction().rollback();
						EcologicalRecordsPlugIn.displayLog(Messages.SurveyDesignSamplingUnitAttributeDialog_ErrorCreatingNew + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
					}
					initValues(true);
				}		
			}
		});
		
		initValues(false);
		
		setTitle(Messages.SurveyDesignSamplingUnitAttributeDialog_Title);
		getShell().setText(Messages.SurveyDesignSamplingUnitAttributeDialog_Title);
		setMessage(Messages.SurveyDesignSamplingUnitAttributeDialog_Message);
		
		return parent;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
