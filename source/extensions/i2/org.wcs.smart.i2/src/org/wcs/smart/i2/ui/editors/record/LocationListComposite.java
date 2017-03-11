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
package org.wcs.smart.i2.ui.editors.record;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.geotools.legend.Glyph;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.gpx.GPSBabel;
import org.wcs.smart.gpx.GPSDataImport;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.ui.DateCellEditor;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.FileLocationParser;
import org.wcs.smart.i2.ui.WKTGeometryDialog;
import org.wcs.smart.i2.ui.ObservationDialog;
import org.wcs.smart.i2.ui.TransparentInfoDialog;
import org.wcs.smart.i2.ui.dialogs.GPSDeviceSelectionDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for displaying all locations in a list.
 * @author Emily
 *
 */
public class LocationListComposite extends Composite{
	
	public enum Column{ID,COMMENT,DATE,TIME,OBSERVATION};
	
	private TableViewer tblObservations;
	private RecordEditor editor;
	
	private LocationDetailsShell detailsShell = null; 
	
	private MenuItem deleteItem;
	private MenuItem addLinkItem;
	private MenuItem dropLinkItem;
	private MenuItem editObsItem;
	private MenuItem editGeometry;
	private MenuItem importItem;
	
	public LocationListComposite(Composite parent, FormToolkit toolkit, RecordEditor editor){
		super(parent, SWT.NONE);
		this.editor = editor;
		toolkit.adapt(this);
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginHeight = 0;
		((GridLayout)getLayout()).marginWidth = 0;
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblObservations = new TableViewer(this, SWT.FULL_SELECTION | SWT.BORDER);
		tblObservations.getTable().setLinesVisible(true);
		tblObservations.getTable().setHeaderVisible(true);
		tblObservations.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblObservations.setContentProvider(ArrayContentProvider.getInstance());
		
		
		
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(tblObservations, new FocusCellHighlighter(tblObservations){});
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tblObservations) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TableViewerEditor.create(tblObservations, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.KEYBOARD_ACTIVATION);

