package org.wcs.smart.asset.ui.views.station;

import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.wcs.smart.asset.AssetHibernateManager;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.map.StationLocationDrawCommand;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.properties.DialogConstants;

public class StationCurrentPage {

	@Inject
	private IEclipseContext parentContext;
	private StationEditor parentEditor;
	
	private Composite mainControl;
	private Composite mapComposite;
	private MapViewer mapViewer;
	
	private FormToolkit toolkit;
	
	private TableViewer tblCnts;
	private TableViewer tblAssetDeployments;
	
	private Composite statDetailsSection;
	private Composite bottomPart;
	
	private SashForm currentForm;

	private StationLocationDrawCommand drawCommand;
	
	public StationCurrentPage(StationEditor parent) {
		this.parentEditor = parent;
	}
	
	public Composite createControl(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		
		mainControl = toolkit.createComposite(parent, SWT.NONE);
		mainControl.setLayout(new GridLayout());
		mainControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)mainControl.getLayout()).marginWidth = 0;
		((GridLayout)mainControl.getLayout()).marginHeight = 0;
		
		return mainControl;
	}
	
	
	public void initializePanel(AssetStation station) {
		initializePanelJob.schedule();
	}
	
	private Job initializePanelJob = new Job("initialize current panel") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			drawCommand = new StationLocationDrawCommand();
			final List<AssetDeployment> currentDeployments = new ArrayList<>();
			try(Session s = HibernateManager.openSession()){
				String hql = "SELECT d FROM AssetDeployment d join d.stationLocation l WHERE l.station = :station and d.endDate is null";
				currentDeployments.addAll( s.createQuery(hql).setParameter("station",  parentEditor.getAssetStation()).list() );
				currentDeployments.forEach(d->{
					d.getAsset().getId();
					d.getAsset().getAssetType().getName();
					d.getStationLocation().getId();
					d.getStationLocation().getStation().getId();
					
					d.getAttributeValues().forEach(a->{
						a.getAttribute().getName();
						a.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
					});
				});
				
				drawCommand.setBuffers(AssetHibernateManager.getStationBuffer(s, SmartDB.getCurrentConservationArea()), AssetHibernateManager.getStationLocationBuffer(s, SmartDB.getCurrentConservationArea()));
			}
			if (currentDeployments.isEmpty()) {
				Display.getDefault().syncExec(()->createNotActivePanel());
				
			}else {
				Display.getDefault().syncExec(()->createDeploymentPanel(currentDeployments));
				initializeDeploymentPanel();
			}
			return Status.OK_STATUS;
		}
		
	};
	
	public MapViewer getMapViewer() {
		return this.mapViewer;
	}
	
	private void createDeploymentPanel(List<AssetDeployment> currentDeployments) {
		if (mainControl == null) return;
		
		for (Control c : mainControl.getChildren()) c.dispose();
		
		SashForm topBottom = new SashForm(mainControl, SWT.VERTICAL);
		currentForm = topBottom;
		topBottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm topLeftRight = new SashForm(topBottom, SWT.HORIZONTAL);
		
		Composite topLeft = toolkit.createComposite(topLeftRight, SWT.BORDER);
		topLeft.setLayout(new GridLayout(2, false));

		Label l = toolkit.createLabel(topLeft, "Summary");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->boldFont.dispose());
		l.setFont(boldFont);
		
		l = toolkit.createLabel(topLeft, MessageFormat.format("Number of Assets Currently Deployed: {0}",currentDeployments.size()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		Composite mapPart = toolkit.createComposite(topLeftRight, SWT.BORDER);
		
		topLeftRight.setWeights(new int[] {60,40});
	
		statDetailsSection = toolkit.createComposite(topLeft, SWT.NONE);
		statDetailsSection.setLayout(new GridLayout(2, false));
		statDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
	
		tblCnts = new TableViewer(topLeft, SWT.FULL_SELECTION);
		tblCnts.getTable().setHeaderVisible(true);
		tblCnts.getTable().setLinesVisible(true);
		tblCnts.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		tblCnts.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn categoryColumn = new TableViewerColumn(tblCnts, SWT.NONE);
		categoryColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Object[]) {
					return ((Category)((Object[])element)[0]).getFullCategoryName();
				}
				return super.getText(element);
			}
		});
		categoryColumn.getColumn().setText("Category");
		categoryColumn.getColumn().setResizable(true);
		categoryColumn.getColumn().setWidth(200);
		
		TableViewerColumn cntColumn = new TableViewerColumn(tblCnts, SWT.NONE);
		cntColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Object[]) {
					return String.valueOf(((Long)((Object[])element)[1]));
				}
				return super.getText(element);
			}
		});
		cntColumn.getColumn().setText("Number of Incidents");
		cntColumn.getColumn().setResizable(true);
		cntColumn.getColumn().setWidth(200);
		tblCnts.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		mapPart.setLayout(new GridLayout(2, false));
		
		mapComposite = toolkit.createComposite(mapPart, SWT.BORDER);
		mapComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

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
		
		
		mapViewer.getViewport().addDrawCommand(drawCommand);
		mapViewer.getViewport().enableDrawCommands(true);
		
		mapComposite.addListener(SWT.Resize, e->{
			mapViewer.getControl().setBounds(0, 0, mapComposite.getSize().x, mapComposite.getSize().y);
			org.eclipse.swt.graphics.Point size = toolComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			toolComposite.setBounds(mapComposite.getSize().x - size.x, 0, size.x, size.y);
		});
		
		bottomPart = toolkit.createComposite(topBottom, SWT.BORDER);
		bottomPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		bottomPart.setLayout(new GridLayout(2, false));
		
		topBottom.setWeights(new int[] {70, 30});
		
		tblAssetDeployments = new TableViewer(bottomPart, SWT.FULL_SELECTION);
		tblAssetDeployments.getTable().setHeaderVisible(true);
		tblAssetDeployments.getTable().setLinesVisible(true);
		tblAssetDeployments.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		tblAssetDeployments.setContentProvider(ArrayContentProvider.getInstance());
		
		Set<AssetAttribute> attributeColumns = new HashSet<>();
		Set<AssetStationLocation> locations = new HashSet<>();
		currentDeployments.stream().forEach(d->{
			d.getAttributeValues().forEach(v->attributeColumns.add(v.getAttribute()));
			locations.add(d.getStationLocation());
		});
		
		for (Column c : Column.values()) {
			TableViewerColumn tc = new TableViewerColumn(tblAssetDeployments, SWT.NONE);
			tc.getColumn().setText(c.guiName);
			tc.getColumn().setToolTipText(c.guiName);
			tc.getColumn().setWidth( 100 );
			tc.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof AssetDeployment) {
						return c.getText((AssetDeployment) element);
					}
					return super.getText(element);
				}
			});
		}
		List<AssetAttribute> sortedColumns = new ArrayList<>();
		sortedColumns.addAll(attributeColumns);
		sortedColumns.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		for (AssetAttribute a : sortedColumns) {
			TableViewerColumn tc = new TableViewerColumn(tblAssetDeployments, SWT.NONE);
			tc.getColumn().setText(a.getName());
			tc.getColumn().setToolTipText(a.getName());
			tc.getColumn().setWidth( 100 );
			
			tc.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof AssetDeployment) {
						for (AssetDeploymentAttributeValue v : ((AssetDeployment) element).getAttributeValues()) {
							if (v.getAttribute().equals(a)) return v.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);//TODO: crs
						}
						return "";
					}
					return super.getText(element);
				}
			});
		}
		tblAssetDeployments.setInput(currentDeployments);
		
		drawCommand.setStations(Collections.singleton(parentEditor.getAssetStation()));
		drawCommand.setLocations(locations);
		SetViewportBBoxCommand cmd = new SetViewportBBoxCommand(drawCommand.getBounds());
		getMapViewer().getMap().executeSyncWithoutUndo(cmd);
		
		
		ApplicationGIS.getToolManager().setCurrentEditor(parentEditor);
		
		mainControl.layout(true);
	}
	

	private void initializeDeploymentPanel() {
		computeSummaryStatisticsJob.schedule();
	}
	
	private void createNotActivePanel() {
		if (mainControl == null) return;
		for (Control c : mainControl.getChildren()) c.dispose();
		toolkit.createLabel(mainControl, "This station has no active assets deployed to it at this time.");
		mainControl.layout(true);
		
		ApplicationGIS.getToolManager().setCurrentEditor(null);
	}
	

	private Job computeSummaryStatisticsJob = new Job("compute statistics for current deployment") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
