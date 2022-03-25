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
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentComposite;
import org.wcs.smart.common.attachment.IAttachmentsChangeListener;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.ui.MoveAttachmentDialog;
import org.wcs.smart.observation.ui.ObservationAttachmentLabelProvider;

/**
 * Incident attachments composite.
 * @author Emily
 *
 */
public class IncidentAttachmentComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.attachment"; //$NON-NLS-1$
	
	private AttachmentComposite<WaypointAttachment> attachmentComp;
	
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

	@Override
	public Composite createComposite(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attachmentComp = new AttachmentComposite<WaypointAttachment>(part, SWT.NONE) {
			@Override
			protected WaypointAttachment createNewAttachement() {
				return new WaypointAttachment();
			}
			@Override
			protected void createControls() {
				super.createControls();
				tblAttachments.setLabelProvider(new ObservationAttachmentLabelProvider(){
					@Override
					public String getText(Object element) {
						String text = super.getText(element);
						if (element instanceof ISmartAttachment){
							if (other.contains(element)) text = "**" + text; //$NON-NLS-1$
						}
						return text;
					}
				});
			}
		};
		attachmentComp.addAttachmentsChangeListener(new IAttachmentsChangeListener() {
			@Override
			public void attachmentsChanged() {
				fireChange(new Event());
				
			}
		});
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
		
		return part;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		if (incident.getAttachments() == null){
			incident.setAttachments(new ArrayList<WaypointAttachment>());
		}
		List<WaypointAttachment> atts = attachmentComp.getAttchments();
		
		List<WaypointAttachment> toDelete = new ArrayList<>();
		for (WaypointAttachment existing : incident.getAttachments()) {
			if (!atts.contains(existing)) toDelete.add(existing);
		}
		incident.getAttachments().removeAll(toDelete);
		
		for (WaypointAttachment a : atts){
			if (!incident.getAttachments().contains(a)) {
				incident.getAttachments().add((WaypointAttachment)a);
				a.setWaypoint(incident);
			}
		}
		
				
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		this.wp = incident;
		if (incident.getAttachments() != null){
			attachmentComp.initAttachments(incident.getAttachments());
		}
		
		if (isEditing) {
			List<ISmartAttachment> obs = new ArrayList<ISmartAttachment>();
			
			for (WaypointObservation o : incident.getAllObservations()) {
				if (o.getAttachments() != null) obs.addAll(o.getAttachments());
			}
			attachmentComp.initOtherAttachments(obs);
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
