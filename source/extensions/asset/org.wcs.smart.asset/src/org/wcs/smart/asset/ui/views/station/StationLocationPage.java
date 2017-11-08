package org.wcs.smart.asset.ui.views.station;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hibernate.Session;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.ApplicationGIS.DrawMapParameter;
import org.locationtech.udig.project.ui.commands.AbstractDrawCommand;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.locationtech.udig.ui.graphics.ViewportGraphics;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetCoreLabelProvider;
import org.wcs.smart.asset.AssetHibernateManager;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttributeValue;
import org.wcs.smart.asset.ui.StationLocationDialog;
import org.wcs.smart.asset.ui.map.StationLocationDrawCommand;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.properties.DialogConstants;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;

public class StationLocationPage {

	
	private StationEditor parentEditor;
	
	private TableViewer tblLocations;
	
	private List<AssetStationLocationAttribute> locationAttributes;
	
	private Composite infoPanel;
	private FormToolkit toolkit;
	
	private Composite mapComposite;
	private MapViewer mapViewer;
	
	
	private StationLocationDrawCommand drawCommand;
	
	@Inject
	private IEclipseContext context;
	
	public StationLocationPage(StationEditor editor) {
		this.parentEditor = editor;
	}
	
	public void createControl(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		
		SashForm topBottom = new SashForm(parent, SWT.VERTICAL);
		toolkit.adapt(topBottom);
		topBottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm topPanel = new SashForm(topBottom, SWT.HORIZONTAL);
		toolkit.adapt(topPanel);

		Composite topLeft = toolkit.createComposite(topPanel, SWT.BORDER);
		topLeft.setLayout(new GridLayout());
		((GridLayout)topLeft.getLayout()).marginWidth = 0;
		((GridLayout)topLeft.getLayout()).marginHeight = 0;
		createLocationsTable(topLeft);
		
		Composite topRight = toolkit.createComposite(topPanel, SWT.BORDER);
		topRight.setLayout(new GridLayout());
		((GridLayout)topRight.getLayout()).marginWidth = 0;
		((GridLayout)topRight.getLayout()).marginHeight = 0;
		createDetailsPanel (topRight);
		
		Composite bottomPanel = toolkit.createComposite(topBottom, SWT.BORDER);
		bottomPanel.setLayout(new GridLayout());
		((GridLayout)bottomPanel.getLayout()).marginWidth = 0;
		((GridLayout)bottomPanel.getLayout()).marginHeight = 0;
		createMapPanel(bottomPanel);
		
		topBottom.setWeights(new int[] {1,1});
		topPanel.setWeights(new int[] {3,2});
	}
	
