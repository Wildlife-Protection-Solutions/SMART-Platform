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
package org.wcs.smart.asset.ui.views.asset;

import java.text.DateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.engine.StatisticsEngine;
import org.wcs.smart.asset.engine.StatisticsEngine.Statistic;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AbstractAssetAttributeValue;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.handler.OpenStationHandler;
import org.wcs.smart.asset.ui.handler.OpenStationLocationHandler;
import org.wcs.smart.asset.ui.map.StationLocationDrawCommand;
import org.wcs.smart.asset.ui.views.station.StationEditorInput;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Current asset page in the asset editor.
 * 
 * @author Emily
 *
 */
public class AssetCurrentPage {

	private static final String HL_UUID_KEY = "uuid"; //$NON-NLS-1$

	private AssetEditor parentEditor;
	
	private Composite mainControl;
	private Composite mapComposite;
	private MapViewer mapViewer;
	
	private AssetDeployment currentDeployment = null;
	private FormToolkit toolkit;
	
	private TableViewer tblCnts;
	
	private Hyperlink lblStatStation;
	private Hyperlink lblStatLocation;
	
	private Composite statDetailsSection;
	private Composite bottomPart;
	
	private SashForm currentForm;

	private UUID deployUuid = null;
	private StationLocationDrawCommand drawCommand;
	
	@Inject
	private IEclipseContext context;
	
	public AssetCurrentPage(AssetEditor parent) {
		this.parentEditor = parent;
	}
	