//			AssetDeployment deploy = null;
//			try(Session session = HibernateManager.openSession()){
//				deploy = session.get(AssetDeployment.class, deployUuid);
//				if (deploy == null) return Status.OK_STATUS;//todo
//				deploy.getAsset().getAssetType().getUuid();
//				deploy.getAttributeValues().forEach(e->{
//					e.getAttribute().getName();
//					e.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
//				});
//				deploy.getStationLocation().getStation().getId();
//				deploy.getStationLocation().getId();
//				
//				deploy.getStationLocation().getStation().getAttributeValues().forEach(e->{
//					e.getAttribute().getName();
//					e.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
//				});
//				
//				deploy.getStationLocation().getAttributeValues().forEach(e->{
//					e.getAttribute().getName();
//					e.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
//				});
//				
//				currentDeployment = deploy;
//				
//				double stationBuffer = AssetHibernateManager.getStationBuffer(session, SmartDB.getCurrentConservationArea());
//				double locationBuffer = AssetHibernateManager.getStationLocationBuffer(session, SmartDB.getCurrentConservationArea());
//				drawCommand.setBuffers(stationBuffer, locationBuffer);
//			}
//			
//			final Map<Statistic, Object> stats = new HashMap<>();
//			
//			Set<Statistic> toCompute = new HashSet<>();
//			toCompute.add(DeploymentStatisticsEngine.Statistic.INCIDENTS_PER_CAT);
//			toCompute.add(DeploymentStatisticsEngine.Statistic.NUMBER_INCIDENTS);
//			toCompute.add(DeploymentStatisticsEngine.Statistic.NUMBER_UNTAGGED);
//			
//			stats.putAll(DeploymentStatisticsEngine.INSTANCE.computeStatistics(toCompute, deploy));
//			
//			final AssetDeployment thisdeploy = deploy;
//			Display.getDefault().syncExec(()->{
//				
//				Object v = stats.get(DeploymentStatisticsEngine.Statistic.NUMBER_INCIDENTS);
//				String numIncidents = "";
//				if (v != null) {
//					if (v instanceof Long) {
//						numIncidents = String.valueOf((Long)v);
//					}else if (v instanceof String) {
//						numIncidents = (String)v;
//					}
//				}
//				
//				v = stats.get(DeploymentStatisticsEngine.Statistic.NUMBER_UNTAGGED);
//				String untagged = "";
//				if (v != null) {
//					if (v instanceof Long) {
//						untagged = String.valueOf((Long)v);
//					}else if (v instanceof String) {
//						untagged = (String)v;
//					}
//				}
//				
//				v = stats.get(DeploymentStatisticsEngine.Statistic.INCIDENTS_PER_CAT);
//				if (v != null) {
//					tblCnts.setInput(v);
//				}else {
//					tblCnts.setInput(null);
//				}
//				
//				for (Control c : statDetailsSection.getChildren()) c.dispose();
//				for (Control c : bottomPart.getChildren()) c.dispose();
//				
//				lblStatStation.setText(thisdeploy.getStationLocation().getStation().getId());
//				lblStatStation.setData(HL_UUID_KEY, thisdeploy.getStationLocation().getStation().getUuid());
//				lblStatLocation.setText(thisdeploy.getStationLocation().getId());
//				lblStatLocation.getParent().layout(true);
//				
//				Label ll = toolkit.createLabel(statDetailsSection, "Start Date:");
//				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
//				toolkit.createLabel(statDetailsSection, DateFormat.getDateTimeInstance().format(thisdeploy.getStartDate()));
//				
//				ll = toolkit.createLabel(statDetailsSection, "Time Deployed:");
//				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
//				toolkit.createLabel(statDetailsSection, (AssetUtils.formatTime( ((new Date()).getTime() - thisdeploy.getStartDate().getTime()) / 1000.0 )));
//				
//				ll = toolkit.createLabel(statDetailsSection, "", SWT.SEPARATOR | SWT.HORIZONTAL);
//				ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
//				
//				ll = toolkit.createLabel(statDetailsSection, "Incidents:");
//				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
//				toolkit.createLabel(statDetailsSection, numIncidents);
//				
//				ll = toolkit.createLabel(statDetailsSection, "Untagged Incidents:");
//				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
//				toolkit.createLabel(statDetailsSection, untagged);
//				
//				
//				Label sl = toolkit.createLabel(statDetailsSection, "", SWT.SEPARATOR | SWT.HORIZONTAL);
//				sl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
//				
//				Composite attributesComposite = toolkit.createComposite(bottomPart, SWT.NONE);
//				attributesComposite.setLayout(new GridLayout(3, true));
//				attributesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//				((GridLayout)attributesComposite.getLayout()).marginWidth = 0;
//				((GridLayout)attributesComposite.getLayout()).marginHeight = 0;
//				
//				Label l = toolkit.createLabel(attributesComposite, "Deployment Attributes");
//				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//				FontData fd = l.getFont().getFontData()[0];
//				fd.setStyle(SWT.BOLD);
//				Font boldFont = new Font(l.getDisplay(), fd);
//				l.addListener(SWT.Dispose, e->boldFont.dispose());
//				l.setFont(boldFont);
//				
//				l = toolkit.createLabel(attributesComposite, "Location Attributes");
//				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//				l.setFont(boldFont);
//				
//				l = toolkit.createLabel(attributesComposite, "Station Attributes");
//				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//				l.setFont(boldFont);
//
//				for (int i = 0; i < 3; i ++) {
//					Composite thisPart1 = toolkit.createComposite(attributesComposite, SWT.NONE);
//					thisPart1.setLayout(new GridLayout());
//					((GridLayout)thisPart1.getLayout()).marginWidth = 0;
//					((GridLayout)thisPart1.getLayout()).marginHeight = 0;
//					thisPart1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//					
//					ScrolledComposite scrollDeployment = new ScrolledComposite(thisPart1, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
//					scrollDeployment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//					scrollDeployment.setExpandHorizontal(true);
//					scrollDeployment.setExpandVertical(true);
//					
//					Composite deployAtt = toolkit.createComposite(scrollDeployment);
//					deployAtt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//	
//					List<? extends AbstractAssetAttributeValue> attributes = null;
//					if (i == 0) attributes = thisdeploy.getAttributeValues();
//					if (i == 1) attributes = thisdeploy.getStationLocation().getAttributeValues();
//					if (i == 2) attributes = thisdeploy.getStationLocation().getStation().getAttributeValues();
//					
//					scrollDeployment.setContent(deployAtt);
//					deployAtt.setLayout(new GridLayout(2, false));
//					for (AbstractAssetAttributeValue value : attributes) {
//						ll = toolkit.createLabel(deployAtt, value.getAttribute().getName() + ":");
//						ll.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
//						toolkit.createLabel(deployAtt, value.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS));//TODO:
//					}
//					scrollDeployment.setMinSize(deployAtt.computeSize(SWT.DEFAULT, SWT.DEFAULT));
//				}
//
//				mainControl.layout(true, true);
//			});

			
			
			return Status.OK_STATUS;
		}
		
	};

	private enum Column{
		ASSET_TYPE ("Asset Type"),
		ASSET_ID ("Asset ID"),
		STATION ("Station"),
		LOCATION ("Location"),
		START_DATE("Start Date");
		
		public String guiName;
		
		Column(String gui){
			this.guiName = gui;
		}
		
		public String getText(AssetDeployment element) {
			switch(this){
			case ASSET_ID:
				return element.getAsset().getId();
			case ASSET_TYPE:
				return element.getAsset().getAssetType().getName();
			case LOCATION:
				return element.getStationLocation().getId();
			case START_DATE:
				return DateFormat.getDateTimeInstance().format(element.getStartDate());
			case STATION:
				return element.getStationLocation().getStation().getId();
				
			}
			return null;
		}
	}
}
