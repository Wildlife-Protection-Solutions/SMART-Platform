/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.data;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypoint.State;
import org.wcs.smart.asset.model.AssetWaypointAttachment;
import org.wcs.smart.asset.ui.AttachmentTable;
import org.wcs.smart.asset.ui.DataDisplaySettings;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.ui.AttachmentPropertiesDialog;
import org.wcs.smart.ui.AttachmentPropertiesDialog.Entry;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Page for reviewing results
 * 
 * @author Emily
 *
 */
public class DataReviewPage {

	private DataImporterView view;
	private FormToolkit toolkit;
	
	private List<UUID> waypointsToReview;
	
	private int startIndex = 0;
	private int pageSize = 25;
	
	private ScrolledComposite scroll;
	private Composite details;
	
	private List<RowItem> rows;
	
	private DataDisplaySettings.IconSize iconSize = DataDisplaySettings.IconSize.SMALL;
	
	Color headerColor = null;
	Color selectionColor = null;
	Color mouseOverColor = null;
	
	public DataReviewPage(DataImporterView view, FormToolkit toolkit) {
		this.view = view;
		this.toolkit = toolkit;
	}
	
	public Control createControl(Composite parent) {
		headerColor = new Color(parent.getDisplay(), 230, 230, 230);
		selectionColor = SmartUtils.getListSelectedColor(parent.getDisplay());
		mouseOverColor = SmartUtils.getListHighlightColor(parent.getDisplay());
		
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		main.addListener(SWT.Dispose, e->{
			selectionColor.dispose();
			mouseOverColor.dispose();
			headerColor.dispose();
		});
		
		
		scroll = new ScrolledComposite(main, SWT.V_SCROLL | SWT.BORDER);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(scroll);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		details = toolkit.createComposite(scroll, SWT.NONE);
		scroll.setContent(details);
		
		details.setLayout(new GridLayout());
		
		resizeScroll();
		scroll.addListener(SWT.Resize, e->{
			resizeScroll();
		});
		
		if (waypointsToReview == null) {
			loadWaypointsJob.schedule();
		}else {
			loadPage.schedule();
		}
		return main;
	}
	
	void resizeScroll() {
		int width = scroll.getBounds().width;
		if (rows != null) rows.forEach(a->a.resize(width, iconSize));
		scroll.setMinSize(details.computeSize(scroll.getBounds().width - scroll.getVerticalBar().getSize().x, SWT.DEFAULT));
	}
	
