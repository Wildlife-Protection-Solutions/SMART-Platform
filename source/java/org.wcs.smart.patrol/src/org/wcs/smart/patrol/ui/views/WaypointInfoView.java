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
package org.wcs.smart.patrol.ui.views;

import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointAttachment;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.util.SmartUtils;

/**
 * View for displaying all observation information at a
 * given waypoint.
 * 
 * @author egouge
 *
 */
public class WaypointInfoView extends ViewPart implements ISelectionListener {

	public static final String ID = "org.wcs.smart.patrol.waypointInfo"; //$NON-NLS-1$
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private Label lblWaypointId;
	private Label lblDateTime;
	
	private Font boldFont = null;
	private ScrolledForm infoSection = null;

	private Waypoint currentWp;
	private byte[] selectedWaypointUuid;
		
	//listener for modifications to waypoints
	private IPatrolEventListener waypointListener = new IPatrolEventListener() {
		@Override
		public void eventFired(int attributeChanged, Object source) {
			Waypoint wp = ((Waypoint)source);
			if (Arrays.equals(wp.getUuid(), selectedWaypointUuid)){
				updateUiJob.schedule();
			}
			
		}
	};
	
	private Composite compThumbnails;
	
	// job to update view
	private Job updateUiJob = new Job(Messages.WaypointInfoView_UpdateJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (infoSection.isDisposed()) return Status.OK_STATUS;
			
			final HashMap<String, List<List<String[]>>> displayData = new HashMap<String, List<List<String[]>>>();
			final List<Thumbnail> thumbnails = new ArrayList<Thumbnail>();
			
			final List<Label> categoryLabels = new ArrayList<Label>();
			final List<Label> attributeLabels = new ArrayList<Label>();
			final List<Label> attributeValuesLabels = new ArrayList<Label>();
			
			Date wpDate = null;
			
			//load waypoint information
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				currentWp = (Waypoint) s.get(Waypoint.class, selectedWaypointUuid);	//reload waypoint to get latest info
				if (currentWp != null) {
					wpDate = currentWp.getPatrolLegDay().getDate();
					HashMap<Category, List<List<String[]>>> data = new HashMap<Category, List<List<String[]>>>();
					if (currentWp.getObservations() != null) {
						for (WaypointObservation wo : currentWp.getObservations()) {
							List<List<String[]>> ops = data.get(wo.getCategory());
							if (ops == null) {
								ops = new ArrayList<List<String[]>>();
								data.put(wo.getCategory(), ops);
							}
							ArrayList<String[]> attributeValues = new ArrayList<String[]>();
							for (WaypointObservationAttribute woa : wo.getAttributes()) {
								String[] info = new String[] {
										woa.getAttribute().getName(),
										woa.getAttributeValueAsString() };
								attributeValues.add(info);
							}
							ops.add(attributeValues);
						}
					}
					for (Entry<Category, List<List<String[]>>> cat : data.entrySet()) {
						Category c = (Category) s.merge(cat.getKey());
						displayData.put(c.getFullCategoryName(), cat.getValue());
					}
					//load attachment information
					if (currentWp.getAttachments() != null){
						for(WaypointAttachment att: currentWp.getAttachments()){
							att.getFullFile();
						}
					}
				}
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			final Date wpDate2 = wpDate;

			// update ui with observation information 
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (lblWaypointId.isDisposed())
						return;

					for (Control c : infoSection.getBody().getChildren()) {
						c.dispose();
					}
					if (currentWp == null){
						clearContents();
					}else{
					lblWaypointId.setText(String.valueOf(currentWp.getId()));
					lblDateTime
							.setText(DateFormat.getDateInstance(
									DateFormat.SHORT).format(wpDate2)
									+ " " + DateFormat.getTimeInstance(DateFormat.SHORT).format(currentWp.getTime())); //$NON-NLS-1$

					for (Entry<String, List<List<String[]>>> cat : displayData
							.entrySet()) {
						Label lbl = toolkit.createLabel(infoSection.getBody(),SmartUtils.formatStringForLabel(cat.getKey()),SWT.WRAP);
						lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
						((GridData)lbl.getLayoutData()).widthHint = 100;
						lbl.setFont(boldFont);
						categoryLabels.add(lbl);

						Composite attributeComp = toolkit.createComposite(infoSection.getBody());
						attributeComp.setLayout(new GridLayout(2, false));
						((GridLayout) attributeComp.getLayout()).marginLeft = 5;
						
						
						attributeComp.setLayoutData(new GridData(SWT.FILL,
								SWT.FILL, true, false));
						
						for (int i = 0; i < cat.getValue().size(); i ++){
							List<String[]> obs = cat.getValue().get(i);
							Collections.sort(obs, new Comparator<String[]>() {
								@Override
								public int compare(String[] o1, String[] o2) {
									return Collator.getInstance().compare(o1[0], o2[0]);
								}});
							for (String[] att : obs){
								Label l = toolkit.createLabel(attributeComp, SmartUtils.formatStringForLabel(att[0] + ":"), SWT.WRAP); //$NON-NLS-1$
								l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
								((GridData)l.getLayoutData()).widthHint = 100;
								attributeLabels.add(l);
								
								l = toolkit.createLabel(attributeComp, SmartUtils.formatStringForLabel(att[1]), SWT.WRAP);
								l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
								((GridData)l.getLayoutData()).widthHint = 100;
								attributeValuesLabels.add(l);
							}
							if ( i < cat.getValue().size() - 1){
								Label l = toolkit.createLabel(attributeComp, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
								l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
							}
						}
						
						Label l2 = toolkit.createLabel(infoSection.getBody(), "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
						l2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
					}
					}
					
			
					if (currentWp != null && currentWp.getAttachments() != null && currentWp.getAttachments().size() > 1){
						compThumbnails = toolkit.createComposite(infoSection.getBody());
						compThumbnails.setLayout(new GridLayout());
						toolkit.createLabel(compThumbnails, Messages.WaypointInfoView_LoadingThumbnails); 
					}else{
						compThumbnails = null;
					}
					
					infoSection.getBody().pack();
					infoSection.getBody().layout();
					infoSection.reflow(true);
					lblWaypointId.getParent().layout();
				}	
			});
			
			
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			//load thumbnails
			if (currentWp != null && currentWp.getAttachments() != null){
				for(WaypointAttachment att: currentWp.getAttachments()){
					thumbnails.add(new Thumbnail(att));
				}
			}
			
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			if (thumbnails.size() > 0){
				//update thumbnails
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						if (lblWaypointId.isDisposed()) return ;
					
						//display of loading label
						if (compThumbnails != null) compThumbnails.dispose();
					
						compThumbnails = toolkit.createComposite(infoSection.getBody(), SWT.NONE);
						GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
						gd.widthHint = 100;
						compThumbnails.setLayoutData(gd);
				
						compThumbnails.setLayout(new GridLayout());
						for (Thumbnail nail : thumbnails){
							Composite parent = toolkit.createComposite(compThumbnails);
							nail.createThumbnail(parent);
						}
				
						Listener resize = new Listener(){
							@Override
							public void handleEvent(Event event) {
								
								int width = infoSection.getClientArea().width - infoSection.getVerticalBar().getSize().x;
								
								for (Label l : categoryLabels){
									((GridData)l.getLayoutData()).widthHint = width;
								}
								for (Label l : attributeLabels){
									int x = l.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
									if (x > 0.5 * width){
										x = (int)(0.5 * width);
									}
									((GridData)l.getLayoutData()).widthHint = x;
								
								}
								
								for (Label l : attributeValuesLabels){
									int x = l.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
									if (x > 0.5 * width){
										x = (int)(0.5 * width);
									}
									((GridData)l.getLayoutData()).widthHint = x;
								}
								
								infoSection.getBody().layout(true);
								
								width = compThumbnails.getSize().x - infoSection.getVerticalBar().getSize().x;
								int cols = (int)Math.floor(width / 100.0);
								compThumbnails.setLayout(new GridLayout(cols, false));
								
								compThumbnails.layout(true);
								infoSection.reflow(true);
							}
						};
						compThumbnails.addListener(SWT.Resize, resize);
						compThumbnails.layout(true);
						infoSection.getBody().pack();
						infoSection.getBody().layout();
						infoSection.reflow(true);
						lblWaypointId.getParent().layout();
						
					}
				});
			}
			return Status.OK_STATUS;
		}};
	
	/**
	 * Creates new view
	 */
	public WaypointInfoView() {
		PatrolEventManager.getInstance().addListener(EventType.WAYPOINT_DELETED, waypointListener);
		PatrolEventManager.getInstance().addListener(EventType.WAYPOINT_MODIFIED, waypointListener);
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		
		toolkit.dispose();
		
		PatrolEventManager.getInstance().removeListener(EventType.WAYPOINT_DELETED, waypointListener);
		PatrolEventManager.getInstance().removeListener(EventType.WAYPOINT_MODIFIED, waypointListener);
		
		getSite().getPage().removeSelectionListener(this);
		if (boldFont != null && !boldFont.isDisposed()){
			boldFont.dispose();
			boldFont = null;
		}
	}
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		getSite().getPage().addSelectionListener(this);
		
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = gl.marginHeight = 0;
		parent.setLayout(gl);
		
		
		Composite main = toolkit.createComposite(parent);
		gl = new GridLayout(1, false);
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(main);
		
		Label l = toolkit.createLabel(main, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		infoSection = toolkit.createScrolledForm(main);
		gl = new GridLayout(1, false);
		gl.marginWidth = gl.marginHeight = 0;
		
		infoSection.getBody().setLayout(gl);
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		infoSection.setLayoutData(gd);
	}
	
	
	
	private void createHeader(Composite parent) {
		Composite header = toolkit.createComposite(parent);
		header.setLayout(new GridLayout(4, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Label l = toolkit.createLabel(header, Messages.WaypointInfoView_WaypointIdLabel);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(getViewSite().getShell().getDisplay(), fd);
		//l.setFont(boldFont);
		
		lblWaypointId = toolkit.createLabel(header, ""); //$NON-NLS-1$
		lblWaypointId.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		l = toolkit.createLabel(header,  Messages.WaypointInfoView_DateTimeLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		//l.setFont(boldFont);
		
		lblDateTime = toolkit.createLabel(header, ""); //$NON-NLS-1$
		lblDateTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
	}

	/**
	 * Updates the contents of with the information
	 * in the provided waypoint
	 * @param wp
	 */
	private void updateContents(final Waypoint wp){
		if (selectedWaypointUuid != null && Arrays.equals(selectedWaypointUuid,wp.getUuid())){
			//same waypoint do nothing
			return;
		}
		this.selectedWaypointUuid = wp.getUuid();
		updateUiJob.cancel();
		updateUiJob.schedule();
	}
	
	/**
	 * Clears the current contents
	 */
	private void clearContents(){
		selectedWaypointUuid = null;
		currentWp = null;
		for (Control c : infoSection.getBody().getChildren()) {
			c.dispose();
		}

		lblWaypointId.setText(""); //$NON-NLS-1$
		lblDateTime.setText(""); //$NON-NLS-1$
	}
	
	@Override
	public void setFocus() {
		lblWaypointId.setFocus();
	}

	/**
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection.isEmpty()) {
			return;
		}
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection ss = (IStructuredSelection) selection;
		Object o = ss.getFirstElement();
		boolean update = false;
		if (o instanceof Waypoint) {
			update = true;
			updateContents((Waypoint) o);
		}else if (o instanceof IAdaptable){
			Waypoint wp = (Waypoint) ((IAdaptable)o).getAdapter(Waypoint.class);
			if (wp != null){
				update = true;
				updateContents(wp);
			}
		}
		if (!update){
			clearContents();
		}
	}
}
