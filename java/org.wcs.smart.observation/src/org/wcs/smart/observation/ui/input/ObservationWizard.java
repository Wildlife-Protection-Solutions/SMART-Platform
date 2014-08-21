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
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.ObservationOptions;
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
	
	//current observations
	private HashMap<Category, List<WaypointObservation>> observations = new HashMap<Category, List<WaypointObservation>>();
	private HashMap<Category, List<WaypointObservation>> workingObservations = new HashMap<Category, List<WaypointObservation>>();
	
	private DataModel dm = null;
	public boolean canFinish = false;
	private Session session;
	private boolean isModified = false;
	
	//categories to process
	private List<Category> toProcess;
	private List<Employee> observers;
	
	private ObservationOptions ops = null;
	/**
	 * Creates a new wizard. 
	 * 
	 * @param wp Waypoint to gather observations for
	 */
	public ObservationWizard(Waypoint wp, List<Employee> observers){
		setWindowTitle(MessageFormat.format(Messages.ObservationWizard_PageName, new Object[]{String.valueOf(wp.getId())}));
		
		super.setForcePreviousAndNextButtons(true);
		super.setNeedsProgressMonitor(false);
		
		this.observers = observers;
		
		session = HibernateManager.openSession(new AttachmentInterceptor());
		session.beginTransaction();
		
		ops = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), session);
		session.update(wp);
		
		this.wp = wp;
		if (this.wp.getObservations() != null){
			for (WaypointObservation ob : this.wp.getObservations()){
				//add to list
				ob.setCategory((Category)session.load(Category.class, ob.getCategory().getUuid()));
				for (CategoryAttribute ca : ob.getCategory().getAttributes()){
					ca.setAttribute((Attribute) session.load(Attribute.class, ca.getAttribute().getUuid()));
				}
				List<WaypointObservation> lst = observations.get(ob.getCategory());
				if (lst == null) {
					lst = new ArrayList<WaypointObservation>();
					observations.put(ob.getCategory(), lst);
				}
				lst.add(ob);
				
//				for (WaypointObservationAttribute a : ob.getAttributes()){
//					a.setAttribute((Attribute)session.merge(a.getAttribute()));
//				}
			}
		}
		this.workingObservations.putAll(observations);
	}
	
	/**
	 * 
	 * @return the observation options
	 */
	public ObservationOptions getObservationOptions(){
		return this.ops;
	}
	
	/**
	 * Initial categories to process.  This must be called
	 * when initialzed and not setCategoriesToProcess so
	 * that the categories can be properly associated
	 * with the current database sesion.
	 * 
	 * @param toProcess
	 */
	public void setInitialCategories(List<Category> toProcess){
		this.toProcess = new ArrayList<Category>();
		for (Category c : toProcess){
			this.toProcess.add((Category)session.load(Category.class, c.getUuid()));
		}
	}
	/**
	 * @param toProcess The list of categories to gather addition attribute information.
	 */
	public void setCategoriesToProcess(List<Category> toProcess){
		this.toProcess = toProcess;
	}
	
	/**
	 * Sets the modified sate of the wizard to true so cancel prompts
	 * to ensure they want to cancel.
	 */
	public void setModified(){
		this.isModified = true;
	}
	
	/**
	 * Clears the working observations
	 */
	public void clearWorkingObservations(){
		this.workingObservations.clear();
		this.workingObservations.putAll(observations);
	}
	/**
	 * 
	 * @param index
	 * @return the categories to process at the given index
	 */
	public Category  getCategoryToProcess(int index){
		return toProcess.get(index);
	}
	
	/**
	 * @return the total number of categories to process
	 */
	public int getCategoryCount(){
		if (toProcess == null) return 0;
		return toProcess.size();
	}
	
	/**
	 * Sets the current waypoint observation category selected by the 
	 * user
	 * 
	 * @param category category
	 * @param catObservations set of observations associated with the category
	 */
	public void setWaypointObservation(Category category, Collection<WaypointObservation> catObservations){
		this.isModified = true;
		if (catObservations == null || catObservations.size() == 0){
			catObservations = new ArrayList<WaypointObservation>();
			WaypointObservation wo = new WaypointObservation();
			wo.setCategory(category);
			wo.setAttributes(new ArrayList<WaypointObservationAttribute>()); //required for hibernate
			catObservations.add(wo);
		}
		ArrayList<WaypointObservation> ops = new ArrayList<WaypointObservation>();
		ops.addAll(catObservations);
//		this.observations.put(category, ops);
		this.workingObservations.put(category, ops);
	}
	
	
	/**
	 * Gets the waypoint observations for a given category 
	 * @param category category
	 * @return set of waypoint observations
	 */
	public Collection<WaypointObservation> getWaypointObservation(Category category){
//		return this.observations.get(category);
		return this.workingObservations.get(category);
	}
	
	/**
	 * @return all observations make at this waypoint
	 */
	public HashMap<Category, List<WaypointObservation>> getAllObservations(){
		return this.observations;
	}
	
	/**
	 * Removes all observations associated with the given category
	 * @param category
	 */
	public void removeObservations(Category category){
		this.isModified = true;
		this.workingObservations.remove(category);
		this.observations.remove(category);
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
			dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
		}
		return dm;
	}
	
	public Session getSession(){
		return this.session;
	}
	
	/**
	 * @see org.eclipse.jface.wizard.Wizard#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (session != null && session.isOpen()){
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
			session.close();
		}
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
			new ObservationSummaryWizardPage(this);
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
		
		setObservations();
		List<WaypointObservation> wobservations = new ArrayList<WaypointObservation>();
		for (Entry<Category,List<WaypointObservation>> entry : this.observations.entrySet()){
			wobservations.addAll(entry.getValue());	
		}
		//set the waypoint of all observations
		for (WaypointObservation ob : wobservations){
			ob.setWaypoint(wp);
		}

		//update the waypoint observation list
		if (wp.getObservations() == null){
			wp.setObservations(new ArrayList<WaypointObservation>());
		}
		
		for (WaypointObservation wo : wp.getObservations()){
			//remove attachments that are not longer attached to new observations
			if (wo.getAttachments() == null){
				wo.setAttachments(new ArrayList<ObservationAttachment>());
			}
			for (ObservationAttachment att : wo.getAttachments()){
				if (att.getObservation().equals(wo) && !wobservations.contains(wo)){
					session.delete(att);
				}
			}
			if (!wobservations.contains(wo)){
				wo.getAttachments().clear();
			}
		}
		
		wp.getObservations().clear();
		wp.getObservations().addAll(wobservations);
		for (WaypointObservation wo : wp.getObservations()){
			session.saveOrUpdate(wo);
		}
		//commit changes
		try{
			session.getTransaction().commit();
		}catch (Exception ex){
			ObservationPlugIn.displayLog(Messages.ObservationWizard_SaveError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			return false;
		}
		
		try{
			WaypointEventManager.getInstance().waypointModified(wp);
		}catch (Exception ex){
			ObservationPlugIn.log("Error firing events after waypoint observation modifications", ex); //$NON-NLS-1$
		}
		return true;
	}

	/**
	 * Merges the current working observations with the 'final' observation
	 * list.
	 */
	public void setObservations(){
		HashMap<Category, List<WaypointObservation>> joined = new HashMap<Category, List<WaypointObservation>>();
		joined.putAll(observations);
		joined.putAll(workingObservations);
		observations= joined;
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
    	session.getTransaction().rollback();
        return true;
    }
    
    /**
     * 
     * @return
     */
    public Waypoint getWaypoint(){
    	return this.wp;
    }
    
    /**
     * 
     * @return list of possible observers
     */
    public List<Employee> getObservers(){
    	return this.observers;
    }
}
