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
package org.wcs.smart.birt.map.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.refractions.udig.project.internal.StyleEntry;

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
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.wcs.smart.birt.map.BirtMapUtils;
import org.wcs.smart.birt.map.SmartMapItem;
import org.wcs.smart.birt.map.SmartMapItemPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.BasemapLabelProvider;
import org.wcs.smart.util.SmartUtils;
/**
 * A birt property tab for displaying map options
 * to user.
 * 
 * @author Emily
 *
 */
public class SmartLayersPage extends AttributesUtil.PageWrapper {

	public static final String PAGE_KEY = "MapLayers";
	protected FormToolkit toolkit;

	private Composite contentpane;
	private ComboViewer basemapCombo;
	private TableViewer tblLayers ;
	
	private WritableList layerItems;

	private Text txtBounds;

	private ExtendedItemHandle itemHandle;
	private SmartMapItem mapItem;
	private StyleCellEditor cellEditor;
	private Text txtSrid;
	
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
		createBasemap(contentpane);
		
		//separator
		SeperatorSection seperator2 = new SeperatorSection( contentpane, SWT.HORIZONTAL );
		seperator2.createSection();
		seperator2.layout();
		
		//bounds section
		createBounds(contentpane);
		
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

		Label l = toolkit.createLabel(contentpane, "Map Layers:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		tblLayers = new TableViewer(contentpane, SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.adapt(tblLayers.getTable());
		tblLayers.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblLayers.setContentProvider(new ObservableListContentProvider());
		tblLayers.getTable().setHeaderVisible(true);
		tblLayers.getTable().setLinesVisible(true);
		
		
		//dataset column
		final TableViewerColumn col1 = new TableViewerColumn(tblLayers, SWT.DEFAULT);
		col1.getColumn().setText("Report Dataset");
		col1.getColumn().setWidth(200);
		col1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof LayerDefinition){
					return ((LayerDefinition) element).handle.getDisplayName();
				}
				return super.getText(element);
			}
		});
		
		//name column
		final TableViewerColumn col2 = new TableViewerColumn(tblLayers, SWT.DEFAULT);
		col2.getColumn().setText("Layer Name");
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
		col3.getColumn().setText("Style");
		col3.getColumn().setWidth(200);
		col3.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof LayerDefinition){
					return ((LayerDefinition) element).style;
				}
				return super.getText(element);
			}
		});
		cellEditor = new StyleCellEditor(tblLayers.getTable());
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
					((LayerDefinition)element).style = null;
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
		
		Button btnAdd = toolkit.createButton(btnPanel, "Add", SWT.PUSH);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				DatasetComobInputDialog dialog = new DatasetComobInputDialog(
					Display.getDefault().getActiveShell(),
					"Add Layer",
					"Select the dataset to add to the map",
					getHandles());
				
				if (dialog.open() != Window.OK){
					return;
				}
				
				LayerDefinition ld = new LayerDefinition();
				ld.handle = dialog.getValue();
				ld.name = ld.handle.getDisplayName();
				ld.style = "default";
				
				layerItems.add(ld);
				tblLayers.refresh();
				
				updateModel(SmartMapItem.SMART_LAYER_PROP);
			}
		});
		
		
		Button btnRemove = toolkit.createButton(btnPanel, "Remove", SWT.PUSH);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				layerItems.removeAll(getSelectedLayers());
				updateModel(SmartMapItem.SMART_LAYER_PROP);
			}
		});
		
		Button btnUp = toolkit.createButton(btnPanel, "Move Up", SWT.PUSH);
		btnUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnUp.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
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
		});
		Button btnDown = toolkit.createButton(btnPanel, "Move Down", SWT.PUSH);
		btnDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDown.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
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
		});
	}
	
	/**
	 * Creates the bounds section
	 * @param parent
	 */
	private void createBounds(Composite parent){
		Composite bm = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout(6, false);
		layout.horizontalSpacing = 15;
		
		bm.setLayout(layout);
		bm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		toolkit.createLabel(bm, "Map Bounds:");
		
		txtBounds = toolkit.createText(bm, "");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.widthHint = 200;
		txtBounds.setLayoutData(gd);
		
		toolkit.createLabel(bm, "SRID:");
		txtSrid = toolkit.createText(bm, Area.AREA_CRS.getName().getCode());
		txtSrid.setEditable(false);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.widthHint = 200;
		txtSrid.setLayoutData(gd);
		

		Hyperlink btnSetBounds = toolkit.createHyperlink(bm, "Set Bounds", SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = 20;
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
				
				if(!CRS.equalsIgnoreMetadata(re.getCoordinateReferenceSystem(), Area.AREA_CRS)){
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Coordinate reference system must be lat/long");
				}else{
					txtBounds.setData(re);
					updateModel(SmartMapItem.SMART_BOUNDS_GROUP);
				}
			}
		});
		
		
		Hyperlink btnClearBounds = toolkit.createHyperlink(bm, "Clear", SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		btnClearBounds.setLayoutData(gd);
		btnClearBounds.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				txtBounds.setData(null);
				updateModel(SmartMapItem.SMART_BOUNDS_GROUP);
			}
		});
	}
	
	/**
	 * Create the basemap section
	 * @param parent
	 */
	private void createBasemap(Composite parent){
		Composite bm = toolkit.createComposite(parent);
		bm.setLayout(new GridLayout(2, false));
		bm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		toolkit.createLabel(bm, "Basemap:");
		
		basemapCombo = new ComboViewer(bm, SWT.READ_ONLY | SWT.DROP_DOWN);
		basemapCombo.setLabelProvider(new BasemapLabelProvider());
		basemapCombo.setContentProvider(ArrayContentProvider.getInstance());
		basemapCombo.setInput(basemapCombo);
	
		toolkit.adapt(basemapCombo.getCombo());
		basemapCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)basemapCombo.getControl().getLayoutData()).widthHint = 200;
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		List<BasemapDefinition> maps = null;
		try {
			maps = HibernateManager.getBasemaps(session);
			BasemapDefinition defaultdef = new BasemapDefinition();
			defaultdef.setName("(none)");
			maps.add(defaultdef);
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

				for (Iterator<?> iterator = layerItems.iterator(); iterator.hasNext();) {
					LayerDefinition type = (LayerDefinition) iterator.next();
					names.add(type.name);
					defs.add(type.handle.getQueryText());
					styles.add(type.style);
				}
				mapItem.setLayers(defs);
				mapItem.setLayerNames(names);
				mapItem.setLayerStyles(styles);

			} else if (prop.equals(SmartMapItem.SMART_BASEMAP_PROP)) {
				byte[] uuid = getSelectedBasemapUuid();
				if (uuid == null){
					mapItem.setBasemapName(null);
				}else{
					String name = SmartUtils.encodeHex(uuid);
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
					"Could not set property." + ex.getMessage(), ex);
		}
	}
	

	/**
	 * 
	 * @return the selected basemap uuid or null
	 */
	private byte[] getSelectedBasemapUuid(){
		IStructuredSelection selection = ((IStructuredSelection) basemapCombo.getSelection());
		if (selection.isEmpty()) {
			return null;
		} else {
			byte[] uuid = ((BasemapDefinition) selection
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
				SmartMapItemPlugIn.displayLog("Could not set map input. " + ex.getMessage(), ex);
				return;
			}
			
		}else{
			return;
		}
			
		if (layerItems == null) {
			layerItems = new WritableList();
			if (mapItem != null && mapItem.getLayers() != null) {
				for (int i = 0; i < mapItem.getLayers().size(); i++) {
					LayerDefinition def = new LayerDefinition();
					def.handle = BirtMapUtils.findHandle(
							(ReportDesignHandle) this.itemHandle.getRoot(),
							mapItem.getLayers().get(i));
					if (def.handle != null) {
						if (mapItem.getLayerNames() != null){
							def.name = mapItem.getLayerNames().get(i);
						}else{
							def.name = def.handle.getName();
						}
						if (mapItem.getLayerStyles() != null){
							def.style = mapItem.getLayerStyles().get(i);
						}
						layerItems.add(def);
					}
				}
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
			return BirtMapUtils.getSmartDataSetHandles(itemHandle);
		}
		return new OdaDataSetHandle[0];
	}
	
	
	
	protected void updateUI() {
		if (tblLayers == null){
			return;
		}
		tblLayers.setInput(layerItems);
		
		String uuid =mapItem.getBasemapName();
		Object[] data = (Object[]) basemapCombo.getInput();
		Object selection = data[0];
		for (int i = 0; i < data.length; i ++){
			BasemapDefinition bm = (BasemapDefinition) data[i];
			if ((uuid == null && bm.getUuid() == null) ||
					(SmartUtils.encodeHex(bm.getUuid()).equals(uuid))){
				selection = data[i];
				break;
			}
		}
		basemapCombo.setSelection(new StructuredSelection(selection));
		
		if (mapItem.getMapBounds() == null){
			txtBounds.setText("(map extents)");
		}else{
			ReferencedEnvelope env = mapItem.getMapBounds();
			txtSrid.setText(env.getCoordinateReferenceSystem().getName().getCode());
			txtBounds.setText("(" + env.getMinX() + "," + env.getMinY() + "),(" + env.getMaxX() + "," + env.getMaxY() + ")");
		}

	}
	
}


class LayerDefinition{
	
	OdaDataSetHandle handle;
	String name;
	String style;
	
	@Override
	public boolean equals(Object other){
		if (!(other instanceof LayerDefinition)){
			return false;
		}
		LayerDefinition o = (LayerDefinition) other;
		return o.handle.equals(this.handle) && strEquals(o.name, this.name) && strEquals(o.style, this.style);
	}
	
	private boolean strEquals(String x, String y){
		if (x == null && y == null) return true;
		return (x != null && y != null && x.equals(y));
	}
}