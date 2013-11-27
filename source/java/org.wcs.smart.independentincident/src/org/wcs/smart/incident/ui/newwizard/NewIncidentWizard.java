package org.wcs.smart.incident.ui.newwizard;

import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.ui.IncidentEditor;
import org.wcs.smart.incident.ui.IncidentEditorInput;
import org.wcs.smart.observation.model.Waypoint;

public class NewIncidentWizard extends Wizard implements IPageChangingListener {

	private Waypoint newIncident;
	private Session session;
	
	public NewIncidentWizard(){
		super();
		
		setWindowTitle("New Incident");

		newIncident = new Waypoint();
		newIncident.setConservationArea(SmartDB.getCurrentConservationArea());
		newIncident.setSourceId(IndepedentIncidentSource.KEY);
		
		session = HibernateManager.openSession();
		
		Query q = session.createQuery("SELECT max(id) FROM Waypoint WHERE sourceId = ?");
		q.setParameter(0, IndepedentIncidentSource.KEY);
		List<?> maxIs = q.list();
		if (maxIs.size() > 0){
			newIncident.setId((Integer)maxIs.get(0) + 1);
		}else{
			//start at 1
			newIncident.setId(1);
		}
		
	}

	@Override
	public boolean performFinish() {
		//update the last page
		((IncidentWizardPage)this.getPages()[this.getPageCount()-1]).updateIncident(newIncident);
		
		//save to db
		session.beginTransaction();
		try{
			session.save(newIncident);
			session.getTransaction().commit();
		}catch(Exception ex){
			session.getTransaction().rollback();
			IncidentPlugIn.displayLog("Error occurred while saving incident. " + ex.getMessage(), ex);
			return false;
		}
		
		// fire events
		IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_ADDED, newIncident);
		
		// open in editor
		IncidentEditorInput input = new IncidentEditorInput(this.newIncident.getUuid(),this.newIncident.getId(), this.newIncident.getDateTime());
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, IncidentEditor.ID);
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
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
    	super.addPage(new IncidentWizardPage(this, new DistanceDirectionComposite()));
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
