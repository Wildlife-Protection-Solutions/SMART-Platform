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
import net.refractions.udig.project.ui.internal.tool.ToolContext;
import net.refractions.udig.project.ui.internal.tool.impl.ToolContextImpl;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseMotionListener;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.tool.ModalTool;
import net.refractions.udig.project.ui.tool.Tool;
import net.refractions.udig.project.ui.viewers.MapViewer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.birt.map.tools.ZoomTool;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.ProjectionDialog;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Map dialog for picking map extents
 * @author Emily
 *
 */
public class MapDialog extends Dialog implements MapPart{

	private MapViewer viewer;
	private Map map;
	
	private Label lblCoordinates;
	
	private ToolContext toolcontext;
	private ModalTool activeTool;
	private byte[] basemapUuid = null;
	 
	
	private ReferencedEnvelope  bounds = null;
	
	protected MapDialog(Shell parentShell, byte[] basemapUuid, ReferencedEnvelope mapBounds) {
		super(parentShell);
		this.basemapUuid = basemapUuid;
		this.bounds = mapBounds;
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		if (p.x < 400){
			p.x = 400;
		}
		if (p.y < 400){
			p.y = 400;
		}
		return p;
	}
	
	public ReferencedEnvelope  getBounds(){
		return this.bounds;
	}
	
	@Override
	public void okPressed(){
		bounds = map.getViewportModel().getBounds();
		close();
	}
	@Override
	public boolean close(){
		boolean ok = super.close();
		if (ok && viewer != null){
			viewer.dispose();
			viewer = null;
		}
		return ok;
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		composite.setLayout(gd);
		
		
		viewer = new MapViewer(composite, SWT.SINGLE | SWT.DOUBLE_BUFFERED);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	    map = (Map) ProjectFactory.eINSTANCE.createMap();
        map.setName("Smart Map");
        viewer.setMap(map);
		
        //set default crs
        map.getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		map.getViewportModelInternal().setCRS(Area.AREA_CRS);
		
		String[] thisTools = new String[]{
				"org.wcs.smart.birt.map.tools.ZoomExtents", 
				"org.wcs.smart.birt.map.tools.Pan",
				ZoomTool.ID};
		
		MapToolComposite tools = new MapToolComposite(thisTools);
		tools.createComposite(composite);
		
		createInfoPanel(composite);

		ApplicationGIS.getToolManager().setCurrentEditor(this);
		
		
		LoadDefaultLayersJob layer = new LoadDefaultLayersJob(map, bounds == null, this.basemapUuid);
		layer.schedule();
		
		if (bounds != null){
			map.getViewportModelInternal().setBounds(bounds);
		}else{
			//we need to do this because this map is in a dialog box and
			//events does work correctly 
			layer.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					map.getRenderManager().refresh(null);
				}
			});
		}
		
		getShell().setText("Set Map Bounds");
		super.getShell().addListener(SWT.Resize, new Listener(){
			Job j = new Job("resize"){
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					map.getRenderManager().refresh(null);
					return Status.OK_STATUS;
				}
			};
			
			@Override
			public void handleEvent(Event event) {
				j.schedule(500);
			}});

		tools.selectTool("org.wcs.smart.birt.map.tools.Pan");
		return composite;
	}

	
	private void createInfoPanel(Composite parent){
		 Composite infoArea = new Composite(parent, SWT.NONE);
		 	GridLayout gl = new GridLayout(3, false);
		 	gl.marginBottom = gl.marginTop = gl.verticalSpacing = gl.marginHeight = 0;
	        infoArea.setLayout(gl);
	        infoArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
	        lblCoordinates = new Label(infoArea, SWT.NONE);
	        lblCoordinates.setText("Coordinates");
	        lblCoordinates.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	        lblCoordinates.setAlignment(SWT.RIGHT);
	        
	        final Label lblSeparator = new Label(infoArea, SWT.SEPARATOR | SWT.VERTICAL);
	        GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
	        gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
	        lblSeparator.setLayoutData(gd);
	        
	        final Button lblSRID = new Button(infoArea, SWT.NONE);
	        lblSRID.setText(map.getViewportModel().getCRS().getName().getCode());
	        lblSRID.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
	        lblSRID.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					ProjectionDialog pd = new ProjectionDialog(getShell());
					if (pd.open() == IDialogConstants.OK_ID){
						try{
							ChangeCRSCommand command = new ChangeCRSCommand(pd.getSelection().getCrs());
							map.sendCommandASync(command);
						}catch (Exception ex){
							SmartPlugIn.displayLog(getShell(), "Error setting map projection.\n\n" + ex.getMessage(), ex);
						}
					}
					
					
				}
			});
	        map.getViewportModel().addViewportModelListener(new IViewportModelListener() {
				
				@Override
				public void changed(ViewportModelEvent event) {
					if(event.getType() == ViewportModelEvent.EventType.CRS){
						getShell().getDisplay().asyncExec(new Runnable(){
							@Override
							public void run() {
								lblSRID.setText(map.getViewportModel().getCRS().getName().getCode());
								lblSRID.getParent().layout();
							}});
					}					
				}
			});
	      
	        
	        viewer.getViewport().addMouseMotionListener(new MapMouseMotionListener() {
				
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
	
	
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "Set Bounds",
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
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
  
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(Class adaptee) {
		if (adaptee.isAssignableFrom(Map.class)) {
			return map;
		}
		return null;
	}

	@Override
	public Map getMap() {
		return map;
	}


    public void openContextMenu() {
        viewer.openContextMenu();
    }

    public void setFont( Control control ) {
        viewer.setFont(control);
    }

    public void setSelectionProvider( IMapEditorSelectionProvider selectionProvider ) {
        viewer.setSelectionProvider(selectionProvider);
    }

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}

}
