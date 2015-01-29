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

import java.text.DecimalFormat;
import java.text.NumberFormat;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.commands.ChangeCRSCommand;
import net.refractions.udig.project.internal.render.RenderPackage;
import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.render.displayAdapter.IMapDisplayListener;
import net.refractions.udig.project.render.displayAdapter.MapDisplayEvent;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseMotionListener;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseWheelEvent;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseWheelListener;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.viewers.MapViewer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.report.birt.map.internal.Messages;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.ProjectionDialog;
import org.wcs.smart.ui.map.ScaleRatioComposite;
import org.wcs.smart.ui.map.tool.PanTool;
import org.wcs.smart.ui.map.tool.ZoomExtentTool;
import org.wcs.smart.ui.map.tool.ZoomInTool;
import org.wcs.smart.ui.map.tool.ZoomOutTool;
import org.wcs.smart.ui.map.tool.ZoomTool;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Map dialog for picking map extents
 * @author Emily
 *
 */
public class MapDialog extends Dialog implements MapPart{

	private MapViewer viewer;
	private Label lblCoordinates;
	private byte[] basemapUuid = null;
	private ReferencedEnvelope  bounds = null;
	
	private Job refreshJob = new Job(Messages.MapDialog_ResizeJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			getMap().getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}
	};
	
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
		bounds = getMap().getViewportModel().getBounds();
		close();
	}
	
	@Override
	public boolean close(){
		boolean ok = super.close();
		if (ok && viewer != null){
			viewer.getRenderManager().stopRendering();
			viewer.getRenderManager().dispose();
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

		Map map = (Map) ProjectFactory.eINSTANCE.createMap();
        map.setName(Messages.MapDialog_MapName);
        viewer.setMap(map);
        
        
        //set default crs
        map.getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		map.getViewportModelInternal().setCRS(Area.AREA_CRS);
		
		LoadDefaultLayersJob layer = new LoadDefaultLayersJob(map, bounds == null, this.basemapUuid);
		// we need to do this because this map is in a dialog box and
		// events does work correctly
		layer.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (bounds == null) return;
				getMap().getViewportModelInternal().setBounds(bounds);
				getMap().getRenderManager().refresh(null);
			}
		});
		layer.schedule();
		
		
		ApplicationGIS.getToolManager().setCurrentEditor(this);
		String[] thisTools = new String[]{
				ZoomExtentTool.ID,
				PanTool.ID,
				ZoomTool.ID,
				ZoomInTool.ID,
				ZoomOutTool.ID};
		
		MapToolComposite tools = new MapToolComposite(thisTools);
		tools.createComposite(composite);
		createInfoPanel(composite);

		tools.selectTool(PanTool.ID);
		
		getShell().setText(Messages.MapDialog_DialogTitle);
		viewer.getViewport().addPaneListener(new IMapDisplayListener() {
			@Override
			public void sizeChanged(MapDisplayEvent event) {
				refreshJob.schedule();
			}
		});
		viewer.getViewport().addMouseWheelListener(new MapMouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MapMouseWheelEvent e) {
				refreshJob.cancel();
				refreshJob.schedule(600);
			}
		});

		return composite;
	}

	
	private void createInfoPanel(Composite parent) {
		Composite infoArea = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(5, false);
		gl.marginBottom = gl.marginTop = gl.verticalSpacing = gl.marginHeight = 0;
		infoArea.setLayout(gl);
		infoArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false,
				2, 1));
		lblCoordinates = new Label(infoArea, SWT.NONE);
		lblCoordinates.setText(Messages.MapDialog_CoordinatesLabel);
		lblCoordinates.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		lblCoordinates.setAlignment(SWT.RIGHT);

		Label lblSeparator = new Label(infoArea, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		lblSeparator.setLayoutData(gd);

		ScaleRatioComposite scale = new ScaleRatioComposite(infoArea, getMap(), true);
		scale.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		lblSeparator = new Label(infoArea, SWT.SEPARATOR | SWT.VERTICAL);
		gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		lblSeparator.setLayoutData(gd);

		final Button lblSRID = new Button(infoArea, SWT.NONE);
		lblSRID.setText( getMap().getViewportModel().getCRS().getName().getCode());
		lblSRID.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		lblSRID.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ProjectionDialog pd = new ProjectionDialog(getShell(), viewer.getMap().getViewportModel().getCRS());
				if (pd.open() == IDialogConstants.OK_ID) {
					try {
						ChangeCRSCommand command = new ChangeCRSCommand(pd
								.getSelection().getCrs());
						getMap().sendCommandASync(command);
					} catch (Exception ex) {
						SmartPlugIn.displayLog(
								getShell(),
								Messages.MapDialog_Error_SettingMapProjection
										+ ex.getMessage(), ex);
					}
				}
			}
		});
		
		 getMap().getViewportModelInternal().eAdapters().add(new AdapterImpl(){
        	public void notifyChanged(Notification notification) {
        		if (notification.getEventType() == Notification.SET &&
        				notification.getFeatureID( getMap().getViewportModelInternal().getClass()) == RenderPackage.VIEWPORT_MODEL__CRS){
        			getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							lblSRID.setText( getMap().getViewportModel()
									.getCRS().getName().getCode());
							lblSRID.getParent().layout();
						}
					});
        		}
        	}
        	
        });

		viewer.getViewport().addMouseMotionListener(
				new MapMouseMotionListener() {

					@Override
					public void mouseMoved(MapMouseEvent event) {
						event.getPoint();
						Coordinate c =  getMap().getViewportModelInternal().pixelToWorld(event.x, event.y);
						lblCoordinates
								.setText(format(c.x) + ", " + format(c.y)); //$NON-NLS-1$
						// see CursorPosition Tool
					}

					private String format(double d) {
						DecimalFormat format = (DecimalFormat) NumberFormat
								.getNumberInstance();
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
		createButton(parent, IDialogConstants.OK_ID, Messages.MapDialog_SetBounds_Button,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
  
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(Class adaptee) {
		if (adaptee.isAssignableFrom(Map.class)) {
			return viewer.getMap();
		}
		return null;
	}

	@Override
	public Map getMap() {
		return viewer.getMap();
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
