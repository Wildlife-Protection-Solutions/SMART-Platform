package org.wcs.smart.incident.ui.newwizard;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

public class EditIncidentDialog extends AbstractPropertyJHeaderDialog {

	private String panelId;
	private Waypoint incident;
	
	private AbstractIncidentComposite panel;
	
	public EditIncidentDialog(Shell parentShell, String panelId, Waypoint incident) {
		super(parentShell, "Edit Incident");
		this.panelId = panelId;
		this.incident = incident;
	}


	@Override
	protected Composite createContent(Composite parent) {
		
		if (panelId == CommentComposite.ID){
			panel = new CommentComposite();
		}else if (panelId == DateTimeComposite.ID){
			panel = new DateTimeComposite();
		}else if(panelId == IdComposite.ID){
			panel = new IdComposite();
		}else if (panelId == LocationComposite.ID){
			panel = new LocationComposite();
		}else if (panelId == IncidentAttachmentComposite.ID){
			panel = new IncidentAttachmentComposite();
		}else if (panelId == DistanceDirectionComposite.ID){
			panel = new DistanceDirectionComposite();
		}
		
		if (panel != null){
			Composite center = new Composite(parent, SWT.NONE);
			GridLayout gl = new GridLayout();
			gl.marginWidth = 20;
			center.setLayout(gl);
			
			Composite x = panel.createComposite(center);
			x.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
			
			panel.addChangeListener(new Listener(){
				@Override
				public void handleEvent(Event event) {
					setChangesMade(true);
					setErrorMessage(panel.validate());
				}});
			
			setTitle(panel.getName());
			getShell().setText("Edit Incident");
			setMessage(panel.getDescription());
			
			panel.initFields(incident, getSession());
			setChangesMade(false);
			return center;
		}
		return parent;
	}

	@Override
	public Session getSession(){
		if (session == null || !session.isOpen()){
			session = HibernateManager.openSession(new AttachmentInterceptor());
		}
		return session;
	}

	@Override
	protected boolean performSave() {
		if (panel != null){
			String error = panel.validate();
			if (error != null){
				MessageDialog.openError(getShell(), "Error", "You cannot save until you've resolved all errors. " + error );
				return false;
			}
			Session session = getSession();
			session.beginTransaction();
			try{
				panel.updateIncident(incident);
				session.saveOrUpdate(incident);
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				IncidentPlugIn.displayLog("Error saving changes.  Please restart the application.", ex);
			}
			IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_MODIFIED, incident);
			setChangesMade(false);
		}
		return true;
	}
}
