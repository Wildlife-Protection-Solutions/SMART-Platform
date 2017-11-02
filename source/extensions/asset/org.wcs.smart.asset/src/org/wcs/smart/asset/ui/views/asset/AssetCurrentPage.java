package org.wcs.smart.asset.ui.views.asset;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.render.impl.RenderManagerImpl;
import org.locationtech.udig.project.internal.render.impl.ViewportModelImpl;
import org.locationtech.udig.project.render.displayAdapter.IMapDisplay;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.ApplicationGIS.DrawMapParameter;
import org.locationtech.udig.project.ui.BoundsStrategy;
import org.locationtech.udig.project.ui.SelectionStyle;
import org.locationtech.udig.project.ui.internal.NextGenRenderManager;
import org.locationtech.udig.project.ui.wizard.export.image.GeotiffImageExportFormat;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.engine.DeploymentStatisticsEngine;
import org.wcs.smart.asset.engine.DeploymentStatisticsEngine.Statistic;
import org.wcs.smart.asset.model.AbstractAssetAttributeValue;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.properties.DialogConstants;

public class AssetCurrentPage {

	@Inject
	private IEclipseContext parentContext;
	private AssetEditor parentEditor;
	
	private Composite mainControl;
	private Composite mapComposite;
	
	private AssetDeployment currentDeployment = null;
	private FormToolkit toolkit;
	
	private TableViewer tblCnts;
	
	private Label lblStatStation;
	private Label lblStatLocation;
	
	private Composite statDetailsSection;
	private Composite bottomPart;
	
	private SashForm currentForm;

	private Image mapImage;
	private IMap mapMap;

	public AssetCurrentPage(AssetEditor parent) {
		this.parentEditor = parent;
	}
	
	public Composite createSummarySection(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		
		mainControl = toolkit.createComposite(parent, SWT.NONE);
		mainControl.setLayout(new GridLayout());
		mainControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		return mainControl;
	}
	
	
	public void initializePanel(AssetDeployment activeDeployment) {
		if (this.currentDeployment == null && activeDeployment == null) {
			createNotActivePanel();
			return;
		}
		if (activeDeployment == null) {
			this.currentDeployment = activeDeployment;
			createNotActivePanel();
			return;
		}
		if (currentForm == null || currentForm.isDisposed()) {
			createDeploymentPanel();
		}
		this.currentDeployment = activeDeployment;
		initializeDeploymentPanel();
		return;
	}
	
	
	private void createDeploymentPanel() {
		if (mainControl == null) return;
		for (Control c : mainControl.getChildren()) c.dispose();
		
		SashForm mainPart = new SashForm(mainControl, SWT.VERTICAL);
		currentForm = mainPart;
		mainPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm topPart = new SashForm(mainPart, SWT.HORIZONTAL);
		
		
		Composite summaryPart = toolkit.createComposite(topPart, SWT.BORDER);
		summaryPart.setLayout(new GridLayout(2, false));

		Label l = toolkit.createLabel(summaryPart, "Summary");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->boldFont.dispose());
		l.setFont(boldFont);
		
		Composite mapPart = toolkit.createComposite(topPart, SWT.BORDER);
		
		topPart.setWeights(new int[] {60,40});
	
		statDetailsSection = toolkit.createComposite(summaryPart, SWT.NONE);
		statDetailsSection.setLayout(new GridLayout(2, false));
		statDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
	
		tblCnts = new TableViewer(summaryPart, SWT.FULL_SELECTION);
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
		
		toolkit.createLabel(mapPart, "Station:");
		lblStatStation = toolkit.createLabel(mapPart, "");
		
		toolkit.createLabel(mapPart, "Location:");
		lblStatLocation= toolkit.createLabel(mapPart, "");
		
		mapComposite = toolkit.createComposite(mapPart, SWT.BORDER);
		mapComposite.setLayout(new GridLayout());
		mapComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		mapComposite.addListener(SWT.Paint, e->{
			if (mapImage != null && !mapImage.isDisposed()) e.gc.drawImage(mapImage, 0, 0);
		});
		mapComposite.addListener(SWT.Dispose, e->{
			if (mapImage != null && !mapImage.isDisposed()) {
				mapImage.dispose();
				mapImage = null;
			}
		});
		mapComposite.addListener(SWT.Resize, e->createAndRenderMap(currentDeployment));
	
		
		toolkit.createLabel(mapComposite, "MAP HERE");
		
		bottomPart = toolkit.createComposite(mainPart, SWT.BORDER);
		bottomPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		bottomPart.setLayout(new GridLayout(2, false));
		
		mainPart.setWeights(new int[] {70, 30});
		
