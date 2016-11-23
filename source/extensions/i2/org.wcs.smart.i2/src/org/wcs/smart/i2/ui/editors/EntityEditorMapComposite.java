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
package org.wcs.smart.i2.ui.editors;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.legend.Glyph;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.commands.ChangeCRSCommand;
import org.locationtech.udig.project.internal.render.RenderPackage;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.ui.AnimationUpdater;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.commands.IDrawCommand;
import org.locationtech.udig.project.ui.internal.FeatureAnimation;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.internal.commands.draw.DrawFeatureCommand;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseMotionListener;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.project.ui.tool.IToolManager;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.locationtech.udig.ui.IBlockingSelection;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.udig.AddContentFilterLayersCommand;
import org.wcs.smart.i2.udig.ContentFilterLayerImpl;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.i2.udig.entity.IntelEntityDataSource;
import org.wcs.smart.i2.udig.entity.IntelEntityService;
import org.wcs.smart.i2.udig.entity.IntelEntityServiceExtension;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.ProjectionDialog;
import org.wcs.smart.ui.map.ScaleRatioComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.ClearSelectionTool;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class EntityEditorMapComposite extends Composite implements MapPart{

	private enum LocationTableColumn{
		ID("ID"),
		DATETIME("Date/Time"),
		COMMENT("Comment"),
		RECORD("Record"),
		RECORDDATE("Record Date"),
		OBSERVATION("Observation");
		
		String guiName;
	
		LocationTableColumn(String guiName){
			this.guiName = guiName;
		}
		
		public String getLabel(IntelLocation location){
			if (this == ID) return location.getId();
			if (this == DATETIME) return DateFormat.getDateTimeInstance().format(location.getDateTime());
			if (this==COMMENT) return location.getComment() == null ? "" : location.getComment();
			if (this == RECORD) return location.getRecord().getTitle();
			if (this == RECORDDATE) return DateFormat.getDateTimeInstance().format(location.getRecord().getDateCreated());
			if (this == OBSERVATION) return "TODO:";
			return "";
			
		}
	}
	
	private EntityEditor editor;

	// map components
	private Label lblCoordinates;
	private Button lblSRID;
	protected MapViewer mapViewer;
	protected MapToolComposite tools;
	
	private DateFilterDropDownComposite dateComp;
	private Date[] dateFilter = null;
	
	private IPartListener2 partlistener = new IPartListener2(){
	        public void partActivated( IWorkbenchPartReference partRef ) {
	            if (partRef.getPart(false) == editor) {
	                IToolManager toolManager = ApplicationGIS.getToolManager();
	                toolManager.setCurrentEditor( mapViewer );
                	tools.selectLastTool();
	            }
	        }

	        public void partBroughtToTop( IWorkbenchPartReference partRef ) {
	        }

	        public void partClosed( IWorkbenchPartReference partRef ) {
	        }

	        public void partDeactivated( IWorkbenchPartReference partRef ) {
	        }

	        public void partOpened( IWorkbenchPartReference partRef ) {
	        }

	        public void partHidden( IWorkbenchPartReference partRef ) {
	        	if (partRef.getPart(false) == editor) {
	        		deregisterFeatureFlasher();
	        	}
	        }

	        public void partVisible( IWorkbenchPartReference partRef ) {
	        	if (partRef.getPart(false) == editor) {
	        		registerFeatureFlasher();
	        	}
	        }

	        public void partInputChanged( IWorkbenchPartReference partRef ) {
	        }

	    };
	    
    private FlashFeatureListener selectFeatureListener = new FlashFeatureListener();
    private boolean flashFeatureRegistered = false;
    private List<ContentFilterLayerImpl> locationLayers = null;
	
    private FormToolkit toolkit;
    private TableViewer locationTable;
    
	public EntityEditorMapComposite(Composite parent, EntityEditor parentEditor, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.editor = parentEditor;
		this.toolkit = toolkit;
		
		createPartControl();
		
		//add default layers
		new LoadDefaultLayersJob(getMap()).schedule();
		
	}

	private IntelEntityService service = null;
	
	private void addLayers(){
		locationLayers = new ArrayList<ContentFilterLayerImpl>();
		final Date[] dFilters = new Date[2];
		if (dateComp != null){
			if (dateComp.getDateFilter() == DateFilter.CUSTOM){
				dFilters[0] = dateComp.getCustomStartDate();
				dFilters[1] = dateComp.getCustomEndDate();
			}else{
				dFilters[0] = dateComp.getDateFilter().getStartDate();
				dFilters[1] = dateComp.getDateFilter().getEndDate();
			}
		}

		Job j = new Job("add location layers job"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				HashMap<String, Serializable> params = new HashMap<String,Serializable>();
				params.put(IntelEntityServiceExtension.ENTITY_UUID_KEY, UuidUtils.uuidToString(editor.getEntity().getUuid()));
				service = new IntelEntityService(params);
				try {
					Filter dateFilter = IntelEntityDataSource.createDateFilter(dFilters[0], dFilters[1]);
					AddContentFilterLayersCommand cmd = new AddContentFilterLayersCommand(service.resources(monitor), 1, dateFilter){
						 public void run( IProgressMonitor monitor ) throws Exception {
							 super.run(monitor);
							 locationLayers.clear();
							 for (Layer layer : getLayers()){
								 if (layer instanceof ContentFilterLayerImpl){
									 locationLayers.add((ContentFilterLayerImpl) layer);
								 }
							 }
						 }
					};
					getMap().sendCommandASync(cmd);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
	}
	 /**
     * registers a listener with the current page that flashes a feature each time the current
     * selected feature changes.
     */
    protected synchronized void registerFeatureFlasher() {
        if (!flashFeatureRegistered) {
            flashFeatureRegistered = true;
            IWorkbenchPage page = editor.getSite().getPage();
            page.addPostSelectionListener(selectFeatureListener);
        }
    }

    protected synchronized void deregisterFeatureFlasher() {
        flashFeatureRegistered = false;
        //AnimationUpdater.cancel(getMap().getRenderManager().getMapDisplay());
        editor.getSite().getPage().removePostSelectionListener(selectFeatureListener);
    }
    
	
	
	
	/**
	 *  Creates the map
	 * 
	 */
	public void createPartControl() {
		setLayout(new GridLayout());
		
		SashForm parent = new SashForm(this, SWT.VERTICAL);
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				
		Composite mapPart = toolkit.createComposite(parent);
		mapPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		DateFilterComposite.DateFilter[] defaultFilters = new DateFilter[]{
					DateFilter.LAST_30_DAYS,
					DateFilter.LAST_60_DAYS,
					DateFilter.LAST_YEAR,
					DateFilter.LAST_5_YEARS,
					DateFilter.ALL,
					DateFilter.CUSTOM
		};
		DateFilterComposite.DateFilter initialDateFilter = DateFilter.LAST_YEAR;
		dateFilter = new Date[]{initialDateFilter.getStartDate(), initialDateFilter.getEndDate()};
		dateComp = new DateFilterDropDownComposite(mapPart, defaultFilters, initialDateFilter);
        dateComp.setBounds(0, 0, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        dateComp.addChangeListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				dateComp.setBounds(0, 0, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
				dateFilterChanged();
			}
		});
        
		Composite composite = new Composite(mapPart, SWT.NONE);
		mapPart.addListener(SWT.Resize, new Listener(){
			@Override
			public void handleEvent(Event event) {
				composite.setBounds(0,0,mapPart.getBounds().width-5,mapPart.getBounds().height-5);		
			}
        });
		GridLayout layout = new GridLayout(2,false);
    	layout.marginBottom=0;
    	layout.marginHeight = 0;
    	layout.marginLeft = 0;
    	layout.marginRight = 0;
    	layout.marginTop = 0;
    	layout.marginWidth = 0;
    	layout.horizontalSpacing = 0;
    	layout.verticalSpacing = 2;
		composite.setLayout(layout);

        mapViewer = new MapViewer(composite,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
        mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        Map map = (Map) ProjectFactory.eINSTANCE.createMap();
        map.setName(editor.getEditorInput().getName());
        mapViewer.setMap(map);
        //set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(GeometryUtils.SMART_CRS);
	    
		String[] strtools = Arrays.copyOf(MapToolComposite.DEFAULT_MAP_TOOLS, MapToolComposite.DEFAULT_MAP_TOOLS.length + 1);
		strtools[strtools.length - 1] = ClearSelectionTool.ID;
       	tools = new MapToolComposite(strtools);
		tools.createComposite(composite);
		tools.selectTool("org.locationtech.udig.tools.Pan"); //$NON-NLS-1$
		
        Composite infoArea = new Composite(composite, SWT.NONE);
        GridLayout gl = new GridLayout(5, false);
        gl.marginTop = gl.marginBottom = gl.marginHeight= 0;
        gl.marginRight = 0;
        gl.marginWidth = 0;
        infoArea.setLayout(gl);
        infoArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false,2, 1));
        lblCoordinates = new Label(infoArea, SWT.NONE);
        lblCoordinates.setText(SmartMapEditorPart.COORDINATE_LABEL);
        lblCoordinates.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        lblCoordinates.setAlignment(SWT.RIGHT);
        
        Label lblSeparator = new Label(infoArea, SWT.SEPARATOR | SWT.VERTICAL);
        GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        lblSeparator.setLayoutData(gd);
        
        ScaleRatioComposite scale = new ScaleRatioComposite(infoArea, getMap());
        scale.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        
        
        lblSeparator = new Label(infoArea, SWT.SEPARATOR | SWT.VERTICAL);
        gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        lblSeparator.setLayoutData(gd);
        
        lblSRID = new Button(infoArea, SWT.NONE);
        lblSRID.setText(map.getViewportModel().getCRS().getName().getCode());
        lblSRID.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
        lblSRID.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ProjectionDialog pd = new ProjectionDialog(editor.getSite().getShell(), mapViewer.getMap().getViewportModel().getCRS());
				if (pd.open() == IDialogConstants.OK_ID){
					try{
						ChangeCRSCommand command = new ChangeCRSCommand(
								ReprojectUtils.stringToCrs(pd.getSelection().getDefinition()));
						getMap().sendCommandASync(command);
					}catch (Exception ex){
						SmartPlugIn.displayLog(SmartMapEditorPart.ERROR_SETTING_MAP_PROJECTION + ex.getLocalizedMessage(), ex);
					}	
				}
			}
		});
        
        final Map thisMap = map;
        map.getViewportModelInternal().eAdapters().add(new AdapterImpl(){
        	public void notifyChanged(Notification notification) {
        		if (notification.getEventType() == Notification.SET &&
        				notification.getFeatureID(thisMap.getViewportModelInternal().getClass()) == RenderPackage.VIEWPORT_MODEL__CRS){
        			updateLabel();
        		}
        	}
        	
        });
        
      
        
        mapViewer.getViewport().addMouseMotionListener(new MapMouseMotionListener() {
			@Override
			public void mouseMoved(MapMouseEvent event) {
				event.getPoint();
				Coordinate c = mapViewer.getMap().getViewportModelInternal().pixelToWorld(event.x, event.y);
				lblCoordinates.setText(format(c.x) + SmartMapEditorPart.COORDINATE_XYSEPARATOR + format(c.y));
			}
			
			private String format(double d){
				 DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
		         format.setMaximumFractionDigits(4);
		         format.setMinimumIntegerDigits(1);
		         format.setGroupingUsed(false);
		         String string = format.format(d);
		         return string;
			}
			@Override
			public void mouseHovered(MapMouseEvent event) {
			}
			
			@Override
			public void mouseDragged(MapMouseEvent event) {
			}
		});   
        mapViewer.init(editor);
 
        
        editor.getSite().getWorkbenchWindow().getPartService().addPartListener(partlistener);
        registerFeatureFlasher();
        
        Composite tableArea = createTableArea(parent);
        tableArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        parent.setWeights(new int[] {7,3});
	}

	public Date[] getDateFilter(){
		return dateFilter;
	}
	private void dateFilterChanged(){
		if (dateComp.getDateFilter() == DateFilter.CUSTOM){
			dateFilter = new Date[]{dateComp.getCustomStartDate(), dateComp.getCustomEndDate()};
		}else{
			dateFilter = new Date[]{dateComp.getDateFilter().getStartDate(),dateComp.getDateFilter().getEndDate()};
		}
		loadLocationsLink.schedule();
		
		if (service != null){
			Filter udigDateFilter = IntelEntityDataSource.createDateFilter(dateFilter[0], dateFilter[1]);
			 for (ContentFilterLayerImpl layer : locationLayers){
				 layer.setContentFilter(udigDateFilter);
			 }
			getMap().getRenderManager().refresh(null);
		}
	}

	public Composite createTableArea(Composite parent){
		Composite locationsTableComp = toolkit.createComposite(parent);
		locationsTableComp.setLayout(new GridLayout());
		((GridLayout)locationsTableComp.getLayout()).marginWidth = 0;
		((GridLayout)locationsTableComp.getLayout()).marginHeight = 0;
		
		locationTable = new TableViewer(locationsTableComp, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.BORDER);
		locationTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		locationTable.setContentProvider(ArrayContentProvider.getInstance());
		locationTable.getTable().setLinesVisible(true);
		locationTable.getTable().setHeaderVisible(true);
		
		TableViewerColumn geomTypeColumn = new TableViewerColumn(locationTable, SWT.CENTER);
		geomTypeColumn.getColumn().setText("");
		geomTypeColumn.getColumn().setWidth(25);
		geomTypeColumn.setLabelProvider(new ColumnLabelProvider() {
			
			private Image polygon = AWTSWTImageUtils.createSWTImage(Glyph.polygon(new Color(15,58,122, 50), new Color(15,58,122), 1));
			private Image point = AWTSWTImageUtils.createSWTImage(Glyph.point(new Color(15,58,122), new Color(15,58,122, 50)));
			
			@Override
			public void dispose(){
				polygon.dispose();
				point.dispose();
				super.dispose();
			}
			@Override
			public String getText(Object element) {
				return "";
			}
			
			@Override
			public Image getImage(Object element) {
				if (element instanceof IntelEntityLocation){
					if (((IntelEntityLocation) element).getLocation().isPoint()) return point;
					if (((IntelEntityLocation) element).getLocation().isPolygon()) return polygon;
				}
				return null;
			}
		});
		
		for (LocationTableColumn column : LocationTableColumn.values()){
			TableViewerColumn col = new TableViewerColumn(locationTable, SWT.LEFT);
			col.getColumn().setText(column.guiName);
			col.getColumn().setWidth(150);
			
			col.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof IntelEntityLocation){
						IntelLocation location = ((IntelEntityLocation)element).getLocation();
						return column.getLabel(location);
					}
					return super.getText(element);
				}
			});
		}
		
		locationTable.setInput(new String[]{DialogConstants.LOADING_TEXT});
		locationTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				//filter features on map
				try {
					highlightFeature(getSelectedLocation());
				} catch (IOException e) {
				}
			}
		});
		
		Menu mnu = new Menu(locationTable.getTable());
		locationTable.getTable().setMenu(mnu);
		
		MenuItem mi = new MenuItem(mnu, SWT.PUSH);
		mi.setText("Open Record");
		mi.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getSelectedLocation() != null){
					(new OpenRecordHandler()).openRecord(getSelectedLocation().getRecord(),false);
				}
			}
		});
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mi.setEnabled(getSelectedLocation() != null);
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		return locationsTableComp;
	}
	
	private IntelLocation getSelectedLocation(){
		if (!locationTable.getSelection().isEmpty()){
			Object x = ((IStructuredSelection)locationTable.getSelection()).getFirstElement();
			if (x instanceof IntelEntityLocation){
				return ((IntelEntityLocation) x).getLocation();
			}
		}
		return null;
	}
	
	//udig does not support selection from multiple layers 
	private void highlightFeature(IntelLocation location) throws IOException{
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		if (location == null){
			for (Layer l : locationLayers){
				l.setFilter(Filter.EXCLUDE);
			}
		}else{
			for (Layer l : locationLayers){
				if ((l.getGeoResource().resolve(FeatureSource.class, null).getSchema().getName().getLocalPart().equals(LocationLayerType.POINT.name()) && location.isPoint()) ||
					( l.getGeoResource().resolve(FeatureSource.class, null).getSchema().getName().getLocalPart().equals(LocationLayerType.POLYGON.name()) && location.isPolygon())){
					l.setFilter(ff.equals(ff.property("system_id"), ff.literal(UuidUtils.uuidToString(location.getUuid()))));
				}else{
					l.setFilter(Filter.EXCLUDE);
				}
			}
		}
	}
		
	private void updateLabel() {
		editor.getSite().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (lblSRID == null || lblSRID.isDisposed()) return;
				lblSRID.setText(getMap().getViewportModel().getCRS().getName()
						.getCode());
				lblSRID.getParent().layout();
			}
		});

	}
        
	@Override
    public Map getMap() {
        return mapViewer.getMap();
    }
	
	public void refresh(){
		if (locationLayers == null){
			//add location layers
			addLayers();
		}else{
			//refresh existing layers
			getMap().getRenderManager().refresh(null);
		}
		
		loadLocationsLink.schedule();
	}
	
    @Override
    public void dispose() {
        super.dispose();
        deregisterFeatureFlasher();
        editor.getSite().getWorkbenchWindow().getPartService().removePartListener(partlistener);
        
        this.partlistener = null;
        this.selectFeatureListener = null;
        
        if (mapViewer != null && mapViewer.getViewport() != null && getMap() != null){
        	mapViewer.getViewport().removePaneListener(getMap().getViewportModelInternal());
        }
        if (getMap() != null)  getMap().getViewportModelInternal().setInitialized(false);
        if (mapViewer != null){
        	if (mapViewer.getRenderManager() != null){
        		mapViewer.getRenderManager().disableRendering();
                mapViewer.getRenderManager().stopRendering();
               	mapViewer.getRenderManager().dispose();		
        	}
            mapViewer.dispose();
        }
    }

    public void openContextMenu() {
    	mapViewer.openContextMenu();
    }

    public void setFont( Control control ) {
    	mapViewer.setFont(control);
    }

    public void setSelectionProvider( IMapEditorSelectionProvider selectionProvider ) {
    	if (selectionProvider == null) {
            throw new NullPointerException("selection provider must not be null!"); //$NON-NLS-1$
        }
    	selectionProvider.setActiveMap(mapViewer.getMap(), mapViewer);
    	mapViewer.setSelectionProvider(selectionProvider);
        
    }

	

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getStatusLineManager()
	 */
	@Override
	public IStatusLineManager getStatusLineManager() {
		return ((IEditorSite)editor.getSite()).getActionBars().getStatusLineManager();		
	}
	
	
	private class FlashFeatureListener implements ISelectionListener {

		
        public void selectionChanged( IWorkbenchPart part, final ISelection selection ) {
            if (part == editor || editor.getSite().getPage().getActivePart() != part
                    || selection instanceof IBlockingSelection)
                return;
            
            ISafeRunnable sendAnimation = new ISafeRunnable(){
                public void run() {
                    if (selection instanceof IStructuredSelection) {
                        IStructuredSelection s = (IStructuredSelection) selection;
                        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
                        for( Iterator<?> iter = s.iterator(); iter.hasNext(); ) {
                            Object element = iter.next();

                            if (element instanceof SimpleFeature) {
                                SimpleFeature feature = (SimpleFeature) element;
                                features.add(feature);
                            }
                        }
                        if (features.size() == 0)
                            return;
                        if (!mapViewer.getRenderManager().isDisposed()) {
                        	FeatureAnimation anim = createAnimation(features);
                            if (anim != null){
                                AnimationUpdater.runTimer(getMap().getRenderManager().getMapDisplay(), anim);
                            }
                            
                        }
                    }
                }
                public void handleException( Throwable exception ) {
                	SmartPlugIn.log("Exception preparing animation", exception); //$NON-NLS-1$
                }
            };

            try {
                sendAnimation.run();
            } catch (Exception e) {
            	SmartPlugIn.log("", e); //$NON-NLS-1$
            }
        }

        private FeatureAnimation createAnimation( List<SimpleFeature> current ) {
            final List<IDrawCommand> commands = new ArrayList<IDrawCommand>();
            for( SimpleFeature feature : current ) {
                if (feature == null || feature.getFeatureType().getGeometryDescriptor() == null)
                    continue;
                DrawFeatureCommand command = null;
                if (feature instanceof IAdaptable) {
                    Layer layer = (Layer) ((IAdaptable) feature).getAdapter(Layer.class);
                    if (layer != null)
                        try {
                            command = new DrawFeatureCommand(feature, layer);
                        } catch (IOException e) {
                            // do nothing... thats life
                        	e.printStackTrace();
                        }
                }
                if (command == null) {
                    command = new DrawFeatureCommand(feature);
                }
                command.setMap(getMap());
                commands.add(command);
            }
            Rectangle2D rect = new Rectangle();
            
            final Rectangle validArea = (Rectangle) rect;
            FeatureAnimation anim = new FeatureAnimation(commands, validArea);
            return anim;
        }
    }
	
	/* initial zoom function */
	protected ReferencedEnvelope initialZoom = null;
	
	public void setInitialZoom(ReferencedEnvelope zoom){
		this.initialZoom = zoom;
	}

	private Job loadLocationsLink = new Job("loading location links"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(() -> { 
				if (locationTable.getTable().isDisposed()) return;
				locationTable.setInput(new String[]{DialogConstants.LOADING_TEXT});
			});
			final List<IntelEntityLocation> alllocations  = new ArrayList<IntelEntityLocation>();
			Session s = HibernateManager.openSession();
			try{
				alllocations.addAll(EntityManager.INSTANCE.getEntityLocations(s, editor.getEntity().getUuid(), dateFilter));
				for (IntelEntityLocation l : alllocations){
					l.getLocation().getId();
					l.getLocation().getRecord().getTitle();
				}
			}finally{
				s.close();
			}
			Collections.sort(alllocations,
					(x,y)-> -1*x.getLocation().getDateTime().compareTo(y.getLocation().getDateTime()));
			
			Display.getDefault().syncExec(() -> { 
				if (locationTable.getTable().isDisposed()) return;
				locationTable.setInput(alllocations);
			});
			return Status.OK_STATUS;
		}
		
	};
}
