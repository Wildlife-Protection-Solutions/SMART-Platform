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

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointAttachment;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.asset.ui.views.data.StationAssetSelectionDialog.Type;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.ProgressAreaComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.UuidUtils;

/**
 * Data import page for data importer view
 * 
 * @author Emily
 *
 */
public class DataImportPage {

	private static final String SEP_CHAR = ","; //$NON-NLS-1$
	private static final String STATIONLOCATION_LIST_KEY = "org.wcs.smart.asset.ui.views.data.stations"; //$NON-NLS-1$
	private static final String ASSET_LIST_KEY = "org.wcs.smart.asset.ui.views.data.assets"; //$NON-NLS-1$
	
	private DataImporterView view;
	
	private Composite details; 
	private ResultsPanel tblResults;
		
	private Label fileCnt;
	
	private ToolItem itemSaveAll;
	private ToolItem itemSave;
	private ToolItem itemDelete;
	
	private List<Asset> selectedAssets = new ArrayList<>();
	private List<AssetStationLocation> selectedLocations = new ArrayList<>();

	private List<FileProxy> deletedItems;
	
	private Color[] rowColors;
		
	private FormToolkit toolkit;
	
	private FileProcessor processor;
	
	private EventHandler listRefreshHandler = e->{
		loadLists(500);
	};
	
	public DataImportPage(DataImporterView view, FormToolkit toolkit) {
		this.view = view;
		this.toolkit = toolkit;
		
		processor = new FileProcessor(SmartDB.getCurrentConservationArea(), Locale.getDefault());
		deletedItems = new ArrayList<>();
		
		loadLists(0);
		
		view.getContext().get(IEventBroker.class).subscribe(AssetEvents.ASSET_DELETE, listRefreshHandler);
		view.getContext().get(IEventBroker.class).subscribe(AssetEvents.ASSETSTATION_DELETE, listRefreshHandler);
		view.getContext().get(IEventBroker.class).subscribe(AssetEvents.ASSETSTATIONLOCATION_DELETE, listRefreshHandler);
	}
	
	public void dispose() {
		view.getContext().get(IEventBroker.class).unsubscribe(listRefreshHandler);
		this.view = null;
	}
	
