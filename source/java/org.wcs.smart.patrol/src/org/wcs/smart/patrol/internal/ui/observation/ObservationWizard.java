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
package org.wcs.smart.patrol.internal.ui.observation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class ObservationWizard extends Wizard implements IPageChangingListener{

	private HashMap<Category, List<WaypointObservation>> observations = new HashMap<Category, List<WaypointObservation>>();
	
	private DataModel dm = null;
	private Session session;
	private Waypoint wp;

	public boolean canFinish = false;
	
	public ObservationWizard(Waypoint wp){
		setWindowTitle("Modify Waypoint Observations - Waypoint Id: " + wp.getId());
		this.wp = wp;
		super.setForcePreviousAndNextButtons(true);
		super.setNeedsProgressMonitor(false);
		
		
		if (wp.getObservations() != null){
			for (WaypointObservation ob : wp.getObservations()){
				if(ob.getUuid() == null){
					//this is not yet part of hibernate but if
					//attributes/categories exist they need to be loaded into the session
					ob = ob.clone();
					getSession().refresh(ob.getCategory());
					if (ob.getAttributes() != null){
						for(WaypointObservationAttribute attribute : ob.getAttributes()){
							getSession().refresh(attribute.getAttribute());
							if (attribute.getAttributeListItem() != null){
								getSession().refresh(attribute.getAttributeListItem());
							}
							if (attribute.getAttributeTreeNode() != null){
								getSession().refresh(attribute.getAttributeTreeNode());
							}
						}
					}
				}else{
					//we need to merge this with hibernate so
					//we can lazy load things
					ob = (WaypointObservation) getSession().merge(ob);
				}
				
				
				if (ob.getAttributes() == null || ob.getAttributes().isEmpty()) {
					observations.put(ob.getCategory(), null);
				} else {
					List<WaypointObservation> lst = observations.get(ob.getCategory());
					if (lst == null) {
						lst = new ArrayList<WaypointObservation>();
						observations.put(ob.getCategory(), lst);
					}
					lst.add(ob);
				}
			}
		}
	}
	
	public void setWaypointObservation(Category category, Collection<WaypointObservation> catObservations){
		if (catObservations == null){
			this.observations.put(category, null);
		}else{
			ArrayList<WaypointObservation> ops = new ArrayList<WaypointObservation>();
			ops.addAll(catObservations);
			this.observations.put(category, ops);
		}
	}
	
	public Collection<WaypointObservation> getWaypointObservation(Category category){
		return this.observations.get(category);
	}
	
	public HashMap<Category, List<WaypointObservation>> getAllObservations(){
		return this.observations;
	}
	
	private WizardDialog wizardDialog;
	public void setWizardDialog(WizardDialog wd){
		this.wizardDialog = wd;
	}
	public WizardDialog getWizardDialog(){
		return this.wizardDialog;
	}
	
	@Override
	public boolean canFinish() {
		 return canFinish; 
	}
	
	public void setCanFinish(boolean canFinish){
		this.canFinish = canFinish;
	}
	
	private Session getSession(){
		if (session == null || !session.isOpen()){
			session = HibernateManager.openSession();
		}
		return session;
	}
	
	public DataModel getDataModel(){
		if (dm == null){
			dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), getSession());
		}
		return dm;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (session != null && session.isOpen()){
			session.close();
		}
	}
	
	@Override
    public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		if (observations.size() == 0){
			new ObservationWizardPage(this);
		}else{
			new ObservationSummaryWizardPage(this);
		}
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		
		if (event.getCurrentPage() instanceof IObservationWizardPage){			
			event.doit = ((IObservationWizardPage)event.getCurrentPage()).beforeMoveNext( (IWizardPage)event.getTargetPage() );
		}
		
	}

	private Category current;
	public void setCurrentObservation(Category current){
		this.current = current;
	}
	public Category getCurrentObservation(){
		return this.current;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		
		
		List<WaypointObservation> wobservations = new ArrayList<WaypointObservation>();
		for (Entry<Category,List<WaypointObservation>> entry : this.observations.entrySet()){
			if (entry.getValue() == null){
				WaypointObservation wo = new WaypointObservation();
				wo.setCategory(entry.getKey());
				wobservations.add(wo);
			}else{
				wobservations.addAll(entry.getValue());	
			}
		}
		
		for (WaypointObservation ob : wobservations){
			ob.setWaypoint(wp);
		}
		//TODO: fix this with hibernate
		if (wp.getObservations() == null){
			wp.setObservations(new ArrayList<WaypointObservation>());
		}
		wp.getObservations().clear();
		wp.getObservations().addAll(wobservations);
		
		return true;
	}

	
    public boolean performCancel() {
        return true;
    }
}
