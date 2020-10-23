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

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.eclipse.swt.widgets.Display;
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
import org.wcs.smart.common.control.SmartUiUtils;
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
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.util.SmartUtils;

/**
 * View for displaying all observation information at a
 * given waypoint.
 * 
 * @author egouge
 *
 */
@SuppressWarnings("restriction")
public class WaypointInfoView {

	public static final String ID = "org.wcs.smart.observation.waypointInfo"; //$NON-NLS-1$
	private FormToolkit toolkit = null;

	private Label lblWaypointId;
	private Label lblDateTime;
	
	private Font boldFont = null;
	private ScrolledForm infoSection = null;

	private UUID selectedWaypointUuid;
	
	private boolean showImages = true;
	private boolean showText = true;
	
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
			if (infoSection == null || infoSection.isDisposed()){
				selectedWaypointUuid = null;
				return Status.OK_STATUS;
			}
			if (selectedWaypointUuid == null){
				Display.getDefault().syncExec(()->clearContents());
				return Status.OK_STATUS;
			}
			final List<Thumbnail> thumbnails = new ArrayList<Thumbnail>();

			Waypoint currentWp = null;
			
			//load waypoint information
			try(final Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try{
					currentWp = (Waypoint) s.get(Waypoint.class, selectedWaypointUuid);	//reload waypoint to get latest info
					if (currentWp != null) {
						if (currentWp.getLastModifiedBy() != null) currentWp.getLastModifiedBy().getGender();
						if (currentWp.getObservationGroups() != null) {
							
							for (WaypointObservationGroup g : currentWp.getObservationGroups()) {
								for (WaypointObservation wo : g.getObservations()) {
									wo.getCategory().getFullCategoryName();
									
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
										woa.getAttribute().getName();
										woa.getAttributeValueAsString(Locale.getDefault());
									}
									for (ObservationAttachment att: wo.getAttachments()){
										try{
											att.computeFileLocation(s);
										}catch (Exception ex){
											ObservationPlugIn.log(ex.getMessage(), ex);
										}
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
				}
			}
			
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;


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
						lblWaypointId.setText(lcurrentWp.getId());
						lblDateTime.setText( DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(lcurrentWp.getDateTime())); 

						int widthHint = infoSection.getBody().getBounds().width - 20;		
						
						for (WaypointObservationGroup g : lcurrentWp.getObservationGroups()) {
							
							Composite spacer = toolkit.createComposite(infoSection.getBody(), SWT.NONE );
							spacer.setLayout(new GridLayout());
							((GridLayout)spacer.getLayout()).marginWidth = 0;
							((GridLayout)spacer.getLayout()).marginHeight = 0;
							
							if (showText && lcurrentWp.getObservationGroups().size() > 1) {
								SmartUiUtils.createHeaderLabel(spacer, Messages.WaypointInfoView_ObsGroupHeader);
							}
							
							for (WaypointObservation wo : g.getObservations()) {

								if (showText) {
									Label lbl = toolkit.createLabel(spacer, SmartUtils.formatStringForLabel(wo.getCategory().getFullCategoryName()), SWT.WRAP);
									lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
									((GridData) lbl.getLayoutData()).widthHint = widthHint;
									lbl.setFont(boldFont);
									categoryLabels.add(lbl);
								}
		
								Composite attributeComp = toolkit.createComposite(spacer);
								attributeComp.setLayout(new GridLayout(2, false));
								((GridLayout) attributeComp.getLayout()).marginLeft = 5;
		
								attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
								if (showText) {
									for (WaypointObservationAttribute woa : wo.getAttributes()) {
										Label l = toolkit.createLabel(attributeComp,SmartUtils.formatStringForLabel(woa.getAttribute().getName() + ":"), SWT.WRAP); //$NON-NLS-1$
										l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
										((GridData) l.getLayoutData()).widthHint = 100;
										attributeLabels.add(l);
			
										l = toolkit.createLabel(attributeComp, SmartUtils.formatStringForLabel(woa.getAttributeValueAsString(Locale.getDefault())), SWT.WRAP);
										l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
										((GridData) l.getLayoutData()).widthHint = 100;
										attributeValuesLabels.add(l);
									}
								}
								if (showImages) {
									//Waypoint Composite
									List<ISmartAttachment> files = new ArrayList<>();
									for (ObservationAttachment att : wo.getAttachments()) files.add(att);
									if (!files.isEmpty()) {
										ThumbnailComposite tc = new ThumbnailComposite(attributeComp);
										tc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
										toolkit.adapt(tc);
										tc.setFiles(files);
										obsThumbs.add(tc);
									}
								}
								if (attributeComp.getChildren().length == 0) attributeComp.dispose();
										
								if (showText) {
									Label l = toolkit.createLabel(spacer, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
									l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
								}
							}

						}
					}
					
			
					compThumbnails = toolkit.createComposite(infoSection.getBody());
					compThumbnails.setLayout(new GridLayout());
					compThumbnails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					if (showImages && lcurrentWp != null && lcurrentWp.getAttachments() != null && lcurrentWp.getAttachments().size() > 1){
						toolkit.createLabel(compThumbnails, Messages.WaypointInfoView_LoadingThumbnails); 
					}

					createHiddenLabels();
					
					infoSection.getBody().pack();
					infoSection.getBody().layout();
					infoSection.reflow(true);
					
					createLastModifiedLabel(infoSection.getBody(), lcurrentWp);
					infoSection.getBody().layout();
					
					lblWaypointId.getParent().layout();
					
					
				}
			});
			
			
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			
			
				
			if (!showImages) return Status.OK_STATUS;
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
			
				//update thumbnails
				infoSection.getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						if (lblWaypointId.isDisposed()) return ;
					
						for (ThumbnailComposite c : obsThumbs){
							c.createThumbs();
						}
						for (Control c : compThumbnails.getChildren()) c.dispose();
						
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
								
								int width = compThumbnails.getSize().x - infoSection.getVerticalBar().getSize().x-5;
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
			
			return Status.OK_STATUS;
		}
		
