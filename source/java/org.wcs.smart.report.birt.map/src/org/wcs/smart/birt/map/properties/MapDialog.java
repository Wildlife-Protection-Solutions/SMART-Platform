package org.wcs.smart.birt.map.properties;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;

import com.vividsolutions.jts.geom.Coordinate;

public class MapDialog extends Dialog implements IAdaptable, MapPart, IWorkbenchPart{

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
		if (p.x < 300){
			p.x = 300;
		}
		if (p.y < 300){
			p.y = 300;
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
				"org.wcs.smart.birt.map.tools.Zoom"};
		
		MapToolComposite tools = new MapToolComposite(thisTools);
		tools.createComposite(composite);
		
		createInfoPanel(composite);

		ApplicationGIS.getToolManager().setCurrentEditor(this);
		
		
		LoadDefaultLayersJob layer = new LoadDefaultLayersJob(map, false, this.basemapUuid);
		layer.schedule();

		if (bounds != null){
			map.getViewportModelInternal().setBounds(bounds);
		}
		map.getRenderManager().refresh(null);
		
		
		return composite;
	}

	
	private void createInfoPanel(Composite parent){
		 Composite infoArea = new Composite(parent, SWT.BORDER);
	        infoArea.setLayout(new GridLayout(3, false));
	        infoArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
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

	@Override
	public void addPropertyListener(IPropertyListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createPartControl(Composite parent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IWorkbenchPartSite getSite() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Image getTitleImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTitleToolTip() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removePropertyListener(IPropertyListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}

}
