/*
 * Copyright (C) 2020 Wildlife Conservation Society
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

package org.wcs.smart.query.ui.model.impl;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.command.navigation.ZoomExtentCommand;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.render.displayAdapter.IMapDisplayListener;
import org.locationtech.udig.project.render.displayAdapter.MapDisplayEvent;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseWheelEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseWheelListener;
import org.locationtech.udig.project.ui.render.glass.GlassPane;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.udig.SetBasemapTool;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapInfoAreaComposite;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.tool.DrawPolygonTool;
import org.wcs.smart.ui.map.tool.PanTool;
import org.wcs.smart.ui.map.tool.ZoomExtentTool;
import org.wcs.smart.ui.map.tool.ZoomInTool;
import org.wcs.smart.ui.map.tool.ZoomOutTool;
import org.wcs.smart.ui.map.tool.ZoomTool;
import org.wcs.smart.util.GeometryUtils;

/**
 * Dialog for drawing an area on a map
 * 
 * @author Emily
 *
 */
public class DrawAreaMapDialog extends SmartStyledDialog implements MapPart{

	private MapViewer mapViewer;
	private Polygon polygon;
	
	private Job refreshJob = new Job(Messages.DrawAreaMapDialog_refreshJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (!getShell().isDisposed() && mapViewer != null){
				mapViewer.getMap().getRenderManager().refresh(null);
			}
			return Status.OK_STATUS;
		}
	};
	
	protected DrawAreaMapDialog(Shell parentShell) {
		this(parentShell, null);
	}
	
	protected DrawAreaMapDialog(Shell parentShell, Polygon polygon) {
		super(parentShell);
		this.polygon = polygon;
	}
	
	@Override
	protected void okPressed(){
		super.okPressed();
	}
	
	/**
	 * Gets the polygon added to the
	 * map.
	 *  
	 */
	public Polygon getPolygon() {
		return this.polygon;
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(450, 400);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite outer = new Composite(parent, SWT.NONE);
				
		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		outer.setLayout(gd);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		mapViewer = new MapViewer(outer,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
		mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Map map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName(Messages.DrawAreaMapDialog_MapName);
		mapViewer.setMap(map);
		//set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(GeometryUtils.SMART_CRS);

		ApplicationGIS.getToolManager().setCurrentEditor(this);
		String[] thisTools = new String[] {
				SetBasemapTool.ID,
				ZoomExtentTool.ID,
				PanTool.ID,
				ZoomTool.ID,
				ZoomInTool.ID,
				ZoomOutTool.ID,
				DrawPolygonTool.ID };

		MapToolComposite tools = new MapToolComposite(thisTools);
		tools.createComposite(outer);
		
		new MapInfoAreaComposite(outer, SWT.NONE, mapViewer) ;

		tools.selectTool(PanTool.ID);

		final LoadDefaultLayersJob defaultLayer = new LoadDefaultLayersJob(mapViewer.getMap());
		// we need to do this because this map is in a dialog box and
		// events does work correctly
		defaultLayer.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (outer.isDisposed() || mapViewer == null) return;
				
				mapViewer.getMap().sendCommandSync(new ZoomExtentCommand());
				mapViewer.getMap().getRenderManager().refresh(null);
			}
		});
		defaultLayer.schedule();
		
		//if I am disposed before finished cancel job
		outer.addListener(SWT.Dispose, e->defaultLayer.cancel());
	
		mapViewer.getViewport().addPaneListener(new IMapDisplayListener() {
			@Override
			public void sizeChanged(MapDisplayEvent event) {
				refreshJob.cancel();
				refreshJob.schedule(600);
			}
		});
		mapViewer.getViewport().addMouseWheelListener(new MapMouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MapMouseWheelEvent e) {
				refreshJob.cancel();
				refreshJob.schedule(600);
			}
		});


		DrawPolygonTool.INewPolygonEvent event = polygon->{
			DrawAreaMapDialog.this.polygon = polygon;
			mapViewer.getMap().getRenderManager().refresh(null);
			
		};
		mapViewer.getMap().getBlackboard().put(DrawPolygonTool.INewPolygonEvent.class.getName(), event);
		
		mapViewer.getViewport().setGlass(new GlassPane(mapViewer.getViewport()) {

			@Override
			public void draw(GC graphics) {
				if (polygon == null) return;
				
				 // initialize the graphics handle
				int w = graphics.getLineWidth();
		        
				graphics.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
		        graphics.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
		        
		        // figure out our CRS
		        CoordinateReferenceSystem ourCRS = SmartDB.DATABASE_CRS;
		        
		        // figure out how to map our coordinate to the world
		        CoordinateReferenceSystem worldCRS = getSite().getMap().getViewportModel().getCRS();
		        MathTransform dataToWorld;
		        try {
		            dataToWorld = CRS.findMathTransform(ourCRS, worldCRS, false);
		        } catch (FactoryException e1) {
		            throw (RuntimeException) new RuntimeException( ).initCause( e1 );
		        }

		        Polygon todraw = null;
		        try {
		            todraw = (Polygon) JTS.transform(polygon, dataToWorld);
		        } catch (TransformException e) {
		            throw (RuntimeException) new RuntimeException( ).initCause( e );
		        }                
		              
		        Coordinate[] cs = todraw.getCoordinates();
		        int[] pnts = new int[cs.length*2];
		        for (int i = 0; i < cs.length; i ++) {
		        	pnts[i*2] = getSite().worldToPixel(cs[i]).x;
		        	pnts[i*2 + 1] = getSite().worldToPixel(cs[i]).y;
		        }
		        graphics.setAlpha(50);
		        graphics.fillPolygon(pnts);
		        graphics.setAlpha(255);
		        graphics.setLineWidth(2);
		        graphics.drawPolygon(pnts);		        
		        
		        graphics.setLineWidth(w);
			}
			
		});
		
		getShell().setText(Messages.DrawAreaMapDialog_DialogTitle);
		return parent;
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
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

}
