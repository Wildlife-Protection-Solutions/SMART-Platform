/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.asset;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.AssetSecurityManager;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.StationManager;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.ui.AssetLabelProvider;
import org.wcs.smart.asset.ui.SectionHeader;
import org.wcs.smart.asset.ui.StationDialog;
import org.wcs.smart.asset.ui.StationLocationDialog;
import org.wcs.smart.asset.ui.handler.DeleteAssetHandler;
import org.wcs.smart.asset.ui.handler.ImportAssetDataHandler;
import org.wcs.smart.asset.ui.handler.NewAssetHandler;
import org.wcs.smart.asset.ui.handler.OpenAssetHandler;
import org.wcs.smart.asset.ui.handler.OpenStationHandler;
import org.wcs.smart.asset.ui.handler.OpenStationLocationHandler;
import org.wcs.smart.asset.ui.handler.ShowAssetOverviewMapHandler;
import org.wcs.smart.asset.ui.views.station.StationEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

/**
 * Lists all assets and stations
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class AssetListView {
	
	public static final String ID = "org.wcs.smart.asset.view.assets"; //$NON-NLS-1$
	
	@Inject
	private IEclipseContext context;
	
	private Composite assetComposite;
	private Composite stationComposite;
	private Composite content;
	
	private TreeViewer lstAssets;
	private TreeViewer lstStations;
	private FormToolkit toolkit;
	
	private boolean includeRetired = false;
	private boolean includeInactiveAssets = true;
	private boolean includeInactiveStations = true;
	
	public AssetListView() {
		super();
	}
	
	private Composite toolbarHeaderComposite;
	
	@PostConstruct
	public void createPartControl(final Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite headerMain = toolkit.createComposite(main, SWT.NONE);
		headerMain.setLayout(new GridLayout(2, false));
		headerMain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerMain.getLayout()).marginWidth = 0;
		((GridLayout)headerMain.getLayout()).marginHeight = 0;
		
		SectionHeader header = new SectionHeader(headerMain, SWT.NONE,
				new String[] {"Assets", "Stations"},
				new Listener[] {
						e->{
							if (assetComposite == null) assetComposite = createAssetsPanel(content);
							((StackLayout)content.getLayout()).topControl = assetComposite;
							content.layout(true);
							
							for (Control c : toolbarHeaderComposite.getChildren()) c.dispose();
							createAssetToolbar(toolbarHeaderComposite);
							toolbarHeaderComposite.layout(true);
						},
						e->{
							if (stationComposite == null) stationComposite = createStationsPanel(content);
							((StackLayout)content.getLayout()).topControl = stationComposite;
							content.layout(true);
							
							for (Control c : toolbarHeaderComposite.getChildren()) c.dispose();
							createStationsToolbar(toolbarHeaderComposite);
							toolbarHeaderComposite.layout(true);
						}
				}, toolkit);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolbarHeaderComposite = toolkit.createComposite(headerMain, SWT.NONE);
		toolbarHeaderComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		toolbarHeaderComposite.setLayout(new GridLayout());
		((GridLayout)toolbarHeaderComposite.getLayout()).marginWidth = 0;
		((GridLayout)toolbarHeaderComposite.getLayout()).marginHeight = 0;
		
		content = toolkit.createComposite(main, SWT.NONE);
		content.setLayout(new StackLayout());
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//create asset panel
		header.selectPanel(0);
	}

	private Composite createAssetsPanel(Composite parent) {
		Composite assetPanel = toolkit.createComposite(parent);
		assetPanel.setLayout(new GridLayout());
		((GridLayout)assetPanel.getLayout()).marginWidth = 0;
		((GridLayout)assetPanel.getLayout()).marginHeight = 0;
		
		lstAssets = new TreeViewer(assetPanel, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		toolkit.adapt(lstAssets.getControl(), true, true);
		lstAssets.setLabelProvider(new AssetLabelProvider());
		lstAssets.setContentProvider(new MappingContentProvider<AssetType, Asset>((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase())));
		lstAssets.setInput(new String[] {DialogConstants.LOADING_TEXT});
		lstAssets.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstAssets.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openAsset();
			}
		});
		createAssetMenu(lstAssets.getControl());
		
		loadAssets();
		
		return assetPanel;
	}
	
	private void createAssetToolbar(Composite parent) {
		ToolBar toolbar =new ToolBar(parent, SWT.FLAT);
		
		ToolItem overviewMap = new ToolItem(toolbar, SWT.PUSH);
		overviewMap.setToolTipText("view overview map");
		overviewMap.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
		overviewMap.addListener(SWT.Selection, e->showOverviewMap());
		
		if (AssetSecurityManager.INSTANCE.canImportData()) {
			ToolItem importData = new ToolItem(toolbar, SWT.PUSH);
			importData.setToolTipText("import asset data");
			importData.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_IMPORT));
			importData.addListener(SWT.Selection, e->importData());
		}
		
		if (AssetSecurityManager.INSTANCE.canDeleteAsset() || AssetSecurityManager.INSTANCE.canCreateAsset()) {
			new ToolItem(toolbar, SWT.SEPARATOR);
		}
		
		if (AssetSecurityManager.INSTANCE.canDeleteAsset() ) {
			ToolItem deleteAsset = new ToolItem(toolbar, SWT.PUSH);
			deleteAsset.setToolTipText("delete selected assets");
			deleteAsset.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			deleteAsset.addListener(SWT.Selection, e->deleteAssets());
			
			lstAssets.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (deleteAsset.isDisposed()) return;
					deleteAsset.setEnabled(!lstAssets.getSelection().isEmpty());
				}
			});
		}
		
		if (AssetSecurityManager.INSTANCE.canCreateAsset()) {
			ToolItem addAsset = new ToolItem(toolbar, SWT.PUSH);
			addAsset.setToolTipText("create a new asset");
			addAsset.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			addAsset.addListener(SWT.Selection, e->createNewAsset(null));
		}
		
		toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
	}
	
	private void showOverviewMap() {
		(new ShowAssetOverviewMapHandler()).execute(context);
	}
	
	private void createStationsToolbar(Composite parent) {
		ToolBar toolbar =new ToolBar(parent, SWT.FLAT);
		
		ToolItem overviewMap = new ToolItem(toolbar, SWT.PUSH);
		overviewMap.setToolTipText("view overview map");
		overviewMap.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
		overviewMap.addListener(SWT.Selection, e->showOverviewMap());
		
		if (AssetSecurityManager.INSTANCE.canImportData()) {
			ToolItem importData = new ToolItem(toolbar, SWT.PUSH);
			importData.setToolTipText("import asset data");
			importData.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_IMPORT));
			importData.addListener(SWT.Selection, e->importData());
		}
		
		if (AssetSecurityManager.INSTANCE.canDeleteStationLocation() ||
				AssetSecurityManager.INSTANCE.canCreateStationLocation()) {
			new ToolItem(toolbar, SWT.SEPARATOR);
		}
		
		if (AssetSecurityManager.INSTANCE.canDeleteStationLocation()) {
			ToolItem deleteStation = new ToolItem(toolbar, SWT.PUSH);
			deleteStation.setToolTipText("delete the selected station and all related data");
			deleteStation.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			deleteStation.addListener(SWT.Selection, e->deleteStations());
			
			lstStations.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (deleteStation.isDisposed()) return;
					deleteStation.setEnabled(!lstStations.getSelection().isEmpty());
				}
			});
		}
		
		if (AssetSecurityManager.INSTANCE.canCreateStationLocation()) {
			ToolItem addStation = new ToolItem(toolbar, SWT.PUSH);
			addStation.setToolTipText("create a new station");
			addStation.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			addStation.addListener(SWT.Selection, e->createNewStation());
		}
		toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
	}
	
	private void importData() {
		(new ImportAssetDataHandler()).execute(context);
	}
	
	private Composite createStationsPanel(Composite parent) {
		Composite stationsPanel = toolkit.createComposite(parent);
		stationsPanel.setLayout(new GridLayout());
		((GridLayout)stationsPanel.getLayout()).marginWidth = 0;
		((GridLayout)stationsPanel.getLayout()).marginHeight = 0;
		
		lstStations = new TreeViewer(stationsPanel, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
		toolkit.adapt(lstStations.getControl(), true, true);
		lstStations.setLabelProvider(new AssetLabelProvider());
		lstStations.setContentProvider(new MappingContentProvider<AssetStation, AssetStationLocation>((a,b)->Collator.getInstance().compare(a.getId().toLowerCase(), b.getId().toLowerCase())));
		lstStations.setInput(new String[] {DialogConstants.LOADING_TEXT});
		lstStations.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstStations.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openStation();
			}
		});
		createStationMenu(lstStations.getControl());
		loadStations();
		
		return stationsPanel;
	}
	
	private void createAssetMenu(Control control) {
		Menu mnu = new Menu(control);
		control.setMenu(mnu);
		
		if (AssetSecurityManager.INSTANCE.canCreateAsset()) {
			MenuItem addAsset = new MenuItem(mnu, SWT.CASCADE);
			addAsset.setText("New Asset...");
			addAsset.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		
			new MenuItem(mnu, SWT.SEPARATOR);
			
			Menu addAssetMenu = new Menu(addAsset);
			addAsset.setMenu(addAssetMenu);
			new MenuItem(addAssetMenu, SWT.SEPARATOR);
			
			MenuItem otherAssetType = new MenuItem(addAssetMenu, SWT.PUSH);
			otherAssetType.setText("Other...");
			otherAssetType.addListener(SWT.Selection, evt->(new NewAssetHandler()).execute(context));
			
			addAssetMenu.addMenuListener(new MenuListener() {
				
				@Override
				public void menuShown(MenuEvent e) {
					String newTypes = ConfigurationScope.INSTANCE.getNode(NewAssetHandler.NEW_TYPE_OPTIONS_NODE).get(NewAssetHandler.NEW_TYPE_OPTIONS_KEY, null);
					if (newTypes == null || newTypes.isEmpty()) return;
					String[] bits = newTypes.split(NewAssetHandler.OPTION_SEP);
					List<AssetType> menuItems = new ArrayList<>();
					try(Session session = HibernateManager.openSession()){
						for (int i = 0; i < bits.length; i ++) {
							String uuid = bits[i];
							try {
								UUID assetTypeUuid = UuidUtils.stringToUuid(uuid);
								if (assetTypeUuid != null) {
									AssetType assetType = session.get(AssetType.class, assetTypeUuid);
									if (assetType != null) {
										assetType.getName();
										menuItems.add(assetType);
									}
								}
								
							}catch (Exception ex) {
								
							}
						}
					}
					for (MenuItem mi : addAssetMenu.getItems()) {
						if (mi.getData() != null) mi.dispose();
					}
					for (int i = menuItems.size() - 1; i >= 0; i --) {
						MenuItem addAssetType = new MenuItem(addAssetMenu, SWT.PUSH, 0);
						addAssetType.setText(menuItems.get(i).getName());
						addAssetType.setData(menuItems.get(i).getUuid());
						final UUID assetTypeUuid = menuItems.get(i).getUuid();
						addAssetType.addListener(SWT.Selection, evt->createNewAsset(assetTypeUuid));
					}
				}
				
				@Override
				public void menuHidden(MenuEvent e) {}
			});
		}
		
		MenuItem openAsset = new MenuItem(mnu, SWT.PUSH);
		openAsset.setText("Open");
		openAsset.addListener(SWT.Selection, e->openAsset());
		
		MenuItem refresh = new MenuItem(mnu, SWT.PUSH);
		refresh.setText("Refresh");
		refresh.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		refresh.addListener(SWT.Selection, e->loadAssets());
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem inactive = new MenuItem(mnu, SWT.CHECK);
		inactive.setText("Show Inactive Assets");	
		inactive.setSelection(this.includeInactiveAssets);
		inactive.addListener(SWT.Selection, e->{
			this.includeInactiveAssets = inactive.getSelection();
			loadAssets();
		});
		
		MenuItem retired = new MenuItem(mnu, SWT.CHECK);
		retired.setText("Show Retired Assets");	
		retired.addListener(SWT.Selection, e->{
			this.includeRetired = retired.getSelection();
			loadAssets();
		});
		
		if (AssetSecurityManager.INSTANCE.canDeleteAsset()) {
			new MenuItem(mnu, SWT.SEPARATOR);
			
			MenuItem deleteAsset = new MenuItem(mnu, SWT.PUSH);
			deleteAsset.setText(DialogConstants.DELETE_BUTTON_TEXT);
			deleteAsset.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			deleteAsset.addListener(SWT.Selection, e->{
				deleteAssets();
			});
		}
		
		
	}
	
	private void createStationMenu(Control control) {
		Menu mnu = new Menu(control);
		control.setMenu(mnu);
		
		if (AssetSecurityManager.INSTANCE.canCreateStationLocation()) {
			MenuItem newItem = new MenuItem(mnu, SWT.CASCADE);
			newItem.setText("New ...");
			newItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			
			Menu addMenu = new Menu(newItem);
			newItem.setMenu(addMenu);
		
			MenuItem newStation= new MenuItem(addMenu, SWT.PUSH);
			newStation.setText("Station");
			newStation.addListener(SWT.Selection, e->createNewStation());
			
			MenuItem newLocation= new MenuItem(addMenu, SWT.PUSH);
			newLocation.setText("Station Location");
			newLocation.addListener(SWT.Selection, e->createNewStationLocation());
		
		
			new MenuItem(mnu, SWT.SEPARATOR);
		}
		
		MenuItem openAsset = new MenuItem(mnu, SWT.PUSH);
		openAsset.setText("Open");
		openAsset.addListener(SWT.Selection, e->openStation());
		
		MenuItem refresh = new MenuItem(mnu, SWT.PUSH);
		refresh.setText("Refresh");
		refresh.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		refresh.addListener(SWT.Selection, e->loadStations());
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem inactive = new MenuItem(mnu, SWT.CHECK);
		inactive.setText("Show Inactive Stations");	
		inactive.setSelection(this.includeInactiveStations);
		inactive.addListener(SWT.Selection, e->{
			this.includeInactiveStations = inactive.getSelection();
			loadStations();
		});
		
		if (AssetSecurityManager.INSTANCE.canDeleteStationLocation()) {
			new MenuItem(mnu, SWT.SEPARATOR);
			
			MenuItem deleteStation = new MenuItem(mnu, SWT.PUSH);
			deleteStation.setText(DialogConstants.DELETE_BUTTON_TEXT);
			deleteStation.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			deleteStation.addListener(SWT.Selection, e->deleteStations());
		}
	}
	
	/**
	 * Creates new asset of specific type; can be null if type is not selected
	 * @param assetTypeUuid asset type or null if unknown
	 */
	private void createNewAsset(UUID assetTypeUuid) {
		(new NewAssetHandler()).execute(assetTypeUuid, context);
	}
	
	private void openAsset() {
		IStructuredSelection selection = (IStructuredSelection)lstAssets.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof Asset) (new OpenAssetHandler()).openAsset((Asset)type);
			
		}
	}
	
	private void openStation() {
		IStructuredSelection selection = (IStructuredSelection)lstStations.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object station = (Object) iterator.next();
			if (station instanceof AssetStation) openStation((AssetStation)station);
			if (station instanceof AssetStationLocation) (new OpenStationLocationHandler()).openStationLocation(((AssetStationLocation)station));
		}
	}
	
	private void openStation(AssetStation station) {
		IEclipseContext ctx = context.createChild();
		ctx.set(OpenStationHandler.STATION_PARAM, new StationEditorInput(station.getUuid(), station.getId()));
		ContextInjectionFactory.invoke(new OpenStationHandler(), Execute.class, ctx);
	}
	
	private void createNewStation() {
		AssetStation newStation = new AssetStation();
		newStation.setConservationArea(SmartDB.getCurrentConservationArea());
		
		StationDialog dialog = new StationDialog(context.get(Shell.class), newStation);
		ContextInjectionFactory.inject(dialog, context);
		dialog.open();
		if (newStation.getUuid() != null) {
			openStation(newStation);
		}
	}
	
	private void createNewStationLocation() {
		AssetStationLocation newLocation = new AssetStationLocation();	
		Object selection = ((IStructuredSelection)lstStations.getSelection()).getFirstElement();
		if (selection instanceof AssetStation) {
			newLocation.setStation((AssetStation)selection);
		}else if (selection instanceof AssetStationLocation) {
			newLocation.setStation(((AssetStationLocation)selection).getStation());
		}
		
		StationLocationDialog dialog = new StationLocationDialog(context.get(Shell.class), newLocation);
		ContextInjectionFactory.inject(dialog, context);
		dialog.open();
		if (newLocation.getUuid() != null) {
			(new OpenStationLocationHandler()).openStationLocation(newLocation);
		}
	}
	
	private void deleteAssets() {
		List<UUID> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = ((IStructuredSelection)lstAssets.getSelection()).iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof Asset) {
				toDelete.add(((Asset) item).getUuid());
			}
		}
		ContextInjectionFactory.make(DeleteAssetHandler.class, context).deleteAsset(toDelete);
	}
	
	private void deleteStations() {
		//confirm password
		Object s = ((IStructuredSelection)lstStations.getSelection()).getFirstElement();
		if (s instanceof AssetStation) {
		
			if (!MessageDialog.openQuestion(context.get(Shell.class), "Delete Station", 
					MessageFormat.format("Are you sure you want to delete the station {0}?  All data (images, waypoints, observations) will also be deleted", ((AssetStation)s).getId()))){
				return;
			}
		}else if (s instanceof AssetStationLocation) {
			if (!MessageDialog.openQuestion(context.get(Shell.class), "Delete Station Location", 
					MessageFormat.format("Are you sure you want to delete the station location {0}?  All data (images, waypoints, observations) will also be deleted", ((AssetStationLocation)s).getId()))){
				return;
			}
		}else {
			return;
		}
		
		if (!AssetUtils.confirmPassword(context.get(Shell.class), "Delete", "Confirm your password to delete the station or location and associated data.")) {
			return;
		}
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(context.get(Shell.class));
		try {
			pmd.run(true,  false,  new IRunnableWithProgress() {
	
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Deleting selected items.", IProgressMonitor.UNKNOWN);
					if (s instanceof AssetStation) {
						StationManager.INSTANCE.deleteStation((AssetStation)s, context.get(IEventBroker.class));
					}else if (s instanceof AssetStationLocation) {
						StationManager.INSTANCE.deleteStationLocation((AssetStationLocation)s, context.get(IEventBroker.class));
					}
					monitor.done();
				}
				
			});
		}catch (Exception ex) {
			AssetPlugIn.displayLog("Error deleting selected item.  Refresh and try again: " + ex.getMessage(), ex);
		}
	}
	
	@Optional
	@Inject
	private void dbModified(@UIEventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		loadAssets(250);
		loadStations(250);
	}
	
	@Optional
	@Inject
	public void assetsModified(@UIEventTopic(AssetEvents.ASSET_ALL) Object payLoad) {
		loadAssets(250);
	}
	
	@Optional
	@Inject
	public void stationsModified(@UIEventTopic(AssetEvents.ASSETSTATION_ALL) Object payLoad) {
		loadStations(250);
	}
	
	@Optional
	@Inject
	public void deploymentsModified(@UIEventTopic(AssetEvents.ASSETDEPLOYMENT_ALL) Object payLoad) {
		loadStations(250);
		loadAssets(250);
	}
	
	@Optional
	@Inject
	public void stationlocationModified(@UIEventTopic(AssetEvents.ASSETSTATIONLOCATION_ALL) Object payLoad) {
		loadStations(250);
	}
	
	@Optional
	@Inject
	public void dataModified(@UIEventTopic(AssetEvents.ASSETDATA) Object payLoad) {
		loadStations(250);
		loadAssets(250);
	}
	
	@Focus
	public void setFocus() {
	}

	@PreDestroy
	public void dispose() {
		toolkit.dispose();
	}
	
	public static class AssetListViewWrapper extends DIViewPart<AssetListView>{
		public AssetListViewWrapper() {
			super(AssetListView.class);
		}
	}

	private void loadAssets() {
		loadAssets(0);
	}
	private void loadAssets(int delay) {
		loadAssetsJob.cancel();
		loadAssetsJob.setSystem(true);
		loadAssetsJob.schedule(delay);
	}


	private void loadStations() {
		loadStations(0);
	}
	private void loadStations(int delay) {
		loadStationsJob.cancel();
		loadStationsJob.setSystem(true);
		loadStationsJob.schedule(delay);
	}

	
	private Job loadAssetsJob = new Job("loading assets") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Asset> assets = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				Object[][] filters = null;
				if (includeRetired){
					filters = new Object[][] {
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}
					};
				}else {
					filters = new Object[][] {
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"isRetired", false}
					};
				}
				assets.addAll(QueryFactory.buildQuery(session, Asset.class, filters).list()); 
				assets.forEach(a->{a.getAssetType().getUuid().equals(null); a.getAssetType().getName();a.computeStatus(session);});
			}
			if(monitor.isCanceled()) return Status.CANCEL_STATUS;
			
			if (!includeInactiveAssets) {
				for (Iterator<Asset> iterator = assets.iterator(); iterator.hasNext();) {
					Asset asset = iterator.next();
					if (asset.getCachedStatus() == Asset.Status.INACTIVE) {
						iterator.remove();
					}
				}
			}
			
			HashMap<AssetType, List<Asset>> mappings = new HashMap<>();
			assets.forEach(a->{
				List<Asset> list = mappings.get(a.getAssetType());
				if (list == null) {
					list = new ArrayList<>();
					mappings.put(a.getAssetType(), list);
				}
				list.add(a);
			});
			for (List<Asset> allassets : mappings.values()) allassets.sort((a,b)->Collator.getInstance().compare(a.getId(), b.getId()));
			Display.getDefault().syncExec(()->{
				if (lstAssets != null && !lstAssets.getControl().isDisposed()){
					lstAssets.setInput(mappings);
					lstAssets.expandAll();
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private Job loadStationsJob = new Job("loading stations") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<AssetStation> stations = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				stations.addAll(QueryFactory.buildQuery(session, AssetStation.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
				stations.forEach(a->{a.getUuid().equals(null); a.getId(); a.computeStatus(session);});
				stations.forEach(a->a.getLocations().forEach(l-> { l.getId(); l.computeStatus(session); }));
				
			}
			if (!includeInactiveStations) {
				for (Iterator<AssetStation> iterator = stations.iterator(); iterator.hasNext();) {
					AssetStation station = iterator.next();
					if (station.getCachedStatus() == Asset.Status.INACTIVE) {
						iterator.remove();
					}
				}
			}
			
			if(monitor.isCanceled()) return Status.CANCEL_STATUS;
			HashMap<AssetStation, List<AssetStationLocation>> mappings = new HashMap<>();
			stations.forEach(s->mappings.put(s, s.getLocations()));			
			for (List<AssetStationLocation> allassets : mappings.values()) allassets.sort((a,b)->Collator.getInstance().compare(a.getId(), b.getId()));
			Display.getDefault().syncExec(()->{
				if (lstStations != null && !lstStations.getControl().isDisposed()){
					lstStations.setInput(mappings);
					lstStations.expandAll();
					lstStations.refresh();
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private class MappingContentProvider<T,V> implements ITreeContentProvider{

		private HashMap<T, List<V>> mapping = new HashMap<>();
		
		private Comparator<T> sorter;
		
		public MappingContentProvider(Comparator<T> keySorter) {
			this.sorter = keySorter;
		}
		
		@Override
		public Object[] getElements(Object inputElement) {
			List<T> types = new ArrayList<>(mapping.keySet());
			types.sort(sorter);
			return types.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return mapping.get(parentElement).toArray();
		}

		@Override
		public Object getParent(Object element) {
			for (Entry<T,List<V>> kid : mapping.entrySet()) {
				if (kid.getValue().contains(element)) return kid.getKey();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (mapping.containsKey(element)) return true;
			return false;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			mapping = new HashMap<>();
			if (newInput instanceof HashMap) {
				mapping = (HashMap<T, List<V>>) newInput;
			}
		}
	}
}