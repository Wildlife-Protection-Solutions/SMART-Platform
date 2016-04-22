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
package org.wcs.smart.report.birt.map.properties;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.birt.report.designer.internal.ui.views.attributes.section.SeperatorSection;
import org.eclipse.birt.report.designer.ui.views.attributes.AttributesUtil;
import org.eclipse.birt.report.model.api.CachedMetaDataHandle;
import org.eclipse.birt.report.model.api.ColumnHintHandle;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.Expression;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.MemberHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ResultSetColumnHandle;
import org.eclipse.birt.report.model.api.StructureFactory;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.elements.structures.ComputedColumn;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.report.birt.map.BirtMapUtils;
import org.wcs.smart.report.birt.map.BirtUiUtils;
import org.wcs.smart.report.birt.map.ExtensionManager;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;
import org.wcs.smart.report.birt.map.SmartMapItemPlugIn;
import org.wcs.smart.report.birt.map.internal.Messages;
import org.wcs.smart.report.birt.map.item.LayerItem;
import org.wcs.smart.report.birt.map.item.LayerItemFactory;
import org.wcs.smart.report.birt.map.item.SmartMapItem;
import org.wcs.smart.ui.BasemapLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;
/**
 * A birt property tab for displaying map options
 * to user.
 * 
 * @author Emily
 *
 */
public class SmartLayersPage extends AttributesUtil.PageWrapper {

	private static final UUID DEFAULT_BASEMAP = UuidUtils.stringToUuid("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");  //$NON-NLS-1$
	
	private static final String ERROR_DIALOG_TITLE = Messages.SmartLayersPage_ErrorDialog_Title;
	public static final String PAGE_KEY = "MapLayers"; //$NON-NLS-1$
	protected FormToolkit toolkit;

	private Composite contentpane;
	private ComboViewer basemapCombo;
	private boolean basemapListenerEnabled = false;
	private TableViewer tblLayers ;

	private Text txtBounds;

	private ExtendedItemHandle itemHandle;
	private SmartMapItem mapItem;
	private StyleCellEditor cellEditor;
	private HashMap<LayerItem, Image> styleImageCache;
	
	public SmartLayersPage() {
		super();
		basemapJob.setSystem(true);
		styleImageCache = new HashMap<LayerItem, Image>();
	}
	
	@Override
	public void buildUI(Composite parent) {
		if (toolkit == null) {
			toolkit = new FormToolkit(Display.getCurrent());
			toolkit.setBorderStyle(SWT.NULL);
		}

		Control[] children = parent.getChildren();
		contentpane = (Composite) children[children.length - 1];
		contentpane.setLayout(new GridLayout(2, false));

		// basemap section
		createBasemapBounds(contentpane);
		
		//separator
		SeperatorSection seperator3 = new SeperatorSection( contentpane, SWT.HORIZONTAL );
		seperator3.createSection();
		seperator3.layout();
		
		//layers section
		createLayers(contentpane);
		
		//update the ui
		updateUI();
	}
	
	
	private LayerItem getLayerItem(Object item){
		try{
			if (item instanceof ExtendedItemHandle){
				if (((ExtendedItemHandle) item).getReportItem() instanceof LayerItem){
					return (LayerItem) ((ExtendedItemHandle) item).getReportItem();
				}
			}
			return null;
		}catch (Exception ex){
			return null;
		}
	}
	
	/**
	 * Creates the layer table pane
	 * @param contentpane
	 */
	private void createLayers(Composite contentpane){

		Label l = toolkit.createLabel(contentpane, Messages.SmartLayersPage_MapLayersLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		tblLayers = new TableViewer(contentpane, SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.adapt(tblLayers.getTable());
		tblLayers.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblLayers.setContentProvider(ArrayContentProvider.getInstance());
		tblLayers.getTable().setHeaderVisible(true);
		tblLayers.getTable().setLinesVisible(true);
		
		//dataset column
		final TableViewerColumn col1 = new TableViewerColumn(tblLayers, SWT.DEFAULT);
		col1.getColumn().setText(Messages.SmartLayersPage_ReportDataset_ColumnName);
		col1.getColumn().setWidth(200);
		col1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				LayerItem it = getLayerItem(element);
				if (it != null){
					if (it.getHandle().getDataSet() != null){
						return it.getHandle().getDataSet().getElement().getDisplayName();
					}else{
						return Messages.SmartLayersPage_Error_QueryNotFound ;
					}
				}
				return super.getText(element);
			}
		});
		
