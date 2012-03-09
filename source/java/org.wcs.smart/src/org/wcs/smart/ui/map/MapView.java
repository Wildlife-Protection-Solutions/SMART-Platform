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

import java.text.DecimalFormat;
import java.text.NumberFormat;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.internal.LayersView;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.internal.tool.ToolContext;
import net.refractions.udig.project.ui.internal.tool.impl.ToolContextImpl;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseMotionListener;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.tool.ModalTool;
import net.refractions.udig.project.ui.tool.Tool;
import net.refractions.udig.project.ui.viewers.MapViewer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.wcs.smart.ca.Area;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A map view.
 * 
 * @author Emily Gouge, Graham Davis (Refractions Research, Inc.)
 * @since 1.1.0
 * @version 1.3.0
 */
public class MapView extends ViewPart implements MapPart, IAdaptable {

	public static String ID = "org.wcs.smart.ui.map.MapView";
    // private GISWidget widget;
    private MapViewer mapviewer;
    // private RenderManager renderManager;
    private Map map;

    private ModalTool activeTool;
    private ToolContext toolcontext;
    
    
	private Label lblCoordinates;
    public MapView() {
        super();
    }

    @Override
    public void createPartControl( Composite parent ) {
    	GridLayout layout = new GridLayout(1,false);
    	layout.marginBottom=0;
    	layout.marginHeight = 0;
    	layout.marginLeft = 0;
    	layout.marginRight = 0;
    	layout.marginTop = 0;
    	layout.marginWidth = 0;
        parent.setLayout(layout);
        
        // mapviewer = new MapViewer(parent, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED | SWT.MULTI);
        mapviewer = new MapViewer(parent, SWT.SINGLE | SWT.DOUBLE_BUFFERED);
        mapviewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        // create a new empty map
        // if you are going to add layers do so now
        // prior to adding to the mapviewer
        // 
        map = (Map) ProjectFactory.eINSTANCE.createMap();
        mapviewer.setMap(map);
		
        //set default crs
        map.getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		map.getViewportModelInternal().setCRS(Area.AREA_CRS);
		
        Composite infoArea = new Composite(parent, SWT.BORDER);
        infoArea.setLayout(new GridLayout(3, false));
        infoArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        lblCoordinates = new Label(infoArea, SWT.NONE);
        lblCoordinates.setText("Coordinates");
        lblCoordinates.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        lblCoordinates.setAlignment(SWT.RIGHT);
        
        final Label lblSeparator = new Label(infoArea, SWT.SEPARATOR | SWT.VERTICAL);
        GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        lblSeparator.setLayoutData(gd);
        
        final Label lblSRID = new Label(infoArea, SWT.NONE);
        lblSRID.setText(map.getViewportModel().getCRS().getName().getCode());
        lblSRID.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
        map.getViewportModel().addViewportModelListener(new IViewportModelListener() {
			
			@Override
			public void changed(ViewportModelEvent event) {
				if(event.getType() == ViewportModelEvent.EventType.CRS){
					Display display = PlatformUI.getWorkbench().getDisplay();
			        if (display == null){
			        	display = Display.getDefault();
			        }
			        display.asyncExec(new Runnable(){
						@Override
						public void run() {
							lblSRID.setText(map.getViewportModel().getCRS().getName().getCode());
							lblSRID.getParent().layout();
						}});
				}
				
			}
		});
      
        
        mapviewer.getViewport().addMouseMotionListener(new MapMouseMotionListener() {
			
			@Override
			public void mouseMoved(MapMouseEvent event) {
				event.getPoint();
				Coordinate c = map.getViewportModelInternal().pixelToWorld(event.x, event.y);
				lblCoordinates.setText(format(c.x) + ", " + format(c.y));
				//see CursorPosition Tool
			}
			
			private String format(double d){
				 DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
		         format.setMaximumFractionDigits(4);
		         format.setMinimumIntegerDigits(1);
		         format.setGroupingUsed(false);
		         String string = format.format(d);
//		         String[] parts = string.split("\\.");
//		         if(parts[0].length()>6){
//		         	string = parts[0];
//		         }
		         return string;
			}
			@Override
			public void mouseHovered(MapMouseEvent event) {
			}
			
			@Override
			public void mouseDragged(MapMouseEvent event) {
			}
		});        
    }

    public void setModalTool( String toolId ) {
    	   if (activeTool != null) {
               // ask the current tool to stop listening etc...
    		   activeTool.setActive(false);
    		   //activeTool.setEnabled(false);
               activeTool = null;
           }
    	   
    	   Tool tool = ApplicationGIS.getToolManager().findTool(toolId);
   		
           if( tool == null || !(tool instanceof ModalTool)){
               return;
           }
           activeTool = (ModalTool)tool;
           activeTool.setContext(getToolContext());
           activeTool.setActive(true);
           
           Cursor toolCursor = ApplicationGIS.getToolManager().findToolCursor(activeTool.getCursorID());
           if (toolCursor != null){
        	   activeTool.getContext().getViewportPane().setCursor(toolCursor);
           }
    }
    
   
    /**
     * @return tool context (used to teach tools about our MapViewer facilities.
     */
    protected synchronized ToolContext getToolContext(){
        if( toolcontext == null ){
            toolcontext = new ToolContextImpl();
            toolcontext.setMapInternal(map);        
            toolcontext.setRenderManagerInternal(map.getRenderManagerInternal());            
        }
        return toolcontext;
    }
  

    @Override
    public void setFocus() {
        mapviewer.getViewport().getControl().setFocus();
    }

    public Map getMap() {
        return mapviewer.getMap();
    }

    @Override
    public void dispose() {
    	super.dispose();
        if (mapviewer != null && mapviewer.getViewport() != null && getMap() != null) {
            mapviewer.getViewport().removePaneListener(getMap().getViewportModelInternal());
        }
       if (mapviewer != null){
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
		return getViewSite().getActionBars().getStatusLineManager();
	}
	
	public Object getAdapter(Class adaptee) {
		if (adaptee.isAssignableFrom(Map.class)) {
			return getMap();
		}
		return super.getAdapter(adaptee);
	}
}
