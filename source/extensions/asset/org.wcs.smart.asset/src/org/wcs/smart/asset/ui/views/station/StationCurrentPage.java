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
package org.wcs.smart.asset.ui.views.station;

import java.text.Collator;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.engine.StatisticsEngine;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.AssetTypeLabelProvider;
import org.wcs.smart.asset.ui.handler.OpenAssetHandler;
import org.wcs.smart.asset.ui.handler.OpenStationLocationHandler;
import org.wcs.smart.asset.ui.map.StationLocationDrawCommand;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Station editor current page
 * @author Emily
 *
 */
public class StationCurrentPage {

	private StationEditor parentEditor;
	
	private Composite mainControl;
	private Composite mapComposite;
	private MapViewer mapViewer;
	
	private FormToolkit toolkit;
	
	private TableViewer tblCnts;
	private TableViewer tblAssetDeployments;
	
	private Composite bottomPart;
	
	private StationLocationDrawCommand drawCommand;
	
	private Label lblNumIncidents;
	private Label lblNumUnTagged;
	
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
	
	private Job initializePanelJob = new Job(Messages.StationCurrentPage_initJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			drawCommand = new StationLocationDrawCommand();
			final List<AssetDeployment> currentDeployments = new ArrayList<>();
			try(Session s = HibernateManager.openSession()){
				currentDeployments.addAll(parentEditor.getAssetStation().getActiveDeployments(s));
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
		topBottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm topLeftRight = new SashForm(topBottom, SWT.HORIZONTAL);
		
		Composite topLeft = toolkit.createComposite(topLeftRight, SWT.BORDER);
		topLeft.setLayout(new GridLayout(2, false));

		Label l = toolkit.createLabel(topLeft, Messages.StationCurrentPage_SummaryLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->boldFont.dispose());
		l.setFont(boldFont);
		
		l = toolkit.createLabel(topLeft, MessageFormat.format(Messages.StationCurrentPage_NumAssetsLabel, currentDeployments.size()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		lblNumIncidents = toolkit.createLabel(topLeft, DialogConstants.LOADING_TEXT);
		lblNumIncidents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		lblNumUnTagged = toolkit.createLabel(topLeft, DialogConstants.LOADING_TEXT);
		lblNumUnTagged.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		l = toolkit.createLabel(topLeft, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
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
		categoryColumn.getColumn().setText(Messages.StationCurrentPage_CategoryLbl);
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
		cntColumn.getColumn().setText(Messages.StationCurrentPage_NumIncidentsLbl);
		cntColumn.getColumn().setResizable(true);
		cntColumn.getColumn().setWidth(200);
		tblCnts.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		Composite mapPart = toolkit.createComposite(topLeftRight, SWT.NONE);
		mapPart.setLayout(new GridLayout(2, false));
		((GridLayout)mapPart.getLayout()).marginWidth = 0;
		((GridLayout)mapPart.getLayout()).marginHeight = 0;
		
		topLeftRight.setWeights(new int[] {60,40});
		
		
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
			if (c == Column.ASSET_TYPE) {
				tc.setLabelProvider(new ColumnLabelProvider() {
					AssetTypeLabelProvider type = new AssetTypeLabelProvider();
					@Override
					public String getText(Object element) {
						if (element instanceof AssetDeployment) {
							return c.getText((AssetDeployment) element);
						}
						return super.getText(element);
					}
					@Override
					public void dispose() {
						type.dispose();
						super.dispose();
					}
					@Override
					public Image getImage(Object element) {
						if (element instanceof AssetDeployment) {
							return type.getImage(((AssetDeployment) element).getAsset().getAssetType());
						}
						return null;
					}
				});
			}else {
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
							if (v.getAttribute().equals(a)) return v.getAttributeValueAsString(Locale.getDefault(), parentEditor.viewCrs);
						}
						return ""; //$NON-NLS-1$
					}
					return super.getText(element);
				}
			});
		}
		
		tblAssetDeployments.setInput(currentDeployments);
		for (TableColumn c : tblAssetDeployments.getTable().getColumns()) {
			c.pack();
			c.setWidth(c.getWidth() + 20);
		}
		