	private void createMenu(Control control) {
		if (control.getMenu() != null && !control.getMenu().isDisposed()) return;
		Menu mnuHeader = new Menu(details);
		
		MenuItem mnuOk = new MenuItem(mnuHeader, SWT.PUSH);
		mnuOk.setText("Validate Selection");
		mnuOk.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_VALIDATE));
		mnuOk.addListener(SWT.Selection, e->{
			List<WaypointMapping> toClear = rows.stream().filter(i->i.isSelected).map(i->i.waypoint).collect(Collectors.toList());
			markValidated(toClear);
		});
		
		new MenuItem(mnuHeader, SWT.SEPARATOR);

		MenuItem mnuOkAll = new MenuItem(mnuHeader, SWT.PUSH);
		mnuOkAll.setText("Validate All");
		mnuOkAll.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_VALIDATE_ALL));
		mnuOkAll.addListener(SWT.Selection, e->{
			List<WaypointMapping> toClear = rows.stream().map(i->i.waypoint).collect(Collectors.toList());
			markValidated(toClear);
			loadWaypointsJob.schedule();
		});
		
		new MenuItem(mnuHeader, SWT.SEPARATOR);
		
		
		MenuItem mnuMerge = new MenuItem(mnuHeader, SWT.PUSH);
		mnuMerge.setText("Merge Incidents ");
		mnuMerge.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_MERGE));
		mnuMerge.addListener(SWT.Selection, e->{
			List<WaypointMapping> toMerge = rows.stream().filter(i->i.isSelected).map(i->i.waypoint).collect(Collectors.toList());
			mergeItems(toMerge);
			loadWaypointsJob.schedule();
		});
		MenuItem mnuDelete = new MenuItem(mnuHeader, SWT.PUSH);
		mnuDelete.setText("Delete Incidents");
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->{
			List<WaypointMapping> toClear = rows.stream().filter(i->i.isSelected).map(i->i.waypoint).collect(Collectors.toList());
			deleteItems(toClear);
		});
		
		mnuHeader.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				long size = rows.stream().filter(i->i.isSelected).map(i->i.waypoint).collect(Collectors.counting());
				mnuOk.setEnabled(size != 0);
				mnuMerge.setEnabled(size > 1 );
				mnuDelete.setEnabled(size != 0);
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		control.setMenu(mnuHeader);
	}
	
	private void createWidgetPanel(List<WaypointMapping> waypoints) {
		for (Control c : details.getChildren()) c.dispose();
		rows = new ArrayList<>();
		
		if (waypointsToReview.size() == 0) {
			toolkit.createLabel(details, "Nothing to validate.");
			details.layout();
			return;
		}
		
		createPageControl(details, true);
		for (WaypointMapping aw : waypoints) {
			RowItem item = new RowItem(aw);
			item.createControl(details);
			rows.add(item);
		}
		createPageControl(details, false);
		details.layout();
		resizeScroll();
	}
	
	private WaypointMapping newIncident(WaypointMapping aw, List<WaypointAttachment> attachments) {
		if (attachments.size() == 0) return null;
		if (!MessageDialog.openQuestion(view.getSite().getShell(), "Create New Incident", MessageFormat.format("Are you sure you want to move the {0} selected files to a new incident? ", attachments.size()))) return null;
	
		WaypointMapping clonedMapping = null;
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				//clone waypoint
				Waypoint cloneWp = aw.wp.clone(session);
				cloneWp.setAttachments(new ArrayList<>());
				
				//clone attachments and associate deployments
				Map<AssetDeployment, AssetWaypoint> newDeployments = new HashMap<>(); 
				for (WaypointAttachment sp : attachments) {
					WaypointAttachment att = new WaypointAttachment();
					// copy file to temp location so it won't be deleted out from under us
					File tmpLocation = File.createTempFile("smart_" + System.nanoTime(), ""); //$NON-NLS-1$ //$NON-NLS-2$
					tmpLocation.deleteOnExit();
					sp.computeFileLocation(session);
					FileUtils.copyFile(sp.getAttachmentFile(), tmpLocation);
					att.setCopyFromLocation(tmpLocation);
					att.setFilename(sp.getFilename());
					att.setWaypoint(cloneWp);
					cloneWp.getAttachments().add(att);
					
					for (AssetWaypoint oldlink : aw.aw) {
						for (AssetWaypointAttachment attachlink : oldlink.getAttachments()) {
							if (attachlink.getWaypointAttachment().equals(sp)) {
								AssetWaypoint newlink = newDeployments.get(oldlink.getAssetDeployment());
								if (newlink == null) {
									newlink = new AssetWaypoint();
									newlink.setAssetDeployment(oldlink.getAssetDeployment());
									newlink.setState(State.DIRTY);
									newlink.setAttachments(new HashSet<>());
									newlink.setWaypoint(cloneWp);
									newDeployments.put(oldlink.getAssetDeployment(), newlink);
								}
								
								AssetWaypointAttachment newattachlink = new AssetWaypointAttachment();
								newattachlink.setWaypointAttachment(att);
								newattachlink.setAssetWaypoint(newlink);
								newlink.getAttachments().add(newattachlink);
								
								oldlink.getAttachments().remove(attachlink);
								break;
							}
						}
					}
					
				}
				aw.aw.forEach(a->session.saveOrUpdate(a));
				aw.wp.getAttachments().removeAll(attachments);
				for(WaypointAttachment wa : attachments) session.delete(wa);
				
				session.save(cloneWp);
				if (newDeployments.isEmpty()) {
					throw new Exception("New waypoint would not have any links to assets. Every asset related waypoint must link to at least one asset.");
				}
				for (AssetWaypoint newWp : newDeployments.values()) session.save(newWp);
				
				List<AssetWaypoint> toDelete = new ArrayList<>();
				for (AssetWaypoint oldlink : aw.aw) {
					if (oldlink.getAttachments().isEmpty()) {
						session.delete(oldlink);
						toDelete.add(oldlink);
					}
				}
				aw.aw.removeAll(toDelete);
				if (aw.aw.isEmpty()) {
					throw new Exception("Original waypoint would no longer have any links to assets. Every asset related waypoint must link to at least one asset.");
				}

				clonedMapping = new WaypointMapping(cloneWp, newDeployments.values());
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				AssetPlugIn.displayLog("Error saving changes.  You should close and reopen the window before continuing: " + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return null;
			}
		}
		return clonedMapping; 
		
		
	}
	
	
	private void createPageControl(Composite parent, boolean includePageSize) {
		int from = startIndex;
		if (waypointsToReview.isEmpty()) from = -1;
		int to = Math.min(startIndex + pageSize,  waypointsToReview.size());
		
		int cols = 1;
		if (includePageSize) cols ++;
		if (!(from <= 0 && to == waypointsToReview.size())) cols += 3;
				
		Composite part = toolkit.createComposite(parent);
		part.setLayout(new GridLayout(cols, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		
		toolkit.createLabel(part, MessageFormat.format("Displaying {0} to {1} of {2}", from+1, to, waypointsToReview.size()));
		if (!(from <= 0 && to == waypointsToReview.size())) {
			Hyperlink prev = toolkit.createHyperlink(part, "<", SWT.NONE);
			if(from == 0) prev.setEnabled(false);
			prev.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					startIndex = startIndex - pageSize;
					if (startIndex < 0) startIndex = 0;
					loadPage.schedule();
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
				l.addListener(SWT.MouseUp, evt->{startIndex = 0; loadWaypointsJob.schedule();shell.close();shell.dispose();});
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				Label l2 = new Label(c, SWT.NONE);
				l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
				l2.setText("Last");
				l2.addListener(SWT.MouseEnter, evt->l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
				l2.addListener(SWT.MouseExit, evt->l2.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
				l2.addListener(SWT.MouseUp, evt->{
					startIndex = (int)((waypointsToReview.size() / pageSize) * pageSize); 
					loadWaypointsJob.schedule();
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
				for (int i = 0; i < waypointsToReview.size(); i += pageSize) {
					final int ii = i;
					Label l3 = new Label(core, SWT.NONE);
					l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
					l3.setText(MessageFormat.format("{0}-{1}", (i+1), Math.min(i+pageSize, waypointsToReview.size()))); //$NON-NLS-1$
					l3.addListener(SWT.MouseEnter, evt->l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
					l3.addListener(SWT.MouseExit, evt->l3.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND)));
					l3.addListener(SWT.MouseUp, evt->{startIndex = ii; loadWaypointsJob.schedule();shell.close();shell.dispose();});
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
			if (to == waypointsToReview.size()) next.setEnabled(false);
			next.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					startIndex = startIndex + pageSize;
					if (startIndex > waypointsToReview.size()) startIndex = waypointsToReview.size();
					loadWaypointsJob.schedule();
				}
			});
		}
		
		if (includePageSize) {
			DataDisplaySettings.IconSize defaultSize = DataDisplaySettings.IconSize.MEDIUM;
			try {
				String iconSizePref = AssetPlugIn.getDefault().getPreferenceStore().getString(ImagesTablePanel.ICON_SIZE_PREF);
				if (iconSizePref != null) {
					defaultSize = DataDisplaySettings.IconSize.valueOf(iconSizePref);
				}
			}catch (Exception ex) {}
			Composite c = toolkit.createComposite(part);
			c.setLayout(new GridLayout(2, false));
			((GridLayout)c.getLayout()).marginWidth = 0;
			((GridLayout)c.getLayout()).marginHeight = 0;
			c.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			Label l = toolkit.createLabel(c, "Icon Size:", SWT.NONE);
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			Button btnIconSize = new Button(c, SWT.ARROW | SWT.DOWN);
			btnIconSize.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
			Menu mnuIconSize = new Menu(btnIconSize);
			for(DataDisplaySettings.IconSize s : DataDisplaySettings.IconSize.values()) {
				MenuItem item = new MenuItem(mnuIconSize,SWT.RADIO);
				item.setText(s.getOptionName());
				item.addListener(SWT.Selection, e->{
					this.iconSize = s;
					for (RowItem i : rows) {
						i.tt.setThumbnailSize(s.getSize());
					}
					resizeScroll();
				});
				if (s == defaultSize) item.setSelection(true);
			}
			btnIconSize.addListener(SWT.Selection, e->{
				mnuIconSize.setVisible(true);
			});
		}
	}
	
	private Job loadPage = new Job("load waypoint page") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<WaypointMapping> waypoints = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				for(int i = startIndex; i < Math.min(startIndex + pageSize,  waypointsToReview.size()); i ++) {
					
					Waypoint wp = session.get(Waypoint.class, waypointsToReview.get(i));
					if (wp == null) continue;
					
					List<AssetWaypoint> aws = session.createQuery("FROM AssetWaypoint WHERE id.waypoint.uuid = :uuid", AssetWaypoint.class)
							.setParameter("uuid", waypointsToReview.get(i))
							.list();
							
					
					if (wp.getAttachments() != null) {
						wp.getAttachments().forEach(a->{try {
							a.computeFileLocation(session);
						} catch (Exception e) {
							e.printStackTrace();
						}});
					}
					if (wp.getObservations() != null) {
						wp.getObservations().forEach(e->{
							e.getCategory().getFullCategoryName();	
							if (e.getAttachments() != null) e.getAttachments().forEach(a->{try {
								a.computeFileLocation(session);
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}});
							if (e.getAttributes() != null) {
								e.getAttributes().forEach(a->{
									a.getAttribute().getName();
									a.getAttributeValueAsString(Locale.getDefault());
								});
							}
						} );
					}
					aws.forEach(aw->{
						aw.getAssetDeployment().getAsset().getId();
						aw.getAssetDeployment().getAsset().getAssetType();
						aw.getAssetDeployment().getStationLocation().getId();
						aw.getAssetDeployment().getStationLocation().getStation().getId();
						aw.getAttachments().forEach(a->a.getWaypointAttachment());
					});
					waypoints.add(new WaypointMapping(wp, aws));
				}
			}
			Display.getDefault().syncExec(()->{createWidgetPanel(waypoints);});
			return Status.OK_STATUS;
		}
	};
	
	private Job loadWaypointsJob = new Job("load waypoints") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(()->{
				for (Control c : details.getChildren()) c.dispose();
				Label l = toolkit.createLabel(details, DialogConstants.LOADING_TEXT);
				details.layout();
			});
			waypointsToReview = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				List<Object> aw = session.createQuery("SELECT distinct id.waypoint.uuid, id.waypoint.dateTime FROM AssetWaypoint WHERE state = :state AND id.waypoint.conservationArea = :ca ORDER BY id.waypoint.dateTime")
				.setParameter("state", AssetWaypoint.State.DIRTY)
				.setParameter("ca", SmartDB.getCurrentConservationArea())
				.list();
				
				for (Object o : aw) {
					waypointsToReview.add((UUID)((Object[])o)[0]);
				}				
			}
			loadPage.schedule();
			return Status.OK_STATUS;
		}
		
	};
	
	
	
	private void markValidated(Collection<WaypointMapping> tovalidate) {
		try (Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (WaypointMapping aw : tovalidate) {
					aw.aw.forEach(assetWaypoint ->{
						assetWaypoint.setState(AssetWaypoint.State.OK);
						session.saveOrUpdate(assetWaypoint);
					});
				}
				session.getTransaction().commit();
			}catch(Exception ex){
				AssetPlugIn.displayLog("Error saving changes.  You should reload the page before continuing: " + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return;
			}
		}
		List<RowItem> toRemove = new ArrayList<>();
		for (RowItem i : rows) {
			if (tovalidate.contains(i.waypoint)) {
				toRemove.add(i);
				i.item.dispose();
			}
		}
		rows.removeAll(toRemove);
		resizeScroll();
	}
	
	private void deleteItems(Collection<WaypointMapping> todelete) {
		if (todelete.isEmpty()) return;
		if (todelete.size() == 1) {
			if (!MessageDialog.openQuestion(view.getSite().getShell(), "Delete", "Are you sure you want to delete this waypoint?  This action cannot be undone.")) return;
		}else {
			if (!MessageDialog.openQuestion(view.getSite().getShell(), "Delete", MessageFormat.format("Are you sure you want to delete {0} selected waypoints?  This action cannot be undone.", todelete.size()))) return;
		}
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				for (WaypointMapping aw : todelete) {
					//delete waypoint links
					aw.aw.forEach(assetWaypoint->{
						assetWaypoint = session.get(AssetWaypoint.class, assetWaypoint.getUuid());
						assetWaypoint.getAttachments().forEach(a->session.delete(a));
						assetWaypoint.getAttachments().clear();
						session.flush();
						
						session.delete(assetWaypoint);
						session.flush();
						AssetDeployment d = (AssetDeployment)session.get(AssetDeployment.class, assetWaypoint.getAssetDeployment().getUuid());
						//delete deployment if there are no more images in it
						if (d.getAssetWaypoints().size() == 0) {
							d.getAssetWaypoints().clear();
							session.delete(d);
						}
					});
					
					//delete waypoint
					session.delete(session.get(Waypoint.class, aw.wp.getUuid()));
				}
				session.getTransaction().commit();
			}catch(Exception ex){
				AssetPlugIn.displayLog("Error occurred saving changes. You should reload the page before continuing: " +ex.getMessage(), ex);
				session.getTransaction().rollback();
				return;
			}
		}
		
		List<RowItem> toRemove = new ArrayList<>();
		for (RowItem i : rows) {
			if (todelete.contains(i.waypoint)) {
				toRemove.add(i);
				i.item.dispose();
			}
		}
		rows.removeAll(toRemove);
		resizeScroll();
		
		view.getContext().get(IEventBroker.class).post(AssetEvents.ASSETDATA, null);
	}
	
	private void mergeItems(List<WaypointMapping> tomerge) {
		if (tomerge.isEmpty() || tomerge.size() < 2) return;
		
		if (!MessageDialog.openQuestion(view.getSite().getShell(), "Delete", MessageFormat.format("Are you sure you want to merge the {0} selected waypoints into a single incident?  This action cannot be undone.", tomerge.size()))) return;
		
		WaypointMapping core = tomerge.get(0);
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				List<AssetWaypointAttachment> toSave = new ArrayList<>();
				
				for (int i = 1; i < tomerge.size(); i ++) {
				
					WaypointMapping from = tomerge.get(i);
					//copy over attachments
					for (AssetWaypoint fromaw : from.aw) {
						
						AssetWaypoint toAssetWaypoint = null;
						for (AssetWaypoint aw : core.aw) {
							if (aw.getAssetDeployment().equals(fromaw.getAssetDeployment())) {
								toAssetWaypoint = aw;
							}
						}
						if (toAssetWaypoint == null) {
							toAssetWaypoint = new AssetWaypoint();
							toAssetWaypoint.setAssetDeployment(fromaw.getAssetDeployment());
							toAssetWaypoint.setAttachments(new HashSet<>());
							toAssetWaypoint.setState(State.DIRTY);
							toAssetWaypoint.setWaypoint(core.wp);
							core.aw.add(toAssetWaypoint);
							
							//TODO: i think we need to check the asset deployment time range here and make
							//sure it includes the incident date/time..
						}
						session.saveOrUpdate(toAssetWaypoint);

						
						if (fromaw.getAttachments() != null) {
							for (AssetWaypointAttachment fromattachment : fromaw.getAttachments()) {
								
								//create a new waypoint
								WaypointAttachment source = fromattachment.getWaypointAttachment();
								
								WaypointAttachment att = new WaypointAttachment();
								// copy file to temp location so it won't be deleted out from under us
								File tmpLocation = File.createTempFile("smart_" + System.nanoTime(), ""); //$NON-NLS-1$ //$NON-NLS-2$
								tmpLocation.deleteOnExit();
								source.computeFileLocation(session);
								FileUtils.copyFile(source.getAttachmentFile(), tmpLocation);
								att.setCopyFromLocation(tmpLocation);
								att.setFilename(source.getFilename());
								att.setWaypoint(core.wp);
								core.wp.getAttachments().add(att);
								
								AssetWaypointAttachment newAttachment = new AssetWaypointAttachment();
								newAttachment.setAssetWaypoint(toAssetWaypoint);
								newAttachment.setWaypointAttachment(att);
								toAssetWaypoint.getAttachments().add(newAttachment);
								
							}
						}
					}
					//copy over observations
					if (from.wp.getObservations() != null) {
						for (WaypointObservation wo : from.wp.getObservations()) {
							if (!DataImporterView.containsObservation(wo, core.wp.getObservations())) {
								WaypointObservation woClone = wo.clone(session);
								core.wp.getObservations().add(woClone);
								woClone.setWaypoint(core.wp);
							}
						}
					}

					for (AssetWaypoint aw : from.aw) {
						aw.getAttachments().forEach(a->session.delete(a));
						aw.getAttachments().clear();
						session.delete(aw);
					}
					session.delete(from.wp);
					
				}
				
				session.saveOrUpdate(core.wp);
				toSave.forEach(a->session.save(a));
				session.getTransaction().commit();
			}catch(Exception ex){
				AssetPlugIn.displayLog("Error occurred saving changes. You should reload the page before continuing: " +ex.getMessage(), ex);
				session.getTransaction().rollback();
			}
		}
		