		TableViewerColumn geomTypeColumn = new TableViewerColumn(tblObservations, SWT.LEFT);
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
				if (element instanceof IntelLocation){
					if (((IntelLocation) element).isPoint()) return point;
					if (((IntelLocation) element).isPolygon()) return polygon;
				}
				return null;
			}
		});
		
		TableViewerColumn idColumn = new TableViewerColumn(tblObservations, SWT.LEFT);
		idColumn.getColumn().setText(Messages.LocationListComposite_IdColumn);
		idColumn.getColumn().setWidth(100);
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelLocation){
					return ((IntelLocation) element).getId();
				}
				return super.getText(element);
			}
		});
		idColumn.setEditingSupport(new LocationTableEditor(tblObservations, Column.ID));
		
		TableViewerColumn dateColumn = new TableViewerColumn(tblObservations, SWT.LEFT);
		dateColumn.getColumn().setText(Messages.LocationListComposite_DateColumn);
		dateColumn.getColumn().setWidth(100);
		dateColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelLocation){
					return DateFormat.getDateInstance().format(((IntelLocation) element).getDateTime());
				}
				return super.getText(element);
			}
		});
		dateColumn.setEditingSupport(new LocationTableEditor(tblObservations, Column.DATE));
		
		TableViewerColumn timeColumn = new TableViewerColumn(tblObservations, SWT.LEFT);
		timeColumn.getColumn().setText(Messages.LocationListComposite_TimeColumn);
		timeColumn.getColumn().setWidth(100);
		timeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelLocation){
					return DateFormat.getTimeInstance().format(((IntelLocation) element).getDateTime());
				}
				return super.getText(element);
			}
		});
		timeColumn.setEditingSupport(new LocationTableEditor(tblObservations, Column.TIME));
		
		TableViewerColumn commentColumn = new TableViewerColumn(tblObservations, SWT.LEFT);
		commentColumn.getColumn().setText(Messages.LocationListComposite_CommentColumn);
		commentColumn.getColumn().setWidth(200);
		commentColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelLocation){
					return ((IntelLocation) element).getComment();
				}
				return super.getText(element);
			}
		});
		commentColumn.setEditingSupport(new LocationTableEditor(tblObservations, Column.COMMENT));
		
		
		TableViewerColumn obsColumn = new TableViewerColumn(tblObservations, SWT.LEFT);
		obsColumn.getColumn().setText(Messages.LocationListComposite_ObsColumn);
		obsColumn.getColumn().setWidth(200);
		obsColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelLocation){
					int cnt = -1;
					if (((IntelLocation)element).getObservations() == null){
						cnt = 0;
					}else{
						cnt = ((IntelLocation)element).getObservations().size();
					}
					return MessageFormat.format(Messages.LocationListComposite_ObsLabel, cnt);
				}
				return super.getText(element);
			}
		});
		obsColumn.setEditingSupport(new LocationTableEditor(tblObservations, Column.OBSERVATION));
		
		
		TableViewerColumn entityListColumn = new TableViewerColumn(tblObservations, SWT.LEFT);
		entityListColumn.getColumn().setText(Messages.LocationListComposite_EntitiesColumn);
		entityListColumn.getColumn().setWidth(200);
		entityListColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelLocation){
					StringBuilder sb = new StringBuilder();
					int cnt = 0;
					for (IntelEntityLocation l : editor.getEntityLocationLinks()){
						if (l.getLocation().equals(element)){
							sb.append(l.getEntity().getIdAttributeAsText());
							sb.append("; "); //$NON-NLS-1$
							cnt++;
						}
					}
					
					if (sb.length() > 0){
						sb.insert(0, "(" + cnt + ") "); //$NON-NLS-1$ //$NON-NLS-2$
						return sb.substring(0, sb.length() - 2);
					}
					
					return ""; //$NON-NLS-1$
				}
				return super.getText(element);
			}
		});
		
		Menu linkEntities = new Menu(tblObservations.getTable());
		tblObservations.getTable().setMenu(linkEntities);
		
		SelectionListener addEntityLinkListener = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object selection = ((IStructuredSelection)tblObservations.getSelection()).getFirstElement();
				if (!(selection instanceof IntelLocation)) return;
				
				IntelLocation location = (IntelLocation)selection;
				Object data = ((MenuItem)e.widget).getData();
				List<IntelEntity> toLink = new ArrayList<IntelEntity>();
				if (data == null){
					for (IntelEntityRecord r : editor.getRecord().getEntities()){
						toLink.add(r.getEntity());
					}
				}else if (data instanceof IntelEntity){
					toLink.add((IntelEntity) data);
				}
				for (IntelEntity entity : toLink){
					editor.linkEntityToLocation(location, entity);
				}
			}
		};
		SelectionListener dropEntityLinkListener = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object selection = ((IStructuredSelection)tblObservations.getSelection()).getFirstElement();
				if (!(selection instanceof IntelLocation)) return;
				
				IntelLocation location = (IntelLocation)selection;
				Object data = ((MenuItem)e.widget).getData();
				IntelEntity toDrop = null;
				if (data != null){
					toDrop = (IntelEntity)data;
				}
				editor.unlinkEntityFromLocation(location, toDrop);
			}
		};
		
		linkEntities.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				for (MenuItem mi : linkEntities.getItems()){
					if (mi.getMenu() != null) mi.getMenu().dispose();
					mi.dispose();
				}
				
				if (!editor.getEditMode()){
					return;
				}
				Object selection = ((IStructuredSelection)tblObservations.getSelection()).getFirstElement();
				if (selection != null && !(selection instanceof IntelLocation)) return;
				
				if (IntelSecurityManager.INSTANCE.canLinkLocationsToEntities()){
					addLinkItem = new MenuItem(linkEntities, SWT.CASCADE);
					addLinkItem.setText(Messages.LocationListComposite_AddEntityLink);
					addLinkItem.setEnabled(selection != null);
					
					dropLinkItem = new MenuItem(linkEntities, SWT.CASCADE);
					dropLinkItem.setText(Messages.LocationListComposite_DropEntityLink);
					dropLinkItem.setEnabled(selection != null);
					
					Menu linkSubMenu = new Menu(addLinkItem);
					addLinkItem.setMenu(linkSubMenu);
						
					Menu dropLinkSubMenu = new Menu(dropLinkItem);
					dropLinkItem.setMenu(dropLinkSubMenu);
						
					
					IntelLocation location = (IntelLocation)selection;
							
					List<IntelEntityRecord> allEntities = editor.getRecord().getEntities();
					List<IntelEntity> toAdd = new ArrayList<IntelEntity>();
					List<IntelEntity> toDrop= new ArrayList<IntelEntity>();
					for (IntelEntityRecord record : allEntities){
						boolean add = true;
						for (IntelEntityLocation elocation: editor.getEntityLocationLinks()){
							if (elocation.getLocation().equals(location)){
								if (elocation.getEntity().equals(record.getEntity())){
									add = false;
									break;
								}
							}
						}
						if (add){
							toAdd.add(record.getEntity());
						}else{
							toDrop.add(record.getEntity());
						}
					}
					if (toAdd.size() > 0){
						MenuItem linkToAll = new MenuItem(linkSubMenu, SWT.PUSH);
						linkToAll.setText(Messages.LocationListComposite_AddAllOption);
						linkToAll.addSelectionListener(addEntityLinkListener);
						new MenuItem(linkSubMenu, SWT.SEPARATOR);
						for (IntelEntity entity : toAdd){
							MenuItem linkTo = new MenuItem(linkSubMenu, SWT.PUSH);
							linkTo.setText(entity.getIdAttributeAsText());
							if (entity.getEntityType().getIcon() != null){
								linkTo.setImage(EntityTypeLabelProvider.createImageDescriptor(entity.getEntityType()).createImage());
								linkTo.addListener(SWT.Dispose, (event) -> {if (linkTo.getImage() != null) linkTo.getImage().dispose();});
							}
							linkTo.setData(entity);
							linkTo.addSelectionListener(addEntityLinkListener);
						}
					}else{
						MenuItem noMore = new MenuItem(linkSubMenu, SWT.PUSH);
						noMore.setEnabled(false);
						noMore.setText(Messages.LocationListComposite_AddNoOptionsFound);
					}
					if (toDrop.size() > 0){
						MenuItem linkToAll = new MenuItem(dropLinkSubMenu, SWT.PUSH);
						linkToAll.setText(Messages.LocationListComposite_DropallOption);
						linkToAll.addSelectionListener(dropEntityLinkListener);
						new MenuItem(dropLinkSubMenu, SWT.SEPARATOR);
						for (IntelEntity entity : toDrop){
							MenuItem linkTo = new MenuItem(dropLinkSubMenu, SWT.PUSH);
							linkTo.setText(entity.getIdAttributeAsText());
							if (entity.getEntityType().getIcon() != null){
								linkTo.setImage(EntityTypeLabelProvider.createImageDescriptor(entity.getEntityType()).createImage());
								linkTo.addListener(SWT.Dispose, (event) -> {if (linkTo.getImage() != null) linkTo.getImage().dispose();});
							}
							linkTo.setData(entity);
							linkTo.addSelectionListener(dropEntityLinkListener);
						}
					}else{
						MenuItem noMore = new MenuItem(dropLinkSubMenu, SWT.PUSH);
						noMore.setEnabled(false);
						noMore.setText(Messages.LocationListComposite_DropNoOptions);
					}
				}
				
				new MenuItem(linkEntities, SWT.SEPARATOR);
						
				deleteItem = new MenuItem(linkEntities, SWT.PUSH);
				deleteItem.setText(MessageFormat.format(Messages.LocationListComposite_DeleteMenuItem, DialogConstants.DELETE_BUTTON_TEXT));
				deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
				deleteItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						Object selection = ((IStructuredSelection)tblObservations.getSelection()).getFirstElement();
						if (selection instanceof IntelLocation){
							editor.deleteLocation(((IntelLocation)selection));
						}
					}
				});
				deleteItem.setEnabled(selection != null);
				
				editObsItem = new MenuItem(linkEntities, SWT.PUSH);
				editObsItem.setText(Messages.LocationListComposite_EditObsMenuItem);
				editObsItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
				editObsItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						Object selection = ((IStructuredSelection)tblObservations.getSelection()).getFirstElement();
						if (selection instanceof IntelLocation){
							ObservationDialog dialog = new ObservationDialog(getShell(), (IntelLocation)selection);
							if (dialog.open() == Window.OK){
								editor.setDirty(true);
								tblObservations.refresh();
							}
						}
					}
				});
				editObsItem.setEnabled(selection != null);
				
				editGeometry = new MenuItem(linkEntities, SWT.PUSH);
				editGeometry.setText(Messages.LocationListComposite_EditGeomMnuItem);
				editGeometry.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
				editGeometry.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						Object selection = ((IStructuredSelection)tblObservations.getSelection()).getFirstElement();
						if (selection instanceof IntelLocation){
							try{
								WKTGeometryDialog gd = new WKTGeometryDialog(getShell(), ((IntelLocation)selection).getGeometry());
								if (gd.open() == Window.OK){
									((IntelLocation)selection).setGeometry(gd.getNewGeometry());
									editor.locationsUpdated();
								}
							}catch (Exception ex){
								Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
							}
						}
					}
				});
				editGeometry.setEnabled(selection != null);
				
				new MenuItem(linkEntities, SWT.SEPARATOR);
						
				importItem = new MenuItem(linkEntities, SWT.CASCADE);
				importItem.setText(Messages.LocationListComposite_ImportLocationMnuItem);
					
				Menu importOp = new Menu(importItem);
				importItem.setMenu(importOp);
						
				MenuItem importFile = new MenuItem(importOp, SWT.PUSH);
				importFile.setText(Messages.LocationListComposite_ImportFromFileMenu);
				importFile.addListener(SWT.Selection, evt->{
					importLocationsFromFile();
				});
				MenuItem importGps = new MenuItem(importOp, SWT.PUSH);
				importGps.setText(Messages.LocationListComposite_ImportFromGpsOp);
				importGps.addListener(SWT.Selection, evt->{
					importLocationsFromGps();
				});
				
			}
			
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		
		
		Listener tableListener = new Listener(){
			private boolean doHover = true;
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
				
				ViewerCell cell = tblObservations.getCell(new Point(x, y));
				if (cell != null && cell.getElement() instanceof IntelLocation){
					IntelLocation location = (IntelLocation) cell.getElement();
					if (detailsShell == null || detailsShell.isDisposed() || !detailsShell.getLocationRecord().equals(location)){
						detailsShell = new LocationDetailsShell(getShell(),(IntelLocation)cell.getElement(), editor.getEntityLocationLinks());
					
						int height = detailsShell.getSize().y;
						Point p  = tblObservations.getTable().toDisplay(x, y);
						detailsShell.open(new Point(p.x, p.y - height));
					}
				}
			}
			
		};
		tblObservations.getTable().addListener(SWT.MouseDoubleClick, tableListener);
		tblObservations.getTable().addListener(SWT.MouseDown, tableListener);
		tblObservations.getTable().addListener(SWT.MouseUp, tableListener);
		tblObservations.getTable().addListener(SWT.MouseMove, tableListener);
		tblObservations.getTable().addListener(SWT.MouseHover, tableListener);	
	}
	
	/**
	 * imports locations from the gps device
	 */
	@SuppressWarnings("unchecked")
	public void importLocationsFromGps(){
		List<IntelLocation>[] locations = new List[]{null};
		GPSDeviceSelectionDialog gpsDialog = new GPSDeviceSelectionDialog(getShell());
		if (gpsDialog.open() != Window.OK) return;
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.LocationListComposite_GPSTaskName, IProgressMonitor.UNKNOWN);
					try{
						File f = GPSBabel.getData(gpsDialog.getDeviceType(), Collections.singleton(GPSDataImport.ImportType.WAYPOINT));
						locations[0] = importGpx(f, monitor);
					}catch (Exception ex){
						Intelligence2PlugIn.displayLog(Messages.LocationListComposite_GPSError, ex);
						return;
					}
				}
			});
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
		}
		displayInfo(locations[0]);
	} 
	
	/**
	 * imports locations from a gpx file
	 */
	@SuppressWarnings("unchecked")
	public void importLocationsFromFile(){
		List<IntelLocation>[] locations = new List[]{null};
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.LocationListComposite_GPXTaxName, IProgressMonitor.UNKNOWN);
					final String[] file = new String[]{null};
					Display.getDefault().syncExec(()->{
						FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
						fd.setFilterExtensions(new String[]{"*.gpx", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
						fd.setFilterNames(new String[]{Messages.LocationListComposite_GpxFileOption, Messages.LocationListComposite_AllFileOption});
						file[0] = fd.open();	
					});
					
					if (file[0] == null) return;
					
					locations[0] = importGpx(new File(file[0]), monitor);
					monitor.done();
				}
			});
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
		}
		displayInfo(locations[0]);
	}
	
	private void displayInfo(List<?> items){
		if (items != null && items.size() > 0){
			TransparentInfoDialog infodialog = new TransparentInfoDialog(getShell(), MessageFormat.format(Messages.LocationListComposite_LocationsAddedMsg, items.size()));
			infodialog.open();
		}
	}
	/*
	 * imports location from the given file
	 */
	private List<IntelLocation> importGpx(File f, IProgressMonitor monitor){
		List<IntelLocation> locations = FileLocationParser.INSTANCE.parseFromGpx(f, monitor);
		
		Display.getDefault().syncExec(()->editor.addNewLocations(locations));
		return locations;
	}
	
	public void addSelectionListener(ISelectionChangedListener listener){
		tblObservations.addSelectionChangedListener(listener);
	}
	
	public void init(){
		if (editor.getRecord().getLocations() == null){
			editor.getRecord().setLocations(new ArrayList<IntelLocation>());
		}
		tblObservations.setInput(editor.getRecord().getLocations());
	}
	
	public void refreshTable(){
		tblObservations.refresh();
	}
	
	private class LocationTableEditor extends EditingSupport {
		
		private CellEditor editor;
		private Column col;
		
		LocationTableEditor(TableViewer viewer, Column col) {
			super(viewer);
			this.col = col;
			if (col == Column.DATE ){
				this.editor = new DateCellEditor(viewer.getTable(), SWT.DATE | SWT.DROP_DOWN);
			}else if (col == Column.TIME){ 
				this.editor = new DateCellEditor(viewer.getTable(), SWT.TIME | SWT.DROP_DOWN);
			}else if (col == Column.OBSERVATION){
				this.editor = new RecordLocationObservationCellEditor(viewer.getTable());
			}else{
				this.editor = new TextCellEditor(viewer.getTable());
			}
		}

		@Override
		protected void setValue(Object element, Object value) {
			String error = validate(value);
			if (error == null){
				if (col == Column.ID){
					((IntelLocation)element).setId(((String)value).trim());
				}else if (col == Column.COMMENT){
					if (((String)value).isEmpty()){
						((IntelLocation)element).setComment(null);
					}else{
						((IntelLocation)element).setComment(((String)value).trim());
					}
				}else if (col == Column.DATE){
					Date time = ((IntelLocation)element).getDateTime();
					Date date = (Date)value;
					((IntelLocation)element).setDateTime(SmartUtils.combineDateTime(date, time));
				}else if (col == Column.TIME){
					Date time = (Date)value;
					Date date = ((IntelLocation)element).getDateTime();
					((IntelLocation)element).setDateTime(SmartUtils.combineDateTime(date, time));
				}else if (col == Column.OBSERVATION){
				}
				//Fire changes
				LocationListComposite.this.editor.locationsUpdated();
			}else{
				MessageDialog.openError(getShell(), Messages.LocationListComposite_ErrorTitle, error);
			}
		}

		@Override
		protected Object getValue(Object element) {
			if(col == Column.ID){
				return ((IntelLocation)element).getId();
			}else if (col == Column.COMMENT){
				String value = ((IntelLocation)element).getComment();
				if (value == null) return ""; //$NON-NLS-1$
				return value;
			}else if (col == Column.DATE || col == Column.TIME){
				return ((IntelLocation)element).getDateTime();
			}else if (col == Column.OBSERVATION){
				return ((IntelLocation)element);
			}
			return null;
		}

		private String validate(Object value){
			if (col == Column.ID | col == Column.COMMENT){
				String vv = value == null? null:((String)value).trim();
				if (col == Column.ID){
					if (vv == null||vv.isEmpty()){
						return Messages.LocationListComposite_IdRequired;
					}else if (vv != null && vv.length() > IntelLocation.ID_MAX_LENGTH){
						return MessageFormat.format(Messages.LocationListComposite_IdInvalid, IntelLocation.ID_MAX_LENGTH);
					}
				}else if (col == Column.COMMENT){
					if (vv != null && vv.length() > IntelLocation.COMMENT_MAX_LENGTH){
						return MessageFormat.format(Messages.LocationListComposite_CommentInvalid, IntelLocation.COMMENT_MAX_LENGTH);
					}
				}
			}
			return null;
		}
		
		@Override
		protected CellEditor getCellEditor(final Object element) {
			if (editor instanceof RecordLocationObservationCellEditor) return editor;
			
			this.editor.setValidator(new ICellEditorValidator() {
				@Override
				public String isValid(Object value) {
					return validate(value);
				}
			});
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}
}