		//name column
		final TableViewerColumn col2 = new TableViewerColumn(tblLayers, SWT.DEFAULT);
		col2.getColumn().setText(Messages.SmartLayersPage_LayerName_ColumnHeader);
		col2.getColumn().setWidth(200);
		final ColumnLabelProvider col2Lp = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				LayerItem it = getLayerItem(element);
				if (it != null){
					return it.getLayerName();
				}
				return super.getText(element);
			}
		};
		col2.setLabelProvider(col2Lp);
		col2.setEditingSupport(new EditingSupport(col2.getViewer()) {
			@Override
			protected void setValue(Object element, Object value) {
				LayerItem it = getLayerItem(element);
				if (it != null && value instanceof String){
					try {
						it.setLayerName((String)value);
					} catch (SemanticException e) {
					}
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				return col2Lp.getText(element);
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(tblLayers.getTable());
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		//style column 
		final TableViewerColumn col3 = new TableViewerColumn(tblLayers, SWT.DEFAULT);
		col3.getColumn().setText(Messages.SmartLayersPage_Style_ColumnHeader);
		col3.getColumn().setWidth(200);
		ColumnLabelProvider col3Provider = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				LayerItem it = getLayerItem(element);
				if (it != null){
					if (it.getLayerStyles() == null){
						return Messages.SmartLayersPage_DefaultStyleLabel;
					}else if (getImage(element) == null){
						return it.getLayerStyles();
					}
					return ""; //$NON-NLS-1$
				}
				return super.getText(element);
			}
			
			@Override
			public Image getImage(Object element) {
				LayerItem it = getLayerItem(element);
				if (it != null && it.getLayerStyles() != null){
					Image x = styleImageCache.get(it);
					if (x == null){
						x = BirtUiUtils.parseImageFromStyleString(it.getLayerStyles());
						styleImageCache.put(it, x);
					}
					return x;
				}
				return null;
			}
		};
		col3.setLabelProvider(col3Provider);
		
		cellEditor = new StyleCellEditor(tblLayers.getTable(), col3Provider);
		col3.setEditingSupport(new EditingSupport(col3.getViewer()) {

			@Override
			protected CellEditor getCellEditor(Object element) {
				return cellEditor;
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}

			@Override
			protected Object getValue(Object element) {
				LayerItem it = getLayerItem(element);
				return it;
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof ExtendedItemHandle ){
					if (value instanceof String){
						try {
							LayerItem it =(LayerItem)((ExtendedItemHandle)element).getReportItem(); 
							styleImageCache.put(it, null);
							it.setLayerStyles((String)value);
						} catch (SemanticException e) {
						}
					}
					tblLayers.refresh();
				}
			}
		});
		
		//geomtry column binding  
		final TableViewerColumn col4 = new TableViewerColumn(tblLayers, SWT.DEFAULT);
		col4.getColumn().setText(Messages.SmartLayersPage_GeometryColumnLabel);
		col4.getColumn().setWidth(200);
		ColumnLabelProvider col4Provider = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				LayerItem it = getLayerItem(element);
				if (it != null){
					if (it.getLayerType() == LayerType.RASTER){
						return it.getLayerType().name();
					}else if (it.getGeometryColumn() != null){
						return it.getGeometryColumn() + " [" + it.getLayerType().name() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				return Messages.SmartLayersPage_ErrorLabel;
			};
		};
		col4.setLabelProvider(col4Provider);
		
		tblLayers.getTable().pack();
		Composite btnPanel = toolkit.createComposite(contentpane);
		GridLayout gl = new GridLayout(1, true);
		gl.marginHeight = 0;
		btnPanel.setLayout(gl);
		
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		Button btnAdd = toolkit.createButton(btnPanel, DialogConstants.ADD_BUTTON_TEXT, SWT.PUSH);
		btnAdd.setToolTipText(Messages.SmartLayersPage_addtooltip);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				addLayer();
			}
		});
		
		
		Button btnRemove = toolkit.createButton(btnPanel, DialogConstants.DELETE_BUTTON_TEXT, SWT.PUSH);
		btnRemove.setToolTipText(Messages.SmartLayersPage_removetooltip);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteLayers();
			}
		});
		
		Button btnClearStyle = toolkit.createButton(btnPanel, Messages.SmartLayersPage_ClearStyleButton, SWT.PUSH);
		btnClearStyle.setToolTipText(Messages.SmartLayersPage_cleartooltip);
		btnClearStyle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnClearStyle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				clearStyles();
			}
		});
		
		Button btnUp = toolkit.createButton(btnPanel, Messages.SmartLayersPage_MoveUpButton, SWT.PUSH);
		btnUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnUp.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				moveUp();
			}
		});
		Button btnDown = toolkit.createButton(btnPanel, Messages.SmartLayersPage_MoveDownButton, SWT.PUSH);
		btnDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDown.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				moveDown();
			}
		});
		Button btnUpdate = toolkit.createButton(btnPanel, Messages.SmartLayersPage_RefreshLabel, SWT.PUSH);
		btnUpdate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnUpdate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				List<?> layers = (List<?>) tblLayers.getInput();
				for (Object x : layers){
					if (x instanceof ExtendedItemHandle){
						updateLayerGeometry((ExtendedItemHandle)x);
					}
				}
				tblLayers.refresh();
			}
		});
	}	
	
	/**
	 * Create the basemap section
	 * @param parent
	 */
	private void createBasemapBounds(Composite parent){
		Composite bm = toolkit.createComposite(parent);
		bm.setLayout(new GridLayout(6, false));
		bm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		toolkit.createLabel(bm, Messages.SmartLayersPage_BasemapLabel);
		
		basemapCombo = new ComboViewer(bm, SWT.READ_ONLY | SWT.DROP_DOWN);
		basemapCombo.setLabelProvider(new BasemapLabelProvider());
		basemapCombo.setContentProvider(ArrayContentProvider.getInstance());
		basemapCombo.setInput(Messages.SmartLayersPage_LoadingLbl);
		basemapCombo.getCombo().setToolTipText(Messages.SmartLayersPage_basemaptooltipe);
	
		//toolkit.adapt(basemapCombo.getCombo());
		basemapCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)basemapCombo.getControl().getLayoutData()).minimumWidth = 100;
		((GridData)basemapCombo.getControl().getLayoutData()).widthHint = 200;
		
		basemapCombo.getControl().addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (basemapListenerEnabled){
					updateModel(SmartMapItem.SMART_BASEMAP_PROP);
				}
			}
		});
		
		loadBasemaps();
		basemapCombo.getControl().addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
			}
			@Override
			public void focusGained(FocusEvent e) {
				loadBasemaps();
			}
		});
		
		toolkit.createLabel(bm, Messages.SmartLayersPage_MapBoundsLabel);
		
		txtBounds = toolkit.createText(bm, ""); //$NON-NLS-1$
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.minimumWidth = 30;
		txtBounds.setEditable(false);
		txtBounds.setLayoutData(gd);

		Hyperlink btnSetBounds = toolkit.createHyperlink(bm, Messages.SmartLayersPage_SetBoundsLink1, SWT.NONE);
		btnSetBounds.setToolTipText(Messages.SmartLayersPage_boundstooltip);
		gd = new GridData(SWT.FILL, SWT.BOTTOM, false, false);
		btnSetBounds.setLayoutData(gd);
		btnSetBounds.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				MapDialog md = new MapDialog(Display.getDefault().getActiveShell(), 
						getSelectedBasemapUuid(), mapItem.getMapBounds());
				if (md.open() != Window.OK){
					return;
				}
				
				ReferencedEnvelope re = md.getBounds();
				txtBounds.setData(re);
				updateModel(SmartMapItem.SMART_BOUNDS_GROUP);
				
			}
		});
		
		
		Hyperlink btnClearBounds = toolkit.createHyperlink(bm, Messages.SmartLayersPage_ClearBoundsLink, SWT.NONE);
		btnClearBounds.setToolTipText(Messages.SmartLayersPage_resettooltip);
		gd = new GridData(SWT.FILL, SWT.BOTTOM, false, false);
		btnClearBounds.setLayoutData(gd);
		btnClearBounds.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				txtBounds.setData(null);
				updateModel(SmartMapItem.SMART_BOUNDS_GROUP);
			}
		});
	}
	
	Job basemapJob = new Job("loading basemapes"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			basemapListenerEnabled = false;
			List<BasemapDefinition> maps = null;
			try{
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try {
					maps = HibernateManager.getBasemaps(session);
					Collections.sort(maps, new Comparator<BasemapDefinition>(){
						@Override
						public int compare(BasemapDefinition o1,
								BasemapDefinition o2) {
							return Collator.getInstance().compare(o1.getName().toUpperCase(), o2.getName().toUpperCase());
						}						
					});
				} finally {
					if (session.getTransaction().isActive()) {
						session.getTransaction().commit();
					}
					session.close();
				}
				
				BasemapDefinition defaultdef = new BasemapDefinition();
				defaultdef.setName(Messages.SmartLayersPage_DefaultBasemapLabel);
				defaultdef.setUuid( DEFAULT_BASEMAP );
				
				BasemapDefinition nonedef = new BasemapDefinition();
				nonedef.setName(Messages.SmartLayersPage_NoBasemapLabel);
				
				Object selection = defaultdef;
				if (mapItem != null){
					String uuid = mapItem.getBasemapName();
					if (uuid == null){
						selection = nonedef;
					}else if (uuid != null && uuid.equals(SmartMapItem.DEFAULT_BASEMAP_KEY)){
						selection = defaultdef;
					}else if (uuid != null){
						for (BasemapDefinition def : maps){
							if (UuidUtils.uuidToString(def.getUuid()).equals(uuid)){
								selection = def;
								break;
							}
						}
					}
				}
				maps.add(defaultdef);
				maps.add(nonedef);
				
				final List<BasemapDefinition> mymaps = maps;
				final Object myselection = selection;
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
						basemapCombo.setInput(mymaps.toArray());
						basemapCombo.setSelection(new StructuredSelection(myselection));		
					}
					
				});
				
				return Status.OK_STATUS;
			}finally{
				basemapListenerEnabled = true;
			}
		}
		
	};
	
	private synchronized void loadBasemaps(){
		basemapJob.schedule();
	}
	
	private List<LayerItem> getSelectedLayers() throws Exception{
		List<LayerItem> selections = new ArrayList<LayerItem>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblLayers.getSelection()).iterator(); iterator.hasNext();) {
			ExtendedItemHandle layerDefinition = (ExtendedItemHandle) iterator.next();
			selections.add((LayerItem)layerDefinition.getReportItem());
		}
		return selections;
		
	}


	private void updateModel(String prop) {
		// update the model
		try {
			if (prop.equals(SmartMapItem.SMART_BASEMAP_PROP)) {
				UUID uuid = getSelectedBasemapUuid();
				if (uuid == null){
					mapItem.setBasemapName(null);
				}else if (uuid.equals(DEFAULT_BASEMAP)){
					mapItem.setBasemapName(SmartMapItem.DEFAULT_BASEMAP_KEY);
				}else{
					String name = UuidUtils.uuidToString(uuid);
					mapItem.setBasemapName(name);
				}
			} else if (prop.equals(SmartMapItem.SMART_BOUNDS_GROUP)) {
				if (txtBounds.getData() == null){
					mapItem.setMapBounds(null);
				}else{
					mapItem.setMapBounds((ReferencedEnvelope)txtBounds.getData());
				}
			}
			
		} catch (Exception ex) {
			SmartMapItemPlugIn.displayLog(
					Messages.SmartLayersPage_Error_SettingMapProperty + ex.getMessage(), ex);
		}
	}
	

	/**
	 * 
	 * @return the selected basemap uuid or null
	 */
	private UUID getSelectedBasemapUuid(){
		IStructuredSelection selection = ((IStructuredSelection) basemapCombo.getSelection());
		if (selection.isEmpty()) {
			return null;
		} else {
			UUID uuid = ((BasemapDefinition) selection
					.getFirstElement()).getUuid();
			return uuid;
		}
	}
	
	@Override
	public void setInput(Object input) {
		Object element = input;
		if (input instanceof List && ((List<?>) input).size() > 0) {
			element = ((List<?>) input).get(0);
		}
		
		if ( element instanceof ExtendedItemHandle ){
			try{
				itemHandle = (ExtendedItemHandle) element;
				mapItem = (SmartMapItem) itemHandle.getReportItem();
			}catch (Exception ex){
				SmartMapItemPlugIn.displayLog(Messages.SmartLayersPage_Error_settingMap + ex.getMessage(), ex);
				return;
			}
			
		}else{
			return;
		}
	}

	@Override
	public void postElementEvent() {
		if (contentpane != null && !contentpane.isDisposed()) {
			updateUI();
		}
	}

	@Override
	public void dispose() {
		if (toolkit != null) {
			toolkit.dispose();
			toolkit = null;
		}
		contentpane.dispose();
		contentpane = null;
		
		if (cellEditor != null){
			cellEditor.dispose();
			cellEditor = null;
		}
		
	}

	
	private OdaDataSetHandle[] getHandles(){
		if (itemHandle != null){
			OdaDataSetHandle[] handles = BirtMapUtils.getDataSets(itemHandle);
			return handles;
		}
		return new OdaDataSetHandle[0];
	}
	
	protected void updateTable(){
		tblLayers.setInput(mapItem.getLayersProperty().getListValue());
	}
	
	protected void updateUI() {
		if (tblLayers == null){
			return;
		}
		updateTable();
		
		if (mapItem.getMapBounds() == null){
			txtBounds.setText(Messages.SmartLayersPage_MapExtentsBoundsLabel);
		}else{
			ReferencedEnvelope env = mapItem.getMapBounds();
			txtBounds.setText(env.getCoordinateReferenceSystem().getName().getCode() + ": (" + env.getMinX() + "," + env.getMinY() + "),(" + env.getMaxX() + "," + env.getMaxY() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		//basemapCombo.set
		loadBasemaps();
		//for mac ui table issue see smart bug 1349
		//putting this here causes error in layout in window so I've moved this above; after table created.
		//tblLayers.getTable().pack();
	}
	
	@Override
	public void refresh(){
		updateUI();
	}

	private void clearStyles(){
		try{
			List<LayerItem> sels = getSelectedLayers();
			for (LayerItem it : sels){
				it.setLayerStyles(null);
			}
		}catch (Exception ex){
			SmartMapItemPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	
	private void deleteLayers() {
		try{
			List<LayerItem> sels = getSelectedLayers();
			for (LayerItem it : sels){
				it.getHandle().drop();
			}
		}catch (Exception ex){
			SmartMapItemPlugIn.displayLog(ex.getMessage(), ex);
		}
	}

	private void moveUp() {
		try{
			List<LayerItem> sels = getSelectedLayers();
			if (sels.size() > 0){
				LayerItem item = sels.get(0);
				mapItem.moveLayer(item, -1);
				updateTable();
				
				tblLayers.setSelection(new StructuredSelection(item.getHandle()));
			}
		}catch (Exception ex){
			SmartMapItemPlugIn.displayLog(ex.getMessage(), ex);
		}
	}

	private void moveDown() {
		try{
			List<LayerItem> sels = getSelectedLayers();
			if (sels.size() > 0){
				LayerItem item = sels.get(0);
				mapItem.moveLayer(item, 1);
				updateTable();
				
				tblLayers.setSelection(new StructuredSelection(item.getHandle()));
			}
		}catch (Exception ex){
			SmartMapItemPlugIn.displayLog(ex.getMessage(), ex);
		}
	}

	private void updateLayerGeometry(ExtendedItemHandle handle){
		List<LayerDefinition> options = new ArrayList<LayerDefinition>();
		try {
			options = (new ExtensionManager()).getLayerOptions(new DataSetHandle[]{handle.getDataSet()});
			
			if (options.size() != 1) return;

			LayerItem li = (LayerItem) handle.getReportItem();
			String keyName = options.get(0).info.getGeometryColumn();
			
			li.setGeometryColumn(findLabel((OdaDataSetHandle)handle.getDataSet(), keyName));
			li.setLayerType(options.get(0).info.getLayerType());
			
			updateBindings(li, (OdaDataSetHandle)li.getHandle().getDataSet());
		} catch (Exception e) {
			SmartMapItemPlugIn.displayLog(e.getMessage(), e);
		}
		
		
	}
	private void updateBindings(LayerItem li, OdaDataSetHandle dsHandle) throws SemanticException{
		CachedMetaDataHandle meta = dsHandle.getCachedMetaDataHandle();
		MemberHandle resultSet = meta.getResultSet();
		 
		li.getHandle().getColumnBindings().clearValue();
		
		if (resultSet.getListValue() != null) {
			for (int i=0; i < resultSet.getListValue().size(); i++) {
				ResultSetColumnHandle resultSetColumn=(ResultSetColumnHandle)resultSet.getAt(i);
				ComputedColumn column=StructureFactory.newComputedColumn(li.getHandle(),resultSetColumn.getColumnName());
				column.setDataType(resultSetColumn.getDataType());
				
				column.setExpression("dataSetRow[\"" + resultSetColumn.getColumnName() + "\"]"); //$NON-NLS-1$ //$NON-NLS-2$
				column.setExpressionProperty("type", new Expression("javascript", "String"));  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
				
				try {
					
					li.getHandle().addColumnBinding(column,false);
				} catch (SemanticException e) {
					SmartMapItemPlugIn.log(e.getMessage(), e);
				}
			}
		}
	}
	private String findLabel(OdaDataSetHandle handle, String geomKey){
		Iterator<?> it = handle.columnHintsIterator();
		while(it.hasNext()){
			Object x = it.next();
			if (x instanceof ColumnHintHandle){
				ColumnHintHandle hh = (ColumnHintHandle) x;
				if (hh.getColumnName().equals(geomKey)){
					if (hh.getAlias() != null) return hh.getAlias();
					return hh.getColumnName();
				}
			}
		}
		return geomKey;
	}
	
	private void addLayer() {
		OdaDataSetHandle[] handles = getHandles();
		if (handles.length == 0){
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), ERROR_DIALOG_TITLE, Messages.SmartLayersPage_Error_NoDatasets);
			return;
		}
		List<LayerDefinition> options = new ArrayList<LayerDefinition>();
		try {
			options = (new ExtensionManager()).getLayerOptions(handles);
		} catch (Exception e) {
			SmartMapItemPlugIn.displayLog(e.getMessage(), e);
		}
		
		DatasetComobInputDialog dialog = new DatasetComobInputDialog(
			Display.getDefault().getActiveShell(),
			Messages.SmartLayersPage_AddLayer_DialogTitle,
			Messages.SmartLayersPage_AddLayer_DialogMessage,
			options);
		
		if (dialog.open() != Window.OK){
			return;
		}
		
		LayerDefinition ld = dialog.getValue();
		
		// attempt to parse id out of name; users do not want ids appearing the legends on maps
		//#1051
		String name = ld.handle.getDisplayName();
		int start = name.lastIndexOf('[');
		int end = name.lastIndexOf(']');
		if (start >= 0 && end >= 0 && start < end){
			name = name.substring(0, start);
		}
		ld.info.setLayerName(name);
		try{
			ExtendedItemHandle eihandle = itemHandle.getModuleHandle().getElementFactory().newExtendedItem(null, LayerItem.EXTENSION_NAME);
			LayerItem handle = (LayerItem)(new LayerItemFactory()).newReportItem(eihandle);
			handle.setLayerName(ld.info.getLayerName());
			handle.setLayerStyles(ld.info.getMapStyle());
			handle.setGeometryColumn(ld.info.getGeometryColumn());
			handle.setLayerType(ld.info.getLayerType());
			if (ld.handle != null){
				handle.getHandle().setDataSet(ld.handle);
				//find the column with the name matching ld.info.getGeometryColumnand update
				//to the displayName value
				handle.setGeometryColumn(findLabel(ld.handle, ld.info.getGeometryColumn()));
				updateBindings(handle, (OdaDataSetHandle)handle.getHandle().getDataSet());
			}
			mapItem.addLayers(Collections.singletonList(handle));

		}catch (Exception ex){
			SmartMapItemPlugIn.displayLog(ex.getMessage(), ex);
		}
		updateTable();
	}
}
