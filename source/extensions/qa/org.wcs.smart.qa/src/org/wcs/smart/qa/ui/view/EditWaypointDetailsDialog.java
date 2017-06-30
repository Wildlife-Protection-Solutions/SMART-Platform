/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.view;

import java.text.DateFormat;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.render.IViewportModel;
import org.locationtech.udig.project.render.IViewportModelListener;
import org.locationtech.udig.project.render.ViewportModelEvent;
import org.locationtech.udig.project.render.ViewportModelEvent.EventType;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.udig.EditPointTool;
import org.wcs.smart.udig.IMapEditManager;
import org.wcs.smart.udig.SetBasemapTool;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapInfoAreaComposite;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.tool.PanTool;
import org.wcs.smart.ui.map.tool.ZoomExtentTool;
import org.wcs.smart.ui.map.tool.ZoomInTool;
import org.wcs.smart.ui.map.tool.ZoomOutTool;
import org.wcs.smart.ui.map.tool.ZoomTool;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Dialog for editing waypoint details.
 * 
 * @author Emily
 *
 */
public abstract class EditWaypointDetailsDialog extends TitleAreaDialog implements MapPart{

	protected UUID waypointUuid;
	
	private Text txtX;
	private Text txtY;
	
	protected Waypoint waypoint = null;
	
	private Double originalX = null;
	private Double originalY = null;
	
	protected Map map;
	protected MapViewer mapViewer;
	
	private boolean isUpdating = false;
	private boolean fireXYChange = true;
	
	/**
	 * Creates a new dialog for editing waypoint locations
	 * @param parentShell
	 * @param wpUuid
	 */
	public EditWaypointDetailsDialog(Shell parentShell, UUID wpUuid) {
		super(parentShell);
		this.waypointUuid = wpUuid;
	}

	/**
	 * Add additional background layers to the map.
	 */
	protected abstract void initBackgroundLayers();
	
	/**
	 * Update the position feature on the map
	 * 
	 * @param newPosition
	 */
	protected abstract void updateFeature(Coordinate newPosition);
	
	/**
	 * Add additional tools to the map toolbar
	 * @param toolbar
	 */
	protected void addToolbarContributions(ToolBar toolbar){
	}
	
	@Override
	public org.eclipse.swt.graphics.Point getInitialSize(){
		return new org.eclipse.swt.graphics.Point(600,600);
	}
	
	/*
	 * Updates the waypoint location to the new position.  The new position
	 * must be in lat/lon.  Updates the local waypoint object and map feature, but 
	 * does not update the text box values - call updateLabels()
	 * to update the text box values.
	 */
	protected void updateWaypointLocation(Coordinate newPosition){
		if (isUpdating) return;
		isUpdating = true;
		try{
			setErrorMessage(null);
			waypoint.setX(newPosition.x);
			waypoint.setY(newPosition.y);
			updateFeature(newPosition);
			validate();
		}finally{
			isUpdating = false;
		}
		
	}

	/**
	 * Updates the x & y labels with the current position value supplied in the
	 * waypoint object.
	 */
	protected void updateLabels(){
		if (waypoint == null) return;
		Coordinate display = new Coordinate(waypoint.getX(), waypoint.getY());
		try {
			display = ReprojectUtils.reproject(display.x, display.y, SmartDB.DATABASE_CRS, getMap().getViewportModel().getCRS());
		} catch (Exception e) {
			QaPlugIn.log(e.getMessage(), e);
		}
		fireXYChange = false;
		try{
			if (!txtX.getText().equals(String.valueOf(display.x))) txtX.setText(String.valueOf(display.x));
			if (!txtY.getText().equals(String.valueOf(display.y))) txtY.setText(String.valueOf(display.y));
		}finally{
			fireXYChange = true;
		}
	}
	
	/**
	 * Reverts the waypoint position back to the original value 
	 * 
	 */
	protected void revert(){
		updateWaypointLocation(new Coordinate(originalX, originalY));
		updateLabels();
	}
	
	/**
	 * Loads the waypoint from the database and initializes 
	 * ui controls
	 */
	private void initControls(){
		Session s = HibernateManager.openSession();
		try{
			waypoint = (Waypoint) s.get(Waypoint.class, waypointUuid);
			if (waypoint != null){
				originalX = waypoint.getX();
				originalY = waypoint.getY();
			}else{
				setErrorMessage("Waypoint not found.  Close dialog and rerun validation routines.");
				return;
			}
		}finally{
			s.close();
		}
		updateLabels();
		setTitle("Waypoint: " + waypoint.getId() + " - " + DateFormat.getDateTimeInstance().format(waypoint.getDateTime()));
		initBackgroundLayers();
	}
	
	/**
	 * 
	 * @return the updated waypoint
	 */
	public Waypoint getUpdatedPoint(){
		return this.waypoint;
	}
	
