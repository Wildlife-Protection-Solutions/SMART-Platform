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
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.legend.Glyph;
import org.hibernate.Session;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.model.IntelValueItem;
import org.wcs.smart.i2.udig.AddContentFilterLayersCommand;
import org.wcs.smart.i2.udig.ContentFilterLayerImpl;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.i2.udig.entity.IntelEntityDataSource;
import org.wcs.smart.i2.udig.entity.IntelEntityService;
import org.wcs.smart.i2.udig.entity.IntelEntityServiceExtension;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.io.ParseException;

/**
 * Entity map editor composite 
 * @author Emily
 *
 */
public class EntityEditorMapComposite extends Composite implements MapPart{

	private enum LocationTableColumn{
		ID(Messages.EntityEditorMapComposite_IDColumnName),
		DATETIME(Messages.EntityEditorMapComposite_DateTimeColumnName),
		COMMENT(Messages.EntityEditorMapComposite_CommentColumnName),
		RECORD(Messages.EntityEditorMapComposite_RecordColumnName),
		RECORDDATE(Messages.EntityEditorMapComposite_RecordDatecolumnName),
		OBSERVATION(Messages.EntityEditorMapComposite_ObservationColumnName);
		
		String guiName;
	
		LocationTableColumn(String guiName){
			this.guiName = guiName;
		}
		
		public String getLabel(IntelLocation location){
			if (this == ID) return location.getId();
			if (this == DATETIME) return DateFormat.getDateTimeInstance().format(location.getDateTime());
			if (this==COMMENT) return location.getComment() == null ? "" : location.getComment(); //$NON-NLS-1$
			if (this == RECORD) return location.getRecord().getTitle();
			if (this == RECORDDATE) return DateFormat.getDateTimeInstance().format(location.getRecord().getDateCreated());
			if (this == OBSERVATION){
				int cnt = 0;
				if (location.getObservations() != null ){
					cnt = location.getObservations().size();
				}
				return MessageFormat.format(Messages.EntityEditorMapComposite_ObservationsLabel, cnt);
			};
			return ""; //$NON-NLS-1$
			
		}
	}
	
	private EntityEditor editor;

	// map components
	private MapComposite mapPart;
	
	private DateFilterDropDownComposite dateComp;
	private Date[] dateFilter = null;
	
    private List<ContentFilterLayerImpl> locationLayers = null; //record locations layers
	
    private FormToolkit toolkit;
    private TableViewer locationTable;
    
    private IntelEntityService service = null;
    private LocationAttributeMapLayer locationLayer;	//entity attribute layer
    
	public EntityEditorMapComposite(Composite parent, EntityEditor parentEditor, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.editor = parentEditor;
		this.toolkit = toolkit;
		
		createPartControl();
	}

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

