/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.observation.ui;

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.ui.SmartShellDialog;
import org.wcs.smart.ui.Thumbnail;

/**
 * Shell for displaying the observation details of a set of 
 * waypoints.
 * 
 * @author Emily
 *
 */
public class WaypointDetailsShell extends SmartShellDialog{
	
	private List<Waypoint> waypoints;
	
	private Color bgColor = null;
	
	private boolean blnMouseDown;
	private int xPos, yPos;

	public WaypointDetailsShell(Shell ownerShell, List<Waypoint> waypoint){
		super(ownerShell, SWT.RESIZE );
		this.waypoints = waypoint;
	}
	
	private void createInfo(Composite parent, String field, String value) {
		Label l = new Label(parent, SWT.NONE);
		l.setText(MessageFormat.format("{0}:", field)); //$NON-NLS-1$
		
		l = new Label(parent, SWT.NONE);
		l.setText(value == null ? "" : value); //$NON-NLS-1$
	}
	
	public void createContents(Composite parent){
		
		ScrolledComposite scroll = new ScrolledComposite(parent,  SWT.V_SCROLL | SWT.H_SCROLL );
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite owner = new Composite(scroll, SWT.NONE);
		owner.setLayout(new GridLayout());
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scroll.setContent(owner);
		
		FontData fd = owner.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		
		Font boldFont = new Font(parent.getDisplay(), fd);
		parent.addListener(SWT.Dispose, e->boldFont.dispose());
		
		try(Session session = HibernateManager.openSession()){
			for (Waypoint wp : waypoints) {
				wp = session.get(Waypoint.class, wp.getUuid());
				
				Composite lblId = SmartUiUtils.createHeaderLabel(owner, MessageFormat.format(Messages.WaypointDetailsShell_WaypointHeader, wp.getId(), DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(wp.getDateTime())));
				lblId.addListener(SWT.MouseDown, this);
				lblId.addListener(SWT.MouseMove, this);
				lblId.addListener(SWT.MouseUp, this);
				lblId.addListener(SWT.MouseEnter, this);
				lblId.addListener(SWT.MouseExit, this);
				for (Control c : lblId.getChildren()) {
					c.addListener(SWT.MouseDown, this);
					c.addListener(SWT.MouseMove, this);
					c.addListener(SWT.MouseUp, this);
					c.addListener(SWT.MouseEnter, this);
					c.addListener(SWT.MouseExit, this);
				}
				
				
				Composite obsinfo = new Composite(owner, SWT.NONE);
				obsinfo.setLayout(new GridLayout(2, false));
				obsinfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				for (WaypointObservationGroup group : wp.getObservationGroups()) {
					if (wp.getObservationGroups().size() > 1) {
						Composite t = SmartUiUtils.createSubHeaderLabel(obsinfo, Messages.WaypointDetailsShell_GroupHeader);
						((GridData)t.getLayoutData()).horizontalSpan = 2;
					}
					
					for (WaypointObservation o : group.getObservations()) {
						Label l = new Label(obsinfo, SWT.NONE);
						l.setText(o.getCategory().getName());
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
						l.setFont(boldFont);
						
						for (WaypointObservationAttribute a : o.getAttributes()) {
							createInfo(obsinfo, a.getAttribute().getName(), a.getAttributeValueAsString(Locale.getDefault()));
						}
						createAttachments(owner, session, o.getAttachments());

					}
						
				}
				if (!wp.getAttachments().isEmpty()) {
					Label l = new Label(owner, SWT.SEPARATOR | SWT.HORIZONTAL);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
					createAttachments(owner, session, wp.getAttachments());
				}
			}
		}
		
		//configure background color
		bgColor = new Color(parent.getDisplay(), 255, 255, 225);
		List<Control> items = new ArrayList<Control>();
		items.add(owner);
		while(!items.isEmpty()){
			Control c = items.remove(0);
			c.setBackground(bgColor);
			if (c instanceof Composite){
				for (Control kid : ((Composite) c).getChildren()){
					items.add(kid);
				}
			}
		}	
		
		 scroll.setMinSize(owner.computeSize(SWT.DEFAULT, SWT.DEFAULT));

	}

	private void createAttachments(Composite parent, Session session, List<? extends ISmartAttachment> attachments) {
		if (attachments.isEmpty()) return;
		
		Composite a = new Composite(parent, SWT.NONE);
		a.setLayout(new GridLayout(3, false));
		
		for (ISmartAttachment wa : attachments) {
			try {
				wa.computeFileLocation(session);
				Thumbnail t = new Thumbnail(wa,100);
				Composite cc = t.createThumbnail(a);
				cc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				((GridData)cc.getLayoutData()).widthHint = 100;
				((GridData)cc.getLayoutData()).heightHint = 100;
			}catch (Exception ex) {
				ObservationPlugIn.log(ex.getMessage(), ex);
			}
		}
	}
	
	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		
		if (event.type == SWT.Dispose){
			if (bgColor != null){
				bgColor.dispose();
				bgColor = null;
			}
			return;
		}else if (event.type == SWT.Deactivate){
			close();
			return;
		}else if (event.type == SWT.MouseDown){
			blnMouseDown = true;
			xPos = event.x;
			yPos = event.y;
		} else if (event.type == SWT.MouseUp) {
			blnMouseDown = false;
		} else if (event.type == SWT.MouseMove) {
			if (blnMouseDown) {
				shell.setLocation(shell.getLocation().x + (event.x - xPos),
						shell.getLocation().y + (event.y - yPos));
			}
		} else if (event.type == SWT.MouseEnter) {
			shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_SIZEALL));
		} else if (event.type == SWT.MouseExit) {
			shell.setCursor(null);
		}
	}
	
}