		tblAssetDeployments.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = tblAssetDeployments.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				int colIndex = cell.getColumnIndex();
				if (colIndex == Column.ASSET_ID.ordinal()){
					AssetDeployment d = (AssetDeployment) cell.getElement();
					(new OpenAssetHandler()).openAsset(d.getAsset());
				}else if (colIndex == Column.LOCATION.ordinal()) {
					AssetDeployment d = (AssetDeployment) cell.getElement();
					(new OpenStationLocationHandler()).openStationLocation(d.getStationLocation());
				}
			}
					
		});
		
		Menu mnu = new Menu(tblAssetDeployments.getTable());
		tblAssetDeployments.getTable().setMenu(mnu);
		mnu.addMenuListener(new MenuListener() {

			@Override
			public void menuHidden(MenuEvent e) {
				
			}

			@Override
			public void menuShown(MenuEvent e) {
				for (MenuItem mi : mnu.getItems()) mi.dispose();
				
				Object x = tblAssetDeployments.getStructuredSelection().getFirstElement();
				if (!(x instanceof AssetDeployment)) return;
				
				AssetDeployment d = (AssetDeployment)x;
				MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
				miAdd.setText(MessageFormat.format(Messages.StationCurrentPage_GotoLbl, d.getAsset().getId()));
				miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON));
				miAdd.addListener(SWT.Selection, e1->{
					(new OpenAssetHandler()).openAsset(d.getAsset());
				});
				
				MenuItem miAdd2 = new MenuItem(mnu, SWT.PUSH);
				miAdd2.setText(MessageFormat.format(Messages.StationCurrentPage_GotoLbl, d.getStationLocation().getId()));
				miAdd2.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON));
				miAdd2.addListener(SWT.Selection, e2->{
					(new OpenStationLocationHandler()).openStationLocation(d.getStationLocation());
				});
			}
		});
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
		toolkit.createLabel(mainControl, Messages.StationCurrentPage_NoAssetsMsg);
		mainControl.layout(true);
		
		ApplicationGIS.getToolManager().setCurrentEditor(null);
	}
	

	private Job computeSummaryStatisticsJob = new Job(Messages.StationCurrentPage_statsJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final Map<StatisticsEngine.Statistic, Object> stats = new HashMap<>();
			
			Set<StatisticsEngine.Statistic> toCompute = new HashSet<>();
			toCompute.add(StatisticsEngine.Statistic.INCIDENTS_PER_CAT);
			toCompute.add(StatisticsEngine.Statistic.NUMBER_INCIDENTS);
			toCompute.add(StatisticsEngine.Statistic.NUMBER_UNTAGGED);
			
			stats.putAll(StatisticsEngine.INSTANCE.computeActiveStatistics(toCompute, parentEditor.getAssetStation()));
//			
//			final AssetDeployment thisdeploy = deploy;
			Display.getDefault().syncExec(()->{
				
				Object v = stats.get(StatisticsEngine.Statistic.INCIDENTS_PER_CAT);
				if (v != null) {
					tblCnts.setInput(v);
				}else {
					tblCnts.setInput(null);
				}
						
				v = stats.get(StatisticsEngine.Statistic.NUMBER_INCIDENTS);
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
				lblNumIncidents.setText(MessageFormat.format(Messages.StationCurrentPage_NumIncidentsLabel, numIncidents));
				lblNumUnTagged.setText(MessageFormat.format(Messages.StationCurrentPage_NumUntaggedIncidentsLbl, untagged));
			});
			return Status.OK_STATUS;
		}
		
	};

	private enum Column{
		ASSET_TYPE (Messages.StationCurrentPage_TypeColumName),
		ASSET_ID (Messages.StationCurrentPage_IdColumName),
		LOCATION (Messages.StationCurrentPage_LocationColumName),
		START_DATE(Messages.StationCurrentPage_DateColumName),
		END_DATE(Messages.StationCurrentPage_EndDateColumnName);
		
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
				return element.getStartDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
			case END_DATE:
				if (element.getEndDate() == null) return ""; //$NON-NLS-1$
				return element.getEndDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
			}
			return null;
		}
	}
}
