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
package org.wcs.smart.ui.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.mapgraphic.internal.MapGraphicService;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.udig.SetBasemapTool;
import org.wcs.smart.udig.SmartDistanceTool;
import org.wcs.smart.ui.map.tool.ClearSelectionTool;
import org.wcs.smart.util.GeometryUtils;

/**
 * Composite to display list of tracks.
 * 
 * @author elitvin
 * @since 6.0.0
 */
public abstract class TracksComposite extends Composite implements MapPart {

	private List<ITracksCompositeListener> changeListeners = new ArrayList<>();
	
	private TableViewer trackViewer;
	private MapViewer mapViewer;
	private MapToolComposite toolComp;
	
	private ToolItem editItem;
	private ToolItem deleteItem;
	private ToolItem splitItem;
	private ToolItem mergeItem;
	
	private Label infoLabel;
	private Label infoImage;

	private boolean canEdit;
	
	public TracksComposite(Composite parent) {
		this(parent, true);
	}

	public TracksComposite(Composite parent, boolean canEdit) {
		super(parent, SWT.NONE);
		this.canEdit = canEdit;
	}

	@Override
	public void dispose(){
		super.dispose();
		mapViewer.getRenderManager().stopRendering();
		mapViewer.getRenderManager().dispose();
		mapViewer.dispose();
		mapViewer = null;
	}
	