	@Override
	protected void okPressed(){
		validate();
		if (!getButton(IDialogConstants.OK_ID).isEnabled()) return;	//data error

		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			Waypoint wp = (Waypoint) s.get(Waypoint.class, waypointUuid);
			wp.setX(waypoint.getX());
			wp.setY(waypoint.getY());
			waypoint.setX(wp.getX());
			waypoint.setY(wp.getY());
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			QaPlugIn.displayLog("Error saving changes to QA Routine: " + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		super.okPressed();
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		Composite coordinateCmp = new Composite(composite, SWT.NONE);
		coordinateCmp.setLayout(new GridLayout(5, false));
		coordinateCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(coordinateCmp, SWT.NONE);
		lbl.setText("X Coordinate:");
		
		ModifyListener validation = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!fireXYChange) return;
				validate();
				updatePosition.schedule(200);
			}
		};
		
		txtX = new Text(coordinateCmp, SWT.BORDER);
		txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtX.addModifyListener(validation);
		txtX.addListener(SWT.FocusIn, e-> txtX.selectAll());
		
		lbl = new Label(coordinateCmp, SWT.NONE);
		lbl.setText("Y Coordinate:");
		
		txtY = new Text(coordinateCmp, SWT.BORDER);
		txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtY.addModifyListener(validation);
		txtY.addListener(SWT.FocusIn, e-> txtY.selectAll());
		
		Composite mapComposite = new Composite(composite, SWT.NONE);
		mapComposite.setLayout(new GridLayout(2, false));
		mapComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		mapViewer = new MapViewer(mapComposite,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
		mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName("Edit Waypoint Map");
		mapViewer.setMap(map);
		//set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(GeometryUtils.SMART_CRS);
		mapViewer.getMap().getViewportModel().addViewportModelListener(new IViewportModelListener() {
			@Override
			public void changed(ViewportModelEvent event) {
				if (event.getType() == EventType.CRS){
					Display.getDefault().syncExec(()->updateLabels());
				}
			}
		});
		ApplicationGIS.getToolManager().setCurrentEditor(this);
		String[] thisTools = new String[] {
				SetBasemapTool.ID,
				ZoomExtentTool.ID,
				PanTool.ID,
				ZoomTool.ID,
				ZoomInTool.ID,
				ZoomOutTool.ID,
				MapToolComposite.SEPERATOR_TOOL_ID,
				EditPointTool.ID};

		MapToolComposite tools = new MapToolComposite(thisTools);
		tools.createComposite(mapComposite);
		new MapInfoAreaComposite(mapComposite, SWT.NONE, mapViewer) ;

		tools.selectTool(EditPointTool.ID);
		getMap().getBlackboard().put(IMapEditManager.BLACKBOARD_KEY, getEditManager());
		
		final LoadDefaultLayersJob defaultLayer = new LoadDefaultLayersJob(mapViewer.getMap(), false);
		defaultLayer.schedule();
		mapComposite.addListener(SWT.Dispose, e-> defaultLayer.cancel());
		
		//add toolbar contributions
		addToolbarContributions(tools.getToolbar());
		
		//add revert option
		ToolItem btnReset = new ToolItem(tools.getToolbar(), SWT.PUSH);
		btnReset.setToolTipText("revert back to original value");
		btnReset.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.UNDO_ICON));
		btnReset.addListener(SWT.Selection, e->revert());
		
		
		getShell().setText("Edit Waypoint");
		setMessage("Edit waypoint details");
		
		initControls();
		
		updatePosition.setSystem(true);
		return composite;
	}
	
	/**
	 * This is a job and updates the position based on the values
	 * in the text box.  
	 */
	Job updatePosition = new Job("update position"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(()->{
				try{
					Double x = Double.valueOf(txtX.getText());
					Double y = Double.valueOf(txtY.getText());
					Coordinate newC = new Coordinate(x,y);
					newC = ReprojectUtils.reproject(x, y, getMap().getViewportModelInternal().getCRS(), SmartDB.DATABASE_CRS);
					updateWaypointLocation(newC);
				}catch (Exception ex){
					setErrorMessage("Could not parse new value from x y positions.");
				}
			});			
			return Status.OK_STATUS;
		}
		
	};
	
	private void validate(){
		boolean ok = true; 
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null)btn.setEnabled(ok);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

	@Override
	public Map getMap() {
		return map;
	}

	@Override
	public void openContextMenu() {
	}

	@Override
	public void setFont(Control textArea) {
	}

	@Override
	public void setSelectionProvider(IMapEditorSelectionProvider selectionProvider) {	
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}
	
	/*
	 * edit manager for moving waypoint on map
	 */
	 private IMapEditManager getEditManager(){
	    	return new IMapEditManager() {
				
				@Override
				public synchronized void moveFeature(Object feature, int x, int y, IViewportModel vm) {
					if (!(feature instanceof Waypoint)) return ;
					Coordinate crspx = vm.pixelToWorld(x, y);
					//convert to lat/long
					if (!CRS.equalsIgnoreMetadata(vm.getCRS(), SmartDB.DATABASE_CRS)){
						try{
							crspx = ReprojectUtils.reproject(crspx.x, crspx.y, vm.getCRS(), SmartDB.DATABASE_CRS);
						}catch (Exception ex){
							QaPlugIn.displayLog("Error reprojecting new location. Location not updated", ex);
							return;
						}
					}
					updateWaypointLocation(crspx);
					updateLabels();
				}
				
				@Override
				public EditPoint findFeature(int x, int y, IViewportModel vm) {
					try{
						Coordinate crspx = vm.pixelToWorld(x, y);
						//convert to lat/long
						if (!CRS.equalsIgnoreMetadata(vm.getCRS(), SmartDB.DATABASE_CRS)){
							crspx = ReprojectUtils.reproject(crspx.x, crspx.y, vm.getCRS(), SmartDB.DATABASE_CRS);
						}

						Coordinate pnt = ReprojectUtils.reproject(waypoint.getX(), waypoint.getY(), SmartDB.DATABASE_CRS, vm.getCRS());
						java.awt.Point exitPnt = vm.worldToPixel(pnt);
						if (Math.abs(exitPnt.getX() - x) > 5 || Math.abs(exitPnt.getY() - y) > 5) return null;

						return new EditPoint(exitPnt, waypoint, null);
						
					}catch (Exception ex){
						QaPlugIn.log(ex.getMessage(), ex);
						return null;
					}
				}
				
				@Override
				public synchronized void undo() {
					//does not support undo
				}
				
				@Override
				public boolean canUndo() {
					return false;
				}
			};
	    }
}
