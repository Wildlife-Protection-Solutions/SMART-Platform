/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004-2008, Refractions Research Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package org.wcs.smart.ui.map;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.hibernate.Session;
import org.locationtech.udig.internal.ui.IDropTargetProvider;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.internal.tool.display.ToolManager;
import org.locationtech.udig.project.ui.internal.tool.display.ToolProxy;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.project.ui.tool.IToolManager;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.locationtech.udig.ui.UDIGDragDropUtilities;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.map.internal.LoadBasemapHandler;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.util.E3Utils;

/**
 * A map view.
 * 
 * @author Emily Gouge
 * @since 1.0.0
 */
public class MapView implements IDropTargetProvider, MapPart, IAdaptable {

	public static final String ID = "org.wcs.smart.ui.map.MapView"; //$NON-NLS-1$
	private static final String MAP_NAME = Messages.MapView_MapName;
	
    protected MapViewer mapviewer;
    protected Map map;
    
    @Inject 
    private EPartService partService;
    
    private ToolProxy activeTool = null;
    
    private IPartListener listener = new IPartListener() {
		@Override
		public void partVisible(MPart part) {
		}
		
		@Override
		public void partHidden(MPart part) {
		}
		
		@Override
		public void partDeactivated(MPart part) {
			if (part.getElementId().equals(ID)){
		    	Object x = E3Utils.getSourceObject(part);
				if (x instanceof IAdaptable
						&& ((IAdaptable) x).getAdapter(MapView.class) == MapView.this) {
					activeTool = ((ToolManager)ApplicationGIS.getToolManager()).getActiveToolProxy();
				}
			}
		}
		
		@Override
		public void partBroughtToTop(MPart part) {
		}
		
		@Override
		public void partActivated(MPart part) {
			if (part.getElementId().equals(ID)){
		    	Object x = E3Utils.getSourceObject(part);
				if (x instanceof IAdaptable
						&& ((IAdaptable) x).getAdapter(MapView.class) == MapView.this) {
					IToolManager tools = ApplicationGIS.getToolManager();
					tools.setCurrentEditor(MapView.this);
					selectLastTool();
				}
			}
		}
	};
	
	
    public MapView() {
        super();
    }
    
    @PostConstruct
    public void createPartControl( Composite parent ) {
    	GridLayout layout = new GridLayout(1,false);
    	layout.marginBottom=0;
    	layout.marginHeight = 0;
    	layout.marginLeft = 0;
    	layout.marginRight = 0;
    	layout.marginTop = 0;
    	layout.marginWidth = 0;
    	layout.verticalSpacing = 2; 
    	layout.horizontalSpacing = 0;
        parent.setLayout(layout);
        
        // mapviewer = new MapViewer(parent, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED | SWT.MULTI);
        mapviewer = new MapViewer(parent, SWT.SINGLE | SWT.DOUBLE_BUFFERED);
        mapviewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
		// create a new empty map
        // if you are going to add layers do so now
        // prior to adding to the mapviewer
        // 
        map = (Map) ProjectFactory.eINSTANCE.createMap();
        map.setName(MAP_NAME);
        mapviewer.setMap(map);
		
        //set default crs
        map.getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		map.getViewportModelInternal().setCRS(Area.AREA_CRS);
		
		new MapInfoAreaComposite(parent, SWT.NONE, mapviewer);

        setTool("org.locationtech.udig.tools.Zoom"); //$NON-NLS-1$
        
        UDIGDragDropUtilities.addDropSupport(mapviewer.getViewport().getControl(), this);
        
        partService.addPartListener(listener);
        
        LoadDefaultLayersJob job = new LoadDefaultLayersJob(getMap());
		job.schedule();
    }

    private void setTool( String toolId ) {
    	ToolProxy mi =((ToolManager)ApplicationGIS.getToolManager()).findToolProxy(toolId);
    	if (mi != null){
    		ApplicationGIS.getToolManager().getToolAction(mi.getId(), mi.getCategoryId()).run();
    	}
    }
    
    private void selectLastTool(){
    	if (this.activeTool != null){
    		setTool(activeTool.getId());
    	}
    }
    
    @Optional
    @Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
    	//what we need to do is reload the current selected basemap
    	//load it from the database; and apply it to the map
    	UUID basemapuuid = (UUID) mapviewer.getMap().getBlackboard().get(MapSettings.BASEMAP_BLACKBOARD_UUID_KEY);
    	BasemapDefinition def = null;
    	if (basemapuuid != null){
    		
    		Session s = HibernateManager.openSession();
    		try{
    			def = (BasemapDefinition) s.get(BasemapDefinition.class, basemapuuid);
    		}finally{
    			s.close();
    		}
    	}
    	if (def == null){
    		//reload default basemap
    		LoadDefaultLayersJob job = new LoadDefaultLayersJob(getMap());
    		job.schedule();
    	}else{
    		LoadBasemapHandler.loadBasemap(mapviewer.getMap(), def);
    	}
    	
		mapviewer.getRenderManager().refresh(null);
	}
    
    @Focus
    public void setFocus() {
        mapviewer.getViewport().getControl().setFocus();
    }

    public Map getMap() {
        return mapviewer.getMap();
    }

    @PreDestroy
    public void dispose() {
    	partService.removePartListener(listener);
		if (mapviewer != null && mapviewer.getViewport() != null && getMap() != null) {
			mapviewer.getViewport().removePaneListener(getMap().getViewportModelInternal());
		}
		if (mapviewer != null) {
			mapviewer.getRenderManager().stopRendering();
			mapviewer.getRenderManager().dispose();
			mapviewer.dispose();
		}
    }

    public void openContextMenu() {
        mapviewer.openContextMenu();
    }

    public void setFont( Control control ) {
        mapviewer.setFont(control);
    }

    public void setSelectionProvider( IMapEditorSelectionProvider selectionProvider ) {
        mapviewer.setSelectionProvider(selectionProvider);
    }

	@Override
	public IStatusLineManager getStatusLineManager() {
//		return statusLineManager;
		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(Class adaptee) {
		if (adaptee.isAssignableFrom(MapView.class)){
			return this;
		}else if (adaptee.isAssignableFrom(Map.class)) {
			return getMap();
		}
		return null;
	}

	@Override
	public Object getTarget(DropTargetEvent event) {
		return this;
	}
}
