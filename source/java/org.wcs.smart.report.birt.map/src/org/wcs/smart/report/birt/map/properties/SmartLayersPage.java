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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.birt.report.designer.internal.ui.views.attributes.section.SeperatorSection;
import org.eclipse.birt.report.designer.ui.views.attributes.AttributesUtil;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
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
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;
import org.wcs.smart.report.birt.map.SmartMapItem;
import org.wcs.smart.report.birt.map.SmartMapItemPlugIn;
import org.wcs.smart.report.birt.map.internal.Messages;
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

	private static final UUID DEFAULT_BASEMAP = UuidUtils.stringToUuid("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"); 
	
	private static final String ERROR_DIALOG_TITLE = Messages.SmartLayersPage_ErrorDialog_Title;
	public static final String PAGE_KEY = "MapLayers"; //$NON-NLS-1$
	protected FormToolkit toolkit;

	private Composite contentpane;
	private ComboViewer basemapCombo;
	private TableViewer tblLayers ;
	
	private WritableList layerItems;

	private Text txtBounds;

	private ExtendedItemHandle itemHandle;
	private SmartMapItem mapItem;
	private StyleCellEditor cellEditor;
	
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
		
		tblLayers.setContentProvider(new ObservableListContentProvider());
		tblLayers.getTable().setHeaderVisible(true);
		tblLayers.getTable().setLinesVisible(true);
		
		
		//dataset column
		final TableViewerColumn col1 = new TableViewerColumn(tblLayers, SWT.DEFAULT);
		col1.getColumn().setText(Messages.SmartLayersPage_ReportDataset_ColumnName);
		col1.getColumn().setWidth(200);
		col1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof LayerDefinition){
					LayerDefinition ld = (LayerDefinition)element;
					if (ld.handle != null){
						return ld.handle.getDisplayName();
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
				if (element instanceof LayerDefinition){
					return ((LayerDefinition) element).name;
				}
				return super.getText(element);
			}
		};
		col2.setLabelProvider(col2Lp);
		col2.setEditingSupport(new EditingSupport(col2.getViewer()) {
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof LayerDefinition && value instanceof String){
					((LayerDefinition)element).name = (String) value;
					tblLayers.refresh();
					updateModel(SmartMapItem.SMART_LAYER_PROP);
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
				if (element instanceof LayerDefinition){
					if (((LayerDefinition) element).style == null){
						return Messages.SmartLayersPage_DefaultStyleLabel;
					}else if (getImage(element) == null){
						return ((LayerDefinition) element).style;
					}
					return ""; //$NON-NLS-1$
				}
				return super.getText(element);
			}
			
			@Override
			public Image getImage(Object element) {
				if (element instanceof LayerDefinition){
					if (((LayerDefinition) element).style != null){
						return BirtMapUtils.parseImageFromStyleString(((LayerDefinition)element).style);
					}
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
				if (element instanceof LayerDefinition){
					return ((LayerDefinition)element);
				}
				return null;
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof LayerDefinition ){
					if (value instanceof String){
						((LayerDefinition)element).style = (String)value;
					}
					tblLayers.refresh();
					updateModel(SmartMapItem.SMART_LAYER_PROP);
				}
			}
		});
        
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
		basemapCombo.setInput(basemapCombo);
		basemapCombo.getCombo().setToolTipText(Messages.SmartLayersPage_basemaptooltipe);
	
		//toolkit.adapt(basemapCombo.getCombo());
		basemapCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)basemapCombo.getControl().getLayoutData()).minimumWidth = 100;
		((GridData)basemapCombo.getControl().getLayoutData()).widthHint = 200;
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		List<BasemapDefinition> maps = null;
		try {
			maps = HibernateManager.getBasemaps(session);
			
			BasemapDefinition defaultdef = new BasemapDefinition();
			defaultdef.setName(Messages.SmartLayersPage_DefaultBasemapLabel);
			defaultdef.setUuid( DEFAULT_BASEMAP );
			maps.add(defaultdef);
			
			BasemapDefinition nonedef = new BasemapDefinition();
			nonedef.setName(Messages.SmartLayersPage_NoBasemapLabel);
			maps.add(nonedef);
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().commit();
			}
			session.close();
		}
		
		basemapCombo.setInput(maps.toArray());
		basemapCombo.getControl().addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateModel(SmartMapItem.SMART_BASEMAP_PROP);		
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
	
	private List<LayerDefinition> getSelectedLayers(){
		List<LayerDefinition> selections = new ArrayList<LayerDefinition>();
		for (@SuppressWarnings("unchecked")
		Iterator<LayerDefinition> iterator = ((IStructuredSelection)tblLayers.getSelection()).iterator(); iterator.hasNext();) {
			LayerDefinition layerDefinition = (LayerDefinition) iterator.next();
			selections.add(layerDefinition);
		}
		return selections;
		
	}


	private void updateModel(String prop) {
		// update the model
		try {
			if (prop.equals(SmartMapItem.SMART_LAYER_PROP)) {
				ArrayList<String> names = new ArrayList<String>();
				ArrayList<String> defs = new ArrayList<String>();
				ArrayList<String> styles = new ArrayList<String>();
				ArrayList<String> datasets = new ArrayList<String>();
				
				for (Iterator<?> iterator = layerItems.iterator(); iterator.hasNext();) {
					LayerDefinition type = (LayerDefinition) iterator.next();
					
					names.add(type.name);
					if (type.handle != null){
						defs.add(type.handle.getQueryText());
						datasets.add(type.handle.getName());	
					}else{
						defs.add(null);
						datasets.add(""); //$NON-NLS-1$
					}
					styles.add(type.style);
				}
				mapItem.setLayers(defs);
				mapItem.setLayerNames(names);
				mapItem.setLayerStyles(styles);
				mapItem.setDatasets(datasets);
				
			} else if (prop.equals(SmartMapItem.SMART_BASEMAP_PROP)) {
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
			
		layerItems = new WritableList();
		if (mapItem != null && mapItem.getLayers() != null) {
			for (int i = 0; i < mapItem.getLayers().size(); i++) {
				LayerDefinition def = new LayerDefinition();

				def.handle = BirtMapUtils.findHandle(
						(ReportDesignHandle) this.itemHandle.getRoot(),
						i < mapItem.getDatasets().size() ? mapItem.getDatasets().get(i) : null,
						mapItem.getLayers().get(i));
				def.mapLayer = mapItem.findMapLayerManager(def.handle);
				if (mapItem.getLayerNames() != null  && i < mapItem.getLayerNames().size()){
					def.name = mapItem.getLayerNames().get(i);
				}else if (def.handle != null){
					def.name = def.handle.getName();
				}else{
					def.name = Messages.SmartLayersPage_MapLayerNameErrorLabel; 
				}
				if (mapItem.getLayerStyles() != null && i < mapItem.getLayerStyles().size()){
					def.style = mapItem.getLayerStyles().get(i);
				}
				layerItems.add(def);
				
			}
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
			List<IBirtMapLayerManager> mapLayers = BirtMapUtils.getMapLayerExtensions();
			
			List<OdaDataSetHandle> thisHandles = new ArrayList<OdaDataSetHandle>();
			for (int i = 0; i < handles.length; i ++){
				for (IBirtMapLayerManager l : mapLayers){
					if (l.canAddToMap(handles[i])){
						thisHandles.add(handles[i]);
					}
				}
			}
			return thisHandles.toArray(new OdaDataSetHandle[thisHandles.size()]);	
		}
		return new OdaDataSetHandle[0];
	}
	
	
	
	protected void updateUI() {
		if (tblLayers == null){
			return;
		}
		tblLayers.setInput(layerItems);
		
		String uuid = mapItem.getBasemapName();
		Object[] data = (Object[]) basemapCombo.getInput();
		Object selection = data[0];
		for (int i = 0; i < data.length; i ++){
			BasemapDefinition bm = (BasemapDefinition) data[i];
			if ((uuid == null && bm.getUuid() == null) ||
				(uuid != null && uuid.equals(SmartMapItem.DEFAULT_BASEMAP_KEY) && bm.getUuid().equals(DEFAULT_BASEMAP)) || 
					UuidUtils.uuidToString(bm.getUuid()).equals(uuid)){
				selection = data[i];
				break;
			}
		}
		basemapCombo.setSelection(new StructuredSelection(selection));
		
		if (mapItem.getMapBounds() == null){
			txtBounds.setText(Messages.SmartLayersPage_MapExtentsBoundsLabel);
		}else{
			ReferencedEnvelope env = mapItem.getMapBounds();
			txtBounds.setText(env.getCoordinateReferenceSystem().getName().getCode() + ": (" + env.getMinX() + "," + env.getMinY() + "),(" + env.getMaxX() + "," + env.getMaxY() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		//for mac ui table issue see smart bug 1349
		tblLayers.getTable().pack();
	}
	
	@Override
	public void refresh(){
		updateUI();
	}

	private void clearStyles(){
		List<LayerDefinition> sels = getSelectedLayers();
		for (LayerDefinition sel : sels){
			sel.style = null;
		}
		updateModel(SmartMapItem.SMART_LAYER_PROP);
	}
	
	private void deleteLayers() {
		layerItems.removeAll(getSelectedLayers());
		updateModel(SmartMapItem.SMART_LAYER_PROP);
	}

	private void moveUp() {
		List<LayerDefinition> sels = getSelectedLayers();
		if (sels.size() > 0){
			LayerDefinition def = sels.get(0);
			int index = layerItems.indexOf(def);
			layerItems.remove(def);
			index--;
			if (index < 0){
				index = 0;
			}
			layerItems.add(index, def);
			tblLayers.setSelection(new StructuredSelection(def));
			
			updateModel(SmartMapItem.SMART_LAYER_PROP);
		}
	}

	private void moveDown() {
		List<LayerDefinition> sels = getSelectedLayers();
		if (sels.size() > 0){
			LayerDefinition def = sels.get(0);
			int index = layerItems.indexOf(def);
			layerItems.remove(def);
			index++;
			if (index > layerItems.size()){
				index = layerItems.size();
			}
			layerItems.add(index, def);
			tblLayers.setSelection(new StructuredSelection(def));
			
			updateModel(SmartMapItem.SMART_LAYER_PROP);
		}
	}

	private void addLayer() {
		OdaDataSetHandle[] handles = getHandles();
		if (handles.length == 0){
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), ERROR_DIALOG_TITLE, Messages.SmartLayersPage_Error_NoDatasets);
			return;
		}
		DatasetComobInputDialog dialog = new DatasetComobInputDialog(
			Display.getDefault().getActiveShell(),
			Messages.SmartLayersPage_AddLayer_DialogTitle,
			Messages.SmartLayersPage_AddLayer_DialogMessage,
			handles);
		
		if (dialog.open() != Window.OK){
			return;
		}
		
		LayerDefinition ld = new LayerDefinition();
		ld.handle = dialog.getValue();
		ld.name = ld.handle.getDisplayName();
		
		layerItems.add(ld);
		tblLayers.refresh();
		
		updateModel(SmartMapItem.SMART_LAYER_PROP);
	}
}


class LayerDefinition{
	
	IBirtMapLayerManager mapLayer;
	OdaDataSetHandle handle;
	String name;
	String style;
	
	@Override
	public boolean equals(Object other){
		if (!(other instanceof LayerDefinition)){
			return false;
		}
		LayerDefinition o = (LayerDefinition) other;
		if (o.handle != null && this.handle != null){
			return (o.handle.equals(this.handle))
				&& strEquals(o.name, this.name) 
				&& strEquals(o.style, this.style);
		}else{
			return super.equals(other);
		}
	}
	
	private boolean strEquals(String x, String y){
		if (x == null && y == null) return true;
		return (x != null && y != null && x.equals(y));
	}
}