	protected void createControls() {
		setLayout(new GridLayout());
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm sash = new SashForm(this, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftArea = new Composite(sash, SWT.NONE);
		leftArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		leftArea.setLayout(gl);
		
		TabFolder tabFolder = new TabFolder(leftArea, SWT.TOP);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TabItem  tracksTabItem = new TabItem(tabFolder, SWT.NONE);
		tracksTabItem.setText(Messages.TracksComposite_Tracks);
		
		Composite tableCompOuter = new Composite(tabFolder, SWT.NONE);
		gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		tableCompOuter.setLayout(gl);
		tableCompOuter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//========links========
		ToolBar bar = new ToolBar(tableCompOuter, SWT.HORIZONTAL);
		
		addImportToolbarItem(bar);
		
		editItem = new ToolItem(bar, SWT.PUSH);
		editItem.setText(canEdit ? Messages.TracksComposite_Edit : Messages.TracksComposite_View);
		editItem.setToolTipText(canEdit ? Messages.TracksComposite_Edit_Tooltip : Messages.TracksComposite_View_Tooltip);
		editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_TRACK_ICON));
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editTrack();
			}
		});
		editItem.setEnabled(false);

		if (canEdit) {
			splitItem = new ToolItem(bar, SWT.RADIO);
			splitItem.setText(Messages.TracksComposite_Split);
			splitItem.setToolTipText(Messages.TracksComposite_Split_Tooltip);
			splitItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.SPLIT_TRACK_ICON));
			splitItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					splitTrack(splitItem);
				}
			});
			splitItem.setEnabled(false);
			
			mergeItem = new ToolItem(bar, SWT.PUSH);
			mergeItem.setText(Messages.TracksComposite_Merge);
			mergeItem.setToolTipText(Messages.TracksComposite_Merge_Tooltip);
			mergeItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MERGE_TRACK_ICON));
			mergeItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					mergeTrack();
				}
			});
			mergeItem.setEnabled(false);
			
			deleteItem = new ToolItem(bar, SWT.PUSH);
			deleteItem.setText(Messages.TracksComposite_Delete);
			deleteItem.setToolTipText(Messages.TracksComposite_Delete_Tooltip);
			deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			deleteItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteTrack();
				}
			});
			deleteItem.setEnabled(false);
		}
		
		Composite tableComp = new Composite(tableCompOuter, SWT.BORDER);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tracksTabItem.setControl(tableCompOuter);
		
		TableColumnLayout layout = new TableColumnLayout();
		tableComp.setLayout(layout);
		
		trackViewer = new TableViewer(tableComp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		trackViewer.setContentProvider(ArrayContentProvider.getInstance());
		trackViewer.getTable().setHeaderVisible(true);
		trackViewer.getTable().setLinesVisible(true);
		trackViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateMapSelection();	
			}
		});
		
		MenuManager mgr = new MenuManager();
		Menu menu = mgr.createContextMenu(trackViewer.getTable());
		trackViewer.getTable().setMenu(menu);
		mgr.add(new Action(Messages.TracksComposite_ZoomTo, 
				SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.ZOOM_TRACK_ICON)) {
			@Override
			public void run(){
				zoomTrack();
			}
		});
		if (canEdit) {
			mgr.add(new Separator());
			mgr.add(new Action(Messages.TracksComposite_Edit, 
					SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.EDIT_TRACK_ICON)) {
				@Override
				public void run(){
					editTrack();
				}
			});
			trackViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					editTrack();
				}
			});
			mgr.add(new Action(Messages.TracksComposite_Merge, 
					SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.MERGE_TRACK_ICON)) {
				@Override
				public void run(){
					mergeTrack();
				}
			}); 
			mgr.add(new Action(Messages.TracksComposite_Delete, 
					SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.DELETE_ICON)) {
				@Override
				public void run(){
					deleteTrack();
				}
			}); 
		}
		
		createTableViewerColumns(trackViewer, layout);
		
		TabItem  layerListTabItem = new TabItem(tabFolder, SWT.NONE);
		layerListTabItem.setText(Messages.TracksComposite_MapLayers);
		
		Composite layersTab = new Composite(tabFolder, SWT.NONE);
		gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		layersTab.setLayout(gl);
		LayerListComposite lv = new LayerListComposite(layersTab);
		lv.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		layerListTabItem.setControl(layersTab);
		
		//========map part========
		final Composite mapPart = new Composite(sash, SWT.NONE);
		gl = new GridLayout(2, false);
		gl.marginWidth = gl.marginHeight = 0;
		mapPart.setLayout(gl);

		Composite infoArea = new Composite(mapPart, SWT.NONE);
		infoArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		gl = new GridLayout(2, false);
		gl.marginWidth = gl.marginHeight = 0;
		infoArea.setLayout(gl);
		
		infoImage = new Label(infoArea, SWT.NONE);
		infoImage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		infoLabel = new Label(infoArea, SWT.NONE);
		infoLabel.setText(""); //$NON-NLS-1$
		infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		setupMap(mapPart);
		
		sash.setWeights(new int[]{30,70});
		
		lv.setMap(mapViewer.getMap());
		
		addLayers(mapViewer);
		
		ID tmp = new ID(MapGraphicService.SERVICE_ID, "legend"); //$NON-NLS-1$
		IGeoResource resource = CatalogPlugin.getDefault().getLocalCatalog().getById(IGeoResource.class, tmp, null);
		if (resource != null){
			AddLayersCommand command = new AddLayersCommand(Collections.singleton(resource));
			mapViewer.getMap().sendCommandASync(command);
		}
	}

	protected abstract void createTableViewerColumns(TableViewer trackTableViewer, TableColumnLayout layout);

	protected abstract void addLayers(MapViewer viewer);

	protected String getMapName() {
		return Messages.TracksComposite_MapName;
	}

	private void setupMap(Composite parent){
		Composite mapComp = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = gl.marginHeight = 0;
		mapComp.setLayout(gl);
		mapComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
		mapViewer = new MapViewer(mapComp, SWT.NONE);
		mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
		Map map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName(getMapName());
		mapViewer.setMap(map);
		
		//set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(GeometryUtils.SMART_CRS);
		
		final LoadDefaultLayersJob defaultLayer = new LoadDefaultLayersJob(mapViewer.getMap());
		// we need to do this because this map is in a dialog box and
		// events does work correctly
		defaultLayer.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (mapViewer == null || TracksComposite.this.isDisposed()){
					return;
				}
				mapViewer.getMap().getRenderManager().refresh(null);
				
			}
		});
		defaultLayer.schedule();
			
			
		ApplicationGIS.getToolManager().setCurrentEditor(this);
		String[] thisTools = new String[] {
				SetBasemapTool.ID,
				MapToolComposite.UDIG_ZOOM_EXTENT_ID,
				MapToolComposite.UDIG_PAN_ID,
				MapToolComposite.UDIG_ZOOM_ID,
				MapToolComposite.UDIG_ZOOM_IN_ID,
				MapToolComposite.UDIG_ZOOM_OUT_ID,
				ClearSelectionTool.ID,
				SmartDistanceTool.ID};

		toolComp = new MapToolComposite(thisTools);
		toolComp.createComposite(mapComp);
		MapInfoAreaComposite infoComp = new MapInfoAreaComposite(mapComp, SWT.NONE, mapViewer) ;
		infoComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		toolComp.selectTool(MapToolComposite.UDIG_PAN_ID);
	}
	
	protected void updateMapSelection() {
		//update map language
		IStructuredSelection selection = (IStructuredSelection) trackViewer.getSelection();
		
		boolean isSelected = !selection.isEmpty();
		if (editItem != null)   editItem.setEnabled(isSelected);
		if (splitItem != null)  splitItem.setEnabled(isSelected);
		if (deleteItem != null) deleteItem.setEnabled(isSelected);
		if (mergeItem != null)  mergeItem.setEnabled(isSelected);
	}

	protected void addImportToolbarItem(ToolBar bar) {
		ToolItem importItem = new ToolItem(bar, SWT.PUSH);
		importItem.setText(Messages.TracksComposite_Import);
		importItem.setToolTipText(Messages.TracksComposite_Import_Tooltip);
		importItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_TRACK_ICON));
		importItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importTracks();	
			}
		});
	}

	protected final boolean isEditAllowed() {
		return canEdit;
	}

	protected abstract void importTracks();
	
	protected abstract void mergeTrack();
	
	protected abstract void splitTrack(ToolItem splitToolItem);
	
	protected abstract void deleteTrack();

	protected abstract void editTrack();

	protected abstract void zoomTrack();

	public void addChangeListener(ITracksCompositeListener listener){
		changeListeners.add(listener);
	}
	
	protected void fireChangeListeners() {
		for (ITracksCompositeListener listener: changeListeners) {
			listener.compositeModified();
		}
	}
	
	protected void clearMessage(){
		infoImage.setImage(null);
		infoLabel.setText(""); //$NON-NLS-1$
	}
	protected void setError(String message){
		infoLabel.setText(message);
		infoImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		infoImage.getParent().layout();
	}
	
	protected void setInfo(String message){
		infoLabel.setText(message);
		infoImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.INFO_ICON));
		infoImage.getParent().layout();
	}
	
	protected MapViewer getMapViewer() {
		return mapViewer;
	}
	
	protected TableViewer getTrackViewer() {
		return trackViewer;
	}
	
	protected void selectLastTool() {
		toolComp.selectLastTool();
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
	public void setSelectionProvider(IMapEditorSelectionProvider selectionProvider) {
		mapViewer.setSelectionProvider(selectionProvider);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}

	public static interface ITracksCompositeListener {
		public void compositeModified();
	}
}