		mainControl.layout(true);
	}
	

	private void initializeDeploymentPanel() {
		computeSummaryStatisticsJob.schedule();
	}
	
	private void createNotActivePanel() {
		if (mainControl == null) return;
		for (Control c : mainControl.getChildren()) c.dispose();
		toolkit.createLabel(mainControl, "This asset is not currently active");
		mainControl.layout(true);
	}
	
	private void createAndRenderMap(AssetDeployment deploy) {
		if (deploy == null)  return;
		createMapJob.schedule(100);
	}
	
	
	Job createMapJob = new Job("create map job") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			AssetDeployment deploy = currentDeployment;
			if (deploy == null) return Status.OK_STATUS;
			final Point size = new Point();
			Display.getDefault().syncExec(()->{
				size.x = mapComposite.getSize().x;
				size.y = mapComposite.getSize().y;
			});
			if (mapMap == null) {
				mapMap = ProjectFactory.eINSTANCE.createMap();
				LoadDefaultLayersJob basemap = new LoadDefaultLayersJob(mapMap);
				basemap.schedule();
				try {
					basemap.join();
				}catch (Exception ex) {
					//TODO:
				}

                ((org.locationtech.udig.project.internal.Map)mapMap).setRenderManagerInternal(new NextGenRenderManager());
                ((RenderManagerImpl)mapMap.getRenderManager()).setMapDisplay(new IMapDisplay() {
					
					@Override
					public int getWidth() {
						return size.x;
					}
					
					@Override
					public int getHeight() {
						return size.y;
					}
					
					@Override
					public Dimension getDisplaySize() {
						return new Dimension(getWidth(), getHeight());
					}
					
					@Override
					public int getDPI() {
						return 70;
					}
				});
				((ViewportModelImpl)mapMap.getViewportModel()).zoomToExtent();
			}
			
			BufferedImage image = new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			try {
				double offset = 0.2;
				BoundsStrategy zoomTo = new BoundsStrategy(new ReferencedEnvelope(
						deploy.getStationLocation().getX() - offset, deploy.getStationLocation().getX() + offset,
						deploy.getStationLocation().getY() - offset, deploy.getStationLocation().getY() + offset,
						SmartDB.DATABASE_CRS));
				
				DrawMapParameter drawMapParameter = new DrawMapParameter(g,
						new java.awt.Dimension(size.x, size.y), mapMap,
						zoomTo,
						90,SelectionStyle.IGNORE,
						new NullProgressMonitor());

				ApplicationGIS.drawMap(drawMapParameter);
			}catch (Exception ex) {
				ex.printStackTrace();
				//TODO:
			} finally {
				g.dispose();
			}
			
			Display.getDefault().syncExec(()->{
				if (mapImage != null && !mapImage.isDisposed()) mapImage.dispose();
				mapImage = AWTSWTImageUtils.convertToSWTImage(image);
				mapComposite.redraw();
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
	private Job computeSummaryStatisticsJob = new Job("compute statistics for current deployment") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			AssetDeployment deploy = currentDeployment;
			if (deploy == null) {
				//TODO: 
				return Status.OK_STATUS;
			}
			
			try(Session session = HibernateManager.openSession()){
				deploy = session.get(AssetDeployment.class, deploy.getUuid());
				if (deploy == null) return Status.OK_STATUS;//todo
				deploy.getAsset().getAssetType().getUuid();
				deploy.getAttributeValues().forEach(e->{
					e.getAttribute().getName();
					e.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
				});
				deploy.getStationLocation().getStation().getId();
				deploy.getStationLocation().getId();
				
				deploy.getStationLocation().getStation().getAttributeValues().forEach(e->{
					e.getAttribute().getName();
					e.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
				});
				
				deploy.getStationLocation().getAttributeValues().forEach(e->{
					e.getAttribute().getName();
					e.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
				});
				
				currentDeployment = deploy;
			}
			
			final Map<Statistic, Object> stats = new HashMap<>();
			
			Set<Statistic> toCompute = new HashSet<>();
			toCompute.add(DeploymentStatisticsEngine.Statistic.INCIDENTS_PER_CAT);
			toCompute.add(DeploymentStatisticsEngine.Statistic.NUMBER_INCIDENTS);
			toCompute.add(DeploymentStatisticsEngine.Statistic.NUMBER_UNTAGGED);
			
			stats.putAll(DeploymentStatisticsEngine.INSTANCE.computeStatistics(toCompute, deploy));
			
			final AssetDeployment thisdeploy = deploy;
			Display.getDefault().syncExec(()->{
				
				Object v = stats.get(DeploymentStatisticsEngine.Statistic.NUMBER_INCIDENTS);
				String numIncidents = "";
				if (v != null) {
					if (v instanceof Long) {
						numIncidents = String.valueOf((Long)v);
					}else if (v instanceof String) {
						numIncidents = (String)v;
					}
				}
				
				v = stats.get(DeploymentStatisticsEngine.Statistic.NUMBER_UNTAGGED);
				String untagged = "";
				if (v != null) {
					if (v instanceof Long) {
						untagged = String.valueOf((Long)v);
					}else if (v instanceof String) {
						untagged = (String)v;
					}
				}
				
				v = stats.get(DeploymentStatisticsEngine.Statistic.INCIDENTS_PER_CAT);
				if (v != null) {
					tblCnts.setInput(v);
				}else {
					tblCnts.setInput(null);
				}
				
				for (Control c : statDetailsSection.getChildren()) c.dispose();
				for (Control c : bottomPart.getChildren()) c.dispose();
				
				lblStatStation.setText(thisdeploy.getStationLocation().getStation().getId());
				lblStatLocation.setText(thisdeploy.getStationLocation().getId());
				lblStatLocation.getParent().layout(true);
				
				Label ll = toolkit.createLabel(statDetailsSection, "Start Date:");
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				toolkit.createLabel(statDetailsSection, DateFormat.getDateTimeInstance().format(thisdeploy.getStartDate()));
				
				ll = toolkit.createLabel(statDetailsSection, "Time Deployed:");
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				toolkit.createLabel(statDetailsSection, (AssetUtils.formatTime( ((new Date()).getTime() - thisdeploy.getStartDate().getTime()) / 1000.0 )));
				
				ll = toolkit.createLabel(statDetailsSection, "", SWT.SEPARATOR | SWT.HORIZONTAL);
				ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				
				ll = toolkit.createLabel(statDetailsSection, "Incidents:");
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				toolkit.createLabel(statDetailsSection, numIncidents);
				
				ll = toolkit.createLabel(statDetailsSection, "Untagged Incidents:");
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				toolkit.createLabel(statDetailsSection, untagged);
				
				
				Label sl = toolkit.createLabel(statDetailsSection, "", SWT.SEPARATOR | SWT.HORIZONTAL);
				sl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				
				Composite attributesComposite = toolkit.createComposite(bottomPart, SWT.NONE);
				attributesComposite.setLayout(new GridLayout(3, true));
				attributesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				((GridLayout)attributesComposite.getLayout()).marginWidth = 0;
				((GridLayout)attributesComposite.getLayout()).marginHeight = 0;
				
				Label l = toolkit.createLabel(attributesComposite, "Deployment Attributes");
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				FontData fd = l.getFont().getFontData()[0];
				fd.setStyle(SWT.BOLD);
				Font boldFont = new Font(l.getDisplay(), fd);
				l.addListener(SWT.Dispose, e->boldFont.dispose());
				l.setFont(boldFont);
				
				l = toolkit.createLabel(attributesComposite, "Location Attributes");
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setFont(boldFont);
				
				l = toolkit.createLabel(attributesComposite, "Station Attributes");
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setFont(boldFont);

				for (int i = 0; i < 3; i ++) {
					Composite thisPart1 = toolkit.createComposite(attributesComposite, SWT.NONE);
					thisPart1.setLayout(new GridLayout());
					((GridLayout)thisPart1.getLayout()).marginWidth = 0;
					((GridLayout)thisPart1.getLayout()).marginHeight = 0;
					thisPart1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					
					ScrolledComposite scrollDeployment = new ScrolledComposite(thisPart1, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
					scrollDeployment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					scrollDeployment.setExpandHorizontal(true);
					scrollDeployment.setExpandVertical(true);
					
					Composite deployAtt = toolkit.createComposite(scrollDeployment);
					deployAtt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	
					List<? extends AbstractAssetAttributeValue> attributes = null;
					if (i == 0) attributes = thisdeploy.getAttributeValues();
					if (i == 1) attributes = thisdeploy.getStationLocation().getAttributeValues();
					if (i == 2) attributes = thisdeploy.getStationLocation().getStation().getAttributeValues();
					
					scrollDeployment.setContent(deployAtt);
					deployAtt.setLayout(new GridLayout(2, false));
					for (AbstractAssetAttributeValue value : attributes) {
						ll = toolkit.createLabel(deployAtt, value.getAttribute().getName() + ":");
						ll.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
						toolkit.createLabel(deployAtt, value.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS));//TODO:
					}
					scrollDeployment.setMinSize(deployAtt.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				}

				mainControl.layout(true, true);
			});
			
			createAndRenderMap(deploy);
			return Status.OK_STATUS;
		}
		
	};
	
}