//		List<RowItem> toRemove = new ArrayList<>();
//		for (RowItem i : rows) {
//			if (tomerge.contains(i.waypoint) && ! i.waypoint.equals(core)) {
//				toRemove.add(i);
//				i.item.dispose();
//			}else if (i.waypoint.equals(core)) {
//				i.tt.refresh();
//			}
//		}
//		rows.removeAll(toRemove);
//		resizeScroll();
	}
	
	private int lastSelection;
	private void processMouseClickEvent(RowItem item, Event event) {
		int index = rows.indexOf(item);
		if ((event.stateMask & SWT.CTRL) != 0){
			item.setSelected(!item.isSelected);
		}else if ((event.stateMask & SWT.SHIFT) != 0){
			boolean newSelection = !item.isSelected;
			//clearSelection();
			
			int from = lastSelection;
			int to = index;
			if (index < lastSelection){
				from = index;
				to = lastSelection;
			}
			
			for (int i = from; i <= to; i ++){
				if (i == index){
					rows.get(i).setSelected(true);
				}else{
					rows.get(i).setSelected(newSelection);		
				}
			}
			
		}else{					
			boolean newSelection = item.isSelected;
			if (event.button == 1) {
				newSelection = !newSelection;
			}else {
				newSelection = true;
			}
			if (event.button != 3 || !item.isSelected) {
				rows.forEach(e->e.setSelected(false));
				item.setSelected(newSelection);
			}
		}	
		List<Waypoint> selection = rows.stream().filter(e->e.isSelected).map(e->{ return e.waypoint.wp; }).collect(Collectors.toList());
		view.getContext().get(ESelectionService.class).setSelection(new StructuredSelection(selection));
		lastSelection = index;
		
	}
	
	static void colorControl(Control control, Color color) {
		forEachChild(control, e->{
			Boolean c = (Boolean) e.getData("COLOR");
			if (c == null || c) {
				e.setBackground(color);
				e.redraw();
				return true;
			}
			return false;
		});
	}
	
	static void forEachChild( Control control, Predicate<Control> consumer) {
		if (!consumer.test(control)) return;
		if (!(control instanceof Composite)) return;
		List<Control> kids = new ArrayList<>();
		for (Control c : ((Composite)control).getChildren()) kids.add(c);
		while(!kids.isEmpty()) {
			Control c = kids.remove(0);
			if (!consumer.test(c)) continue;
			if (c instanceof Composite) {
				for (Control c3 : ((Composite)c).getChildren()) {
					kids.add(c3);
				}
			}
		}
	}
	private class RowItem{
		
		private WaypointMapping waypoint;
		private AttachmentTable tt;
		
		private int fileCnt = 0;
		private boolean isSelected = false;
		private boolean isMouseOver = false;
		
		private Composite item;
		private Composite header;
		private Composite wppart ;
		
		private Label headerLabel;
		
		private Color bgColor = null;
		
		public RowItem(WaypointMapping waypoint) {
			this.waypoint = waypoint;
		}
		
		public void setSelected(boolean isSelected) {
			if (this.isSelected == isSelected) return;
			
			this.isSelected = isSelected;
			if (isSelected) {
				colorControl(header, selectionColor);
				colorControl(wppart, mouseOverColor);
			}else {
				colorControl(header, headerColor);
				colorControl(wppart, bgColor);
			}
			item.redraw();
		}
		
		public void createControl(Composite parent) {
			Listener clickListener = e->{
				switch(e.type) {
				case SWT.MouseUp:
					processMouseClickEvent(RowItem.this, e);
				case SWT.MouseEnter:
					this.isMouseOver = true;
					if (!isSelected) colorControl(header, mouseOverColor);
					item.redraw();
					break;
				case SWT.MouseExit:
					this.isMouseOver = false;
					if (isSelected) colorControl(header, selectionColor);
					else colorControl(header, headerColor);
					item.redraw();
					break;
				}
			};
			
			item = toolkit.createComposite(parent, SWT.NONE);
			item.setLayout(new GridLayout());
			item.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
			((GridLayout)item.getLayout()).marginWidth = 2;
			((GridLayout)item.getLayout()).marginHeight = 2;
			((GridLayout)item.getLayout()).verticalSpacing = 0;
			bgColor = item.getBackground();
			
			item.addPaintListener(e->{
				if (!isMouseOver && !isSelected) return;
				
				if (isMouseOver) {
					e.gc.setLineWidth(2);
					e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
				}else if (isSelected) {
					e.gc.setLineWidth(2);
					e.gc.setForeground(selectionColor);
				}	
				e.gc.drawRectangle(0, 0, item.getBounds().width,item.getBounds().height);
				
			});			
			
			header = toolkit.createComposite(item, SWT.NONE);
			header.setLayout(new GridLayout(2, false));
			header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
			header.setBackground(headerColor);
			
			StringBuilder sb = new StringBuilder();
			sb.append(DateFormat.getDateTimeInstance().format(waypoint.wp.getDateTime()));
//			sb.append(" (");
//			sb.append(aw.getWaypoint().getId());
//			sb.append(") ");
			
			Set<Asset> assets = waypoint.aw.stream().map(e->e.getAssetDeployment().getAsset()).collect(Collectors.toSet());
			Set<AssetStationLocation> locations= waypoint.aw.stream().map(e->e.getAssetDeployment().getStationLocation()).collect(Collectors.toSet());
			sb.append("      ");
			for (AssetStationLocation l : locations) {
				sb.append(l.getId());
				sb.append(" (");
				sb.append(l.getStation().getId());
				sb.append(")");
			}
			
			sb.append("      ");
			for (Asset a : assets) {
				sb.append(a.getId());
				sb.append(", ");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			
			
			headerLabel = toolkit.createLabel(header, sb.toString());
			headerLabel.setBackground(header.getBackground());
			headerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			headerLabel.addListener(SWT.MouseDown, e->{
				if (e.button == 3) {
					createMenu(headerLabel);
				}
			});
			
			ToolBar tb = new ToolBar(header, SWT.FLAT);
			tb.setBackground(header.getBackground());
			
			ToolItem itemDelete = new ToolItem(tb, SWT.PUSH);
			itemDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			itemDelete.setToolTipText("delete incident");
			itemDelete.addListener(SWT.Selection, e->{
				deleteItems(Collections.singleton(waypoint));
				
			});
			
			
			ToolItem itemCh = new ToolItem(tb, SWT.PUSH);
			itemCh.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_VALIDATE));
			itemCh.setToolTipText("mark as verified");
			itemCh.addListener(SWT.Selection, e->{
				markValidated(Collections.singleton(waypoint));
			});
			
			
			
			wppart = toolkit.createComposite(item, SWT.NONE);
			wppart.setLayout(new GridLayout(2, false));
			wppart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
			
			((GridLayout)wppart.getLayout()).marginWidth = 0;
			((GridLayout)wppart.getLayout()).marginHeight = 0;
			
			
			List<ISmartAttachment> files = new ArrayList<>();
			if (waypoint.wp.getAttachments() != null) {
				waypoint.wp.getAttachments().forEach(a->files.add(a));
			}
			if (waypoint.wp.getObservations() != null) {
				waypoint.wp.getObservations().forEach(o->{
					if (o.getAttachments() != null) {
						o.getAttachments().forEach(a->files.add(a));
					}
				});
			}
			
			AttachmentTable.IMenuCreator mnuCreator = new AttachmentTable.IMenuCreator() {
				
				@Override
				public Menu createMenu(AttachmentTable parent) {
					Menu mnu = new Menu(parent);
					
					MenuItem createIncident = new MenuItem(mnu, SWT.PUSH);
					createIncident.setText("Extract as New Incident...");
					createIncident.addListener(SWT.Selection, e->{
						IStructuredSelection items = tt.getSelection();
						List<WaypointAttachment> toMove = new ArrayList<>();
						for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
							Object attachment = iterator.next();
							if (attachment instanceof WaypointAttachment) toMove.add((WaypointAttachment) attachment);
						}
							
						WaypointMapping newWaypoint = newIncident(waypoint, toMove);
						if (newWaypoint == null) return;
						//refresh entire view as we now have a new incident
						loadWaypointsJob.schedule();
					});
					new MenuItem(mnu, SWT.SEPARATOR);
					
					MenuItem removeImg = new MenuItem(mnu, SWT.PUSH);
					removeImg.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
					removeImg.setText("Remove Attachment...");
					removeImg.addListener(SWT.Selection, e->{
						IStructuredSelection items = tt.getSelection();
						//TODO: verify delete with user
						//TODO: remove deployment link
						try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
							session.beginTransaction();
							try {
								for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
									Object attachment = iterator.next();
									if (attachment instanceof ISmartAttachment) {
										waypoint.wp.getAttachments().remove(attachment);
										session.delete(attachment);
										files.remove(attachment);
									}
									
								}
								session.getTransaction().commit();
							}catch (Exception ex) {
								session.getTransaction().rollback();
								AssetPlugIn.displayLog("Error saving changes.  You should reload the page before continuing. " + ex.getMessage(), ex);
							}
							tt.refresh();
							DataReviewPage.this.resizeScroll();
						}
						
					});					
					MenuItem addAttachment = new MenuItem(mnu, SWT.PUSH);
					addAttachment.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
					addAttachment.setText("Add Attachment...");
					addAttachment.addListener(SWT.Selection, e->{
						FileDialog fd = new FileDialog(parent.getShell(), SWT.OPEN | SWT.MULTI);
						if (fd.open() == null) return;
						String path = fd.getFilterPath();
						List<WaypointAttachment> toAdd = new ArrayList<>();
						for (String s : fd.getFileNames()) {
							Path p = Paths.get(path,s);
						
							//TODO: add deployment link
							WaypointAttachment wa = new WaypointAttachment();
							wa.setCopyFromLocation(p.toFile());
							wa.setWaypoint(waypoint.wp);
							wa.setFilename(s);
							toAdd.add(wa);
							
						}
						try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
							session.beginTransaction();
							try {
								for (WaypointAttachment wa : toAdd) {
									waypoint.wp.getAttachments().add(wa);
									session.save(wa);
									wa.computeFileLocation(session);
								}
								session.getTransaction().commit();
								files.addAll(toAdd);
							}catch (Exception ex) {
								session.getTransaction().rollback();
								AssetPlugIn.displayLog("Error saving changes.  You should reload the page before continuing. " + ex.getMessage(), ex);
							}
						}
			
						tt.refresh();
						DataReviewPage.this.resizeScroll();
					});
					new MenuItem(mnu, SWT.SEPARATOR);
					
					MenuItem properties = new MenuItem(mnu, SWT.PUSH);
					properties.setText("Properties");
					properties.addListener(SWT.Selection, e->{
						Object x = tt.getSelection().getFirstElement();
						if (x instanceof WaypointAttachment) {
							AttachmentPropertiesDialog dialog = new AttachmentPropertiesDialog(parent.getShell(), (WaypointAttachment)x) {
								protected List<Entry> findAdditionalDetails(ISmartAttachment attachment) {
									for (AssetWaypoint aw : waypoint.aw) {
										for (AssetWaypointAttachment attach: aw.getAttachments()) {
											if (attach.getWaypointAttachment().equals(x)) {
												return Collections.singletonList(new AttachmentPropertiesDialog.Entry("Asset", aw.getAssetDeployment().getAsset().getId()));
											}
										}
									}
									return Collections.emptyList();
								}
							};
							dialog.open();

						}
					});
					
					mnu.addMenuListener(new MenuListener() {
						
						@Override
						public void menuShown(MenuEvent e) {
							boolean hasSelection = !tt.getSelection().isEmpty();
							properties.setEnabled(hasSelection);
							removeImg.setEnabled(hasSelection);
							createIncident.setEnabled(hasSelection);
						}
						
						@Override
						public void menuHidden(MenuEvent e) {
						}
					});
					return mnu;
				}
			};
			tt = new AttachmentTable(wppart, toolkit, mnuCreator, files, iconSize.getSize());

			tt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			((GridData)tt.getLayoutData()).widthHint = iconSize.getSize()*2+20;
			fileCnt = files.size();
			
			Composite spacer = toolkit.createComposite(wppart);
			spacer.setLayout(new GridLayout());
			spacer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			((GridLayout)spacer.getLayout()).marginWidth = 0;
			((GridLayout)spacer.getLayout()).marginHeight = 3;
			
			Composite detailsPart = toolkit.createComposite(spacer, SWT.NONE);
			detailsPart.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			detailsPart.setLayout(new GridLayout());
			((GridLayout)detailsPart.getLayout()).verticalSpacing = 1;
			((GridLayout)detailsPart.getLayout()).marginWidth = 1;
			((GridLayout)detailsPart.getLayout()).marginHeight = 1;
			detailsPart.setData("COLOR", false);
			
			if (waypoint.wp.getObservations() != null) {
				new WaypointAttributeTable(detailsPart, toolkit, waypoint.wp, DataReviewPage.this);
			}

			forEachChild(header, e->{
				e.addListener(SWT.MouseUp, clickListener);
				e.addListener(SWT.MouseEnter, clickListener);
				e.addListener(SWT.MouseExit, clickListener);
				return true;
			});
			
			item.addListener(SWT.MouseUp, clickListener);
			item.addListener(SWT.MouseEnter, clickListener);
			item.addListener(SWT.MouseExit, clickListener);
		}
		
		
		
		public void resize(int totalWidth, DataDisplaySettings.IconSize iconSize) {
			if (tt.isDisposed()) return;
			int w1 = iconSize.getSize() * 2 + 20;
			int w2 = (totalWidth * 1) / 2;
			int w3 = fileCnt  * ( iconSize.getSize() + 5 );
			
			int setting = w3;
			
			if (w3 < w1 && w3 < w2) {
				((GridData)tt.getLayoutData()).widthHint = w3;
			}else {
				int numItems = (int)Math.floor(w2 / (iconSize.getSize() + 5 ));
				numItems = Math.min(waypoint.wp.getAttachments() == null || waypoint.wp.getAttachments().isEmpty()? 1 : waypoint.wp.getAttachments().size(), numItems);
				setting = Math.min(numItems * (iconSize.getSize() + 5 ), w2);
				
			}
			if (setting == 0) setting = 50;
			((GridData)tt.getLayoutData()).widthHint = setting;
			tt.getParent().layout(true);
				
			
		}
	}
	
	private class WaypointMapping{
		Waypoint wp;
		Collection<AssetWaypoint> aw;
		
		public WaypointMapping(Waypoint wp, Collection<AssetWaypoint> aw) {
			this.wp = wp;
			this.aw = aw;
		}
	}
}
