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
package org.wcs.smart.incident.ui.newwizard;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentIdGenerator;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;

/**
 * New incident wizard.
 * @author Emily
 *
 */
public class NewIncidentWizard extends Wizard implements IPageChangingListener {

	private Waypoint newIncident;
	private Session session;
	private ObservationOptions ops;
	
	private Employee observer = null;
	private ObserverWizardPage page0;
	
	public NewIncidentWizard(){
		super();
		
		setWindowTitle(Messages.NewIncidentWizard_WizardTitle);

		newIncident = new Waypoint();
		newIncident.setConservationArea(SmartDB.getCurrentConservationArea());
		newIncident.setSourceId(IndepedentIncidentSource.KEY);
		newIncident.setAttachments(new ArrayList<WaypointAttachment>());
		newIncident.setDateTime(LocalDateTime.now());

		session = HibernateManager.openSession();
		newIncident.setId(IncidentManager.getInstance().getNextIncidentId(session, newIncident.getDateTime(), null));
		ops = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
	}

	public Waypoint getNewIncident(){
		return newIncident;
	}
	
	@Override
	public boolean performFinish() {
		//update the last page
		((IncidentWizardPage)getContainer().getCurrentPage()).updateIncident(newIncident);

		//save to db
		//close and reopen with attachment interceptor
		session.close();
		
		session = HibernateManager.openSession(new AttachmentInterceptor());
		session.beginTransaction();
		try{
			session.saveOrUpdate(newIncident);
			
			for (IWizardPage page : getPages()) {
				if (page instanceof IncidentWizardPage) {
					((IncidentWizardPage)page).afterSave(newIncident, session);
				}
			}
			
			IncidentManager.getInstance().getIncidentProvider(newIncident.getSourceId()).waypointCreated(newIncident, session);
			session.getTransaction().commit();
		}catch(Exception ex){
			session.getTransaction().rollback();
			IncidentPlugIn.displayLog(Messages.NewIncidentWizard_SaveError + ex.getMessage(), ex);
			return false;
		}
		
		// fire events
		IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_ADDED, newIncident);
		return true;
	}
	
	public Employee getObserver() {
		return this.observer;
	}
	
	public void setObserver(Employee observer) {
		this.observer = observer;
	}
	
	/**
	 * Closes the active session
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (session != null && session.isOpen()) {
			session.close();
		}
	}

	@Override
	public boolean canFinish(){
		if (!super.canFinish()) return false;
		
		if (getContainer().getCurrentPage() == null) return false;
		if (! (getContainer().getCurrentPage() instanceof IncidentWizardPage)) return false;
		return ((IncidentWizardPage)super.getContainer().getCurrentPage()).canFinish();
	}
	
    public void addPages() {
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    	
    	try(Session session = HibernateManager.openSession()){
    		if (ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), session).getTrackObserver()) {
    			if (IncidentIdGenerator.INSTANCE.requiresObserver(session, SmartDB.getCurrentConservationArea())) {
    				page0 = new ObserverWizardPage(this);
    				super.addPage(page0);
    			}
    		}
    	}
    	super.addPage(new IncidentWizardPage(this, new DateTimeComposite()));
    	super.addPage(new IncidentWizardPage(this, new IdComposite() {
    		@Override
    		public void initFields(Waypoint incident, Session session) {
    			newIncident.setId(IncidentManager	.getInstance().getNextIncidentId(session, newIncident.getDateTime(), getObserver()));
    			super.initFields(incident, session);
    		}
    	}));
    	super.addPage(new IncidentWizardPage(this, new LocationComposite()));
    	if (ops.getTrackDistanceDirection()){
    		super.addPage(new IncidentWizardPage(this, new DistanceDirectionComposite()));
    	}
    	super.addPage(new IncidentWizardPage(this, new CommentComposite()));
    	super.addPage(new IncidentWizardPage(this, new IncidentAttachmentComposite(false)));
    	
    }
    
	
	@Override
	 public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		if (super.getPages()[0] instanceof IncidentWizardPage) {
			((IncidentWizardPage)super.getPages()[0]).initPage(newIncident, session);
		}
	}
	
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() == page0) {
			setObserver(page0.getObserver());
			return;
		}else {
			WizardPage page = (WizardPage) event.getCurrentPage();
			WizardPage next = (WizardPage) event.getTargetPage();
			WizardPage enext = (WizardPage) getNextPage(page);
			if (next == enext){

				//we are moving forward
				if (page instanceof IncidentWizardPage) {
					String error = ((IncidentWizardPage)page).validate();
					if (error != null){
						//there is an error
						event.doit = false;
						return;
					}
					((IncidentWizardPage)page).updateIncident(newIncident);
				}
				if (next instanceof IncidentWizardPage) {
					//init the new page
					((IncidentWizardPage)next).initPage(newIncident, session);
				}
				
			}else{
				//moving backwards so don't do any validation
			}
			if (next instanceof IncidentWizardPage) {
				((IncidentWizardPage)next).validate();
			}
			getContainer().updateButtons();
		}
		
		
	}

}
