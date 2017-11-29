package org.wcs.smart.asset.ui.views.data;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.importer.ActionableWarning;
import org.wcs.smart.asset.data.importer.FileMetadataReader;
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
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.control.ProgressAreaComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

import com.drew.metadata.Directory;
import com.drew.metadata.Tag;

public class DataImporterView extends EditorPart{

	private static final String ACTION_MENU_DATA_KEY = "ACTION";

	public static final String ID = "org.wcs.smart.asset.ui.views.data.importer";
	
	private FileProcessor processor;
	
	private boolean isDirty;
	
	private FormToolkit toolkit;
	
	private Composite details; 
	private TableViewer tblResults;
	
	private Composite fileDetailsComposite;
	private TableViewer tblExif; 
	private Label lblDetailsFileName; 
	private Label lblDetailsStatus ;
	private Canvas imageCanvas;
	private Composite proxyDetailsComp; 
	private IEclipseContext context;
	private Label fileCnt;
	
	private ToolItem itemSaveAll;
	private ToolItem itemSave;
	private ToolItem itemDelete;
	
	private List<Asset> selectedAssets = new ArrayList<>();
	private List<AssetStationLocation> selectedLocations = new ArrayList<>();
	
	private Composite singleSelectDetails;
	private Composite multiSelectDetails;
	
	private Color[] rowColors;
		
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	
	private void save(Collection<FileProxy> toSave) {
		Set<AssetStation> modifiedStations = new HashSet<>();
		Set<Asset> modifiedAssets = new HashSet<>();
		
		//cannot save if not valid
		for (FileProxy p : toSave) {
			if (!p.isValid()) return;
		}
				
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getSite().getShell());
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
							boolean isNewStation = p.getStation().getUuid() == null;
							boolean isNewLocation = p.getStationLocation().getUuid() == null;
							
							if (isNewStation) {
								p.getStation().setId(generateStationId(session));
								if (p.getStation().getX() == null) p.getStation().setX(p.getX());
								if (p.getStation().getY() == null) p.getStation().setY(p.getY());
								session.save(p.getStation());
							}
							
							if (isNewLocation) {
								p.getStationLocation().setId(generateLocationId(p.getStation(), session));
								if (p.getStationLocation().getX() == null) p.getStationLocation().setX(p.getX());
								if (p.getStationLocation().getY() == null) p.getStationLocation().setY(p.getY());
								session.save(p.getStationLocation());
							}
							session.flush();
							
							Waypoint wp = new Waypoint();
							wp.setConservationArea(SmartDB.getCurrentConservationArea());
							wp.setDateTime(p.getImageDate());
							wp.setId(1); //TODO:
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
							
