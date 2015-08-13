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
package org.wcs.smart.observation.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.hibernate.Session;
import org.locationtech.udig.project.AdaptableFeature;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.events.IWaypointEventListener;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.events.WaypointEventManager.EventType;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.util.SmartUtils;

/**
 * View for displaying all observation information at a
 * given waypoint.
 * 
 * @author egouge
 *
 */
public class WaypointInfoView {

	public static final String ID = "org.wcs.smart.observation.waypointInfo"; //$NON-NLS-1$
	private FormToolkit toolkit = null;

	private Label lblWaypointId;
	private Label lblDateTime;
	
	private Font boldFont = null;
	private ScrolledForm infoSection = null;

	private UUID selectedWaypointUuid;
	
	//listener for modifications to waypoints
	private IWaypointEventListener waypointListener = new IWaypointEventListener() {
		@Override
		public void handleEvent(Waypoint wp) {
			if (wp.getUuid().equals(selectedWaypointUuid)){
				updateUiJob.schedule();
			}
		}
	};
	
	private Composite compThumbnails;
	private List<ThumbnailComposite> obsThumbs = null;
	
	// job to update view
	private Job updateUiJob = new Job(Messages.WaypointInfoView_UpdateJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (infoSection == null || infoSection.isDisposed()) return Status.OK_STATUS;
			
			final List<Thumbnail> thumbnails = new ArrayList<Thumbnail>();
			
			final HashMap<Category, List<DisplayData>> data = new HashMap<Category, List<DisplayData>>();

			Waypoint currentWp = null;
			
			//load waypoint information
			final Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				currentWp = (Waypoint) s.get(Waypoint.class, selectedWaypointUuid);	//reload waypoint to get latest info
				if (currentWp != null) {
					if (currentWp.getObservations() != null) {
						for (WaypointObservation wo : currentWp.getObservations()) {
							DisplayData dd = new DisplayData();
							
							List<DisplayData> cData = data.get(wo.getCategory());
							if (cData == null){
								cData = new ArrayList<DisplayData>();
								data.put(wo.getCategory(), cData);
							}
							cData.add(dd);
							
							dd.categoryLabel = wo.getCategory().getFullCategoryName();
							
							//sort observation attribute based on data model order
							Category c = (Category) s.load(Category.class, wo.getCategory().getUuid());
							final List<Attribute> attributes = new ArrayList<Attribute>();
							c.getAllAttribute(attributes, null);
							List<WaypointObservationAttribute> tmp = new ArrayList<WaypointObservationAttribute>();
							tmp.addAll(wo.getAttributes());
							Collections.sort(tmp, new Comparator<WaypointObservationAttribute>() {
								@Override
								public int compare(
										WaypointObservationAttribute o1,
										WaypointObservationAttribute o2) {
									int index1 = attributes.indexOf(o1.getAttribute());
									int index2 = attributes.indexOf(o2.getAttribute());
									if (index1 == index2){
										return 0;
									}
									if (index1 > index2) return 1;
									return -1;
								}
							});
							for (WaypointObservationAttribute woa : tmp) {
								dd.attributeLabels.add(woa.getAttribute().getName());
								dd.attributeValues.add(woa.getAttributeValueAsString(Locale.getDefault()));
							}
							for (ObservationAttachment att: wo.getAttachments()){
								try{
									att.computeFileLocation(s);
									dd.attchmentFileNames.add(att);
								}catch (Exception ex){
									ObservationPlugIn.log(ex.getMessage(), ex);
								}
							}
						}
					}
					
					//load attachment information
					if (currentWp.getAttachments() != null){
						for(WaypointAttachment att: currentWp.getAttachments()){
							try{
								att.computeFileLocation(s);
							}catch (Exception ex){
								ObservationPlugIn.log(ex.getMessage(), ex);
							}
						}
					}
				}
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
//			final Date wpDate2 = wpDate;
			final Waypoint lcurrentWp = currentWp;
			
			final List<Label> categoryLabels = new ArrayList<Label>();
			final List<Label> attributeLabels = new ArrayList<Label>();
			final List<Label> attributeValuesLabels = new ArrayList<Label>();
			
			// update ui with observation information 
			infoSection.getDisplay().syncExec(new Runnable(){
				@Override
				public void run() {
					if (lblWaypointId.isDisposed())
						return;

					for (Control c : infoSection.getBody().getChildren()) {
						c.dispose();
					}
					if (obsThumbs != null){
						for (ThumbnailComposite c : obsThumbs ){
							c.dispose();
						}
					}
					obsThumbs = new ArrayList<ThumbnailComposite>();
					
					if (lcurrentWp == null){
						clearContents();
					}else{
						lblWaypointId.setText(String.valueOf(lcurrentWp.getId()));
						lblDateTime.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(lcurrentWp.getDateTime())); 

						int widthHint = infoSection.getBody().getBounds().width - 20;						
						for (List<DisplayData> d : data.values()) {
							Label lbl = toolkit.createLabel(infoSection.getBody(), SmartUtils.formatStringForLabel(d.get(0).categoryLabel), SWT.WRAP);
							lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
							((GridData) lbl.getLayoutData()).widthHint = widthHint;
							lbl.setFont(boldFont);
							categoryLabels.add(lbl);

							Composite attributeComp = toolkit.createComposite(infoSection.getBody());
							attributeComp.setLayout(new GridLayout(2, false));
							((GridLayout) attributeComp.getLayout()).marginLeft = 5;

							attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

							for (int i = 0 ; i < d.size(); i ++){
								for (int k = 0; k < d.get(i).attributeLabels.size(); k ++){
									Label l = toolkit.createLabel(attributeComp,SmartUtils.formatStringForLabel(d.get(i).attributeLabels.get(k) + ":"), SWT.WRAP); //$NON-NLS-1$
									l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
									((GridData) l.getLayoutData()).widthHint = 100;
									attributeLabels.add(l);

									l = toolkit.createLabel(attributeComp, SmartUtils.formatStringForLabel(d.get(i).attributeValues.get(k)), SWT.WRAP);
									l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
									((GridData) l.getLayoutData()).widthHint = 100;
									attributeValuesLabels.add(l);
								}

								//Waypoint Composite
								if (d.get(i).attchmentFileNames.size() > 0){
									ThumbnailComposite tc = new ThumbnailComposite(attributeComp);
									tc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
									toolkit.adapt(tc);
									tc.setFiles(d.get(i).attchmentFileNames);
									obsThumbs.add(tc);
								}
								
								
								if (i < d.size() - 1) {
									Label l = toolkit.createLabel(attributeComp, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
									l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
								}
							}

							Label l2 = toolkit.createLabel(infoSection.getBody(),"", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
							l2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
						}
					}
					
			
					if (lcurrentWp != null && lcurrentWp.getAttachments() != null && lcurrentWp.getAttachments().size() > 1){
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
			for (ThumbnailComposite com : obsThumbs){
				com.initThumbs();
			}
			
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			if (thumbnails.size() > 0 || obsThumbs.size() > 0){
				//update thumbnails
				infoSection.getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						if (lblWaypointId.isDisposed()) return ;
					
						//display of loading label
						if (compThumbnails != null) compThumbnails.dispose();
					
						for (ThumbnailComposite c : obsThumbs){
							c.createThumbs();
						}
						compThumbnails = toolkit.createComposite(infoSection.getBody(), SWT.NONE);
						GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
						compThumbnails.setLayoutData(gd);
				
						compThumbnails.setLayout(new GridLayout());
						for (Thumbnail nail : thumbnails){
							Composite parent = toolkit.createComposite(compThumbnails);
							nail.createThumbnail(parent);
						}
				
						Listener resize = new Listener(){
							@Override
							public void handleEvent(Event event) {
								
								int mainWidth = infoSection.getClientArea().width - infoSection.getVerticalBar().getSize().x;
								
								for (Label l : categoryLabels){
									((GridData)l.getLayoutData()).widthHint = mainWidth;
								}
								for (Label l : attributeLabels){
									int x = l.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
									if (x > 0.5 * mainWidth){
										x = (int)(0.5 * mainWidth);
									}
									((GridData)l.getLayoutData()).widthHint = x;
								
								}
								
								for (Label l : attributeValuesLabels){
									int x = l.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
									if (x > 0.5 * mainWidth){
										x = (int)(0.5 * mainWidth);
									}
									((GridData)l.getLayoutData()).widthHint = x;
								}
	
								
								infoSection.getBody().layout(true);
								
								int width = compThumbnails.getSize().x - infoSection.getVerticalBar().getSize().x;
								int cols = (int)Math.floor(width / 100.0);
								compThumbnails.setLayout(new GridLayout(cols, false));
								for (ThumbnailComposite c : obsThumbs){
									c.updateLayout(cols);
									((GridData)c.getLayoutData()).widthHint = mainWidth;
									c.layout(true);
								}
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
		WaypointEventManager.getInstance().addListener(EventType.WAYPOINT_DELETED, waypointListener);
		WaypointEventManager.getInstance().addListener(EventType.WAYPOINT_MODIFIED, waypointListener);
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@PreDestroy
	public void dispose(){
		toolkit.dispose();
		WaypointEventManager.getInstance().removeListener(EventType.WAYPOINT_DELETED, waypointListener);
		WaypointEventManager.getInstance().removeListener(EventType.WAYPOINT_MODIFIED, waypointListener);
				
		if (boldFont != null && !boldFont.isDisposed()){
			boldFont.dispose();
			boldFont = null;
		}
	}
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@PostConstruct
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
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
		boldFont = new Font(parent.getDisplay(), fd);
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
		if (selectedWaypointUuid != null && 
				selectedWaypointUuid.equals(wp.getUuid())){
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
		if (infoSection == null) return;
		for (Control c : infoSection.getBody().getChildren()) {
			c.dispose();
		}

		lblWaypointId.setText(""); //$NON-NLS-1$
		lblDateTime.setText(""); //$NON-NLS-1$
	}
	
	@Focus
	public void setFocus() {
		lblWaypointId.setFocus();
	}

	/**
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Inject
	public void selectionChanged(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection){
		if (selection == null || selection.isEmpty()) {
			return;
		}
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		
		Object o = selection.getFirstElement();
		
		Waypoint wp = null;
		if (o instanceof Waypoint) {
			wp = (Waypoint)o;
		}else if (o instanceof WaypointObservation){
			wp = ((WaypointObservation)o).getWaypoint();
		}else if (o instanceof IAdaptable){
			wp = (Waypoint) ((IAdaptable)o).getAdapter(Waypoint.class);
		}
		
		if (wp == null){
			//try the AdapterManager
			wp = (Waypoint) Platform.getAdapterManager().getAdapter(o, Waypoint.class);
			
			if (wp == null){
				if (o instanceof AdaptableFeature){
					Object delegateFeature = ((AdaptableFeature) o).getObject();
					wp = (Waypoint) Platform.getAdapterManager().getAdapter(delegateFeature, Waypoint.class);
				}
			}
		}
		if (wp != null){
			updateContents(wp);
		}else{
			clearContents();
		}
	}
	
	/*
	 * A class for tracking display data
	 */
	private class DisplayData {
		String categoryLabel;
		List<String> attributeLabels;
		List<String> attributeValues;
		List<ISmartAttachment> attchmentFileNames;
		
		public DisplayData(){
			attributeLabels = new ArrayList<String>();
			attributeValues = new ArrayList<String>();
			attchmentFileNames = new ArrayList<ISmartAttachment>();
		}
	}
	
	/*
	 * A composite for thumbnails
	 */
	private class ThumbnailComposite extends Composite{
		private List<ISmartAttachment> fileNames;
		private List<Thumbnail> thumbs;
		public ThumbnailComposite(Composite parent){
			super(parent, SWT.NONE);
			GridLayout gl = new GridLayout();
			gl.marginWidth = gl.marginHeight = 0;
			setLayout(gl);
		}
		
		public void updateLayout(int numCols){
			GridLayout gl = new GridLayout(numCols, false);
			gl.marginWidth = gl.marginHeight = 0;
			setLayout(gl);
		}
		public void setFiles(List<ISmartAttachment> fileNames){
			this.fileNames = fileNames;
		}
		
		public void initThumbs(){
			if (fileNames == null) return;
			thumbs = new ArrayList<Thumbnail>(fileNames.size());
			for (int i = 0; i < fileNames.size(); i ++){
				thumbs.add(new Thumbnail(fileNames.get(i)));
			}
		}
		
		public void createThumbs(){
			if (thumbs == null) return;
			for (Thumbnail t : thumbs){
				Composite parent = toolkit.createComposite(this);
				t.createThumbnail(parent);
			}
		}
	}
	
	public static class WaypointInfoViewWrapper extends DIViewPart<WaypointInfoView>{
		public WaypointInfoViewWrapper(){
			super(WaypointInfoView.class);
		}
	}
}