	public void addFiles() {
		FileDialog fd = new FileDialog(Display.getDefault().getActiveShell(), SWT.MULTI | SWT.OPEN);
		if (fd.open() == null) return;
		List<Path> paths = new ArrayList<>();
		for (String file : fd.getFileNames()) {
			Path temp = Paths.get(fd.getFilterPath(), file);
			paths.add(temp);
		}
		processFiles(paths);
	}
	
	
	public void createPage(Composite parent) {
		rowColors = new Color[] {
				new Color(parent.getDisplay(), 179, 225, 210),
				new Color(parent.getDisplay(), 255, 200, 170),
				new Color(parent.getDisplay(), 200, 200, 230),
				new Color(parent.getDisplay(), 240, 200, 225),
				new Color(parent.getDisplay(), 211, 240, 170),
				new Color(parent.getDisplay(), 255, 236, 150),
				new Color(parent.getDisplay(), 242, 226, 202),
				new Color(parent.getDisplay(), 220, 220, 220),
		};
		
		
		Composite main = toolkit.createComposite(parent);
		main.addListener(SWT.Dispose, e->{
			for (Color c : rowColors) c.dispose();
		});
		
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		Composite topPart = new Composite(main, SWT.NONE);
		topPart.setLayout(new GridLayout(2, false));
		topPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)topPart.getLayout()).marginWidth = 0;
		((GridLayout)topPart.getLayout()).marginHeight = 0;
		
		ToolBar tb = new ToolBar(topPart, SWT.FLAT);
		toolkit.adapt(tb);
		
		ToolItem itemAdd = new ToolItem(tb, SWT.PUSH);
		itemAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
		itemAdd.setToolTipText(Messages.DataImportPage_addTooltip);
		itemAdd.addListener(SWT.Selection, e->{
			addFiles();
		});
		
		itemDelete = new ToolItem(tb, SWT.PUSH);
		itemDelete.setEnabled(false);
		itemDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		itemDelete.setToolTipText(Messages.DataImportPage_removeTooltip);
		itemDelete.addListener(SWT.Selection,e->{
			removeFiles();
		});
		
		new ToolItem(tb,  SWT.SEPARATOR);
		
		itemSaveAll = new ToolItem(tb, SWT.PUSH);
		itemSaveAll.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.SAVEALL_ICON));
		itemSaveAll.setToolTipText(Messages.DataImportPage_saveAllTooltip);
		itemSaveAll.setEnabled(false);
		itemSaveAll.addListener(SWT.Selection, e->saveAll());
		
		itemSave = new ToolItem(tb, SWT.PUSH);
		itemSave.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.SAVE_ICON));
		itemSave.setToolTipText(Messages.DataImportPage_saveTooltip);
		itemSave.setEnabled(false);
		itemSave.addListener(SWT.Selection, e->save(getSelection()));
		
		new ToolItem(tb,  SWT.SEPARATOR);
		
		fileCnt = toolkit.createLabel(topPart, ""); //$NON-NLS-1$
		fileCnt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		details = toolkit.createComposite(main);
		details.setLayout(new GridLayout());
		details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)details.getLayout()).marginWidth = 0;
		((GridLayout)details.getLayout()).marginHeight = 0;
		
		createFileSummary();
	}
	
	void saveAll() {
		save(processor.getFiles());
	}
	void save(Collection<FileProxy> toSave) {
		
		//cannot save if not valid
		for (FileProxy p : toSave) {
			if (!p.isValid()) return;
		}
		
		List<FileProxy> counts = new ArrayList<>();
		counts.addAll(toSave);
		int number = 0;
		while(!counts.isEmpty()) {
			FileProxy c = counts.remove(0);
			number++;
			counts.removeAll(c.getRelations());
			if (c.getFixedRelations() != null) counts.removeAll(c.getFixedRelations());
		}
		final int totalItems = number;
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(view.getSite().getShell());
		try {
		pmd.run(true, false,  new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask(Messages.DataImportPage_ImportTaskName, totalItems + 1);
				List<FileProxy> items = new ArrayList<FileProxy>(toSave);
				List<FileProxy> toProcess = new ArrayList<FileProxy>(items);
				try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
					session.beginTransaction();
					try {
						while(toProcess.size() > 0) {
							FileProxy p = toProcess.remove(0);
							
							monitor.subTask(p.getFile().toString());
							
							if (p.getStation().getUuid() != null) {
								AssetStation stn = session.get(AssetStation.class, p.getStation().getUuid());
								if (stn != null) {
									p.setStation(stn);
								}else {
									//for hiberante
									p.getStation().setUuid( null );
									p.getStation().setAttributeValues(new ArrayList<>());
									ArrayList<AssetStationLocation> locs = new ArrayList<>(p.getStation().getLocations());
									p.getStation().setLocations(locs);
								}
							}
							if (p.getStationLocation().getUuid() != null) {
								AssetStationLocation loc = session.get(AssetStationLocation.class, p.getStationLocation().getUuid());
								if (loc != null) {
									p.setStationLocation(loc);
								}else {
									//for hibernate
									p.getStationLocation().setUuid(null);
									p.getStationLocation().setAttributeValues(new ArrayList<>());
								}
							}
							
							boolean isNewStation = p.getStation().getUuid() == null;
							boolean isNewLocation = p.getStationLocation().getUuid() == null;
							
							if (isNewStation) {
								p.getStation().setId(generateStationId(session));
								if (p.getStation().getX() == null) p.getStation().setX(p.getX());
								if (p.getStation().getY() == null) p.getStation().setY(p.getY());
								session.save(p.getStation());
								session.flush();
							}
							
							if (isNewLocation) {
								session.save(p.getStationLocation());
								
								p.getStationLocation().setId(generateLocationId(p.getStation(), session));
								if (p.getStationLocation().getX() == null) p.getStationLocation().setX(p.getX());
								if (p.getStationLocation().getY() == null) p.getStationLocation().setY(p.getY());
								session.save(p.getStationLocation());
								session.flush();
							}
							
							Waypoint wp = new Waypoint();
							wp.setConservationArea(SmartDB.getCurrentConservationArea());
							wp.setDateTime(p.getImageDate());
							wp.setId(1); //updated below
							wp.setSourceId(AssetWaypointSource.KEY);
							wp.setRawX(p.getX());
							wp.setRawY(p.getY());
							wp.setAttachments(new ArrayList<>());
							wp.setComment(p.getWaypointComment());
							
							Map<Asset, List<WaypointAttachment>> assetAttachmentLink = new HashMap<>();
							
							WaypointAttachment wa = new WaypointAttachment();
							wa.setWaypoint(wp);
							wa.setCopyFromLocation(p.getFile().toFile());
							wa.setFilename(p.getFile().getFileName().toString());
							wp.getAttachments().add(wa);
							wp.setObservationGroups(new ArrayList<>());
							wp.setId(0);
							
							assetAttachmentLink.put(p.getAsset(), new ArrayList<>(Collections.singleton(wa)));
							
							WaypointObservationGroup g = new WaypointObservationGroup();
							g.setObservations(new ArrayList<>());
							g.setWaypoint(wp);
							for (WaypointObservation wo : p.getObservations()) {
								WaypointObservation clone = wo.clone(session);
								clone.setObservationGroup(g);
								g.getObservations().add(clone);
							}
							
							
							//add relations
							List<FileProxy> relations = new ArrayList<>();
							HashMap<Asset, AssetStationLocation> assets = new HashMap<>();
							assets.put(p.getAsset(), p.getStationLocation());
							relations.addAll(p.getRelations());
							if (p.getFixedRelations() != null) relations.addAll(p.getFixedRelations());
							Date minDate = wp.getDateTime();
							Date maxDate = wp.getDateTime();
							
							for (FileProxy pp : relations) {
								if (!items.contains(pp)) items.add(pp);
								toProcess.remove(pp);
								if (pp.getImageDate().before(wp.getDateTime())) {
									wp.setDateTime(pp.getImageDate());
								}
								if (pp.getImageDate().before(minDate)) {
									minDate = pp.getImageDate();
								}
								if (pp.getImageDate().after(maxDate)) {
									maxDate = pp.getImageDate();
								}
								wa = new WaypointAttachment();
								wa.setWaypoint(wp);
								wa.setCopyFromLocation(pp.getFile().toFile());
								wa.setFilename(pp.getFile().getFileName().toString());
								wp.getAttachments().add(wa);
								
								for (WaypointObservation nextobs : pp.getObservations()) {
									if (!AssetUtils.containsObservation(nextobs, g.getObservations())) {
										WaypointObservation clone = nextobs.clone(session);
										clone.setObservationGroup(g);
										g.getObservations().add(clone);
									}
								}
								
								List<WaypointAttachment> links = assetAttachmentLink.get(pp.getAsset());
								if (links == null) {
									links = new ArrayList<>();
									assetAttachmentLink.put(pp.getAsset(), links);
								}
								links.add(wa);
								assets.put(pp.getAsset(), pp.getStationLocation());
							}
							
							if (!g.getObservations().isEmpty()) {
								wp.getObservationGroups().add(g);
							}
							
							int incidentLength =  (int)Math.ceil( ( maxDate.getTime() - minDate.getTime()) / 1000.0);
							
							session.save(wp);
							session.flush();
							
							for (Entry<Asset, AssetStationLocation> entry : assets.entrySet()) {
								Asset asset = entry.getKey();
								//TODO: this is wrong p.getStationLocation is not the station/location for
								//this asset
								AssetDeployment d = FileProcessor.findAssetDeployment(wp, asset, 
										entry.getValue(), session, Locale.getDefault());
								
								if (d.getUuid() == null) {
									session.save(d);
									session.flush();
								}
								int wpcnt = d.getAssetWaypoints().size() + 1;
								wp.setId(Math.max(wpcnt, wp.getId()));
							
								//link waypoint to deployment
								AssetWaypoint aw = new AssetWaypoint();
								aw.setState(AssetWaypoint.State.DIRTY);
								aw.setWaypoint(wp);
								aw.setAssetDeployment(d);
								aw.setIncidentLength(incidentLength);
								aw.setAttachments(new HashSet<>());
								if (d.getAssetWaypoints() == null) d.setAssetWaypoints(new ArrayList<>());
								d.getAssetWaypoints().add(aw);
								session.save(aw);
								
								//link files to deployment
								List<WaypointAttachment> files = assetAttachmentLink.get(asset);
								for(WaypointAttachment f : files) {
									AssetWaypointAttachment link = new AssetWaypointAttachment();
									link.setWaypointAttachment(f);
									link.setAssetWaypoint(aw);
									aw.getAttachments().add(link);
								}
															
								session.flush();
							}
							monitor.worked(1);
						}
						monitor.subTask(Messages.DataImportPage_SaveSubTask);
						session.getTransaction().commit();
					}catch (Exception ex){
						session.getTransaction().rollback();
						AssetPlugIn.displayLog(MessageFormat.format(Messages.DataImportPage_SaveError, ex.getMessage()), ex);
						return;
					}
				}
				
				monitor.subTask(Messages.DataImportPage_RefreshUiSubTask);
				
				items.forEach(e->processor.removeFile(e));				
				Display.getDefault().syncExec(()->{
					//clear deployments & recompute deployments and refresh table
					refreshProxies();
					//fire events
					view.getContext().get(IEventBroker.class).post(AssetEvents.ASSETDATA, null);
					if (view.getReviewPage() != null) view.getReviewPage().refresh();
				});
				monitor.worked(1);
			}
		});
		}catch (Exception ex) {
			AssetPlugIn.displayLog(Messages.DataImportPage_ImportError + ex.getMessage(), ex);
		}
	
	}
	
	private String generateStationId(Session session) {
		int cnt = 1;
		while(true) {
			String id = Messages.DataImportPage_StationIdPrefix + cnt;
			String query =  "SELECT count(*) FROM AssetStation where LOWER(id) = LOWER(:id) AND conservationArea = :ca "; //$NON-NLS-1$
			Long stncnt = (Long) session.createQuery(query)
				.setParameter("id",  id) //$NON-NLS-1$
				.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
				.uniqueResult();
			
			if (stncnt == 0) return id;
			cnt++;
		}
	}
	
	private String generateLocationId(AssetStation station, Session session) {
		int cnt = 1;
		while(true) {
			String id = station.getId() + " - " + cnt; //$NON-NLS-1$
			String query =  "SELECT count(*) FROM AssetStationLocation where LOWER(id) = LOWER(:id) AND station.conservationArea = :ca "; //$NON-NLS-1$
			Long stncnt = (Long) session.createQuery(query)
				.setParameter("id",  id) //$NON-NLS-1$
				.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
				.uniqueResult();
			
			if (stncnt == 0) return id;
			cnt++;
		}
	}
	
	private void updateFileCount() {
		fileCnt.setText(MessageFormat.format(Messages.DataImportPage_FileCntMsg,processor.getFiles().size()));
	}

	private void updateStatus() {
		boolean isValid = processor.isValid();
		if (isValid && !processor.getFiles().isEmpty()) {
			itemSaveAll.setEnabled(true);
			itemSaveAll.setToolTipText(Messages.DataImportPage_saveAllTooltip);
		}else {
			itemSaveAll.setEnabled(false);
			itemSaveAll.setToolTipText(Messages.DataImportPage_processingNotCompleteTooltip);
		}
	}
	
	private void createCancelled(List<Path> files) {
		for (Control c : details.getChildren()) c.dispose();
		Composite c = toolkit.createComposite(details);
		c.setLayout(new GridLayout());
		toolkit.createLabel(c, Messages.DataImportPage_cancelledMsg);
		Button btn = toolkit.createButton(c, Messages.DataImportPage_RestartButton, SWT.NONE);
		btn.addListener(SWT.Selection, e->processFiles(files));
		details.layout();
	}
	
	public IEclipseContext getContext() {
		return view.getContext();
	}
	
	private void createFileSummary() {
		for (Control c : details.getChildren()) c.dispose();
		tblResults = new ResultsPanel(details, this, toolkit);
		
		tblResults.addSelectionListener(e->{
			IStructuredSelection selection = tblResults.getSelection();
			itemDelete.setEnabled(!selection.isEmpty());
			
			boolean canSave = !selection.isEmpty();
			for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
				Object item = (Object) iterator.next();
				if (item instanceof FileProxy && !((FileProxy) item).isValid()) {
					canSave = false;
				}
			}
			itemSave.setEnabled(canSave);
		});
		
		updateFileCount();
		updateStatus();
	}
	
	
	/**
	 * Get selected file proxies
	 * @return
	 */
	List<FileProxy> getSelection(){
		List<FileProxy> proxies = new ArrayList<>();
		IStructuredSelection selection = tblResults.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof FileProxy) {
				proxies.add((FileProxy)type);
			}
		}
		return proxies;
	}
	
	List<FileProxy> getDeletedItems(){
		return this.deletedItems;
	}

	
	/**
	 * Gets list of last selected assets
	 * @return
	 */
	List<Asset> getSelectedAssets(){
		return this.selectedAssets;
	}
	
	/**
	 * Gets list of last selected locations
	 * @return
	 */
	List<AssetStationLocation> getSelectedLocations(){
		return this.selectedLocations;
	}
	
	/**
	 * Updates the selected objects to the given asset.  If asset is null user
	 * will be prompted to pick and asset
	 * @param asset
	 */
	void setAsset( Asset asset ) {
		List<FileProxy> proxies = getSelection();
		int cnt = 0;
		for (FileProxy p : proxies) {
			if (p.getAsset() != null)cnt ++;
		}
		if (cnt > 0) {
			boolean n = MessageDialog.openQuestion(view.getSite().getShell(), Messages.DataImportPage_WarningTitle, 
				MessageFormat.format(Messages.DataImportPage_FilesOverwrittenMsg, cnt, proxies.size()));
			if (!n) return;
		}
		
		if (asset == null) {
			StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(view.getSite().getShell(), Type.ASSET);
			ContextInjectionFactory.inject(dialog, getContext());
			if (dialog.open() == StationAssetSelectionDialog.CANCEL) return;
			asset = dialog.getSelectedAsset();
		}
		addToQueue(selectedAssets, asset, ASSET_LIST_KEY);
		final Asset newAsset = asset;
		proxies.forEach(proxy->{
				proxy.setAsset(newAsset);
		});
		refreshProxies();
	}
	
	private <T extends UuidItem> void addToQueue(List<T> items, T item, String key) {
		if (items.contains(item)) return;
		items.add(0, item);
		while(items.size() > 5) items.remove(items.size() - 1);
		if (key == null) return;
		
		StringBuilder sb = new StringBuilder();
		for (UuidItem j : items) {
			sb.append(UuidUtils.uuidToString(j.getUuid()));
			sb.append(SEP_CHAR);
		}
		AssetPlugIn.getDefault().getPreferenceStore().putValue(key + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()), sb.toString());
	}
	
	/**
	 * Updates the selected objects to the given station location.  If the location is null the user
	 * will be prompted to pick the location
	 * 
	 * @param location
	 */
	void setLocation( AssetStationLocation location ) {
		List<FileProxy> proxies = getSelection();
		int cnt = 0;
		for (FileProxy p : proxies) {
			if (p.getStationLocation() != null) cnt++;
		}
		if (cnt > 0) {
			boolean n = MessageDialog.openQuestion(view.getSite().getShell(), Messages.DataImportPage_WarningTitle, 
				MessageFormat.format(Messages.DataImportPage_FilesOverwrittenMsg3, cnt, proxies.size()));
			if (!n) return;
		}
		if (location == null) {
			StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(view.getSite().getShell(), Type.LOCATION);
			ContextInjectionFactory.inject(dialog, getContext());
			if (dialog.open() == StationAssetSelectionDialog.CANCEL) return;
			location = dialog.getSelectedLocation();
		}
		final AssetStationLocation newLocation = location;
		addToQueue(selectedLocations, newLocation, STATIONLOCATION_LIST_KEY);
		proxies.forEach(proxy->{
				proxy.setStationLocation(newLocation);
		});
		refreshProxies();
	}
	
	/**
	 * Ungroup any manually grouped waypoints
	 */
	void ungroupSelected() {
		for (FileProxy p : getSelection()) {
			p.setFixedRelations(null);
		}
		refreshProxies();
	}
	
	/**
	 * Manually group selected items into waypoint
	 */
	void groupSelected() {
		List<FileProxy> proxies = getSelection();
		
		for (FileProxy p : proxies) {
			p.setFixedRelations(proxies);
		}
		refreshProxies();
	}
	
	
	/**
	 * Manually group selected items into waypoint
	 */
	void restoreFiles(List<FileProxy> proxies ) {
		List<Path> files = proxies.stream().map(e->e.getFile()).collect(Collectors.toList());
		deletedItems.removeAll(proxies);
		processFiles(files);
	}
	
	void setDateTime() {
		List<FileProxy> proxies = getSelection();
		int cnt = 0;
		for (FileProxy p : proxies) {
			if (p.getImageDate() != null) {
				cnt ++;
			}
		}
		if (cnt > 0) {
			boolean n = MessageDialog.openQuestion(view.getSite().getShell(), Messages.DataImportPage_WarningTitle, 
				MessageFormat.format(Messages.DataImportPage_FilesOverwrittenMsg4, cnt, proxies.size()));
			if (!n) return;
		}
		
		StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(view.getSite().getShell(), Type.DATE);
		ContextInjectionFactory.inject(dialog, getContext());
		if (dialog.open() == StationAssetSelectionDialog.CANCEL) return;
		
		proxies.forEach(p->p.setImageDate(dialog.getSelectedDate()));
		refreshProxies();
	}
	
	void removeFiles() {
		List<FileProxy> files = getSelection();
		for (FileProxy file : files) {
			processor.removeFile(file);
			deletedItems.add(file);
		}
		
		//if we delete a file I don't think we need to recompute relations
		processor.updateWaypointAndSort();
		tblResults.refresh();
		updateStatus();
		updateFileCount();
	}
	
	FileProcessor getProcessor() {
		return this.processor;
	}
	
	void setSelection(IStructuredSelection selection) {
		tblResults.setSelection(selection);
	}
	
	Color[] getRowColors() {
		return this.rowColors;
	}
	
	void refreshProxies() {
		processor.update();
		tblResults.refresh();
		updateStatus();
		updateFileCount();
	}
	
	private void processFiles(List<Path> files) {
		for (Control c : details.getChildren()) c.dispose();
		
		ProgressAreaComposite progressComp = new ProgressAreaComposite(details);
		final IProgressMonitor pmonitor = progressComp.createProgressMonitor();
		details.layout(true);
		
		Job processingJob = new Job(Messages.DataImportPage_processingJobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				pmonitor.beginTask(Messages.DataImportPage_ProcessingTaskName, files.size());
				for (Path f : files) {
					pmonitor.subTask(f.toString());
					processor.addFile(f, new FileProcessor.IConnectionProvider() {
						@Override
						public Session openSession() {
							return HibernateManager.openSession();
						}
					});
					pmonitor.worked(1);
					if (pmonitor.isCanceled()) break;
				}
				if (pmonitor.isCanceled()) {
					Display.getDefault().syncExec(()->createCancelled(files));
					return Status.CANCEL_STATUS;
				}
				Display.getDefault().syncExec(()->{createFileSummary();});
				return Status.OK_STATUS;
			}
			
		};
		processingJob.schedule();
	}
	
	/*
	 * load assets and stations location from
	 * preference store
	 */
	private void loadLists(long delay) {
		loadListsJob.schedule(delay);
	}
	
	private Job loadListsJob = new Job(Messages.DataImportPage_initializingJobName) {
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			selectedAssets.clear();
			selectedLocations.clear();
			try(Session session = HibernateManager.openSession()){
				String assets = AssetPlugIn.getDefault().getPreferenceStore().getString(ASSET_LIST_KEY + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()));
				if (assets != null && !assets.isEmpty()) {
					String[] uuids = assets.split(SEP_CHAR);
					for (int i = uuids.length-1; i >= 0 ; i --) {
						try {
							if (uuids[i].isEmpty()) continue;
							UUID uuid = UuidUtils.stringToUuid(uuids[i]);
							Asset a = session.get(Asset.class, uuid);
							
							if (a != null && a.getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
								a.getId();
								a.getAssetType().getIncidentCutoff();
								addToQueue(selectedAssets, a, null);
							}
						}catch (Exception ex) {
							
						}
					}
				}
				
				String locations = AssetPlugIn.getDefault().getPreferenceStore().getString(STATIONLOCATION_LIST_KEY + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()));
				if (locations != null && !locations.isEmpty()) {
					String[] uuids = locations.split(SEP_CHAR);
					for (int i = uuids.length-1; i >= 0 ; i --) {
						try {
							if (uuids[i].isEmpty()) continue;
							UUID uuid = UuidUtils.stringToUuid(uuids[i]);
							AssetStationLocation a = session.get(AssetStationLocation.class, uuid);
							if (a != null && a.getStation().getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
								a.getId();
								a.getStation().getId();
								addToQueue(selectedLocations, a, null);
							}
						}catch (Exception ex) {
							
						}
					}
				}
			}
			return Status.OK_STATUS;
		}
	};
}
