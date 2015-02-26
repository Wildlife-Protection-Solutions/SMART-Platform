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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
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
	
	
	public NewIncidentWizard(){
		super();
		
		setWindowTitle(Messages.NewIncidentWizard_WizardTitle);

		newIncident = new Waypoint();
		newIncident.setConservationArea(SmartDB.getCurrentConservationArea());
		newIncident.setSourceId(IndepedentIncidentSource.KEY);
		newIncident.setAttachments(new ArrayList<WaypointAttachment>());
		
		session = HibernateManager.openSession();
		
		Query q = session.createQuery("SELECT max(id) FROM Waypoint WHERE sourceId = :source AND conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("source", IndepedentIncidentSource.KEY); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		List<?> maxIs = q.list();
		if (maxIs.size() > 0 && maxIs.get(0) != null ){
			newIncident.setId((Integer)maxIs.get(0) + 1);
		}else{
			//start at 1
			newIncident.setId(1);
		}
		
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
		
		if (getContainer().getCurrentPage() != null && ((IncidentWizardPage)super.getContainer().getCurrentPage()).canFinish()){
			return true;
		}
		return false;
	}
	
    public void addPages() {
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    	
    	super.addPage(new IncidentWizardPage(this, new IdComposite()));
    	super.addPage(new IncidentWizardPage(this, new DateTimeComposite()));
    	super.addPage(new IncidentWizardPage(this, new LocationComposite()));
    	if (ops.getTrackDistanceDirection()){
    		super.addPage(new IncidentWizardPage(this, new DistanceDirectionComposite()));
    	}
    	super.addPage(new IncidentWizardPage(this, new CommentComposite()));
    	super.addPage(new IncidentWizardPage(this, new IncidentAttachmentComposite()));
    	
    }
    
	
	@Override
	 public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
	
		((IncidentWizardPage)super.getPages()[0]).initPage(newIncident, session);
	}
	
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		IncidentWizardPage page = (IncidentWizardPage) event.getCurrentPage();
		IncidentWizardPage next = (IncidentWizardPage) event.getTargetPage();
		IncidentWizardPage enext = (IncidentWizardPage) getNextPage(page);
		if (next == enext){
			//we are moving forward
			String error = page.validate();
			if (error != null){
				//there is an error
				event.doit = false;
				return;
			}
			page.updateIncident(newIncident);
			
			//init the new page
			next.initPage(newIncident, session);
			
		}else{
			//moving backwards so don't do any validation
		}
		next.validate();
		getContainer().updateButtons();
		
		
	}

}
