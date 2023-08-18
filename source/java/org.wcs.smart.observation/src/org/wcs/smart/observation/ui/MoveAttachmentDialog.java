/*
 * Copyright (C) 2022 Wildlife Conservation Society
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

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ISignatureAttachment;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for moving attachments between observations and waypoint
 * 
 * @author Emily
 *
 */
public class MoveAttachmentDialog extends SmartStyledTitleDialog {
	
	private static final String WAYPOINT = Messages.MoveAttachmentDialog_WaypointLabel;
	
	private Waypoint waypoint;
	
	private List<Attachment> items;
	private List<WaypointObservation> observations;
	private TableViewer tbViewer;
	private SelectObservationDialog dialog;
	
	private ObservationAttachmentLabelProvider waypointlblprovider = new ObservationAttachmentLabelProvider();
	
	private HashMap<Attachment, Image> images;
	
	/**
	 * @param parentShell
	 */
	public MoveAttachmentDialog(Shell parentShell, Waypoint wp) {
		super(parentShell);
		waypoint = wp;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		images = new HashMap<>();

		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(2, false));
		
		
		tbViewer = new TableViewer(main, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tbViewer.setContentProvider(ArrayContentProvider.getInstance());
		tbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tbViewer.getControl().getLayoutData()).heightHint = 300;
		tbViewer.getTable().setHeaderVisible(true);
		tbViewer.getTable().addListener(SWT.Dispose, e->{
			loadImages.cancel();
			for (Image img : images.values()) {
				img.dispose();
			}
		});
		TableViewerColumn tv = new TableViewerColumn(tbViewer, SWT.NONE);
		tv.getColumn().setWidth(250);
		tv.getColumn().setText(Messages.MoveAttachmentDialog_AttachmentColumn);
		tv.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Attachment) {
					Attachment a = (Attachment)element;
					return waypointlblprovider.getText(a.attachment);
				}
				return super.getText(element);
			}
			
			public Image getImage(Object element) {
				if (element instanceof Attachment) {
					Attachment a = (Attachment)element;
					if (images.containsKey(a)) return images.get(a);
				}
				return null;
			}
		});
		
		tv = new TableViewerColumn(tbViewer, SWT.NONE);
		tv.getColumn().setWidth(200);
		tv.getColumn().setText(Messages.MoveAttachmentDialog_ObservationColumn);
		tv.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Attachment) {
					Attachment a = (Attachment)element;
					if (a.observation == null) return WAYPOINT;
					return a.observation.getCategory().getFullCategoryName();
				}
				return super.getText(element);
			}
		});
		
		tv = new TableViewerColumn(tbViewer, SWT.NONE);
		tv.getColumn().setWidth(200);
		tv.getColumn().setText(Messages.MoveAttachmentDialog_DetailsColumn);
		tv.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Attachment) {
					Attachment a = (Attachment)element;
					if (a.observation == null) return ""; //$NON-NLS-1$
					
					StringBuilder sb = new StringBuilder();
					for (WaypointObservationAttribute attribute : a.observation.getAttributes()) {
						sb.append(attribute.getAttribute().getName());
						sb.append(": "); //$NON-NLS-1$
						sb.append(attribute.getAttributeValueAsString(Locale.getDefault()));
						sb.append(" - "); //$NON-NLS-1$
					}
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		
		tbViewer.addDoubleClickListener(e->{
			Object x = tbViewer.getStructuredSelection().getFirstElement();
			if (x instanceof Attachment) AttachmentUtil.openAttachment(((Attachment) x).attachment);
		});
		Composite buttonPanel = new Composite(main, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnMove = new Button(buttonPanel, SWT.PUSH);
		btnMove.setText(Messages.MoveAttachmentDialog_MoveLbl);
		btnMove.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnMove.addListener(SWT.Selection, e->move());
		btnMove.setBackground(btnMove.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnMove.setEnabled(false);
		
		items = new ArrayList<>();
		observations = new ArrayList<>();
		
		
		try(Session session = HibernateManager.openSession()){
			this.waypoint = session.get(Waypoint.class, waypoint.getUuid());
			
			for (WaypointObservationGroup g : waypoint.getObservationGroups()) {
				for (WaypointObservation wo : g.getObservations()) {
					observations.add(wo);
					wo.getCategory().getFullCategoryName();
					for (ObservationAttachment aa : wo.getAttachments()) {
						try {
							aa.computeFileLocation(session);
						} catch (Exception e1) {
						}
						if (aa.getSignatureType() != null) aa.getSignatureType().getName();
						items.add(new Attachment(aa, wo));
					}
					for (WaypointObservationAttribute a : wo.getAttributes()) {
						a.getAttribute().getName();
						a.getAttributeValueAsString(Locale.getDefault());
					}
					
				}
			}

			for (WaypointAttachment wa : waypoint.getAttachments()) {
				try {
					wa.computeFileLocation(session);
				} catch (Exception e1) {
				}
				items.add(new Attachment(wa, null));
				if (wa.getSignatureType() != null) wa.getSignatureType().getName();
			}
			
			tbViewer.setInput(items);
			
		}
		loadImages.schedule();
		Menu mnu = new Menu(tbViewer.getControl());
		
		MenuItem miMove = new MenuItem(mnu, SWT.PUSH);
		miMove.setText(Messages.MoveAttachmentDialog_MoveLbl);
		miMove.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miMove.setEnabled(false);
		miMove.addListener(SWT.Selection, e->move());
		tbViewer.getControl().setMenu(mnu);
			
		tbViewer.addSelectionChangedListener(e->{
			btnMove.setEnabled(!tbViewer.getSelection().isEmpty());
			miMove.setEnabled(!tbViewer.getSelection().isEmpty());
		});
		
		setMessage(Messages.MoveAttachmentDialog_Message);
		getShell().setText(Messages.MoveAttachmentDialog_Title);
		setTitle(Messages.MoveAttachmentDialog_Title);
		return composite; 
	}

	private void move() {
		if (dialog == null) {
			dialog = new SelectObservationDialog(getShell());
		}
		if (dialog.open() != Window.OK) return;
		Object wo = dialog.getSelection();
		if (wo == null) return;
		
		for (Iterator<?> iterator = tbViewer.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof Attachment) {
				if (wo == WAYPOINT) {
					((Attachment) item).observation = null;
				}else if (wo instanceof WaypointObservation) {
					((Attachment) item).observation = (WaypointObservation)wo;
				}
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(true);
		tbViewer.refresh();
	}
	
	public void okPressed() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (Attachment item : items) {
					session.remove(item.attachment);
					
					if (item.observation == null) {
						WaypointAttachment wa = new WaypointAttachment();
						wa.setFilename(item.attachment.getFilename());
						wa.setSignatureType(((ISignatureAttachment)item.attachment).getSignatureType());
						wa.setWaypoint(waypoint);
						session.persist(wa);
					}else {
						ObservationAttachment oa = new ObservationAttachment();
						oa.setFilename(item.attachment.getFilename());
						oa.setSignatureType(((ISignatureAttachment)item.attachment).getSignatureType());
						oa.setObservation(item.observation);
						session.persist(oa);
					}
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				ObservationPlugIn.displayLog(MessageFormat.format(Messages.MoveAttachmentDialog_MoveError, ex.getMessage()), ex);
			}
		}	
		//fire modified event
		WaypointEventManager.getInstance().waypointModified(waypoint);
		super.okPressed();
	}
	
	class Attachment{
		
		ISmartAttachment attachment;
		WaypointObservation observation;
		
		public Attachment (ISmartAttachment attachment, WaypointObservation observation) {
			this.attachment = attachment;
			this.observation = observation;
		}
	}
	
	class SelectObservationDialog extends SmartStyledTitleDialog{
	
		private Object selection;
		
		private TableViewer tblViewer;
		
		public SelectObservationDialog(Shell parent) {
			super(parent);
		}
		
		public void okPressed() {
			selection = null;
			Object x = tblViewer.getStructuredSelection().getFirstElement();
			if (x instanceof WaypointObservation || x == WAYPOINT) {
				selection = x;
			}
			super.okPressed();
		}
		
		public Object getSelection() {
			return this.selection;
		}
		
		
		@Override
		public Control createDialogArea(Composite parent){
			Composite composite = (Composite)super.createDialogArea(parent);
			
			Composite main = new Composite(composite, SWT.NONE);
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridData)main.getLayoutData()).heightHint = 200;
			
			TableColumnLayout tlayout = new TableColumnLayout();
			main.setLayout(tlayout);
			
			tblViewer = new TableViewer(main, SWT.BORDER | SWT.FULL_SELECTION);
			tblViewer.setContentProvider(ArrayContentProvider.getInstance());
			tblViewer.getTable().setHeaderVisible(true);
			
			TableViewerColumn col = new TableViewerColumn(tblViewer, SWT.NONE);
			tlayout.setColumnData(col.getColumn(), new ColumnWeightData(2));
			col.getColumn().setResizable(true);
			col.getColumn().setText(Messages.MoveAttachmentDialog_CategoryColumn);
			col.setLabelProvider(new ColumnLabelProvider() {
				public String getText(Object element) {
					if (element instanceof WaypointObservation) {
						WaypointObservation wo = (WaypointObservation)element;
						return wo.getCategory().getFullCategoryName();
					}
					return super.getText(element);
				}
			});
			
			col = new TableViewerColumn(tblViewer, SWT.NONE);
			tlayout.setColumnData(col.getColumn(), new ColumnWeightData(3));
			col.getColumn().setResizable(true);
			col.getColumn().setText(Messages.MoveAttachmentDialog_AttributesColumn);
			col.setLabelProvider(new ColumnLabelProvider() {
				public String getText(Object element) {
					if (element instanceof WaypointObservation) {
						WaypointObservation wo = (WaypointObservation)element;
						
						StringBuilder sb = new StringBuilder();
						for (WaypointObservationAttribute woa : wo.getAttributes()) {
							sb.append(woa.getAttribute().getName());
							sb.append(": "); //$NON-NLS-1$
							sb.append(woa.getAttributeValueAsString(Locale.getDefault()));
							sb.append(" - "); //$NON-NLS-1$
						}
						return sb.toString();
					}
					return ""; //$NON-NLS-1$
				}
			});
			List<Object> input = new ArrayList<>();
			input.addAll(observations);
			input.add(WAYPOINT);
			tblViewer.setInput(input);
			
			
			setTitle(Messages.MoveAttachmentDialog_Title);
			setMessage(Messages.MoveAttachmentDialog_ObsSelectionMessage);
			getShell().setText(Messages.MoveAttachmentDialog_Title);
			return composite;
		}
		
	}
	
	
	private Job loadImages = new Job("loading_images") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			for (Attachment a : items) {
				try {
					Path p = EncryptUtils.decryptAttachment(a.attachment);
					Image img = SmartUtils.getImage(p, IconManager.Size.MEDIUM.size);
					images.put(a, img);
					tbViewer.getTable().getDisplay().asyncExec(()->tbViewer.refresh(a));
				} catch (Exception e) {
				}
				if (monitor.isCanceled()) break;
			}
			return Status.OK_STATUS;
		}
		
	};
}
