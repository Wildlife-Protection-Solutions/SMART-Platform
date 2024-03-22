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
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.wcs.smart.AttachmentTagManager;
import org.wcs.smart.ca.AttachmentTag;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateUtil;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.AttachmentTagLink;
import org.wcs.smart.observation.model.ITaggedAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.ui.MoveAttachmentDialog;
import org.wcs.smart.observation.ui.input.AttachmentPreviewTagComposite;

/**
 * Incident attachments composite.
 * @author Emily
 *
 */
public class IncidentAttachmentComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.attachment"; //$NON-NLS-1$
	
	private AttachmentPreviewTagComposite preview;
	private List<ISmartAttachment> attachments;
	
	//if false it means new incident is being created, not
	//an existing one being edited
	private boolean isEditing = false;
	private Supplier<Boolean> beforeEdit;
	private Waypoint wp;
	
	public IncidentAttachmentComposite(boolean isEditing) {
		this(isEditing, null);
	}
	
	public IncidentAttachmentComposite(boolean isEditing, Supplier<Boolean> beforeEdit) {
		this.isEditing = isEditing;
		this.beforeEdit = beforeEdit;
	}
	
	@Override
	public String validate() {
		return null;
	}

	private void addAttachment() {
		preview.addAttachment(()->new WaypointAttachment(), attachments);
		modified();
		
	}
	
	private void deleteAttachment() {
		preview.deleteAttachments(attachments);
		modified();
	}
	
	@Override
	public Composite createComposite(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label ll = new Label(part, SWT.NONE);
		ll.setText(Messages.IncidentSummaryPage_AttachmentsLabel);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		preview = new AttachmentPreviewTagComposite(part,
				Collections.emptyList(),
				e->addAttachment(),
				e->deleteAttachment(),
				a->(a instanceof WaypointAttachment));
		
		if (isEditing) {
			
			Label l = new Label(part, SWT.NONE);
			l.setText(Messages.IncidentAttachmentComposite_observationattachmentsmessage);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Link hl = new Link(part,SWT.NONE);
			hl.setText("<a>" + Messages.IncidentAttachmentComposite_moveLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			hl.setToolTipText(Messages.IncidentAttachmentComposite_moveTooltip);
			hl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			hl.addListener(SWT.Selection, e->{
				if (beforeEdit != null && !beforeEdit.get()) return;
				MoveAttachmentDialog d = new MoveAttachmentDialog(Display.getDefault().getActiveShell(), wp);
				d.open();
			});
		}
		preview.addListener(SWT.Modify, e->modified());
		return part;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		if (incident.getAttachments() == null){
			incident.setAttachments(new ArrayList<WaypointAttachment>());
		}	
		List<WaypointAttachment> items = attachments
				.stream().filter(e->e instanceof WaypointAttachment)
				.map(e->(WaypointAttachment)e)
				.collect(Collectors.toList());
		
		HibernateUtil.mergeCollection(incident.getAttachments(), items);
		
		for (WaypointAttachment wa : incident.getAttachments()) {
			if (wa.getWaypoint() != incident) wa.setWaypoint(incident);
			
			WaypointAttachment updated = HibernateUtil.findInList(items, wa);
			if (updated != null) HibernateUtil.mergeCollection(wa.getAttachmentTags(), updated.getAttachmentTags());
		}
	}
	
	@Override
	public void afterSave(Waypoint incident, Session session) {
		for (WaypointAttachment wa : incident.getAttachments()) {
			if (wa.getAttachmentTags() == null) wa.setAttachmentTags(new ArrayList<>());
			for (AttachmentTagLink link : wa.getAttachmentTags()) {
				if (link.getUuid() == null) session.persist(link);
			}
		}
	}

	private void modified() {
		fireChange(new Event());
	}
	@Override
	public void initFields(Waypoint incident, Session session) {
		this.wp = incident;
		
		List<AttachmentTag> tags = AttachmentTagManager.INSTANCE.getTags(session, incident.getConservationArea());
		preview.setTags(tags);
		
		if (incident.getAttachments() != null){
			try {
				ObservationHibernateManager.computeAttachmentLocations(incident, session);
			} catch (Exception e) {
				IncidentPlugIn.log(e.getMessage(), e);
			}
			
			attachments = new ArrayList<>();
			attachments.addAll(wp.getAttachments());
			for (WaypointObservation o : wp.getAllObservations()) {
				if (o.getAttachments() != null) attachments.addAll(o.getAttachments());
			}
			attachments.forEach(a->Hibernate.initialize(((ITaggedAttachment)a).getAttachmentTags()));
			
			preview.setInput(attachments);
		}	
	}
	
	@Override
	public String getName() {
		return Messages.IncidentAttachmentComposite_Name;
	}

	@Override
	public String getDescription() {
		return Messages.IncidentAttachmentComposite_Description;
	}
} 