	public Composite createSummarySection(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		
		mainControl = toolkit.createComposite(parent, SWT.NONE);
		mainControl.setLayout(new GridLayout());
		mainControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)mainControl.getLayout()).marginWidth = 0;
		((GridLayout)mainControl.getLayout()).marginHeight = 0;
		
		return mainControl;
	}
	
	
	public void initializePanel(AssetDeployment activeDeployment) {
		this.deployUuid = activeDeployment == null ? null : activeDeployment.getUuid();
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
		initializeDeploymentPanel();
		return;
	}
	
	public MapViewer getMapViewer() {
		return this.mapViewer;
	}
	
	private void createDeploymentPanel() {
		if (mainControl == null) return;
		
		disposeMainPanel();
		
		SashForm mainPart = new SashForm(mainControl, SWT.VERTICAL);
		currentForm = mainPart;
		mainPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm topPart = new SashForm(mainPart, SWT.HORIZONTAL);
		
		
		Composite summaryPart = toolkit.createComposite(topPart, SWT.BORDER);
		summaryPart.setLayout(new GridLayout(2, false));

		Label l = toolkit.createLabel(summaryPart, Messages.AssetCurrentPage_SummaryLabel);
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
					Category c = ((Category)((Object[])element)[0]);
					StringBuilder sb = new StringBuilder();
					sb.append(c.getName());
					if (c.getParent() != null) {
						sb.append(" ("); //$NON-NLS-1$
						sb.append(c.getParent().getFullCategoryName());
						sb.append(")"); //$NON-NLS-1$
					}
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		categoryColumn.getColumn().setText(Messages.AssetCurrentPage_CategoryColumn);
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
		cntColumn.getColumn().setText(Messages.AssetCurrentPage_IncidentCntcolumn);
		cntColumn.getColumn().setResizable(true);
		cntColumn.getColumn().setWidth(200);
		tblCnts.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		mapPart.setLayout(new GridLayout(2, false));
		
		toolkit.createLabel(mapPart, Messages.AssetCurrentPage_StationLabel);
		lblStatStation = toolkit.createHyperlink(mapPart, "", SWT.NONE); //$NON-NLS-1$
		lblStatStation.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				UUID uuid = (UUID) lblStatStation.getData(HL_UUID_KEY);
				if (uuid != null) {
					AssetStation temp = new AssetStation();
					temp.setUuid(uuid);
					
					IEclipseContext ctx = context.createChild();
					ctx.set(OpenStationHandler.STATION_PARAM, new StationEditorInput(temp.getUuid(), temp.getId()));
					ContextInjectionFactory.invoke(new OpenStationHandler(), Execute.class, ctx);
				}
			}
		});
		
		toolkit.createLabel(mapPart, Messages.AssetCurrentPage_LocationLbl); 
		lblStatLocation = toolkit.createHyperlink(mapPart, "", SWT.NONE); //$NON-NLS-1$
		lblStatLocation.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				UUID uuid = (UUID) lblStatLocation.getData(HL_UUID_KEY);
				if (uuid != null) {
					AssetStationLocation temp = new AssetStationLocation();
					temp.setUuid(uuid);
					(new OpenStationLocationHandler()).openStationLocation(temp);
				}
			}
		});
		
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
		
		LoadDefaultLayersJob basemap = new LoadDefaultLayersJob(mapViewer.getMap(), false) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus s = super.run(monitor);
				
				SetViewportBBoxCommand cmd = new SetViewportBBoxCommand(drawCommand.getBounds());
				if (getMapViewer() != null && getMapViewer().getMap() != null) getMapViewer().getMap().executeSyncWithoutUndo(cmd);
				return s;
			}
		};
		basemap.schedule();
		
		drawCommand = new StationLocationDrawCommand();
		mapViewer.getViewport().addDrawCommand(drawCommand);
		mapViewer.getViewport().enableDrawCommands(true);
		
		mapComposite.addListener(SWT.Resize, e->{
			mapViewer.getControl().setBounds(0, 0, mapComposite.getSize().x, mapComposite.getSize().y);
			org.eclipse.swt.graphics.Point size = toolComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			toolComposite.setBounds(mapComposite.getSize().x - size.x, 0, size.x, size.y);
		});
		
		ApplicationGIS.getToolManager().setCurrentEditor(parentEditor);
		
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
		disposeMainPanel();
		toolkit.createLabel(mainControl, Messages.AssetCurrentPage_AssetNotActive);
		mainControl.layout(true);
	}

	private void disposeMainPanel() {
		for (Control c : mainControl.getChildren()) c.dispose();
		
		if (mapViewer != null) {
			mapViewer.dispose();
			mapViewer = null;
		}
	}
	private Job computeSummaryStatisticsJob = new Job(Messages.AssetCurrentPage_statusJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (deployUuid == null) {
				return Status.OK_STATUS;
			}

			AssetDeployment deploy = null;
			try(Session session = HibernateManager.openSession()){
				deploy = session.get(AssetDeployment.class, deployUuid);
				if (deploy == null) return Status.OK_STATUS;
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
				
				deploy.getActiveTimeOutInSeconds();
				deploy.getTimeOutInSeconds();
				
				currentDeployment = deploy;
			}
			
			final Map<Statistic, Object> stats = new HashMap<>();
			
			Set<Statistic> toCompute = new HashSet<>();
			toCompute.add(StatisticsEngine.Statistic.INCIDENTS_PER_CAT);
			toCompute.add(StatisticsEngine.Statistic.NUMBER_INCIDENTS);
			toCompute.add(StatisticsEngine.Statistic.NUMBER_UNTAGGED);
			toCompute.add(StatisticsEngine.Statistic.NUMBER_NOT_VLIDATED);
			
			stats.putAll(StatisticsEngine.INSTANCE.computeStatistics(toCompute, deploy));
			
			final AssetDeployment thisdeploy = deploy;
			Display.getDefault().syncExec(()->{
				
				Object v = stats.get(StatisticsEngine.Statistic.NUMBER_INCIDENTS);
				String numIncidents = ""; //$NON-NLS-1$
				if (v != null) {
					if (v instanceof Long) {
						numIncidents = String.valueOf((Long)v);
					}else if (v instanceof String) {
						numIncidents = (String)v;
					}
				}
				
				v = stats.get(StatisticsEngine.Statistic.NUMBER_UNTAGGED);
				String untagged = ""; //$NON-NLS-1$
				if (v != null) {
					if (v instanceof Long) {
						untagged = String.valueOf((Long)v);
					}else if (v instanceof String) {
						untagged = (String)v;
					}
				}
				
				v = stats.get(StatisticsEngine.Statistic.NUMBER_NOT_VLIDATED);
				String notvalidated = ""; //$NON-NLS-1$
				if (v != null) {
					if (v instanceof Long) {
						notvalidated = String.valueOf((Long)v);
					}else if (v instanceof String) {
						notvalidated = (String)v;
					}
				}
				
				v = stats.get(StatisticsEngine.Statistic.INCIDENTS_PER_CAT);
				if (v != null) {
					tblCnts.setInput(v);
				}else {
					tblCnts.setInput(null);
				}
				
				for (Control c : statDetailsSection.getChildren()) c.dispose();
				for (Control c : bottomPart.getChildren()) c.dispose();
				
				lblStatStation.setText(thisdeploy.getStationLocation().getStation().getId());
				lblStatStation.setData(HL_UUID_KEY, thisdeploy.getStationLocation().getStation().getUuid());
				lblStatLocation.setText(thisdeploy.getStationLocation().getId());
				lblStatLocation.getParent().layout(true);
				lblStatLocation.setData(HL_UUID_KEY, thisdeploy.getStationLocation().getUuid());
				
				Label ll = toolkit.createLabel(statDetailsSection, Messages.AssetCurrentPage_StartDateLabel);
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				toolkit.createLabel(statDetailsSection, DateFormat.getDateTimeInstance().format(thisdeploy.getStartDate()));
				
				ll = toolkit.createLabel(statDetailsSection, Messages.AssetCurrentPage_EndDateLabel);
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				toolkit.createLabel(statDetailsSection, thisdeploy.getEndDate() == null ? "" : DateFormat.getDateTimeInstance().format(thisdeploy.getEndDate())); //$NON-NLS-1$
				
				ll = toolkit.createLabel(statDetailsSection, Messages.AssetCurrentPage_ActiveTimeDeployedLbl);
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				toolkit.createLabel(statDetailsSection, (AssetUtils.formatTime( thisdeploy.getActiveTimeOutInSeconds() )));
				
				ll = toolkit.createLabel(statDetailsSection, Messages.AssetCurrentPage_TotalTimeDeployedLbl);
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				toolkit.createLabel(statDetailsSection, (AssetUtils.formatTime( thisdeploy.getTimeOutInSeconds() )));
				
				if (thisdeploy.getEndDate() != null) {
					ll = toolkit.createLabel(statDetailsSection, Messages.AssetCurrentPage_TimeToEndDate);
					ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
					toolkit.createLabel(statDetailsSection, (AssetUtils.formatTime( thisdeploy.getTimeToEndDate() )));
				}
				
				ll = toolkit.createLabel(statDetailsSection, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
				ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				
				ll = toolkit.createLabel(statDetailsSection, Messages.AssetCurrentPage_IncidentsLabel);
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				toolkit.createLabel(statDetailsSection, numIncidents);
				
				ll = toolkit.createLabel(statDetailsSection, Messages.AssetCurrentPage_UntaggedLabel);
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				ll.setToolTipText(Messages.AssetCurrentPage_unTaggedTooltip);
				toolkit.createLabel(statDetailsSection, untagged);
				
				ll = toolkit.createLabel(statDetailsSection, Messages.AssetCurrentPage_NotValidatedLabel);
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				ll.setToolTipText(Messages.AssetCurrentPage_NotValidatedTooltip);
				toolkit.createLabel(statDetailsSection, notvalidated);
				
				Label sl = toolkit.createLabel(statDetailsSection, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
				sl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				
				Composite attributesComposite = toolkit.createComposite(bottomPart, SWT.NONE);
				attributesComposite.setLayout(new GridLayout(3, true));
				attributesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				((GridLayout)attributesComposite.getLayout()).marginWidth = 0;
				((GridLayout)attributesComposite.getLayout()).marginHeight = 0;
				
				Label l = toolkit.createLabel(attributesComposite, Messages.AssetCurrentPage_DeploymentAttributesLabel);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				FontData fd = l.getFont().getFontData()[0];
				fd.setStyle(SWT.BOLD);
				Font boldFont = new Font(l.getDisplay(), fd);
				l.addListener(SWT.Dispose, e->boldFont.dispose());
				l.setFont(boldFont);
				
				l = toolkit.createLabel(attributesComposite, Messages.AssetCurrentPage_LocationAttributesLabel);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setFont(boldFont);
				
				l = toolkit.createLabel(attributesComposite, Messages.AssetCurrentPage_StationAttributesLabel);
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
	
					if (i == 1) {
						ll = toolkit.createLabel(deployAtt, Messages.AssetCurrentPage_IDLabel);
						ll.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
						ll = toolkit.createLabel(deployAtt, thisdeploy.getStationLocation().getId());
					}
					if (i == 2) {
						ll = toolkit.createLabel(deployAtt, Messages.AssetCurrentPage_IDLabel);
						ll.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
						ll = toolkit.createLabel(deployAtt, thisdeploy.getStationLocation().getStation().getId());
					}
					
					List<? extends AbstractAssetAttributeValue> attributes = null;
					if (i == 0) attributes = thisdeploy.getAttributeValues();
					if (i == 1) attributes = thisdeploy.getStationLocation().getAttributeValues();
					if (i == 2) attributes = thisdeploy.getStationLocation().getStation().getAttributeValues();
					
					scrollDeployment.setContent(deployAtt);
					deployAtt.setLayout(new GridLayout(2, false));
					for (AbstractAssetAttributeValue value : attributes) {
						ll = toolkit.createLabel(deployAtt, value.getAttribute().getName() + ":"); //$NON-NLS-1$
						ll.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
						toolkit.createLabel(deployAtt, value.getAttributeValueAsString(Locale.getDefault(), parentEditor.currentCrs));
					}
					scrollDeployment.setMinSize(deployAtt.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				}

				mainControl.layout(true, true);
			});

			drawCommand.setStations(Collections.singleton(currentDeployment.getStationLocation().getStation()));
			drawCommand.setLocations(Collections.singleton(currentDeployment.getStationLocation()));
			SetViewportBBoxCommand cmd = new SetViewportBBoxCommand(drawCommand.getBounds());
			getMapViewer().getMap().executeSyncWithoutUndo(cmd);
			
			return Status.OK_STATUS;
		}
		
	};

	
}
