package org.wcs.smart.asset.ui.views.asset;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.ui.input.ObservationWizard;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.ui.properties.DialogConstants;


public class AssetDataPage {

	private AssetEditor parentEditor;
	
	private FormToolkit toolkit;
	private Composite mainControl;
	private Composite headerComp;
	private DateFilterDropDownComposite dateFilter;
	
	private ScrolledComposite scrollComp;
	private Composite dataComposite;
	
	
	int startIndex = 0;
	int pageSize = 100;
	private List<AssetWaypoint.AssetWaypointPk> dataWaypoints = null;
	
	public AssetDataPage(AssetEditor parent) {
		this.parentEditor = parent;
	}
	
	public Composite createDataSection(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		
		mainControl = toolkit.createComposite(parent, SWT.NONE);
		mainControl.setLayout(new GridLayout());
		mainControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)mainControl.getLayout()).marginWidth = 0;
		((GridLayout)mainControl.getLayout()).marginHeight = 0;
		
		
		Composite filterSection = toolkit.createComposite(mainControl, SWT.NONE);
		filterSection.setLayout(new GridLayout(3, false));
		((GridLayout)filterSection.getLayout()).marginWidth = 0;
		filterSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.createLabel(filterSection, "Date Filter:");
		
		 DateFilterComposite.DateFilter[] filters = new  DateFilterComposite.DateFilter[] {
				 DateFilterComposite.DateFilter.LAST_30_DAYS,
				 DateFilterComposite.DateFilter.LAST_60_DAYS,
				 DateFilterComposite.DateFilter.LAST_1_YEARS,
				 DateFilterComposite.DateFilter.LAST_5_YEARS,
				 DateFilterComposite.DateFilter.ALL
				 
		 };
		dateFilter = new DateFilterDropDownComposite(filterSection, filters, DateFilterComposite.DateFilter.LAST_1_YEARS);
		dateFilter.addChangeListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				reloadData();
			}
		});
		
		ToolBar tb = new ToolBar(filterSection, SWT.FLAT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		ToolItem itemSettings = new ToolItem(tb, SWT.PUSH);
		itemSettings.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.INFO_ICON));
		itemSettings.setToolTipText("Configure table settings");
		itemSettings.addListener(SWT.Selection, e->{
			Shell shell = new Shell(tb.getShell(), SWT.NO_TRIM | SWT.ON_TOP | SWT.BORDER );
			shell.setLayout(new GridLayout());
			((GridLayout)shell.getLayout()).marginWidth = 0;
			((GridLayout)shell.getLayout()).marginHeight = 0;
			
			Composite c = new Composite(shell, SWT.BORDER);
			c.setLayout(new GridLayout());
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			c.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			((GridLayout)c.getLayout()).marginHeight = 1;
			((GridLayout)c.getLayout()).marginWidth = 1;
			((GridLayout)c.getLayout()).verticalSpacing = 1;
			
			Label ll = new Label(c, SWT.NONE);
			ll.setText("Icon Size");
			ll.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			
			Button opSmall = new Button(c, SWT.RADIO);
			opSmall.setText("Small");
			opSmall.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			Button opMedium = new Button(c, SWT.RADIO);
			opMedium.setText("Medium");
			opMedium.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			Button opLarge = new Button(c, SWT.RADIO);
			opLarge.setText("Large");
			opLarge.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			c = new Composite(shell, SWT.BORDER);
			c.setLayout(new GridLayout());
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			c.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			((GridLayout)c.getLayout()).marginHeight = 1;
			((GridLayout)c.getLayout()).marginWidth = 1;
			((GridLayout)c.getLayout()).verticalSpacing = 1;
			
			ll = new Label(c, SWT.NONE);
			ll.setText("Observation Details");
			ll.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			
			Button opHide = new Button(c, SWT.RADIO);
			opHide.setText("Show");
			Button opShow = new Button(c, SWT.RADIO);
			opShow.setText("Hide");
			opShow.setEnabled(true);
			
			opShow.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			opHide.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			
			shell.pack();
			shell.setSize(150,  150);
			shell.layout(true);
			//System.out.println(itemSettings.getControl().getSize().y);
//			itemSettings.getControl().getParent().getParent().layout(true);
			Point p2 = tb.toDisplay(tb.getLocation());
			shell.setLocation(p2.x-150, p2.y + tb.getSize().y);
			shell.open();
			shell.addListener(SWT.Deactivate, evt->{shell.dispose();});
		});
		
		headerComp = new Composite(mainControl, SWT.NONE);
		headerComp.setLayout(new GridLayout());
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerComp.getLayout()).marginWidth = 0;
		((GridLayout)headerComp.getLayout()).marginHeight = 0;
		
		scrollComp = new ScrolledComposite(mainControl, SWT.V_SCROLL | SWT.H_SCROLL);
		scrollComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrollComp.setLayout(new GridLayout());
		scrollComp.setExpandHorizontal(true);
		scrollComp.setExpandVertical(true);
		
		dataComposite = toolkit.createComposite(scrollComp, SWT.NONE);
		dataComposite.setLayout(new GridLayout());
		dataComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)dataComposite.getLayout()).marginWidth = 0;
		((GridLayout)dataComposite.getLayout()).marginHeight = 0;
		
		scrollComp.setContent(dataComposite);
		scrollComp.addListener(SWT.Resize, e->scrollComp.setMinSize(dataComposite.computeSize(scrollComp.getBounds().width-30, SWT.DEFAULT)));
		reloadData();
		return mainControl;
	}
	
	
	public void initializePanel() {
		reloadData();
	}
	
	
	private void  createNavigationControl(Composite parent) {
		if (dataWaypoints == null || dataWaypoints.size() == 0) return;
		int from = startIndex;
		int to = Math.min(startIndex + pageSize,  dataWaypoints.size());
		
		Composite part = toolkit.createComposite(parent);
		part.setLayout(new GridLayout(4, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		
		toolkit.createLabel(part, MessageFormat.format("Displaying {0} to {1} of {2}", from+1, to, dataWaypoints.size()));
		
		if (from == 0 && to == dataWaypoints.size()) return;
			
		Hyperlink prev = toolkit.createHyperlink(part, "<", SWT.NONE);
		if(from == 0) prev.setEnabled(false);
		prev.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				startIndex = startIndex - pageSize;
				if (startIndex < 0) startIndex = 0;
				updateUiJob.schedule();
			}
		});
		Hyperlink more = toolkit.createHyperlink(part, "...",  SWT.NONE);
		more.addListener(SWT.MouseDown, e->{
			Shell shell = new Shell(more.getShell(), SWT.NO_TRIM | SWT.ON_TOP );
			shell.setLayout(new GridLayout());
			((GridLayout)shell.getLayout()).marginWidth = 0;
			((GridLayout)shell.getLayout()).marginHeight = 0;
			
			Composite c = new Composite(shell, SWT.BORDER);
			c.setLayout(new GridLayout());
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			c.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			((GridLayout)c.getLayout()).marginHeight = 1;
			((GridLayout)c.getLayout()).marginWidth = 1;
			((GridLayout)c.getLayout()).verticalSpacing = 1;
			
			Label l = new Label(c, SWT.NONE);
			l.setText("First");
			l.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			l.addListener(SWT.MouseEnter, evt->l.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
			l.addListener(SWT.MouseExit, evt->l.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
			l.addListener(SWT.MouseUp, evt->{startIndex = 0; updateUiJob.schedule();shell.close();shell.dispose();});
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Label l2 = new Label(c, SWT.NONE);
			l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			l2.setText("Last");
			l2.addListener(SWT.MouseEnter, evt->l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
			l2.addListener(SWT.MouseExit, evt->l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
			l2.addListener(SWT.MouseUp, evt->{
				startIndex = (int)((dataWaypoints.size() / pageSize) * pageSize); 
				updateUiJob.schedule();
				shell.close();
				shell.dispose();
			});
			l2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			ScrolledComposite src = new ScrolledComposite(c,  SWT.V_SCROLL | SWT.READ_ONLY);
			src.setExpandHorizontal(true);
			src.setExpandVertical(true);
			src.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			src.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

			Composite core = new Composite(src, SWT.NONE);
			core.setLayout(new GridLayout());
			core.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			((GridLayout)core.getLayout()).marginWidth = 0;
			((GridLayout)core.getLayout()).marginHeight = 0;
			((GridLayout)core.getLayout()).verticalSpacing = 1;
			src.setContent(core);
			for (int i = 0; i < dataWaypoints.size(); i += pageSize) {
				final int ii = i;
				Label l3 = new Label(core, SWT.NONE);
				l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
				l3.setText(MessageFormat.format("{0}-{1}", (i+1), Math.min(i+pageSize, dataWaypoints.size()))); //$NON-NLS-1$
				l3.addListener(SWT.MouseEnter, evt->l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
				l3.addListener(SWT.MouseExit, evt->l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
				l3.addListener(SWT.MouseUp, evt->{startIndex = ii; updateUiJob.schedule();shell.close();shell.dispose();});
				l3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
			src.setMinSize(core.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			shell.pack();
			shell.setSize(150,  150);
			shell.layout(true);
			more.getParent().getParent().layout(true);
			Point p2 = more.toDisplay(more.getLocation());
			shell.setLocation(p2.x-150, p2.y + more.getSize().y);
			shell.open();
			shell.addListener(SWT.Deactivate, evt->{shell.dispose();});
			
		});
		
		Hyperlink next = toolkit.createHyperlink(part, ">", SWT.NONE);
		if (to == dataWaypoints.size()) next.setEnabled(false);
		next.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				startIndex = startIndex + pageSize;
				if (startIndex > dataWaypoints.size()) startIndex = dataWaypoints.size();
				updateUiJob.schedule();
			}
		});
	}
	
	
	public void reloadData() {
		final Date startDate = dateFilter.getDateFilter().getStartDate();
		final Date endDate = dateFilter.getDateFilter().getEndDate();
		
		for (Control c : dataComposite.getChildren()) c.dispose();
		toolkit.createLabel(dataComposite, DialogConstants.LOADING_TEXT);
		dataComposite.layout(true);
		
		Job loadData = new Job("load data") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				dataWaypoints = new ArrayList<>();
				startIndex = 0;
				try(Session session = HibernateManager.openSession()){
					Query q = session.createQuery(" FROM AssetWaypoint WHERE id.waypoint.dateTime >= :startDate and id.waypoint.dateTime <= :endDate");
					q.setParameter("startDate",  startDate);
					q.setParameter("endDate", endDate);
					for (Object x : q.list()) {
						dataWaypoints.add(((AssetWaypoint)x).getId());
					}
				}
				updateUiJob.schedule();
				return Status.OK_STATUS;
			}
			
		};
		loadData.schedule();
	}

	private void createDataPanel(List<AssetWaypoint> waypoints) {
		for (Control c : dataComposite.getChildren()) c.dispose();
		
//		List<Date> allDates = new ArrayList<>();
//		allDates.addAll(waypoints.keySet());
//		allDates.sort((a,b)->-1*a.compareTo(b));
		
		
		
//		for (Date d : allDates) {
//			Composite dayComp = toolkit.createComposite(dataComposite);
//			dayComp.setLayout(new GridLayout());
//			dayComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//			((GridLayout)dayComp.getLayout()).marginWidth = 0;
//			((GridLayout)dayComp.getLayout()).marginHeight = 0;
//		
//			Color dayHeaderColor = new Color(dayComp.getDisplay(), 200, 200, 200);
//			dayComp.addListener(SWT.Dispose, e->dayHeaderColor.dispose());
//			
//			Composite header = toolkit.createComposite(dayComp);
//			header.setLayout(new GridLayout());
//			header.setBackground(dayHeaderColor);
//			header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//
//			Label l = toolkit.createLabel(header, DateFormat.getDateInstance().format(d));
//			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//			l.setBackground(dayHeaderColor);
//			
			for(AssetWaypoint aw : waypoints) {
				Composite outer = toolkit.createComposite(dataComposite, SWT.BORDER);
				outer.setLayout(new GridLayout());
				outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//				StringBuilder sb = new StringBuilder();
//				sb.append(DateFormat.getDateTimeInstance().format(aw.getWaypoint().getDateTime()));
//				sb.append(" (");
//				sb.append(aw.getAssetDeployment().getStationLocation().getStation().getId());
//				sb.append(" ");
//				sb.append(aw.getAssetDeployment().getStationLocation().getId());
//				sb.append(" ");
//				sb.append(aw.getWaypoint().getId());
//				sb.append(") ");
				Composite top = toolkit.createComposite(outer, SWT.NONE);
				top.setLayout(new GridLayout(3, true));
				top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				Color dayHeaderColor = new Color(top.getDisplay(), 200, 200, 200);
				top.addListener(SWT.Dispose, e->dayHeaderColor.dispose());
				top.setBackground(dayHeaderColor);
				
				Label l = toolkit.createLabel(top, MessageFormat.format("{0}", DateFormat.getDateTimeInstance().format(aw.getWaypoint().getDateTime())));
				l.setBackground(dayHeaderColor);
				l = toolkit.createLabel(top, MessageFormat.format("Location: {0}", aw.getAssetDeployment().getStationLocation().getId()));
				l.setBackground(dayHeaderColor);
				l = toolkit.createLabel(top, MessageFormat.format("Station: {0}", aw.getAssetDeployment().getStationLocation().getStation().getId()));
				l.setBackground(dayHeaderColor);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				Composite info = toolkit.createComposite(outer);
				info.setLayout(new GridLayout());
				info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				
				Composite obsComp = toolkit.createComposite(info);
				obsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				obsComp.setLayout(new GridLayout());
				
				for (WaypointObservation wo : aw.getWaypoint().getObservations()) {
					l = toolkit.createLabel(obsComp, wo.getCategory().getFullCategoryName());
					
					for (WaypointObservationAttribute woa : wo.getAttributes()) {
						l = toolkit.createLabel(obsComp, woa.getAttribute().getName() + ": " + woa.getAttributeValueAsString(Locale.getDefault()));
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						((GridData)l.getLayoutData()).horizontalIndent = 20;
					}	
				}
				
				Composite thumbnailComp = toolkit.createComposite(info);
				thumbnailComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				thumbnailComp.setLayout(new RowLayout());
				((RowLayout)thumbnailComp.getLayout()).wrap = true;
				
				
				if (aw.getWaypoint().getAttachments() != null) {
					for (WaypointAttachment wa : aw.getWaypoint().getAttachments()) {
						Thumbnail t = new Thumbnail(wa, 100, true);
						Composite c = t.createThumbnail(thumbnailComp);
						c.setSize(100, 100);
						c.setLayoutData(new RowData(100, 100));
					}
				}
				if (aw.getWaypoint().getObservations() != null) {
					for (WaypointObservation wo : aw.getWaypoint().getObservations()) {
						if (wo.getAttachments() != null) {
							for (ObservationAttachment a : wo.getAttachments()) {
								Thumbnail t = new Thumbnail(a, 100, true);
								Composite c = t.createThumbnail(thumbnailComp);
								c.setSize(100, 100);
								c.setLayoutData(new RowData(100, 100));
							}
						}
					}
				}
				//TODO: observation attachments
				
				Hyperlink link = toolkit.createHyperlink(outer, "Edit...", SWT.NONE);
				link.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						ObservationWizard wizard = new ObservationWizard(aw.getWaypoint(),null);
						WizardDialog dialog = new WizardDialog(dataComposite.getShell(), wizard);
						dialog.open();
					}
				});
			}
