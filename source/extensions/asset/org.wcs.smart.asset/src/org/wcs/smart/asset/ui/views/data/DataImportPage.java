package org.wcs.smart.asset.ui.views.data;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import org.eclipse.swt.internal.C;
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
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.asset.ui.views.data.StationAssetSelectionDialog.Type;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.ProgressAreaComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

public class DataImportPage {

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
	
	public DataImportPage(DataImporterView view, FormToolkit toolkit) {
		this.view = view;
		this.toolkit = toolkit;
		
		processor = new FileProcessor(SmartDB.getCurrentConservationArea());
		deletedItems = new ArrayList<>();
	}
	
	
	public void createPage(Composite parent) {
		rowColors = new Color[] {
//				new Color(parent.getDisplay(), 255, 212, 127),
//				new Color(parent.getDisplay(), 255, 255, 170)
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
		itemAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		itemAdd.setToolTipText("Add files to import");
		itemAdd.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(Display.getDefault().getActiveShell(), SWT.MULTI | SWT.OPEN);
			if (fd.open() == null) return;
			List<Path> paths = new ArrayList<>();
			for (String file : fd.getFileNames()) {
				Path temp = Paths.get(fd.getFilterPath(), file);
				paths.add(temp);
			}
			processFiles(paths);
		});
		
		itemDelete = new ToolItem(tb, SWT.PUSH);
		itemDelete.setEnabled(false);
		itemDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		itemDelete.setToolTipText("Remove file from import list");
		itemDelete.addListener(SWT.Selection,e->{
			removeFiles();
		});
		
		new ToolItem(tb,  SWT.SEPARATOR);
		
		itemSaveAll = new ToolItem(tb, SWT.PUSH);
		itemSaveAll.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVEALL_EDIT));
		itemSaveAll.setToolTipText("Save all files to the database");
		itemSaveAll.setEnabled(false);
		itemSaveAll.addListener(SWT.Selection, e->save(processor.getFileDetails()));
		
		itemSave = new ToolItem(tb, SWT.PUSH);
		itemSave.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
		itemSave.setToolTipText("Save selected files to the database");
		itemSave.setEnabled(false);
		itemSave.addListener(SWT.Selection, e->save(getSelection()));
		
		new ToolItem(tb,  SWT.SEPARATOR);
		
		fileCnt = toolkit.createLabel(topPart, "");
		fileCnt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		details = toolkit.createComposite(main);
		details.setLayout(new GridLayout());
		details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)details.getLayout()).marginWidth = 0;
		((GridLayout)details.getLayout()).marginHeight = 0;
		
		createFileSummary();
	}
	
	void save(Collection<FileProxy> toSave) {
		Set<AssetStation> modifiedStations = new HashSet<>();
		Set<Asset> modifiedAssets = new HashSet<>();
		
		//cannot save if not valid
		for (FileProxy p : toSave) {
			if (!p.isValid()) return;
		}
				
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(view.getSite().getShell());
		try {
		pmd.run(true, false,  new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Importing files", toSave.size() + 1);
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
							wp.setX(p.getX());
							wp.setY(p.getY());
							wp.setAttachments(new ArrayList<>());
							
							WaypointAttachment wa = new WaypointAttachment();
							wa.setWaypoint(wp);
							wa.setCopyFromLocation(p.getFile().toFile());
							wa.setFilename(p.getFile().getFileName().toString());
							wp.getAttachments().add(wa);
							wp.setObservations(new ArrayList<>());
							
							for (WaypointObservation wo : p.getObservations()) {
								WaypointObservation clone = wo.clone(session);
								clone.setWaypoint(wp);
								wp.getObservations().add(clone);
							}
							
							//add relations
							List<FileProxy> relations = new ArrayList<>();
							relations.addAll(p.getRelations());
							if (p.getFixedRelations() != null) relations.addAll(p.getFixedRelations());							
							for (FileProxy pp : relations) {
								if (!items.contains(pp)) items.add(pp);
								toProcess.remove(pp);
								wa = new WaypointAttachment();
								wa.setWaypoint(wp);
								wa.setCopyFromLocation(pp.getFile().toFile());
								wa.setFilename(pp.getFile().getFileName().toString());
								wp.getAttachments().add(wa);
								
								for (WaypointObservation nextobs : pp.getObservations()) {
									if (!containsObservation(nextobs, wp.getObservations())) {
										WaypointObservation clone = nextobs.clone(session);
										clone.setWaypoint(wp);
										wp.getObservations().add(clone);
									}
								}
							}
							
							session.save(wp);
							session.flush();
							
							AssetDeployment d = processor.findAssetDeployment(wp, p.getAsset(), p.getStationLocation(), session);
							if (d.getUuid() == null) {
								session.save(d);
								session.flush();
							}
							int wpcnt = d.getAssetWaypoints().size() + 1;
//							String wpid = d.getStationLocation().getId() + "_" + df.format(wp.getDateTime()) + "_" + wpcnt;
							wp.setId(wpcnt);
							
							session.flush();
							
							AssetWaypoint aw = new AssetWaypoint();
							aw.setState(AssetWaypoint.State.DIRTY);
							aw.setWaypoint(wp);
							aw.setAssetDeployment(d);
							if (d.getAssetWaypoints() == null) d.setAssetWaypoints(new ArrayList<>());
							d.getAssetWaypoints().add(aw);
							session.save(aw);
							
							//TODO: verify we do not have overlapping deployments
							if (d.getEndDate() == null) {
								//ensure we have no other deployments that also have no end date
							}else {
								//ensure there are no other deployments whose start date is before this end date
							}
							
							//ensure there are no other deployments whose end date is before this start date
							
							session.flush();
							
							modifiedStations.add(p.getStation());
							modifiedAssets.add(d.getAsset());
							monitor.worked(1);
						}
						monitor.subTask("Saving to database...");
						session.getTransaction().commit();
					}catch (Exception ex){
						session.getTransaction().rollback();
						AssetPlugIn.displayLog(MessageFormat.format("Error saving asset files: {0}", ex.getMessage()), ex);
						return;
					}
				}
				
				monitor.subTask("Updating UI...");
				items.forEach(e->processor.removeFile(e));
				Display.getDefault().syncExec(()->{
					//clear deployments & recompute deployments and refresh table
					refreshProxies();

					//fire events
					view.getContext().get(IEventBroker.class).post(AssetEvents.ASSET_MODIFIED, modifiedAssets);
					view.getContext().get(IEventBroker.class).post(AssetEvents.ASSETSTATION_MODIFIED, modifiedStations);
				});
				monitor.worked(1);
			}
		});
		}catch (Exception ex) {
			AssetPlugIn.displayLog("Error importing asset files: " + ex.getMessage(), ex);
		}
		
