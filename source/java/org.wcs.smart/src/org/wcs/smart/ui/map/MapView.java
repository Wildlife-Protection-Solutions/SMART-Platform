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
import net.refractions.udig.project.internal.commands.ChangeCRSCommand;
import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.internal.tool.display.ToolManager;
import net.refractions.udig.project.ui.internal.tool.display.ToolProxy;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseMotionListener;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.tool.IToolManager;
import net.refractions.udig.project.ui.viewers.MapViewer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.map.internal.ZoomHandler;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A map view.
 * 
 * @author Emily Gouge, Graham Davis (Refractions Research, Inc.)
 * @since 1.1.0
 * @version 1.3.0
 */
public class MapView extends ViewPart implements MapPart, IAdaptable {

	public static final String ID = "org.wcs.smart.ui.map.MapView"; //$NON-NLS-1$
	private static final String MAP_NAME = Messages.MapView_MapName;
	
    private MapViewer mapviewer;
    private Map map;

    IPartListener2 partlistener = new IPartListener2(){
        public void partActivated( IWorkbenchPartReference partRef ) {
        	if (partRef.getPart(false) == MapView.this){
                IToolManager tools = ApplicationGIS.getToolManager();
                tools.setCurrentEditor(MapView.this );
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
        }

        public void partVisible( IWorkbenchPartReference partRef ) {
        }

        public void partInputChanged( IWorkbenchPartReference partRef ) {
        }

    };
    
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
		
        Composite infoArea = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(5, false);
        gl.marginTop = gl.marginBottom = gl.marginHeight= 0;
        infoArea.setLayout(gl);
        infoArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
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
        
        final Button lblSRID = new Button(infoArea, SWT.NONE);
        lblSRID.setText(map.getViewportModel().getCRS().getName().getCode());
        lblSRID.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
        lblSRID.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ProjectionDialog pd = new ProjectionDialog(getSite().getShell());
				if (pd.open() == IDialogConstants.OK_ID){
					try{
						ChangeCRSCommand command = new ChangeCRSCommand(pd.getSelection().getCrs());
						map.sendCommandASync(command);
					}catch (Exception ex){
						SmartPlugIn.displayLog(getSite().getShell(), SmartMapEditorPart.ERROR_SETTING_MAP_PROJECTION + ex.getMessage(), ex);
					}	
				}
			}
		});
        
        map.getViewportModel().addViewportModelListener(new IViewportModelListener() {
			@Override
			public void changed(ViewportModelEvent event) {
				if(event.getType() == ViewportModelEvent.EventType.CRS){
					getSite().getShell().getDisplay().asyncExec(new Runnable(){
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
				lblCoordinates.setText(format(c.x) + SmartMapEditorPart.COORDINATE_XYSEPARATOR + format(c.y));
				//see CursorPosition Tool
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
        getSite().getWorkbenchWindow().getPartService().addPartListener(partlistener);
        setTool(ZoomHandler.ZoomToolId);
    }

    public void setTool( String toolId ) {
    	ToolProxy mi =((ToolManager)ApplicationGIS.getToolManager()).findToolProxy(toolId);
    	if (mi != null){
    		ApplicationGIS.getToolManager().getToolAction(mi.getId(), mi.getCategoryId()).run();
    	}
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
		
		getSite().getWorkbenchWindow().getPartService().removePartListener(partlistener);
		
		if (mapviewer != null && mapviewer.getViewport() != null
				&& getMap() != null) {
			mapviewer.getViewport().removePaneListener(
					getMap().getViewportModelInternal());
		}
		if (mapviewer != null) {
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(Class adaptee) {
		if (adaptee.isAssignableFrom(Map.class)) {
			return getMap();
		}
		return super.getAdapter(adaptee);
	}
}
