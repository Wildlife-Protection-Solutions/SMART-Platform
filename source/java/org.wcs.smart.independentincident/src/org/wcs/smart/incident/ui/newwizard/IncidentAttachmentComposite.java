package org.wcs.smart.incident.ui.newwizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentComposite;
import org.wcs.smart.common.attachment.IAttachmentsChangeListener;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;

public class IncidentAttachmentComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.attachment";
	
	private AttachmentComposite<WaypointAttachment> attachmentComp;
	
	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		attachmentComp = new AttachmentComposite<WaypointAttachment>(parent, SWT.NONE) {
			@Override
			protected WaypointAttachment createNewAttachement() {
				return new WaypointAttachment();
			}
		};
		attachmentComp.addAttachmentsChangeListener(new IAttachmentsChangeListener() {
			@Override
			public void attachmentsChanged() {
				fireChange(new Event());
				
			}
		});
		return attachmentComp;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		if (incident.getAttachments() == null){
			incident.setAttachments(new ArrayList<WaypointAttachment>());
		}
		List<WaypointAttachment> atts = attachmentComp.getAttchments();
		incident.getAttachments().clear();
		
		for (WaypointAttachment a : atts){
			incident.getAttachments().add((WaypointAttachment)a);
			a.setWaypoint(incident);
		}
				
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		if (incident.getAttachments() != null){
			attachmentComp.initAttachments(incident.getAttachments());
		}
	}

	@Override
	public String getName() {
		return "Attachments";
	}

	@Override
	public String getDescription() {
		return "Any attachments associated with the incident.";
	}
} 
