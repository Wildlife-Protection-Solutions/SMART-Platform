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
package org.wcs.smart.observation.ui.input;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * Wizard for collecting observation information for a given
 * waypoint.
 * <p>After creating the wizard and before displaying to the 
 * user setWizardDialog(WizardDialog) must be called.</p>
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ObservationWizard extends Wizard implements IPageChangingListener{

	private Waypoint wp;
	private ObservationWizardDialog wizardDialog;
	
	private WaypointObservation toEdit;
	
	private DataModel dm = null;
	public boolean canFinish = false;
	private boolean isModified = false;
	
	private List<Employee> observers;
	private Employee observer = null;
	
	private ConfigurableModel cm;
	
	private boolean trackObserver = false;
	
	private HashMap<Category, List<WaypointObservation>> observations;
	private List<WaypointObservation> deletedObservations;
	private List<ObservationAttachment> deletedAttachments;
	private List<Category> selectedCategories;
	

	//category counter for tracking categories 
	private int categoryIndex = 0;
	
	/**
	 * Creates a new observation wizard that displays only the
	 * data model. 
	 * 
	 * @param wp waypoint to update
	 * @param observers observer list; if null observer will not be recorded
	 */
	public ObservationWizard(Waypoint wp, List<Employee> observers){
		this(wp, observers, null);
	}
	
	
	/**
	 * Creates a new wizard that displays only the data model and 
	 * the configurable model. 
	 * 
	 * @param wp waypoint to update
	 * @param observers observer list; if null observer will not be recorded
	 * @param cm configurable model
	 */
	public ObservationWizard(Waypoint wp, List<Employee> observers, ConfigurableModel cm){
		
		setWindowTitle(MessageFormat.format(Messages.ObservationWizard_PageName, new Object[]{String.valueOf(wp.getId())}));
		this.cm = cm;
		
		super.setForcePreviousAndNextButtons(true);
		super.setNeedsProgressMonitor(false);
		
		this.observers = observers;
		this.trackObserver = (observers != null);
		this.selectedCategories = new ArrayList<>();
		
		deletedObservations = new ArrayList<>();
		deletedAttachments = new ArrayList<>();
		observations = new HashMap<>();
		this.wp = wp;
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){			
			//load observation, categories, and attribute details	
			session.saveOrUpdate(wp);
			for (WaypointObservation ob : wp.getObservations()) {
				Category c = session.get(Category.class,  ob.getCategory().getUuid());
				ob.setCategory(c);
				
				List<WaypointObservation> cobs = observations.get(ob.getCategory());
				if (cobs == null) {
					cobs = new ArrayList<>();
					observations.put(ob.getCategory() , cobs);
				}
				cobs.add(ob);
				ob.getAttributes().forEach(a->a.getAttributeValueAsString(Locale.getDefault()));
				
				for (ObservationAttachment oa : ob.getAttachments()) {
					try {
						oa.computeFileLocation(session);
					}catch (Exception ex) {
						ObservationPlugIn.log(ex.getMessage(), ex);
					}
				}
				ob.getCategory().getFullCategoryName();
				ob.getCategory().getName();
				for (CategoryAttribute ca : ob.getCategory().getAttributes()) {
					ca.getAttribute().getType();
					ca.getAttribute().getName();
				}
				Category temp = ob.getCategory();
				while(temp != null) {
					temp.getAttributes().forEach(a->{
						a.getAttribute().getName();
						if (a.getAttribute().getAttributeList() != null) a.getAttribute().getAttributeList().forEach(li->li.getName());
						if (a.getAttribute().getActiveTreeNodes() != null) {
							List<AttributeTreeNode> tovisit = new ArrayList<AttributeTreeNode>();
							tovisit.addAll(a.getAttribute().getActiveTreeNodes());
							while(!tovisit.isEmpty()) {
								AttributeTreeNode n = tovisit.remove(0);
								n.getName();
								if (n.getActiveChildren() != null) tovisit.addAll(n.getActiveChildren());
							}
						}
					});
					temp = temp.getParent();
				}
				
				for (WaypointObservationAttribute woa : ob.getAttributes()) {
					woa.getAttribute().getName();
				}
				
				if (ob.getObserver() != null) {
					ob.getObserver().getFamilyName();
					ob.getObserver().getGivenName();
				}
			}
			
			if (this.cm != null){
				this.cm = (ConfigurableModel) session.get(ConfigurableModel.class, cm.getUuid());
				
				//lazy load all nodes
				List<CmNode> allNodes = new ArrayList<>();
				allNodes.addAll( this.cm.getNodes() );
				while(!allNodes.isEmpty()) {
					CmNode nn = allNodes.remove(0);
					if (nn.getCategory() != null) {
						nn.getCategory().getName();
						nn.getCategory().getFullCategoryName();
					}
					nn.getName();
					if (nn.getChildren() != null) allNodes.addAll(nn.getChildren());
				}
			}
		}
	}
	
	public void setToEdit(WaypointObservation toEdit) {
		for (List<WaypointObservation> wos : observations.values()) {
			for (WaypointObservation wo : wos) {
				if (wo.getUuid().equals(toEdit.getUuid())) {
					this.toEdit = wo;
				}
			}
		}
	}
	
	/**
	 * The configurable model to display or null if configurable
	 * model option not used.
	 * 
	 * @return
	 */
	public ConfigurableModel getConfigurableModel(){
		return this.cm;
	}
	
	/**
	 * 
	 * @return the observation options
	 */
	public boolean getTrackObserver(){
		return this.trackObserver;
	}

	
	/**
	 * Sets the modified sate of the wizard to true so cancel prompts
	 * to ensure they want to cancel.
	 */
	public void setModified(){
		this.isModified = true;
	}
	
	/**
	 * 
	 * @return the next category to process
	 */
	public Category getNextCategory() {
		Category c = selectedCategories.get(categoryIndex);
		categoryIndex++;
		return c;
	}
	
	/**
	 * the total number of categories to process
	 * @return
	 */
	public int getCategoryCount() {
		return selectedCategories.size();
	}
	
	/**
	 * 
	 * @param c
	 * @return the index of the given category
	 */
	public int getCategoryIndex(Category c) {
		return selectedCategories.indexOf(c);
	}
	
	/**
	 * 
	 * @return true if there are more categories to process
	 */
	public boolean hasMoreCategories() {
		return categoryIndex < selectedCategories.size();
		
	}
	/**
	 * Sets the list of categories to process; resetting index to 0
	 * @param categories
	 */
	public void setCategoriesToProcess(List<Category> categories) {
		selectedCategories = new ArrayList<>();

		selectedCategories.addAll(categories);
		categoryIndex = 0;
	}
	
	
	/**
	 * 
	 * @param index
	 * @return the categories to process at the given index
	 */
	public List<Category> getCategoriesToProcess(){
		return selectedCategories;
	}
	
	/**
	 * Gets the waypoint observations for a given category 
	 * @param category category
	 * @return set of waypoint observations
	 */
	public Collection<WaypointObservation> getWaypointObservation(Category category){
		List<WaypointObservation> cobs = observations.get(category);
		if (cobs == null) {
			cobs = new ArrayList<>();
			observations.put(category, cobs);
		}
		return cobs;
	}
	
	/**
	 * @return all observations make at this waypoint
	 */
	public HashMap<Category, List<WaypointObservation>> getAllObservations(){
		return observations;
	}
	
	/**
	 * Adds a new observation
	 * @param newObs
	 */
	public void addObservation(WaypointObservation newObs) {
		List<WaypointObservation> cobs = observations.get(newObs.getCategory());
		if (cobs == null) {
			cobs = new ArrayList<>();
			observations.put(newObs.getCategory(), cobs);
		}
		cobs.add(newObs);
	}
	
	/**
	 * Removes an observation 
	 * @param newObs
	 */
	public void removeObservation(WaypointObservation newObs) {
		List<WaypointObservation> cobs = observations.get(newObs.getCategory());
		cobs.remove(newObs);
		deletedObservations.add(newObs);
	}
	
	/**
	 * Flags as attachment for removal
	 * @param attachment
	 */
	public void removeAttachment(ObservationAttachment attachment) {
		this.deletedAttachments.add(attachment);
	}
	
	/**
	 * Removes all observations associated with the given category
	 * @param category
	 */
	public void removeObservations(Category category){
		this.isModified = true;
		List<WaypointObservation> cobs = observations.get(category);
		deletedObservations.addAll(cobs);
		cobs.clear();
	}
	
	/**
	 * Sets the wizard dialog.
	 * @param wd wizard dialog
	 */
	public void setWizardDialog(ObservationWizardDialog wd){
		this.wizardDialog = wd;
	}
	
	/**
	 * Sets the current focus to the next button
	 * of the wizard dialog if applicable.
	 */
	public void setFocusNextButton(){
		if (this.wizardDialog != null){
			this.wizardDialog.setNextFocus();
		}
	}
	
	/**
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		 return canFinish; 
	}
	
	/**
	 * Sets if the wizard can finish
	 * @param canFinish
	 */
	public void setCanFinish(boolean canFinish){
		this.canFinish = canFinish;
	}
	
	/**
	 * @return the observation data model
	 */
	public DataModel getDataModel(){
		if (dm == null){
			try(Session session = HibernateManager.openSession()){
				dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
				List<Category> cats = new ArrayList<>();
				cats.addAll(dm.getCategories());
				while(!cats.isEmpty()) {
					Category cc = cats.remove(0);
					cc.getFullCategoryName();
					cc.getName();
					cc.getAttributes().size();
					if (cc.getActiveChildren() != null) cats.addAll(cc.getActiveChildren());
				}
			}
		}
		return dm;
	}
	
	/**
	 * @see org.eclipse.jface.wizard.Wizard#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
	}
	
	/**
	 * Adds the first page to the wizard dialog.  Other pages
	 * are added dynamically.
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
    public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		if (observations.size() == 0){
			new ObservationWizardPage(this);
		}else{
			if (toEdit == null) {
				new ObservationSummaryWizardPage(this);
			}else {
				new AttributeWizardPage(this, toEdit);
			}
		}
    }
    
	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() instanceof IObservationWizardPage){			
			event.doit = ((IObservationWizardPage)event.getCurrentPage()).beforeMoveNext( (IWizardPage)event.getTargetPage() );
		}
		if (event.doit && event.getTargetPage() instanceof IObservationWizardPage){
			((IObservationWizardPage)event.getTargetPage()).beforeShow(  );
		}
		
	}
	
	/**
	 * Saves the cloned data to the waypoint observation.
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		if (getContainer().getCurrentPage() instanceof IObservationWizardPage){
			//need to finish the page
			if (!((IObservationWizardPage)getContainer().getCurrentPage()).beforeMoveNext(null)){
				return false;
			}
		}
		
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				wp.getObservations().removeAll(deletedObservations);
				for (WaypointObservation wo : deletedObservations){
					if (wo.getUuid() != null) session.delete(wo);
				}
				for (ObservationAttachment a : deletedAttachments) {
					session.delete(a);
				}
				session.saveOrUpdate(wp);
				for (List<WaypointObservation> wos : observations.values()){
					for (WaypointObservation wo : wos) {
						wo.setWaypoint(wp);
						session.saveOrUpdate(wo);
						if (!wp.getObservations().contains(wo)) wp.getObservations().add(wo);
					}
				}
				session.flush();
				//commit changes
				session.getTransaction().commit();
				this.deletedObservations.clear();
				this.deletedAttachments.clear();
				
			}catch (Exception ex){
				ObservationPlugIn.displayLog(Messages.ObservationWizard_SaveError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				return false;
			}
		}
		
		try{
			WaypointEventManager.getInstance().waypointModified(wp);
		}catch (Exception ex){
			ObservationPlugIn.log("Error firing events after waypoint observation modifications", ex); //$NON-NLS-1$
		}
		return true;
	}

	/**
	 * Sets the observer.  Will be used to update waypoints
	 * on save.
	 * 
	 * @param observer
	 */
	public void setObserver(Employee observer){
		this.observer = observer;
	}
	
	/**
	 * Current observer
	 * 
	 * @return
	 */
	public Employee getObserver(){
		return this.observer;
	}

    /**
     * Does nothing.
     * @see org.eclipse.jface.wizard.Wizard#performCancel()
     */
    public boolean performCancel() {
    	if (isModified){
    		if (!MessageDialog.openQuestion(getShell(), Messages.ObservationWizard_ConfirmCancel_DialogTitle, Messages.ObservationWizard_ConfirmCancel_DialogMessage)){
    			return false;
    		}
    	}
        return true;
    }

    
    /**
     * 
     * @return list of possible observers
     */
    public List<Employee> getObservers(){
    	return this.observers;
    }
}