		Job j = new Job("add location layers job"){ //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				HashMap<String, Serializable> params = new HashMap<String,Serializable>();
				params.put(IntelEntityServiceExtension.ENTITY_UUID_KEY, UuidUtils.uuidToString(editor.getEntity().getUuid()));
				service = new IntelEntityService(params);
				try {
					Filter dateFilter = IntelEntityDataSource.createDateFilter(dFilters[0], dFilters[1]);
					List<? extends IGeoResource> resources = service.resources(monitor);
					for (Iterator<? extends IGeoResource> iterator = resources.iterator(); iterator.hasNext();) {
						IGeoResource iGeoResource = (IGeoResource) iterator.next();
						if (iGeoResource.getIdentifier().getRef().equals(LocationLayerType.ATTRIBUTE.name())){
							iterator.remove();
						}
						
					}
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
		
		
		//add location attributes
		boolean hasPosition = false;
		for (IntelEntityTypeAttribute a : editor.getEntity().getEntityType().getAttributes()){
			if (a.getAttribute().getType() == AttributeType.POSITION){
				hasPosition = true;
			}
		}
		if (hasPosition){
			locationLayer = new LocationAttributeMapLayer(getMap(), Messages.EntityEditorMapComposite_LayerName, UuidUtils.uuidToString(editor.getEntity().getUuid()));
			locationLayer.createValueLayers(editor.getEntity().getAttributes());
		}
	}
	
	
	/**
	 *  Creates the map
	 * 
	 */
	public void createPartControl() {
		setLayout(new GridLayout());
		
		SashForm parent = new SashForm(this, SWT.VERTICAL);
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite mapArea = toolkit.createComposite(parent);

		DateFilterComposite.DateFilter[] defaultFilters = new DateFilter[]{
					DateFilter.LAST_30_DAYS,
					DateFilter.LAST_60_DAYS,
					DateFilter.LAST_1_YEARS,
					DateFilter.LAST_5_YEARS,
					DateFilter.ALL,
					DateFilter.CUSTOM
		};
		DateFilterComposite.DateFilter initialDateFilter = DateFilter.LAST_1_YEARS;
		dateFilter = new Date[]{initialDateFilter.getStartDate(), initialDateFilter.getEndDate()};
		dateComp = new DateFilterDropDownComposite(mapArea, defaultFilters, initialDateFilter);
        dateComp.setBounds(0, 0, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        dateComp.addChangeListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				dateComp.setBounds(0, 0, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
				dateFilterChanged();
			}
		});
        
        mapPart = new MapComposite(mapArea, editor);
        mapArea.addListener(SWT.Resize, new Listener(){
			@Override
			public void handleEvent(Event event) {
				mapPart.setBounds(0,0,mapArea.getBounds().width-5,mapArea.getBounds().height-5);		
			}
        });
		
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
		geomTypeColumn.getColumn().setText(""); //$NON-NLS-1$
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
				return ""; //$NON-NLS-1$
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
		
		
		//records details tooltip
		new AbstractEntityEditorShellListener<IntelEntityLocation, RecordDetailsShell>(locationTable, 4) {			
			@Override
			protected RecordDetailsShell getShellDialog(IntelEntityLocation currentSelection) {
				return  new RecordDetailsShell(locationTable.getControl().getShell(),currentSelection.getLocation().getRecord());
			}
		};

		Listener tableListener = new Listener(){
			private boolean doHover = true;
			private ObservationDetailsShell detailsShell = null;
			
			@Override
			public void handleEvent(Event event) {
				switch(event.type){
					case SWT.MouseDoubleClick:
					case SWT.MouseDown:
					case SWT.MouseUp:
						doHover = false;
						break;
					case SWT.MouseMove:
						doHover= true;
						break;
					case SWT.MouseHover:
						if (doHover){
							doHover(event.x,event.y);
						}
						break;
				}
					
			}
			private void doHover(int x, int y){
				ViewerCell cell = locationTable.getCell(new Point(x, y));
				if (cell != null && cell.getElement() instanceof IntelEntityLocation && cell.getColumnIndex() == 6){
					IntelEntityLocation location = (IntelEntityLocation) cell.getElement();
					if (detailsShell == null || detailsShell.isDisposed() || !detailsShell.getLocationRecord().equals(location.getLocation())){
						detailsShell = new ObservationDetailsShell(getShell(),((IntelEntityLocation)cell.getElement()).getLocation());
						int height = detailsShell.getSize().y;
						Point p  = locationTable.getTable().toDisplay(x, y);
						detailsShell.open(new Point(p.x, p.y - height));
					}
				}
			}
			
		};
		locationTable.getTable().addListener(SWT.MouseDoubleClick, tableListener);
		locationTable.getTable().addListener(SWT.MouseDown, tableListener);
		locationTable.getTable().addListener(SWT.MouseUp, tableListener);
		locationTable.getTable().addListener(SWT.MouseMove, tableListener);
		locationTable.getTable().addListener(SWT.MouseHover, tableListener);	
		
		Menu mnu = new Menu(locationTable.getTable());
		locationTable.getTable().setMenu(mnu);
		
		MenuItem openItem = new MenuItem(mnu, SWT.PUSH);
		openItem.setText(Messages.EntityEditorMapComposite_OpenRecordMnuItem);
		openItem.addSelectionListener(new SelectionAdapter() {
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
				for (MenuItem mi : mnu.getItems()){
					if (mi != openItem){
						mi.dispose();
					}
				}
				if (!editor.getEditMode()) return;
				
				IntelLocation loc = getSelectedLocation();
				try {
					if (!(loc.getGeometry() instanceof com.vividsolutions.jts.geom.Point)) return;
				} catch (ParseException e1) {
					Intelligence2PlugIn.log(e1.getMessage(),e1);
					return;
				}
				
				List<IntelAttribute> attributes = new ArrayList<IntelAttribute>();
				for (IntelEntityTypeAttribute a: editor.getEntity().getEntityType().getAttributes()){
					if (a.getAttribute().getType() == AttributeType.POSITION){
						attributes.add(a.getAttribute());
					}
				}
				
				if (!attributes.isEmpty()){
					 new MenuItem(mnu, SWT.SEPARATOR);
				}
				for (IntelAttribute ia : attributes){			
					MenuItem setAttribute = new MenuItem(mnu, SWT.PUSH);
					
					setAttribute.setText(MessageFormat.format(Messages.EntityEditorMapComposite_UpdateMenuItem, ia.getName()));
					setAttribute.addSelectionListener(new SelectionAdapter() {
						
						@Override
						public void widgetSelected(SelectionEvent e) {
							com.vividsolutions.jts.geom.Point p = null;
							try{
								IntelLocation loc = getSelectedLocation();
								if (!(loc.getGeometry() instanceof com.vividsolutions.jts.geom.Point)){
									MessageDialog.openError(getShell(), Messages.EntityEditorMapComposite_UpdateAttributeErrorDialogTitle, Messages.EntityEditorMapComposite_UpdateAttributeErrorDialogMessage);
									return;
								}
								p = (com.vividsolutions.jts.geom.Point) loc.getGeometry();
							}catch (Exception ex){
								Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
								return;
							}
							editor.updatePositionAttribute(ia, p.getX(), p.getY());
						}
					});					
				}
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});

		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				for (MenuItem mi : mnu.getItems()){
					mi.setEnabled(getSelectedLocation() != null);
				}
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
					l.setFilter(ff.equals(ff.property("system_id"), ff.literal(UuidUtils.uuidToString(location.getUuid())))); //$NON-NLS-1$
				}else{
					l.setFilter(Filter.EXCLUDE);
				}
			}
		}
	}
		

	@Override
    public Map getMap() {
        return mapPart.getMap();
    }
	
	public void refresh(){
		if (locationLayers == null){
			//add location layers
			addLayers();
		}else{
			//refresh existing layers
			getMap().getRenderManager().refresh(null);

			//necessary to refresh layer
			for (ContentFilterLayerImpl layer : locationLayers){
				 layer.setContentFilter(layer.getContentFilter());
			 }
			
			//refresh attribute geometries
			for (IntelEntityAttributeValue v : editor.getEntity().getAttributes()){
				if (v.getAttribute().getType() == AttributeType.POSITION){
					locationLayer.refreshLayerValue(v);
				}
			}
		}
		loadLocationsLink.schedule();
	}
	
	public void refreshLayerValue(IntelValueItem value) {
		locationLayer.refreshLayerValue(value);
	}
	
    @Override
    public void dispose() {
        super.dispose();
        mapPart.dispose();
        
    }

    @Override
    public void openContextMenu() {
    	mapPart.openContextMenu();
    }

    @Override
    public void setFont( Control control ) {
    	mapPart.setFont(control);
    }

    @Override
    public void setSelectionProvider( IMapEditorSelectionProvider selectionProvider ) {
    	mapPart.setSelectionProvider(selectionProvider);        
    }

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getStatusLineManager()
	 */
	@Override
	public IStatusLineManager getStatusLineManager() {
		return ((IEditorSite)editor.getSite()).getActionBars().getStatusLineManager();		
	}
	
	
	private Job loadLocationsLink = new Job("loading location links"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			Display.getDefault().syncExec(() -> { 
				if (locationTable.getTable().isDisposed()) return;
				locationTable.setInput(new String[]{DialogConstants.LOADING_TEXT});
			});
			
			IntelEntity entity = editor.getEntity();
			if (entity == null) return Status.OK_STATUS;
			
			final List<IntelEntityLocation> alllocations  = new ArrayList<IntelEntityLocation>();
			Session s = HibernateManager.openSession();
			try{
				alllocations.addAll(EntityManager.INSTANCE.getEntityLocations(s, entity.getUuid(), dateFilter));
				for (IntelEntityLocation l : alllocations){
					l.getLocation().getId();
					l.getLocation().getRecord().getTitle();
					
					if (l.getLocation().getObservations() != null){
						for (IntelObservation o : l.getLocation().getObservations()){
							o.getCategory().getFullCategoryName();
							if (o.getObservationAttributes() != null){
								for (IntelObservationAttribute a : o.getObservationAttributes()){
									a.getAttribute().getName();
									if (a.getAttributeListItem() != null)a.getAttributeListItem().getName();
									if (a.getAttributeTreeNode() != null)a.getAttributeTreeNode().getName();
								}
							}
						}
					}
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