//		}
		createNavigationControl(dataComposite);
		dataComposite.layout(true);
		scrollComp.setMinSize(dataComposite.computeSize(scrollComp.getBounds().width-30, SWT.DEFAULT));
	}

			
	Job updateUiJob = new Job("update ui") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(()->{
				for (Control c : headerComp.getChildren()) c.dispose();
				createNavigationControl(headerComp);
				headerComp.getParent().layout(true);
				headerComp.layout(true);
				
				for (Control c : dataComposite.getChildren()) c.dispose();
				toolkit.createLabel(dataComposite, DialogConstants.LOADING_TEXT);
				dataComposite.layout(true);
			});
			
			if (dataWaypoints == null)  return Status.OK_STATUS;
			int to = Math.min(startIndex + pageSize , dataWaypoints.size());
			List<AssetWaypoint> waypoints = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				for (int i = startIndex; i < to; i ++) {
					AssetWaypoint aw = (AssetWaypoint)QueryFactory.buildQuery(session, AssetWaypoint.class, 
							new Object[] {"id.waypoint", dataWaypoints.get(i).getWaypoint()},
							new Object[] {"id.assetDeployment", dataWaypoints.get(i).getAssetDeployment()})
					.uniqueResult();
					if (aw == null) continue;
					waypoints.add(aw);
					aw.getAssetDeployment().getStationLocation().getStation().getId();
				}
				
				waypoints.forEach(w->{
					w.getAssetDeployment().getStationLocation().getStation().getId();
					w.getAssetDeployment().getStationLocation().getId();
					if (w.getWaypoint().getObservations() != null) {
						w.getWaypoint().getObservations().forEach(e->{
							e.getCategory().getFullCategoryName();
							if (e.getAttributes() != null) {
								e.getAttributes().forEach(wa->{
									wa.getAttributeValueAsString(Locale.getDefault());
									wa.getAttribute().getName();
								});
							}
							
						});
						for (WaypointObservation o : w.getWaypoint().getObservations()) {
							if (o.getAttachments() != null) {
								for (ObservationAttachment a : o.getAttachments()) {
									try {
										a.computeFileLocation(session);
									}catch (Exception ex) {
										ex.printStackTrace();
										//TODO:
									}
								}
							}
						}
					}
					if (w.getWaypoint().getAttachments() != null) {
						for (WaypointAttachment a : w.getWaypoint().getAttachments()) {
							try {
								a.computeFileLocation(session);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					
				});
				
			}
			waypoints.sort((a,b)->a.getWaypoint().getDateTime().compareTo(b.getWaypoint().getDateTime()));
			
			Display.getDefault().syncExec(()->{
				createDataPanel(waypoints);
			});
			return Status.OK_STATUS;
		}
		
	};
}
