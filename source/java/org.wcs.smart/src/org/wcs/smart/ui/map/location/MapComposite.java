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
package org.wcs.smart.ui.map.location;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.command.Command;
import org.locationtech.udig.project.command.MapCommand;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.command.navigation.ZoomExtentCommand;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.render.displayAdapter.IMapDisplayListener;
import org.locationtech.udig.project.render.displayAdapter.MapDisplayEvent;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.internal.tool.display.ToolManager;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseMotionListener;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseWheelEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseWheelListener;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.udig.SetBasemapTool;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapInfoAreaComposite;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.location.tool.SelectionTool;
import org.wcs.smart.ui.map.tool.PanTool;
import org.wcs.smart.ui.map.tool.ZoomExtentTool;
import org.wcs.smart.ui.map.tool.ZoomInTool;
import org.wcs.smart.ui.map.tool.ZoomOutTool;
import org.wcs.smart.ui.map.tool.ZoomTool;
import org.wcs.smart.util.GeometryUtils;

/**
 * Map Composite for display a map with some tools on a dialog.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class MapComposite extends Composite implements MapPart {

	private MapViewer mapViewer;
	private Label lblCoordinates;
	
	private String[] mapTools =  new String[] {
			SetBasemapTool.ID,
			ZoomExtentTool.ID,
			PanTool.ID,
			ZoomTool.ID,
			ZoomInTool.ID,
			ZoomOutTool.ID,
			SelectionTool.ID };
	
	private Job refreshJob = new Job(Messages.MapComposite_MapResizeJob_Title){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (!isDisposed() && mapViewer != null){
				mapViewer.getMap().getRenderManager().refresh(null);
			}
			return Status.OK_STATUS;
		}
	};
	
	private IMapDisplayListener displayListener =new IMapDisplayListener() {
		@Override
		public void sizeChanged(MapDisplayEvent event) {
			refreshJob.cancel();
			refreshJob.schedule(600);
		}
	};
	
	private MapMouseWheelListener wheelListener = new MapMouseWheelListener() {
		
		@Override
		public void mouseWheelMoved(MapMouseWheelEvent e) {
			refreshJob.cancel();
			refreshJob.schedule(600);
		}
	};
	
	private MapMouseMotionListener coordinateProvider = new MapMouseMotionListener() {
		@Override
		public void mouseMoved(MapMouseEvent event) {
			event.getPoint();
			Coordinate c = getMap().getViewportModelInternal().pixelToWorld(event.x, event.y);
			lblCoordinates.setText(format(c.x) + ":" + format(c.y)); //$NON-NLS-1$
		}

		private String format(double d) {
			DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
			format.setMaximumFractionDigits(4);
			format.setMinimumIntegerDigits(1);
			format.setGroupingUsed(false);
			String string = format.format(d);
			return string;
		}

		@Override
		public void mouseDragged(MapMouseEvent event) {
		}

		@Override
		public void mouseHovered(MapMouseEvent event) {
		}
	};
	
	/**
	 * @param parent
	 * @param style
	 */
	public MapComposite(Composite parent, int style) {
		this(parent, style, null);
	}
	
	/**
	 * @param parent
	 * @param style
	 */
	public MapComposite(Composite parent, int style, String[] mapTools) {
		super(parent, style);
		if (mapTools != null) this.mapTools = mapTools;
		createControls();
		
	}
	
	@Override
	public void dispose(){
		super.dispose();
		
		mapViewer.getViewport().addMouseMotionListener(coordinateProvider);
		mapViewer.getMap().getViewportModelInternal().setInitialized(false);
		
		mapViewer.getViewport().removePaneListener(displayListener);
		mapViewer.getViewport().removeMouseWheelListener(wheelListener);
		
		mapViewer.getRenderManager().stopRendering();
		mapViewer.getRenderManager().dispose();
		mapViewer.dispose();
		mapViewer = null;
	}
	
	protected void createControls() {

		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		this.setLayout(gd);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		mapViewer = new MapViewer(this,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
		mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Map map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName(Messages.MapComposite_Map_Name);
		mapViewer.setMap(map);
		//set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(GeometryUtils.SMART_CRS);

		mapViewer.getViewport().addPaneListener(displayListener);
		mapViewer.getViewport().addMouseWheelListener(wheelListener);
		
		MapPart current = ((ToolManager) ApplicationGIS.getToolManager()).getCurrentEditor();
		getShell().addListener(SWT.Dispose, e->ApplicationGIS.getToolManager().setCurrentEditor(current));
		
		ApplicationGIS.getToolManager().setCurrentEditor(this);
		
		MapToolComposite tools = new MapToolComposite(this.mapTools);
		tools.createComposite(this);
		new MapInfoAreaComposite(this, SWT.NONE, mapViewer) ;
		
		lblCoordinates = new Label(this, SWT.NONE);
		lblCoordinates.setText("0, 0"); //$NON-NLS-1$
		lblCoordinates.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		lblCoordinates.setAlignment(SWT.RIGHT);
		
		getMapViewer().getViewport().addMouseMotionListener(coordinateProvider);

		addDefaultLayers();
		
		tools.selectTool(PanTool.ID);
	}
	
	protected void addDefaultLayers() {
		final LoadDefaultLayersJob defaultLayer = new LoadDefaultLayersJob(mapViewer.getMap());
		// we need to do this because this map is in a dialog box and
		// events does work correctly
		defaultLayer.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (isDisposed() || mapViewer == null)
					return;

				basemapLoaded();
				
				mapViewer.getMap().sendCommandSync(new ZoomExtentCommand());
				mapViewer.getMap().getRenderManager().refresh(null);
			}
		});
		defaultLayer.schedule();
		
		
		// if I am disposed before finished cancel job
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				defaultLayer.cancel();
			}
		});
	}
	
	/**
	 * called after the basemap is loaded, but before the map is refreshed
	 */
	protected void basemapLoaded(){
		
	}
	
	
	@Override
	public Map getMap() {
		if (mapViewer == null){
			return null;
		}
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
		if (mapViewer == null) return;
		mapViewer.setSelectionProvider(selectionProvider);

	}

	public MapViewer getMapViewer() {
		return this.mapViewer;
	}
	
	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}

}
