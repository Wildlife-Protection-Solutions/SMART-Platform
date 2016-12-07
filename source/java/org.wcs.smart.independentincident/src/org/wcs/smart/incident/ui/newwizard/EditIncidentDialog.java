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

/**
 * Dialog for editing an incident field.
 */
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
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

public class EditIncidentDialog extends AbstractPropertyJHeaderDialog {

	private String panelId;
	private Waypoint incident;
	
	private AbstractIncidentComposite panel;
	
	/**
	 * Creates a new dialog
	 * @param parentShell
	 * @param panelId the field to edit.  A AbstractIncidentcomposite with this
	 * id should exist.
	 * @param incident the incident to edit
	 */
	public EditIncidentDialog(Shell parentShell, String panelId, Waypoint incident) {
		super(parentShell, Messages.EditIncidentDialog_DialogTitle);
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
			getShell().setText(Messages.EditIncidentDialog_ShellTitle);
			setMessage(panel.getDescription());
			
			Session s = openSession();
			try{
				panel.initFields(incident, s);	
			}finally{
				s.close();
			}
			
			setChangesMade(false);
			return center;
		}
		return parent;
	}

	private Session openSession(){
		return HibernateManager.openSession(new AttachmentInterceptor());
	}

	@Override
	protected boolean performSave() {
		if (panel != null){
			String error = panel.validate();
			if (error != null){
				MessageDialog.openError(getShell(), Messages.EditIncidentDialog_Error, Messages.EditIncidentDialog_ErrorExists + error );
				return false;
			}
			Session session = openSession();
			session.beginTransaction();
			try{
				panel.updateIncident(incident);
				session.saveOrUpdate(incident);
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				IncidentPlugIn.displayLog(Messages.EditIncidentDialog_SaveError, ex);
			}finally{
				session.close();
			}
			IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_MODIFIED, incident);
			setChangesMade(false);
		}
		return true;
	}
}