	private void refreshMap() {
		getMapViewer().getRenderManager().refresh(null);
	}
	private void createLocationsTable(Composite parent) {
		tblLocations = new TableViewer(parent, SWT.FULL_SELECTION);
		tblLocations.setContentProvider(ArrayContentProvider.getInstance());
		tblLocations.getTable().setHeaderVisible(true);
		tblLocations.getTable().setLinesVisible(true);
		
		tblLocations.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblLocations.addSelectionChangedListener(e->updateDetailsPanel());
		tblLocations.addDoubleClickListener(e->openLocation());
		tblLocations.getControl().addListener(SWT.FocusOut, e-> {
			drawCommand.clearSelections();
			refreshMap();
		});
		
		Menu mnu = new Menu(tblLocations.getControl());
		
		MenuItem openItem = new MenuItem(mnu, SWT.PUSH);
		openItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON));
		openItem.setText("Goto Location");
		openItem.addListener(SWT.Selection, e->openLocation());


		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem addItem = new MenuItem(mnu, SWT.PUSH);
		addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addItem.setText(DialogConstants.ADD_BUTTON_TEXT);
		addItem.addListener(SWT.Selection, e->addLocation());

		
		MenuItem editItem = new MenuItem(mnu, SWT.PUSH);
		editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editItem.setText(DialogConstants.EDIT_BUTTON_TEXT);
		editItem.addListener(SWT.Selection, e->editLocationDetails());
		
		tblLocations.getControl().setMenu(mnu);
	}
	
	private void createDetailsPanel(Composite parent) {
		ScrolledComposite scroll = new ScrolledComposite(parent,  SWT.V_SCROLL | SWT.H_SCROLL);
		toolkit.adapt(scroll);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		infoPanel = toolkit.createComposite(scroll, SWT.NONE);
		scroll.setContent(infoPanel);
	}
	
	private void createMapPanel(Composite parent) {

		mapComposite = toolkit.createComposite(parent, SWT.BORDER);
		mapComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite toolComposite = toolkit.createComposite(mapComposite, SWT.NONE);
		toolComposite.setLayout(new GridLayout());
		((GridLayout)toolComposite.getLayout()).marginWidth = 2;
		((GridLayout)toolComposite.getLayout()).marginHeight = 2;

		MapToolComposite tools = new MapToolComposite(new String[] {MapToolComposite.UDIG_PAN_ID, MapToolComposite.UDIG_ZOOM_ID});
		tools.selectTool(MapToolComposite.UDIG_PAN_ID);
		tools.createComposite(toolComposite);
		
		mapViewer = new MapViewer(mapComposite, SWT.SINGLE | SWT.DOUBLE_BUFFERED);
		mapViewer.setMap(ProjectFactory.eINSTANCE.createMap());
		mapViewer.init(parentEditor);
		LoadDefaultLayersJob basemap = new LoadDefaultLayersJob(mapViewer.getMap(), false);
		basemap.schedule();
		
		drawCommand = new StationLocationDrawCommand(0, 0);
		
		mapViewer.getViewport().addDrawCommand(drawCommand);
		mapViewer.getViewport().enableDrawCommands(true);
		
		
		mapComposite.addListener(SWT.Resize, e->{
			mapViewer.getControl().setBounds(0, 0, mapComposite.getSize().x, mapComposite.getSize().y);
			org.eclipse.swt.graphics.Point size = toolComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			toolComposite.setBounds(mapComposite.getSize().x - size.x, 0, size.x, size.y);
		});
		
		//ApplicationGIS.getToolManager().setCurrentEditor(parentEditor);
	}
	
	
	
	public MapViewer getMapViewer() {
		return this.mapViewer;
	}
	
	private AssetStationLocationProxy getTableSelection() {
		Object x = ((IStructuredSelection)tblLocations.getSelection()).getFirstElement();
		if (x == null) return null;
		if (!(x instanceof AssetStationLocationProxy)) return null;
		AssetStationLocationProxy proxy = (AssetStationLocationProxy)x;
		return proxy;
	}
	
	private void openLocation() {
		AssetStationLocationProxy proxy = getTableSelection();
		if (proxy == null) return;
	
		//TODO:
		
	}
	
	private void editLocationDetails() {
		AssetStationLocationProxy proxy = getTableSelection();
		if (proxy == null) return;
		
		StationLocationDialog dialog = new StationLocationDialog(tblLocations.getControl().getShell(), proxy.data);
		ContextInjectionFactory.inject(dialog, context);
		dialog.open();
		tblLocations.refresh();
	}
	
	private void addLocation() {
		AssetStationLocation newLocation = new AssetStationLocation();
		newLocation.setStation(parentEditor.getAssetStation());
		
		StationLocationDialog dialog = new StationLocationDialog(tblLocations.getControl().getShell(), newLocation);
		ContextInjectionFactory.inject(dialog, context);
		if (dialog.open() == StationLocationDialog.OK) {
			initialize(parentEditor.getAssetStation());
		}
	}
	
	private void updateDetailsPanel() {
		for (Control c : infoPanel.getChildren()) c.dispose();
		
		AssetStationLocationProxy proxy = getTableSelection();
		if (proxy == null) {
			drawCommand.setLocationSelection(null);
			refreshMap();
			return;
		}
		drawCommand.setLocationSelection(Collections.singleton(proxy.data));
		refreshMap();
		
		infoPanel.setLayout(new GridLayout(2, false));
		
		Label l = toolkit.createLabel(infoPanel,  proxy.data.getId());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font f = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->f.dispose());
		l.setFont(f);
		
		for (int i = 0; i < tblLocations.getTable().getColumnCount(); i ++) {
			l = toolkit.createLabel(infoPanel,  tblLocations.getTable().getColumn(i).getText() + ":");
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
			l = toolkit.createLabel(infoPanel,  ((ColumnLabelProvider)tblLocations.getLabelProvider(i)).getText(proxy));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		infoPanel.layout();
		((ScrolledComposite)infoPanel.getParent()).setMinSize(infoPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		((ScrolledComposite)infoPanel.getParent()).layout(true);
		
	}
	
	public void initialize(AssetStation station) {
		Job j = new Job("initialize locations page") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				List<AssetStationLocationProxy> locationData = new ArrayList<>();
				
				try(Session session = HibernateManager.openSession()){
					initializeTableColumns(session);
					
					List<AssetStationLocation> data = QueryFactory.buildQuery(session, AssetStationLocation.class, new Object[] {"station", station}).list();
					
					for (AssetStationLocation l : data) {
						AssetStationLocationProxy proxy = new AssetStationLocationProxy();
						proxy.data = l;
						locationData.add(proxy);
						
						l.getId();
						
						Long activeCnt = QueryFactory.buildCountQuery(session, AssetDeployment.class, 
								new Object[] {"stationLocation", l},
								new Object[] {"endDate", null});
						proxy.status = activeCnt == 0 ? Asset.Status.INACTIVE : Asset.Status.ACTIVE; 
						
						for (AssetStationLocationAttributeValue value : l.getAttributeValues()) {
							value.getAttribute().getName();
							value.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
						}
					}
					
					//Get Station & Station Location Buffer
					double stationBuffer = AssetHibernateManager.getStationBuffer(session, SmartDB.getCurrentConservationArea());
					double locationBuffer = AssetHibernateManager.getStationLocationBuffer(session,  SmartDB.getCurrentConservationArea());
					drawCommand.setBuffers(stationBuffer, locationBuffer);
				}
				
				//TODO: CRS
				
				drawCommand.setStations(Collections.singletonList(parentEditor.getAssetStation()));
				drawCommand.setLocations(locationData.stream().map(e->e.data).collect(Collectors.toSet()));
				
				SetViewportBBoxCommand cmd = new SetViewportBBoxCommand(drawCommand.getBounds());
				getMapViewer().getMap().executeSyncWithoutUndo(cmd);
				
				Display.getDefault().syncExec(()->{
					if (tblLocations.getTable().isDisposed()) return;
					tblLocations.setInput(locationData);
				});
				
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
	}
	
	private void initializeTableColumns(Session session){
		
		Display.getDefault().syncExec(()->{
			if (tblLocations.getTable().isDisposed()) return;
			for (TableColumn c : tblLocations.getTable().getColumns()) c.dispose();
		});
		
		List<ITableColumn> columns = new ArrayList<>();

		columns.add(new StatusTableColumn());
		columns.add(new IdTableColumn());
		
		String hql = "FROM AssetStationLocationAttribute a WHERE a.attribute.conservationArea = :ca";
		locationAttributes = session.createQuery(hql).setParameter("ca",  SmartDB.getCurrentConservationArea()).list();
		for (AssetStationLocationAttribute a : locationAttributes) {
			a.getAttribute().getName();
			a.getAttribute().getUuid().equals(null);
			
			AttributeTableColumn c = new AttributeTableColumn(a.getAttribute());
			columns.add(c);
		}
		
		Display.getDefault().syncExec(()->{
			if (tblLocations.getTable().isDisposed()) return;
			for (int i = 0; i < columns.size(); i ++) {
				ITableColumn c = columns.get(i);
				TableViewerColumn viewerColumn = new TableViewerColumn(tblLocations, SWT.NONE);
				viewerColumn.getColumn().setText(c.getColumnName());
				viewerColumn.getColumn().setToolTipText(c.getColumnName());
				viewerColumn.getColumn().setWidth(i == 0 ? 24 : 100);
				viewerColumn.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return c.getColumnValue(element);
					}
					
					@Override
					public Image getImage(Object element) {
						return c.getImage(element);
					}
				});
			}
		});
	}
	
	
	private class IdTableColumn implements ITableColumn{
		@Override
		public String getColumnName() {
			return "Location";
		}

		@Override
		public String getColumnValue(Object element) {
			if (element instanceof AssetStationLocationProxy) return ((AssetStationLocationProxy) element).data.getId();
			return element.toString();
		}
	}
	
	private class StatusTableColumn implements ITableColumn{
		@Override
		public String getColumnName() {
			return "Status";
		}

		@Override
		public String getColumnValue(Object element) {
			if (element instanceof AssetStationLocationProxy) return ((AssetStationLocationProxy) element).status.getGuiName(Locale.getDefault());
			return element.toString();
		}
		
		@Override
		public Image getImage(Object element) {
			if (element instanceof AssetStationLocationProxy) return AssetCoreLabelProvider.getStatusImage(((AssetStationLocationProxy) element).status);
			return null;
		}
	}
	
	private class AttributeTableColumn implements ITableColumn{
		AssetAttribute attribute;
		
		public AttributeTableColumn (AssetAttribute attribute){
			this.attribute = attribute;
		}
		@Override
		public String getColumnName() {
			return attribute.getName();
		}

		@Override
		public String getColumnValue(Object element) {
			if (element instanceof AssetStationLocationProxy) {
				AssetStationLocation l = ((AssetStationLocationProxy) element).data;
				for (AssetStationLocationAttributeValue value : l.getAttributeValues()) {
					if (value.getAttribute().equals(attribute)) return value.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS); //TODO:
				}
				return "";
			}
			return element.toString();
		}
	}
	
	private class AssetStationLocationProxy{
		AssetStationLocation data;
		Asset.Status status;
	}
}