//		if (processor.getFileDetails().isEmpty() && deletedItems.isEmpty()) {
//			setDirty(false);
//		}
	
	}
	
	private boolean containsObservation(WaypointObservation obs, List<WaypointObservation> all) {
		for (WaypointObservation wo : all) {
			if (!wo.getCategory().equals(obs.getCategory())) continue;
			
			if (wo.getAttributes().size() != obs.getAttributes().size()) continue;
			
			boolean ok = true;
			for (WaypointObservationAttribute a : wo.getAttributes()) {
				WaypointObservationAttribute matching = null;
				for (WaypointObservationAttribute aa : obs.getAttributes()) {
					if (aa.getAttribute().equals(a.getAttribute())){
						matching = aa;
						break;
					}
				}
				if (matching == null) {
					ok = false;
					break;
				}
				switch(a.getAttribute().getType()) {
					case BOOLEAN:
					case NUMERIC:
						ok = Objects.equals(a.getNumberValue(), matching.getNumberValue());
						break;
					case DATE:
					case TEXT:
						ok = Objects.equals(a.getStringValue(), matching.getStringValue());
						break;
					case LIST:
						ok = Objects.equals(a.getAttributeListItem(), matching.getAttributeListItem());
						break;
					case TREE:
						ok = Objects.equals(a.getAttributeTreeNode(), matching.getAttributeTreeNode());
						break;				
				}
				if (!ok) break;
			}
			if (ok) return true;
		}
		return false;
	}
	
	
	
	private String generateStationId(Session session) {
		int cnt = 1;
		while(true) {
			String id = "Station " + cnt;
			String query =  "SELECT count(*) FROM AssetStation where LOWER(id) = :id AND conservationArea = :ca ";
			Long stncnt = (Long) session.createQuery(query)
				.setParameter("id",  id.toLowerCase())
				.setParameter("ca", SmartDB.getCurrentConservationArea())
				.uniqueResult();
			
			if (stncnt == 0) return id;
			cnt++;
		}
	}
	
	private String generateLocationId(AssetStation station, Session session) {
		int cnt = 1;
		while(true) {
			String id = station.getId() + " - " + cnt;
			String query =  "SELECT count(*) FROM AssetStationLocation where LOWER(id) = :id AND station.conservationArea = :ca ";
			Long stncnt = (Long) session.createQuery(query)
				.setParameter("id",  id.toLowerCase())
				.setParameter("ca", SmartDB.getCurrentConservationArea())
				.uniqueResult();
			
			if (stncnt == 0) return id;
			cnt++;
		}
	}
	
	private void updateFileCount() {
		fileCnt.setText(MessageFormat.format("Number of Files: {0}",processor.getFileDetails().size()));
	}

	private void updateStatus() {
		boolean isValid = processor.isValid();
		if (isValid && !processor.getFileDetails().isEmpty()) {
			itemSaveAll.setEnabled(true);
			itemSaveAll.setToolTipText("Save all files to database");
		}else {
			itemSaveAll.setEnabled(false);
			itemSaveAll.setToolTipText("Data processing not complete.  You must ensure all rows in the table below are complete before you can save all");
		}
	}
	
	private void createCancelled(List<Path> files) {
		for (Control c : details.getChildren()) c.dispose();
		Composite c = toolkit.createComposite(details);
		c.setLayout(new GridLayout());
		toolkit.createLabel(c, "Processing cancelled by user");
		Button btn = toolkit.createButton(c, "Restart", SWT.NONE);
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
			boolean n = MessageDialog.openQuestion(view.getSite().getShell(), "Warning", 
				MessageFormat.format("{0} of the {1} selected files already have assets associated with them.  These will be overwritten.  Are you sure you want to continue?", cnt, proxies.size()));
			if (!n) return;
		}
		
		if (asset == null) {
			StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(view.getSite().getShell(), Type.ASSET);
			ContextInjectionFactory.inject(dialog, getContext());
			if (dialog.open() == StationAssetSelectionDialog.CANCEL) return;
			asset = dialog.getSelectedAsset();
		}
		addToQueue(selectedAssets, asset);
		final Asset newAsset = asset;
		proxies.forEach(proxy->{
				proxy.setAsset(newAsset);
		});
		refreshProxies();
	}
	
	private <T> void addToQueue(List<T> items, T item) {
		if (items.contains(item)) return;
		items.add(0, item);
		while(items.size() > 5) items.remove(items.size() - 1);
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
			boolean n = MessageDialog.openQuestion(view.getSite().getShell(), "Warning", 
				MessageFormat.format("{0} of the {1} selected files already have station locations associated with them.  These will be replaced overwritten if you process.  Are you sure you want to continue?", cnt, proxies.size()));
			if (!n) return;
		}
		if (location == null) {
			StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(view.getSite().getShell(), Type.LOCATION);
			ContextInjectionFactory.inject(dialog, getContext());
			if (dialog.open() == StationAssetSelectionDialog.CANCEL) return;
			location = dialog.getSelectedLocation();
		}
		final AssetStationLocation newLocation = location;
		addToQueue(selectedLocations, newLocation);
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
			boolean n = MessageDialog.openQuestion(view.getSite().getShell(), "Warning", 
				MessageFormat.format("{0} of the {1} selected files already have dates associated with them.  These dates will be replaced with new selected date.  Are you sure you want to continue?", cnt, proxies.size()));
			if (!n) return;
		}
		
		StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(view.getSite().getShell(), Type.DATE);
		ContextInjectionFactory.inject(dialog, getContext());
		if (dialog.open() == StationAssetSelectionDialog.CANCEL) return;
		
		proxies.forEach(p->p.setImageDate(dialog.getSelectedDate()));
		refreshProxies();
	}
	
	void removeFiles() {
//		boolean n = MessageDialog.openQuestion(getSite().getShell(), "Warning", 
//			"Are you sure you want to remove the selected file?  When removed, the files and associated observations will not be imported into SMART.");
//		if (!n) return;
//		
//		List<FileProxy> files = getSelection();
//		files.forEach(f->processor.removeFile(f));
//		refreshProxies();
		List<FileProxy> files = getSelection();
		for (FileProxy file : files) {
			processor.removeFile(file);
			deletedItems.add(file);
		}
		refreshProxies();
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
		
		Job processingJob = new Job("processing asset files") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				pmonitor.beginTask("Processing new files", files.size());
				for (Path f : files) {
					pmonitor.subTask(f.toString());
					processor.addFile(f);
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
//		setDirty(true);
	}
}