							//TODO: FIXED RELATIONS
							//add relations
							for (FileProxy pp : p.getRelations()) {
								if (!items.contains(pp)) items.add(pp);
								toProcess.remove(pp);
								wa = new WaypointAttachment();
								wa.setWaypoint(wp);
								wa.setCopyFromLocation(pp.getFile().toFile());
								wa.setFilename(pp.getFile().getFileName().toString());
								wp.getAttachments().add(wa);
								
								for (WaypointObservation nextobs : pp.getObservations()) {
									//todo check for duplicate
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
							}
							int wpcnt = d.getAssetWaypoints().size() + 1;
//							String wpid = d.getStationLocation().getId() + "_" + df.format(wp.getDateTime()) + "_" + wpcnt;
							wp.setId(wpcnt);
							
							session.flush();
							
							AssetWaypoint aw = new AssetWaypoint();
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
					context.get(IEventBroker.class).post(AssetEvents.ASSET_MODIFIED, modifiedAssets);
					context.get(IEventBroker.class).post(AssetEvents.ASSETSTATION_MODIFIED, modifiedStations);
				});
				monitor.worked(1);
			}
		});
		}catch (Exception ex) {
			AssetPlugIn.displayLog("Error importing asset files: " + ex.getMessage(), ex);
		}
	
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
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		context = (IEclipseContext) getSite().getService(IEclipseContext.class);
		processor = new FileProcessor(SmartDB.getCurrentConservationArea());
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	public void setDirty(boolean isDirty) {
		boolean fire = isDirty != this.isDirty;
		this.isDirty = isDirty;
		if (fire) firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		
		rowColors = new Color[] {
				new Color(parent.getDisplay(), 255, 212, 127),
				new Color(parent.getDisplay(), 255, 255, 212)
		};
		
		toolkit = new FormToolkit(parent.getDisplay());
		
		Form mainform = toolkit.createForm(parent);
		mainform.setText("Import Asset Data");
		mainform.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainform.getBody().setLayout(new GridLayout());
		
		Composite main = toolkit.createComposite(mainform.getBody());
		
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
			removeFile();
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
		
		updateFileCount();
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
	
	private void createFileSummary() {
		for (Control c : details.getChildren()) c.dispose();
		
		//main section
		SashForm sash = new SashForm(details, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = toolkit.createComposite(sash, SWT.NONE);
		leftPart.setLayout(new GridLayout());
		((GridLayout)leftPart.getLayout()).marginWidth = 0;
		((GridLayout)leftPart.getLayout()).marginHeight = 0;
		
		tblResults = new TableViewer(leftPart, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		tblResults.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblResults.getTable().setHeaderVisible(true);
		tblResults.getTable().setLinesVisible(true);
		
		for (ResultsColumn c : ResultsColumn.values()) {
			TableViewerColumn column = new TableViewerColumn(tblResults, SWT.NONE);
			column.getColumn().setResizable(true);
			column.getColumn().setText(c.guiName);
			column.getColumn().setWidth(c.getWidth());
			if (c == ResultsColumn.WAYPOINT) {
				column.getColumn().setToolTipText("* indicates user defined group");
			}
			column.setLabelProvider(c.getLabelProvider(rowColors));
		}
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		tblResults.setInput(processor.getFileDetails());
		tblResults.addSelectionChangedListener(e->updateFileDetails());
		TableColumn dateColumn = tblResults.getTable().getColumn(ResultsColumn.DATE.ordinal());
		dateColumn.pack();
//		dateColumn.setWidth(dateColumn.getWidth() + 20);
		tblResults.refresh();
		
		Menu mnu = new Menu(tblResults.getControl());
		tblResults.getControl().setMenu(mnu);
		
		MenuItem mnuSetAsset = new MenuItem(mnu, SWT.CASCADE);
		mnuSetAsset.setText("Set Asset ...");
		Menu assetMenu = new Menu(mnuSetAsset);
		mnuSetAsset.setMenu(assetMenu);
		assetMenu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				// TODO Auto-generated method stub
				for (MenuItem mi : assetMenu.getItems()) mi.dispose();
				for(Asset a : selectedAssets) {
					MenuItem otherAsset = new MenuItem(assetMenu, SWT.PUSH);
					otherAsset.setText(a.getId());
					otherAsset.addListener(SWT.Selection, evt->setAsset(a));
				}
				if (!selectedAssets.isEmpty()) new MenuItem(assetMenu, SWT.SEPARATOR);
				
				MenuItem otherAsset = new MenuItem(assetMenu, SWT.PUSH);
				otherAsset.setText("Other Asset....");
				otherAsset.addListener(SWT.Selection, evt->setAsset(null));
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});

		MenuItem mnuSetLocation = new MenuItem(mnu, SWT.CASCADE);
		mnuSetLocation.setText("Set Station/Location ...");
		Menu locationMenu = new Menu(mnuSetAsset);
		mnuSetLocation.setMenu(locationMenu);
		locationMenu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				// TODO Auto-generated method stub
				for (MenuItem mi : locationMenu.getItems()) mi.dispose();
				for(AssetStationLocation a : selectedLocations) {
					MenuItem otherAsset = new MenuItem(locationMenu, SWT.PUSH);
					otherAsset.setText(MessageFormat.format("{0} [{1}]", a.getId(),a.getStation().getId()));
					otherAsset.addListener(SWT.Selection, evt->setLocation(a));
				}
				if (!selectedLocations.isEmpty()) new MenuItem(locationMenu, SWT.SEPARATOR);
				
				MenuItem otherAsset = new MenuItem(locationMenu, SWT.PUSH);
				otherAsset.setText("Other Location....");
				otherAsset.addListener(SWT.Selection, evt->setLocation(null));
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		
		
		MenuItem mnuSetDate = new MenuItem(mnu, SWT.PUSH);
		mnuSetDate.setText("Set Date/Time...");
		mnuSetDate.addListener(SWT.Selection, e->setDateTime());
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem mnuGroup = new MenuItem(mnu, SWT.PUSH);
		mnuGroup.setText("Create Custom Incident Group...");
		mnuGroup.addListener(SWT.Selection, e->groupSelected());
		
		MenuItem mnuRemoveGroup = new MenuItem(mnu, SWT.PUSH);
		mnuRemoveGroup.setText("Remove Custom Incident Group...");
		mnuRemoveGroup.addListener(SWT.Selection, e->ungroupSelected());
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem mnuSaveFile = new MenuItem(mnu, SWT.PUSH);
		mnuSaveFile.setText("Save");
		mnuSaveFile.addListener(SWT.Selection, e->{
			List<FileProxy> toSave = new ArrayList<>();
			for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object x = (FileProxy) iterator.next();
				if (x instanceof FileProxy) {
					toSave.add((FileProxy) x);
				}
			}
			save(toSave);
		});
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem mnuRemoveFile = new MenuItem(mnu, SWT.PUSH);
		mnuRemoveFile.setText("Remove File");
		mnuRemoveFile.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuRemoveFile.addListener(SWT.Selection, e->removeFile());
		
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mnuSetAsset.setEnabled(!tblResults.getSelection().isEmpty());
				mnuSetLocation.setEnabled(!tblResults.getSelection().isEmpty());
				mnuSetDate.setEnabled(!tblResults.getSelection().isEmpty());
				mnuRemoveFile.setEnabled(!tblResults.getSelection().isEmpty());
				mnuGroup.setEnabled(!tblResults.getSelection().isEmpty());
				//only save if all items are valid
				boolean ok = true;
				boolean canUngroup = false;
				for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
					Object x = (FileProxy) iterator.next();
					if (x instanceof FileProxy) {
						if (!((FileProxy) x).isValid()) ok = false;
						if (((FileProxy) x).isFixed()) canUngroup = true;
					}
				}
				mnuSaveFile.setEnabled(ok);
				mnuRemoveGroup.setEnabled(canUngroup);
				
				for (MenuItem i : mnu.getItems()) {
					Boolean x = (Boolean)i.getData(ACTION_MENU_DATA_KEY) ;
					if (x != null && x) i.dispose();
				}
				
				if (tblResults.getStructuredSelection().size() == 1) {
					FileProxy proxy = (FileProxy)tblResults.getStructuredSelection().getFirstElement();
					int sep = 0;
					for (ActionableWarning aw : proxy.getWarnings()) {
						ImportAction ia = ActionManager.findAction(aw, context);
						if (ia == null) continue;
						MenuItem mi = new MenuItem(mnu, SWT.PUSH, 0);
						mi.setText(ia.getMenuLabel());
						mi.setData(ACTION_MENU_DATA_KEY, true);
						mi.addListener(SWT.Selection, er->{
							if (ia.preformAction(processor, proxy)) {
								refreshProxies();
							}
						});
						sep ++;
					}
					if (sep>0) {
						MenuItem mi = new MenuItem(mnu, SWT.SEPARATOR, sep);
						mi.setData(ACTION_MENU_DATA_KEY, true);
					}
					
				}
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
				
		Composite rightPart = toolkit.createComposite(sash, SWT.NONE);
		rightPart.setLayout(new GridLayout());
		((GridLayout)rightPart.getLayout()).marginWidth = 0;
		((GridLayout)rightPart.getLayout()).marginHeight = 0;
		
		fileDetailsComposite = toolkit.createComposite(rightPart, SWT.BORDER);
		fileDetailsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fileDetailsComposite.setLayout(new StackLayout());
		
		sash.setWeights(new int[] {7,4});
		details.layout(true);
		
		createDetailsComposite();
		updateStatus();
	}
	
	
	private void createDetailsComposite() {
		singleSelectDetails = toolkit.createComposite(fileDetailsComposite);
		singleSelectDetails.setLayout(new GridLayout());
		
		multiSelectDetails = toolkit.createComposite(fileDetailsComposite);
		multiSelectDetails.setLayout(new GridLayout());
		
		((StackLayout)fileDetailsComposite.getLayout()).topControl = singleSelectDetails;
		
		Composite top = toolkit.createComposite(singleSelectDetails);
		top.setLayout(new GridLayout(2, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lblDetailsStatus = toolkit.createLabel(top, "");
		lblDetailsFileName = toolkit.createLabel(top, "");
		lblDetailsFileName .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SashForm detailsSash = new SashForm(singleSelectDetails, SWT.VERTICAL);
		detailsSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite infoComposite = toolkit.createComposite(detailsSash, SWT.NONE);
		infoComposite.setLayout(new GridLayout());
		infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)infoComposite.getLayout()).marginWidth = 0;
		((GridLayout)infoComposite.getLayout()).marginHeight = 0;
		
		Composite header = toolkit.createComposite(infoComposite);
		header.setLayout(new GridLayout(3, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		
		
		Hyperlink lnkDetails = toolkit.createHyperlink(header, "Details", SWT.NONE);
		Hyperlink lnkExif = toolkit.createHyperlink(header, "EXIF Metadata", SWT.NONE);
		Hyperlink lnkXmp = toolkit.createHyperlink(header, "XMP Metadata", SWT.NONE);
		
		Composite stackComposite = toolkit.createComposite(infoComposite, SWT.BORDER);
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackComposite.setLayout(new StackLayout());
		
		proxyDetailsComp = toolkit.createComposite(stackComposite);
		proxyDetailsComp.setLayout(new GridLayout());
		((GridLayout)proxyDetailsComp.getLayout()).marginWidth = 0;
		((GridLayout)proxyDetailsComp.getLayout()).marginHeight = 0;
		
		Composite exifMetadataComp = toolkit.createComposite(stackComposite);
		exifMetadataComp.setLayout(new GridLayout());
		((GridLayout)exifMetadataComp.getLayout()).marginWidth = 0;
		((GridLayout)exifMetadataComp.getLayout()).marginHeight = 0;
		
		tblExif = new TableViewer(exifMetadataComp, SWT.FULL_SELECTION);
		tblExif.getTable().setLinesVisible(false);
		tblExif.getTable().setHeaderVisible(true);
		tblExif.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblExif.setContentProvider(ArrayContentProvider.getInstance());
		
		Color bgColor = new Color(tblExif.getControl().getDisplay(), 160,185,224);
		tblExif.getControl().addListener(SWT.Dispose, e->bgColor.dispose());
		
		TableViewerColumn colTag = new TableViewerColumn(tblExif, SWT.NONE);
		colTag.getColumn().setText("Tag");
		colTag.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof String[]) return ((String[])element)[0];
				if (element instanceof String) return (String)element;
				return "";
			}
			@Override
			public Color getBackground(Object element) {
				if (element instanceof String) return bgColor;
				return null;
			}
		});
		
		
		TableViewerColumn colTagValue = new TableViewerColumn(tblExif, SWT.NONE);
		colTagValue.getColumn().setText("Value");
		colTagValue.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof String[]) return ((String[])element)[1];
				return "";
			}
			@Override
			public Color getBackground(Object element) {
				if (element instanceof String) return bgColor;
				return null;
			}
		});
		
		Composite lnkComp = toolkit.createComposite(stackComposite);
		lnkComp.setLayout(new GridLayout());
		
		
		FontData fd = lnkDetails.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(lnkDetails.getDisplay(), fd);
		Font normalFont = lnkDetails.getFont(); 
		lnkDetails.addListener(SWT.Dispose, e->boldFont.dispose());
		
		
		lnkDetails.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout)stackComposite.getLayout()).topControl = proxyDetailsComp;
				stackComposite.layout();
				lnkDetails.setFont(boldFont);
				lnkExif.setFont(normalFont);
				lnkXmp.setFont(normalFont);
				header.layout();
			}
		});
		lnkExif.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout)stackComposite.getLayout()).topControl = exifMetadataComp;
				stackComposite.layout();
				lnkDetails.setFont(normalFont);
				lnkExif.setFont(boldFont);
				lnkXmp.setFont(normalFont);
				header.layout();
			}
		});
		lnkXmp.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout)stackComposite.getLayout()).topControl = lnkComp;
				stackComposite.layout();
				lnkDetails.setFont(normalFont);
				lnkExif.setFont(normalFont);
				lnkXmp.setFont(boldFont);
				header.layout();
			}
		});
		((StackLayout)stackComposite.getLayout()).topControl = proxyDetailsComp;
		lnkDetails.setFont(boldFont);
		
		imageCanvas = new Canvas(detailsSash,SWT.BORDER);
		imageCanvas.addListener(SWT.Paint, e->{
			Image img = (Image)imageCanvas.getData("IMAGE");
			if (img == null || img.isDisposed()) return;
			
			Rectangle bounds = img.getBounds();
			Rectangle cbounds = imageCanvas.getBounds();	
			// scale image
			int x = 0, y = 0, width = 0, height = 0;
			if (cbounds.width > cbounds.height) {
				height = cbounds.height;
				width = bounds.width * height / bounds.height;
				x = (cbounds.width - width) / 2;
			} else {
				width = cbounds.width;
				height = bounds.height * width / bounds.width;
				y = (cbounds.height - height) / 2;
			}
			e.gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, x, y, width, height);
		});
		imageCanvas.addListener(SWT.Dispose, e->{
			Image img = (Image)imageCanvas.getData("IMAGE");
			if (img != null && img.isDisposed()) img.dispose();
		});
		imageCanvas.addListener(SWT.MouseDoubleClick, e->{
			final Path p = (Path)imageCanvas.getData("IMAGE_FILE");
			if (p == null) return;
			AttachmentUtil.launch(p.toFile());
		});
		detailsSash.setWeights(new int[] {3,2});
		
		fileDetailsComposite.layout(true);
		

		int cwidth = (tblExif.getTable().getBounds().width - 20)/2;
		colTag.getColumn().setWidth(cwidth);
		colTagValue.getColumn().setWidth(cwidth);
	}
	
	private List<FileProxy> getSelection(){
		List<FileProxy> proxies = new ArrayList<>();
		IStructuredSelection selection = (IStructuredSelection) tblResults.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof FileProxy) {
				proxies.add((FileProxy)type);
			}
		}
		return proxies;
	}
	
	/**
	 * Updates the selected objects to the given asset.  If asset is null user
	 * will be prompted to pick and asset
	 * @param asset
	 */
	private void setAsset( Asset asset ) {
		List<FileProxy> proxies = getSelection();
		int cnt = 0;
		for (FileProxy p : proxies) {
			if (p.getAsset() != null)cnt ++;
		}
		if (cnt > 0) {
			boolean n = MessageDialog.openQuestion(getSite().getShell(), "Warning", 
				MessageFormat.format("{0} of the {1} selected files already have assets associated with them.  These will be overwritten.  Are you sure you want to continue?", cnt, proxies.size()));
			if (!n) return;
		}
		
		if (asset == null) {
			StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(getSite().getShell(), Type.ASSET);
			ContextInjectionFactory.inject(dialog, context);
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
	 * Updates the selected objects to the given station location .  If the location is null the user
	 * will be prompted to pick the location
	 * 
	 * @param location
	 */
	private void setLocation( AssetStationLocation location ) {
		List<FileProxy> proxies = getSelection();
		int cnt = 0;
		for (FileProxy p : proxies) {
			if (p.getStationLocation() != null) cnt++;
		}
		if (cnt > 0) {
			boolean n = MessageDialog.openQuestion(getSite().getShell(), "Warning", 
				MessageFormat.format("{0} of the {1} selected files already have station locations associated with them.  These will be replaced overwritten if you process.  Are you sure you want to continue?", cnt, proxies.size()));
			if (!n) return;
		}
		if (location == null) {
			StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(getSite().getShell(), Type.LOCATION);
			ContextInjectionFactory.inject(dialog, context);
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
	
	private void ungroupSelected() {
		for (FileProxy p : getSelection()) {
			p.setFixedRelations(null);
		}
		refreshProxies();
	}
	
	private void groupSelected() {
		List<FileProxy> proxies = getSelection();
		
		for (FileProxy p : proxies) {
			p.setFixedRelations(proxies);
		}
		refreshProxies();
	}
	
	
	private void setDateTime() {
		List<FileProxy> proxies = getSelection();
		int cnt = 0;
		for (FileProxy p : proxies) {
			if (p.getImageDate() != null) {
				cnt ++;
			}
		}
		if (cnt > 0) {
			boolean n = MessageDialog.openQuestion(getSite().getShell(), "Warning", 
				MessageFormat.format("{0} of the {1} selected files already have dates associated with them.  These dates will be replaced with new selected date.  Are you sure you want to continue?", cnt, proxies.size()));
			if (!n) return;
		}
		
		StationAssetSelectionDialog dialog = new StationAssetSelectionDialog(getSite().getShell(), Type.DATE);
		ContextInjectionFactory.inject(dialog, context);
		if (dialog.open() == StationAssetSelectionDialog.CANCEL) return;
		
		proxies.forEach(p->p.setImageDate(dialog.getSelectedDate()));
		refreshProxies();
	}
	
	private void removeFile() {
		boolean n = MessageDialog.openQuestion(getSite().getShell(), "Warning", 
			"Are you sure you want to remove the selected file?  When removed, the files and associated observations will not be imported into SMART.");
		if (!n) return;
		
		List<FileProxy> files = getSelection();
		files.forEach(f->processor.removeFile(f));
		refreshProxies();
	}
	
	private void refreshProxies() {
		processor.update();
		tblResults.refresh();
		updateStatus();
		updateFileDetails();
		updateFileCount();
		updateFileDetails();
	}
	
	private void updateFileDetails() {
		itemDelete.setEnabled(!tblResults.getSelection().isEmpty());
		
		boolean canSave = !tblResults.getStructuredSelection().isEmpty();
		for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof FileProxy && !((FileProxy) item).isValid()) {
				canSave = false;
			}
		}
		itemSave.setEnabled(canSave);
		
		//clear existing
		if (proxyDetailsComp.isDisposed()) return;
		for (Control c : proxyDetailsComp.getChildren()) c.dispose();
		tblExif.setInput(null);
		
		Image lastImage = (Image) imageCanvas.getData("IMAGE");
		if (lastImage != null && !lastImage.isDisposed()) lastImage.dispose();
		imageCanvas.redraw();
				
		for (Control c : multiSelectDetails.getChildren()) c.dispose();
		
		if (tblResults.getStructuredSelection() == null || tblResults.getStructuredSelection().isEmpty()) {
			lblDetailsFileName.setText( "" );
			lblDetailsStatus.setImage( null );
			return;
		}
		
		if (tblResults.getStructuredSelection().size() > 1) {
			ScrolledComposite sc = new ScrolledComposite(multiSelectDetails, SWT.V_SCROLL);
			Composite details = toolkit.createComposite(sc);
			sc.setContent(details);
			sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			int cnt = 0;
			int size = sc.computeSize(SWT.DEFAULT, SWT.DEFAULT).x - sc.getVerticalBar().getSize().x;
			
			for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object item = iterator.next();
				if (!(item instanceof FileProxy)) continue;
				FileProxy proxy = (FileProxy)item;
				
				Canvas canvas = new Canvas(details, SWT.BORDER);
				toolkit.adapt(canvas);
				
				if (proxy.getWaypoint() != null) {
					int colorIndex = proxy.getWaypoint() % rowColors.length;
					canvas.setBackground(rowColors[colorIndex]);
				}
				canvas.setBounds(0, (cnt+2)*size, size, size);
				cnt++;
				
				canvas.setData("IMAGE_PROXY", proxy);
				canvas.addListener(SWT.Paint, e->{
					if (canvas.isDisposed()) return;
					Image img = (Image)canvas.getData("IMAGE");
					if (img == null || img.isDisposed()) return;
					// scale image
					Rectangle cbounds = canvas.getBounds();	
					Rectangle bounds = img.getBounds();
					int x = 0, y = 0, width = 0, height = 0;
					if (cbounds.width > cbounds.height) {
						height = cbounds.height;
						width = bounds.width * height / bounds.height;
						x = (cbounds.width - width) / 2;
					} else {
						width = cbounds.width;
						height = bounds.height * width / bounds.width;
						y = (cbounds.height - height) / 2;
					}
					e.gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, x, y, width, height);
				});
				String tooltip = proxy.getFile().getFileName().toString();
				if (proxy.getWaypoint() != null) {
					tooltip += "\n" + "Incident Group: " + proxy.getWaypoint();
				}
				canvas.setToolTipText(tooltip);
				canvas.addListener(SWT.Dispose, e->{
					Image img = (Image)canvas.getData("IMAGE");
					if (img != null && img.isDisposed()) img.dispose();
				});
				canvas.addListener(SWT.MouseDoubleClick, e->{
					final Path p = (Path)canvas.getData("IMAGE_FILE");
					if (p == null) return;
					AttachmentUtil.launch(p.toFile());
				});
				
				
			}
			details.setSize(size, cnt*size);
			sc.addListener(SWT.Resize, e->{
				int size2 = sc.getBounds().width - sc.getVerticalBar().getSize().x;
				int cnt2 = 0;
				for (Control c : details.getChildren()) {
					c.setBounds(0, cnt2 * (size2 + 2), size2, size2);
					cnt2++;
				}				
				details.setSize(size2, cnt2*(size2+2));
				sc.setMinSize(size2, cnt2 * (size2+2));
				sc.layout(true);
				details.layout(true);
			});
			
			//only load and display images
			//in the viewing range so we don't run
			//out of memory 
			Job refresimage = new Job("refresh images") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Display.getDefault().syncExec(()->{
						if (sc.isDisposed()) return;
						int min = Math.abs(details.getLocation().y);
						int max = min + sc.getBounds().height;
						for (Control c : details.getChildren()) {
							Image img = (Image) c.getData("IMAGE");
							int y = c.getLocation().y;
							int y2 = c.getLocation().y + c.getBounds().height;
							
							if ((y > min && y < max) || (y2>min && y2 < max)) {
								if (img != null) continue;	
								FileProxy proxy = (FileProxy)c.getData("IMAGE_PROXY");
								LoadImageJob imgjob = new LoadImageJob((Canvas)c);
								imgjob.setSystem(true);
								imgjob.schedule();
								
							}else {
								if (img != null) {
									img.dispose();
									c.setData("IMAGE", null);
								}
							}
						}
					});
					
					return Status.OK_STATUS;
				}
				
			};
			sc.getVerticalBar().addListener(SWT.Selection, e->refresimage.schedule(200));
			((StackLayout)fileDetailsComposite.getLayout()).topControl = multiSelectDetails;
			
			fileDetailsComposite.layout(true);
			multiSelectDetails.layout(true);
			refresimage.schedule();
			return;
		}
		
		((StackLayout)fileDetailsComposite.getLayout()).topControl = singleSelectDetails;
		fileDetailsComposite.layout();
		Object selection = tblResults.getStructuredSelection().getFirstElement();
		if (!(selection instanceof FileProxy)) {
			lblDetailsFileName.setText( "" );
			lblDetailsStatus.setImage( null );
			return;
		}
		
		FileProxy proxy = (FileProxy)selection;
		
		lblDetailsFileName.setText(proxy.getFile().getFileName().toString());
		lblDetailsStatus.setImage( AssetPlugIn.getDefault().getImageRegistry().get(  proxy.isValid() ? AssetPlugIn.ICON_IMPORT_COMPLETE : AssetPlugIn.ICON_IMPORT_INCOMPLETE));
		if (!proxy.isValid()) lblDetailsStatus.setToolTipText(proxy.validMessage());
		
		ScrolledComposite scroll = new ScrolledComposite(proxyDetailsComp, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		toolkit.adapt(scroll);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite bits = toolkit.createComposite(scroll);
		scroll.setContent(bits);
		bits.setLayout(new GridLayout());
		bits.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FontData fd = bits.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(bits.getDisplay(), fd);
		bits.addListener(SWT.Dispose, e->boldFont.dispose());
		
		Composite fileSection = toolkit.createComposite(bits);
		fileSection.setLayout(new GridLayout(2, false));
		fileSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = toolkit.createLabel(fileSection, "Summary");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		l.setFont(boldFont);
		
		if (!proxy.isValid()) {
			l = toolkit.createLabel(fileSection, "Status Details:");
			l = toolkit.createLabel(fileSection, proxy.validMessage());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
		l = toolkit.createLabel(fileSection, "Date/Time:");
		l = toolkit.createLabel(fileSection, proxy.getImageDate() == null ? "" : DateFormat.getDateTimeInstance().format(proxy.getImageDate()) );
		
		l = toolkit.createLabel(fileSection, "Asset:");
		l = toolkit.createLabel(fileSection, proxy.getAsset() == null ? "" : proxy.getAsset().getId() );
		
		l = toolkit.createLabel(fileSection, "Station:");
		l = toolkit.createLabel(fileSection, proxy.getStation() == null ? "" : proxy.getStation().getId() );
		
		l = toolkit.createLabel(fileSection, "Station Location:");
		l = toolkit.createLabel(fileSection, proxy.getStationLocation() == null ? "" : proxy.getStationLocation().getId() );
		
		l = toolkit.createLabel(fileSection, "Longitude:");
		l = toolkit.createLabel(fileSection, proxy.getX() == null ? "" : String.valueOf(proxy.getX()) );
		
		l = toolkit.createLabel(fileSection, "Latitude:");
		l = toolkit.createLabel(fileSection, proxy.getY() == null ? "" : String.valueOf(proxy.getY()) );
		
		Composite obsSection = toolkit.createComposite(bits);
		obsSection.setLayout(new GridLayout(2, false));
		obsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(obsSection, "Observations");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		l.setFont(boldFont);
		
		for (WaypointObservation wo : proxy.getObservations()) {
			l = toolkit.createLabel(obsSection, wo.getCategory().getFullCategoryName());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			for (WaypointObservationAttribute a : wo.getAttributes()) {
				l = toolkit.createLabel(obsSection, a.getAttribute().getName() + ":");
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).horizontalIndent = 10;
				
				l = toolkit.createLabel(obsSection, a.getAttributeValueAsString(Locale.getDefault()));
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
		}
		
		Composite warnSection = toolkit.createComposite(bits);
		warnSection.setLayout(new GridLayout());
		warnSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(warnSection, "Processing Warnings");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setFont(boldFont);
		
		for (ActionableWarning aw : proxy.getWarnings()) {
			l = toolkit.createLabel(warnSection, aw.getMessage());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).horizontalIndent = 10;
			
		}
		
		scroll.setMinSize(bits.computeSize(SWT.DEFAULT,  SWT.DEFAULT));
		proxyDetailsComp.layout(true);
		
		

		Job j2 = new Job("read exif metadata") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				HashMap<Directory, List<Tag>> exif = FileMetadataReader.readExifMetadata(proxy.getFile());
				
				Display.getDefault().syncExec(()->{
					if (tblExif.getTable().isDisposed()) return;
					if (exif == null) {
						tblExif.setInput(new String[] {"Error Reading EXIF Metadata"});
						return;
					}
					List<Object> values = new ArrayList<>();
					for (Entry<Directory, List<Tag>> item : exif.entrySet()) {
						values.add(item.getKey().getName());
						for (Tag t : item.getValue()) {
							values.add(new String[] {t.getTagName(), t.getDescription()});
						}
					}
					tblExif.setInput(values);
				});
				return Status.OK_STATUS;
			}
			
		};
		j2.schedule();
		
		imageCanvas.setData("IMAGE_PROXY", proxy);
		LoadImageJob imgLoader = new LoadImageJob(imageCanvas);
		imgLoader.setSystem(true);
		imgLoader.schedule();
		
		fileDetailsComposite.layout(true);
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
				}
				if (pmonitor.isCanceled()) {
//					Display.getDefault().syncExec(()->{createProcessComposite(true);});
					//TODO: cancelled
					return Status.CANCEL_STATUS;
				}
				Display.getDefault().syncExec(()->{createFileSummary();});
				return Status.OK_STATUS;
			}
			
		};
		processingJob.schedule();
	}
	
	
	
	@Override
	public void setFocus() {
	}

	
	private enum ResultsColumn{
		STATUS("Status"),
		FILE("File"),
		DATE("Date"),
		ASSET("Asset"),
		LOCATION("Station Location"),
		STATION("Station"),
		WAYPOINT("Incident Group");
		
		public String guiName;
		
		private ResultsColumn(String name) {
			this.guiName = name;
		}
		
		public String getValue(FileProxy proxy) {
			switch(this) {
			case ASSET:
				return proxy.getAsset() == null ? "" : proxy.getAsset().getId();
			case DATE:
				return proxy.getImageDate() == null ? "" : DateFormat.getDateTimeInstance().format(proxy.getImageDate());
			case FILE:
				return proxy.getFile().getFileName().toString();
			case LOCATION:
				return proxy.getStationLocation() == null ? "" : proxy.getStationLocation().getId();
			case WAYPOINT:
				if (proxy.getWaypoint() == null) return "";
				return proxy.getWaypoint().toString() + (proxy.isFixed() ? "*" : "");
			case STATION:
				return proxy.getStation() == null ? "" : proxy.getStation().getId();
			case STATUS:
				return proxy.isValid() ? "COMPLETE" : "INCOMPLETE";			
			}
			return "";
		}
		
		public ColumnLabelProvider getLabelProvider(Color[] rowColors) {
			return new ColumnLabelProvider() {
				@Override
				public Image getImage(Object element) {
					if (ResultsColumn.this == STATUS && element instanceof FileProxy) {
						if (((FileProxy) element).isValid()) return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_IMPORT_COMPLETE);
						return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_IMPORT_INCOMPLETE);
					}
					return null;
				}
				@Override
				public String getText(Object element) {
					if (element instanceof FileProxy) return getValue((FileProxy)element);
					return super.getText(element);
				}
				
				@Override
				public Color getBackground(Object element) {
					if (element instanceof FileProxy){
						if (((FileProxy) element).getWaypoint() == null) return null;
						int colorIndex = ((FileProxy) element).getWaypoint() % rowColors.length;
						return rowColors[colorIndex];
					}
					return null;
				}
			};
		}
		
		public int getWidth() {
			if (this == STATUS) return 22;
			return 100;
		}
	}
	
	private class LoadImageJob extends Job {
		
		Canvas toUpdate = null;
		FileProxy proxy = null;
		public LoadImageJob(Canvas toUpdate) {
			super("loading image job");
			this.toUpdate = toUpdate;
		}

		protected IStatus run(IProgressMonitor monitor) {
			
			try {
				Display.getDefault().syncExec(()->{
					proxy = (FileProxy) toUpdate.getData("IMAGE_PROXY");
					Image lastImage = (Image) toUpdate.getData("IMAGE");
					if (lastImage != null && !lastImage.isDisposed()) lastImage.dispose();
					toUpdate.setData("IMAGE", null);
				});
				if (proxy == null) return Status.OK_STATUS;
				
				Image img = new Image(toUpdate.getDisplay(), proxy.getFile().toString());
				Display.getDefault().syncExec(()->{
					if (toUpdate.isDisposed()) {
						img.dispose();
						return;
					}
					toUpdate.setData("IMAGE", img);
					toUpdate.redraw();
				});
			}catch (Exception ex) {
				//invalid format TODO:
				ex.printStackTrace();
			}
			return Status.OK_STATUS;
		}
		
	}
}
