/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.patrol.internal.ui.editor;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;
import net.refractions.udig.project.render.ViewportModelEvent.EventType;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.viewers.MapViewer;
import net.refractions.udig.style.sld.SLDContent;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.XMLMemento;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.SavePatrolPartJob;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapInfoAreaComposite;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.location.GeometryFactoryProvider;
import org.wcs.smart.ui.map.tool.PanTool;
import org.wcs.smart.ui.map.tool.ZoomExtentTool;
import org.wcs.smart.ui.map.tool.ZoomTool;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Dialog for display tack points for a track.
 * <p>The track points are displayed in a simple table.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class TrackPointDialog extends TitleAreaDialog implements MapPart{
	private final static FilterFactory2 FACTORY = CommonFactoryFinder.getFilterFactory2(null);
	private final static DateFormat DATEFORMAT = new SimpleDateFormat( ((SimpleDateFormat)DateFormat.getTimeInstance()).toPattern() + "   (" + ((SimpleDateFormat)DateFormat.getDateInstance()).toPattern() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
	static{
		DATEFORMAT.setTimeZone(Track.ZTIMEZONE);
	}

	private final static String SELECTED_FIELD = "selected"; //$NON-NLS-1$
	private final static String X_FIELD = "x"; //$NON-NLS-1$
	private final static String Y_FIELD = "y"; //$NON-NLS-1$
	private final static String DATETIME_FIELD = "datetime"; //$NON-NLS-1$
	private final static String GEOM_FIELD = "geom"; //$NON-NLS-1$
	private final static String INDEX_FIELD = "index"; //$NON-NLS-1$
	
	private Track track;
	private Track editTrack;	//copy of track for editing
	
	private TableViewer trackviewer;
	private MapViewer mapViewer;

	private FeatureStore pointStore;
	private FeatureStore trackStore;

	private Layer trackLayer = null;
	private Layer pointLayer = null;
	
	
	/**
	 * @param parentShell parent shell
	 * @param t the track to display
	 */
	public TrackPointDialog(Shell parentShell, Track t) {
		super(parentShell);
		this.track = t;
		
		editTrack = new Track();
		editTrack.setLineString(t.getLineString());
		editTrack.setUuid(track.getUuid());
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		super.setMessage(Messages.TrackPointDialog_DialogMessage);
		
		SashForm main = new SashForm(parent, SWT.HORIZONTAL);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TabFolder tabFolder = new TabFolder(main, SWT.TOP);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		//point list
		createPointList(tabFolder);
		
		//map layers
		TabItem  layerListTabItem = new TabItem(tabFolder, SWT.NONE);
		layerListTabItem.setText(Messages.TrackPointDialog_MapLayersTableName);
		
		Composite layersTab = new Composite(tabFolder, SWT.NONE);
		layersTab.setLayout(new GridLayout());
		LayerListComposite lv = new LayerListComposite(layersTab);
		lv.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		layerListTabItem.setControl(layersTab);

		//map area
		setupMap(main);
		
		//data setup
		lv.setMap(mapViewer.getMap());
		mapViewer.getMap().getViewportModel().addViewportModelListener(new IViewportModelListener() {
			@Override
			public void changed(ViewportModelEvent event) {
				if (event.getType() == EventType.CRS){
					trackviewer.getControl().getDisplay().asyncExec(new Runnable(){
						@Override
						public void run() {
							trackviewer.refresh();		
						}});
				}
			}
		});
		
		getShell().setText(Messages.TrackPointDialog_DialogTitle);
		setTitle(Messages.TrackPointDialog_DialogTitle);
		
		return main;
	}

	
	/*
	 * setup point list and layer list
	 */
	private void createPointList(TabFolder tabFolder) {
		TabItem pntsTabItem = new TabItem(tabFolder, SWT.NONE);
		pntsTabItem.setText(Messages.TrackPointDialog_TrackPointsTabName);
		
		Composite pntsTab = new Composite(tabFolder, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		pntsTab.setLayout(gl);
		pntsTabItem.setControl(pntsTab);
		
		trackviewer = new TableViewer(pntsTab, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI );
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 400;
		gd.widthHint = 300;
		trackviewer.getTable().setLayoutData(gd);
		trackviewer.getTable().setLinesVisible(true);
		trackviewer.getTable().setHeaderVisible(true);
		trackviewer.setContentProvider(ArrayContentProvider.getInstance());

		//--X
		TableViewerColumn column = new TableViewerColumn(trackviewer,SWT.NONE);
		column.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element) {
				try {
					if (element instanceof Coordinate){
						Coordinate c = (Coordinate)element;
						Coordinate c2 = ReprojectUtils.reproject(c.x, c.y, SmartDB.DATABASE_CRS, mapViewer.getMap().getViewportModel().getCRS());
						return String.valueOf(c2.x);
					}else if (element instanceof SimpleFeature){
						Double x = (Double) (((SimpleFeature)element).getAttribute(X_FIELD));
						Double y = (Double) (((SimpleFeature)element).getAttribute(Y_FIELD));
						Coordinate c2 = ReprojectUtils.reproject(x, y, SmartDB.DATABASE_CRS, mapViewer.getMap().getViewportModel().getCRS());
						return String.valueOf(c2.x);
					}
				} catch (Exception e) {
					return Messages.TrackPointDialog_ReprojectionError;
				}
				return ""; //$NON-NLS-1$
			}
		});
		column.getColumn().setText(Messages.TrackPointDialog_XColumnName);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(false);
		column.getColumn().setWidth(100);
		
		//-Y
		column = new TableViewerColumn(trackviewer,SWT.NONE);
		column.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element) {
				try {
					if (element instanceof Coordinate){
						Coordinate c = (Coordinate)element;
						Coordinate c2 = ReprojectUtils.reproject(c.x, c.y, SmartDB.DATABASE_CRS, mapViewer.getMap().getViewportModel().getCRS());
						return String.valueOf(c2.y);
					}else if (element instanceof SimpleFeature){
						Double x = (Double) (((SimpleFeature)element).getAttribute(X_FIELD));
						Double y = (Double) (((SimpleFeature)element).getAttribute(Y_FIELD));
						Coordinate c2 = ReprojectUtils.reproject(x, y, SmartDB.DATABASE_CRS, mapViewer.getMap().getViewportModel().getCRS());
						return String.valueOf(c2.y);
					}
				} catch (Exception e) {
					return Messages.TrackPointDialog_ReprojectionError;
				}
				return ""; //$NON-NLS-1$
			}
		});
		column.getColumn().setText(Messages.TrackPointDialog_YColumnName);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(false);
		column.getColumn().setWidth(100);
		
		//time
		column = new TableViewerColumn(trackviewer,SWT.NONE);
		column.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element) {
				if (element instanceof Coordinate){
					Date d = new Date( (long) ((Coordinate)element).z );
					return DATEFORMAT.format(d);
				}else if (element instanceof SimpleFeature){
					return (String)(((SimpleFeature)element).getAttribute(DATETIME_FIELD));
				}
				return ""; //$NON-NLS-1$
			}
		});
		column.getColumn().setText(Messages.TrackPointDialog_ZColumnName);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(false);
		column.getColumn().setWidth(150);
		
		
		trackviewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Set<FeatureId> ids = new HashSet<FeatureId>();
				for (Iterator<?> iterator = ((StructuredSelection)trackviewer.getSelection()).iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();
					if (x instanceof SimpleFeature){
						ids.add(((SimpleFeature) x).getIdentifier());
					}
				}
				
				try {
					//clear all
					pointStore.modifyFeatures(pointStore.getSchema().getDescriptor(SELECTED_FIELD).getName(), false, org.opengis.filter.Filter.INCLUDE);
					//select items
					pointStore.modifyFeatures(pointStore.getSchema().getDescriptor(SELECTED_FIELD).getName(), true, FACTORY.id(ids));
				} catch (IOException e) {
					SmartPatrolPlugIn.log(e.getMessage(), e);
				}
				pointLayer.refresh(null);
			}
		});
		
		Composite buttonPanel = new Composite(pntsTab, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnDelete = new Button(buttonPanel, SWT.NONE);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (trackviewer.getSelection().isEmpty()) return;
				StructuredSelection sel = (StructuredSelection) trackviewer.getSelection();
				Set<Integer> toDelete = new HashSet<Integer>();
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();
					if (x instanceof SimpleFeature){
						toDelete.add((Integer)((SimpleFeature) x).getAttribute(INDEX_FIELD));
					}
				}				
				if (toDelete.size() == 0) return;
				
				Coordinate[] oldc = editTrack.getLineString().getCoordinates();
				Coordinate[] newc = new Coordinate[oldc.length - toDelete.size()];
				if (newc.length == 0){
					//remove track entirely
					editTrack.setLineString(null);
					try{
						trackStore.removeFeatures(Filter.INCLUDE);
						pointStore.removeFeatures(Filter.INCLUDE);
					}catch(Exception ex){
						SmartPatrolPlugIn.displayLog(Messages.TrackPointDialog_LayerUpdateError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
					}					
					trackLayer.refresh(null);
					pointLayer.refresh(null);
					trackviewer.setInput(null);
					
					getButton(IDialogConstants.OK_ID).setEnabled(true);
					return;
				}else if (newc.length == 1){
					MessageDialog.openError(getShell(), Messages.TrackPointDialog_ErrorDialogTitle, Messages.TrackPointDialog_TrackError);
					return;
				}
				int newindex = 0;
				for (int i = 0; i < oldc.length; i ++){
					if (!toDelete.contains(i)){
						newc[newindex++] = oldc[i];
					}
				}
				GeometryFactory gf = new GeometryFactory();
				LineString ls = gf.createLineString(newc);
				editTrack.setLineString(ls);
				
				//update track and point layers
				try {
					trackStore.modifyFeatures(trackStore.getSchema().getDescriptor(GEOM_FIELD).getName(),
							ls, Filter.INCLUDE);
					trackLayer.refresh(null);
				} catch (IOException e1) {
					SmartPatrolPlugIn.displayLog(Messages.TrackPointDialog_UpdateTrackLayerError + "\n\n" + e1.getMessage(), e1); //$NON-NLS-1$
				}
				try {
					pointStore.removeFeatures(Filter.INCLUDE);
					createTrackPointFeatures((SimpleFeatureType)pointStore.getSchema());
					pointLayer.refresh(null);
					
					trackviewer.setInput(pointStore.getFeatures().toArray());
				} catch (IOException e1) {
					SmartPatrolPlugIn.displayLog(Messages.TrackPointDialog_UpdatePointLayerError + "\n\n" + e1.getMessage(), e1); //$NON-NLS-1$
				}
				getButton(IDialogConstants.OK_ID).setEnabled(true);
				
				
			}
		});
	}

	/*
	 * setup map area
	 */
	private void setupMap(Composite main) {
		Composite mapComp = new Composite(main, SWT.NONE);
		mapComp.setLayout(new GridLayout(2, false));
		mapComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		mapViewer = new MapViewer(mapComp, SWT.NONE);
		mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Map map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName(Messages.TrackPointDialog_MapName);
		mapViewer.setMap(map);
		//set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(SmartDB.DATABASE_CRS);
		
		final LoadDefaultLayersJob defaultLayer = new LoadDefaultLayersJob(mapViewer.getMap(), false, null);
		// we need to do this because this map is in a dialog box and
		// events does work correctly
		defaultLayer.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (TrackPointDialog.this.getContents() == null || 
						TrackPointDialog.this.getContents().isDisposed() || 
						mapViewer == null) return;
				
				mapViewer.getMap().getRenderManager().refresh(null);
				
				createTrackPointLayer();
				createTrackLayer();

			}
		});
		defaultLayer.schedule();
		
		
		ApplicationGIS.getToolManager().setCurrentEditor(this);
		String[] thisTools = new String[] {
				ZoomExtentTool.ID,
				PanTool.ID,
				ZoomTool.ID,
				PointSelectionTool.ID};

		MapToolComposite tools = new MapToolComposite(thisTools);
		tools.createComposite(mapComp);
		new MapInfoAreaComposite(mapComp, SWT.NONE, mapViewer) ;

		tools.selectTool(PanTool.ID);
		PointSelectionTool tool = (PointSelectionTool) ApplicationGIS.getToolManager().findTool(PointSelectionTool.ID);
		tool.addListener(new PointSelectionTool.PointSelectionListener() {
			@Override
			public void selection(ReferencedEnvelope bbox) {
				try {
					//update selection in table; this will automatically update
					//the map.
					//cannot simply use selected features as different objects are
					//made for the layer, therefore we compare feature ids.
					Envelope env = CRS.transform(bbox, SmartDB.DATABASE_CRS);
					bbox = new ReferencedEnvelope(env);
					FeatureCollection selected = pointStore.getFeatures(
							FACTORY.bbox(FACTORY.property(GEOM_FIELD), bbox));
					HashSet<FeatureId> items = new HashSet<FeatureId>();
					FeatureIterator<Feature> it = selected.features();
					while(it.hasNext()){
						Feature f = it.next();
						items.add(f.getIdentifier());
					}
					
					List<Object> newSelectionItems = new ArrayList<Object>();
					Object[] xx = (Object[]) trackviewer.getInput();
					for (Object x : xx){
						if (items.contains( ((SimpleFeature)x).getIdentifier() )){
							newSelectionItems.add(x);
						}
					}
					IStructuredSelection newSelection = new StructuredSelection(newSelectionItems);
					trackviewer.setSelection(newSelection);
					
					
				} catch (Exception e) {
					SmartPatrolPlugIn.displayLog(Messages.TrackPointDialog_SelectionError + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
				}
			}
		});
		
		
		//dispose of temporary layer when composite is disposed
		mapViewer.getControl().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				try{
					if (trackLayer != null){
						CatalogPlugin.getDefault().getLocalCatalog().remove(trackLayer.getGeoResource().service(null));
					}
					if (pointLayer != null){
						CatalogPlugin.getDefault().getLocalCatalog().remove(pointLayer.getGeoResource().service(null));
					}
				}catch (Exception ex){
					SmartPlugIn.log("Error removing service", ex); //$NON-NLS-1$
				}		
			}
		});
	}
	
	/*
	 * Creates and styles the track layer
	 */
	private void createTrackLayer(){
		try{
			SimpleFeatureType featureType = DataUtilities.createType("smart.PatrolTrack", "fid:String," + GEOM_FIELD + ":LineString:srid=4326"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
			final IGeoResource trackResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(featureType);
			
			trackStore = trackResource.resolve(FeatureStore.class, null);
			List<IGeoResource> layers = new ArrayList<IGeoResource>();
			layers.add(trackResource);
			
			Object[] data = new Object[2];
			data[0] = SmartUtils.encodeHex(editTrack.getUuid());
			data[1] = editTrack.getLineString();
			List<SimpleFeature> features = new ArrayList<SimpleFeature>(1);
			features.add(SimpleFeatureBuilder.build(featureType, data, (String)data[0]));
			ListFeatureCollection featureCollection = new ListFeatureCollection(featureType);
			featureCollection.addAll(features);
			trackStore.addFeatures(featureCollection);
			
			AddLayersCommand command = new AddLayersCommand(layers, mapViewer.getMap().getLayersInternal().size()) {
				@Override
				public void run(IProgressMonitor monitor) throws Exception {
					super.run(monitor);
					//set custom style for points layer
					trackLayer = getLayers().get(0);
					String sld = getTrackStyle();
					XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
					SLDContent c = new SLDContent();
					Style style = (Style)c.load(memento);
					trackLayer.getStyleBlackboard().clear();
					trackLayer.getStyleBlackboard().put(SLDContent.ID, style);
					
					//zoom to buffered layer
					ReferencedEnvelope bounds = trackLayer.getBounds(monitor, mapViewer.getMap().getViewportModel().getCRS());
					bounds.expandBy(Math.max(bounds.getWidth(), bounds.getHeight()) * 0.1);
					mapViewer.getMap().sendCommandASync(new SetViewportBBoxCommand(bounds));
				}
			};
			getMap().sendCommandASync(command);
						
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.TrackPointDialog_ErrorAddingTrackLayer + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}
	    
	}
	
	/*
	 * Creates the point features and adds them to the points layers
	 */
	private void createTrackPointFeatures(SimpleFeatureType featureType) throws IOException{
		final List<SimpleFeature> features = new ArrayList<SimpleFeature>(1);
		
		for (int i = 0; i < editTrack.getLineString().getCoordinates().length; i ++){
			Coordinate c = editTrack.getLineString().getCoordinates()[i];
		
			Object[] data = new Object[7];
			data[0] = SmartUtils.encodeHex(editTrack.getUuid()) + "." + i; //$NON-NLS-1$
			data[1] = c.x;
			data[2] = c.y;
			data[3] = DATEFORMAT.format(new Date( (long) ((Coordinate)c).z ));
			data[4] = i;
			data[5] = false;
			data[6] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(c.x, c.y));
			
			features.add(SimpleFeatureBuilder.build(featureType, data, (String)data[0]));
		}
		
		ListFeatureCollection featureCollection = new ListFeatureCollection(featureType);
		featureCollection.addAll(features);
		pointStore.addFeatures(featureCollection);
	}
	
	/*
	 * Creates the point features layers and initializes the points
	 */
	private void createTrackPointLayer(){
		try{
			SimpleFeatureType featureType = DataUtilities.createType("smart.PatrolTrackPoint", "fid:String," + X_FIELD + ":Double," + Y_FIELD + ":Double," + DATETIME_FIELD + ":String," + INDEX_FIELD + ":Integer," + SELECTED_FIELD + ":Boolean," + GEOM_FIELD + ":Point:srid=4326"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	
			final IGeoResource trackResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(featureType);
			pointStore = trackResource.resolve(FeatureStore.class, null);
			List<IGeoResource> layers = new ArrayList<IGeoResource>();
			layers.add(trackResource);
			
			createTrackPointFeatures(featureType);
			
			AddLayersCommand command = new AddLayersCommand(layers, mapViewer.getMap().getLayersInternal().size()) {
				@Override
				public void run(IProgressMonitor monitor) throws Exception {
					super.run(monitor);
					//set custom style for points layer
					
					pointLayer = getLayers().get(0);
					String sld = getPointStyle();
					XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
					SLDContent c = new SLDContent();
					Style style = (Style)c.load(memento);
					pointLayer.getStyleBlackboard().clear();
					pointLayer.getStyleBlackboard().put(SLDContent.ID, style);
					pointLayer.refresh(null);

				}
			};
			getMap().sendCommandASync(command);
			
			
			//set points
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					try{
						trackviewer.setInput(pointStore.getFeatures().toArray());
					}catch(Exception ex){
						SmartPatrolPlugIn.log(ex.getMessage(), ex);
					}
				}});
			
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.TrackPointDialog_ErrorAddingPointLayer + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}
	    
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
		Button btn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btn.setEnabled(false);
	}
	
	@Override
	/*
	 * Saves changes to track
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed(){
		Patrol p = track.getPatrolLegDay().getPatrolLeg().getPatrol();
		PatrolLegDay pld = track.getPatrolLegDay();
		
		//save then close
		if (editTrack.getLineString() == null){
			//delete track
			track.getPatrolLegDay().setTrack(null);
			track.setPatrolLegDay(null);
		}else{
			track.setLineString(editTrack.getLineString());
		}
		
		//save and fire
		SavePatrolPartJob saveJob = new SavePatrolPartJob(p,pld); 		
		saveJob.schedule();
		try{
			saveJob.join();
			PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_TRACKS, pld);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}catch (InterruptedException ex){
			throw new IllegalStateException("Save Job Interrupted", ex); //$NON-NLS-1$
		}
	}
	
	
	/**
	 * Dialog is resizable
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	public Map getMap() {
		return mapViewer.getMap();
	}

	@Override
	public void openContextMenu() {
		mapViewer.openContextMenu();
	}

	@Override
	public void setFont(Control textArea) {
		mapViewer.setFont(textArea);
	}

	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapViewer.setSelectionProvider(selectionProvider);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}

	/**
	 * point style
	 * @return
	 */
	private String getPointStyle() {
		return	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+ //$NON-NLS-1$
		"<styleEntry version=\"1.0\" type=\"SLDStyle\">"+ //$NON-NLS-1$
		"&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;"+ //$NON-NLS-1$
		"	&lt;sld:UserStyle xmlns=\"http://www.opengis.net/sld\""+ //$NON-NLS-1$
		"		xmlns:sld=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\""+ //$NON-NLS-1$
		"		xmlns:gml=\"http://www.opengis.net/gml\"&gt;"+ //$NON-NLS-1$
		"		&lt;sld:Name&gt;Default Styler&lt;/sld:Name&gt;"+ //$NON-NLS-1$
		"		&lt;sld:Title /&gt;"+ //$NON-NLS-1$
		"		&lt;sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
		"			&lt;sld:Name&gt;simple&lt;/sld:Name&gt;"+ //$NON-NLS-1$
		"			&lt;sld:FeatureTypeName&gt;Feature&lt;/sld:FeatureTypeName&gt;"+ //$NON-NLS-1$
		"			&lt;sld:SemanticTypeIdentifier&gt;generic:geometry&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$
		"			&lt;sld:SemanticTypeIdentifier&gt;simple&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$

		//rule for not selected points (same as default)
		"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
		"				&lt;ogc:Filter&gt;"+ //$NON-NLS-1$
		"                        &lt;ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
		"                            &lt;ogc:PropertyName&gt;selected&lt;/ogc:PropertyName&gt;"+ //$NON-NLS-1$
		"                            &lt;ogc:Literal&gt;false&lt;/ogc:Literal&gt;"+ //$NON-NLS-1$
		"                        &lt;/ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
		"				&lt;/ogc:Filter&gt;"+ //$NON-NLS-1$
		"				&lt;sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
		"					&lt;sld:Graphic&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
		"							&lt;sld:WellKnownName&gt;circle&lt;/sld:WellKnownName&gt;" + //$NON-NLS-1$			
		"							&lt;sld:Fill&gt;"+ //$NON-NLS-1$
		"								&lt;sld:CssParameter name=\"fill\"&gt;#1B9E77&lt;/sld:CssParameter&gt;"+ //$NON-NLS-1$
		"							&lt;/sld:Fill&gt;"+ //$NON-NLS-1$
		"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Size&gt;7.0&lt;/sld:Size&gt;"+ //$NON-NLS-1$
		"					&lt;/sld:Graphic&gt;"+ //$NON-NLS-1$
		"				&lt;/sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
		"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
		
		//rule for selected points
		"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
		"				&lt;ogc:Filter&gt;"+ //$NON-NLS-1$
		"                        &lt;ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
		"                            &lt;ogc:PropertyName&gt;selected&lt;/ogc:PropertyName&gt;"+ //$NON-NLS-1$
		"                            &lt;ogc:Literal&gt;true&lt;/ogc:Literal&gt;"+ //$NON-NLS-1$
		"                        &lt;/ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
		"				&lt;/ogc:Filter&gt;"+ //$NON-NLS-1$
		"				&lt;sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
		"					&lt;sld:Graphic&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
		"							&lt;sld:WellKnownName&gt;circle&lt;/sld:WellKnownName&gt;" + //$NON-NLS-1$		
		"							&lt;sld:Fill&gt;"+ //$NON-NLS-1$
		"								&lt;sld:CssParameter name=\"fill\"&gt;#FF0000&lt;/sld:CssParameter&gt;"+ //$NON-NLS-1$
		"							&lt;/sld:Fill&gt;"+ //$NON-NLS-1$
		"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Size&gt;9.0&lt;/sld:Size&gt;"+ //$NON-NLS-1$
		"					&lt;/sld:Graphic&gt;"+ //$NON-NLS-1$
		"				&lt;/sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
		"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
		
		
		"		&lt;/sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
		"	&lt;/sld:UserStyle&gt;"+ //$NON-NLS-1$
		"</styleEntry>"; //$NON-NLS-1$
	
	}
	
	/**
	 * 
	 * @return track style
	 */
	private String getTrackStyle() {
		return	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+ //$NON-NLS-1$
		"<styleEntry version=\"1.0\" type=\"SLDStyle\">"+ //$NON-NLS-1$
		"&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;"+ //$NON-NLS-1$
		"	&lt;sld:UserStyle xmlns=\"http://www.opengis.net/sld\""+ //$NON-NLS-1$
		"		xmlns:sld=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\""+ //$NON-NLS-1$
		"		xmlns:gml=\"http://www.opengis.net/gml\"&gt;"+ //$NON-NLS-1$
		"		&lt;sld:Name&gt;Default Styler&lt;/sld:Name&gt;"+ //$NON-NLS-1$
		"		&lt;sld:Title /&gt;"+ //$NON-NLS-1$
		"		&lt;sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
		"			&lt;sld:Name&gt;simple&lt;/sld:Name&gt;"+ //$NON-NLS-1$
		"			&lt;sld:FeatureTypeName&gt;Feature&lt;/sld:FeatureTypeName&gt;"+ //$NON-NLS-1$
		"			&lt;sld:SemanticTypeIdentifier&gt;generic:geometry&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$
		"			&lt;sld:SemanticTypeIdentifier&gt;simple&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$
		"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
		"				&lt;sld:LineSymbolizer&gt;"+ //$NON-NLS-1$
		"					&lt;sld:Stroke&gt;"+ //$NON-NLS-1$
		"						&lt;sld:CssParameter name=\"stroke\"&gt;#C10000&lt;/sld:CssParameter&gt;"+ //$NON-NLS-1$
		"					&lt;/sld:Stroke&gt;"+ //$NON-NLS-1$
		"				&lt;/sld:LineSymbolizer&gt;"+ //$NON-NLS-1$
		"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
		"		&lt;/sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
		"	&lt;/sld:UserStyle&gt;"+ //$NON-NLS-1$
		"</styleEntry>"; //$NON-NLS-1$
	
	}
	
}
