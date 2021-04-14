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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentComposite;
import org.wcs.smart.common.attachment.IAttachmentsChangeListener;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.ui.ObservationAttachmentLabelProvider;

/**
 * Incident attachments composite.
 * @author Emily
 *
 */
public class IncidentAttachmentComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.attachment"; //$NON-NLS-1$
	
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
		return Messages.IncidentAttachmentComposite_Name;
	}

	@Override
	public String getDescription() {
		return Messages.IncidentAttachmentComposite_Description;
	}
} 