		private void createHiddenLabels() {
			if (!showText) createLabel(Messages.WaypointInfoView_DetailsHidden);
			if (!showImages) createLabel(Messages.WaypointInfoView_AttachmentsHidden);
		}
		
		private void createLabel(String msg) {
			Label l = toolkit.createLabel(infoSection.getBody(), msg);
			FontData fd = l.getFont().getFontData()[0];
			fd.setStyle(SWT.ITALIC);
			Font f = new Font(l.getDisplay(), fd);
			l.setFont(f);
			l.addListener(SWT.Dispose, e->f.dispose());
			l.setForeground(l.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		}
		
		private void createLastModifiedLabel(Composite parent, Waypoint wp) {
			String data = null;
			int width = infoSection.getBounds().width-30;
			if (wp == null) return;
			if (wp.getLastModifiedBy() != null) {
				data = MessageFormat.format(Messages.WaypointInfoView_LastUpdated1, 
						SmartLabelProvider.getShortLabel(wp.getLastModifiedBy()),  DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(wp.getLastModified()));
			}else {
				data = MessageFormat.format(Messages.WaypointInfoView_LastUpdated2,  DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(wp.getLastModified()));
			}
			Label l = toolkit.createLabel(parent, data, SWT.WRAP);
			l.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
			((GridData)l.getLayoutData()).widthHint = width;
			FontData fd = l.getFont().getFontData()[0];
			fd.setStyle(SWT.ITALIC);
			Font ff = new Font(parent.getDisplay(), fd);
			
			l.setFont(ff);
			l.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			
			Listener resizeListener = e->{
				((GridData)l.getLayoutData()).widthHint = infoSection.getBounds().width-30;	
				infoSection.reflow(true);
				infoSection.getBody().layout();
			};
			

			l.addListener(SWT.Dispose, e->{
				ff.dispose();
				infoSection.removeListener(SWT.Resize,  resizeListener);
			});
			
			infoSection.addListener(SWT.Resize, resizeListener);
			
		}
		
	};
	
	
	/**
	 * Creates new view
	 */
	public WaypointInfoView() {
		WaypointEventManager.getInstance().addListener(EventType.WAYPOINT_DELETED, waypointListener);
		WaypointEventManager.getInstance().addListener(EventType.WAYPOINT_MODIFIED, waypointListener);
	}

	public void refresh() {
		forceRefresh();
	}
	
	public void setImagesVisible(boolean isVisible) {
		this.showImages = isVisible;
		forceRefresh();
	}
	
	public void setTextVisible(boolean isVisible) {
		this.showText = isVisible;
		forceRefresh();
	}
	
	private void forceRefresh() {
		updateUiJob.cancel();
		updateUiJob.schedule();
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
			//clearContents();
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
