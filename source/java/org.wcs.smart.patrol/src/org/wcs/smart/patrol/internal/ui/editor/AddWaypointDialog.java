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
package org.wcs.smart.patrol.internal.ui.editor;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.ui.ProjectionLabelProvider;
import org.wcs.smart.ui.SelectPointOnMapDialog;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Dialog for adding a new waypoint.
 * @author Emily
 * @since 1.0.0
 */
public class AddWaypointDialog extends SmartStyledTitleDialog{

	private static final GeometryFactory gf = GeometryFactoryProvider.getFactory();
	
	private Text txtWaypointId;
	private Text txtX;
	private Text txtY;
	private ComboViewer lstProjections;
	private double y;
	private double x;
	private String waypointId;
	private Projection[] projections;
	private Projection currentProjection;
	
	private PatrolWaypoint newWaypoint;
	
	public AddWaypointDialog(Shell parentShell, Projection[] projections) {
		super(parentShell);
		x = 0;
		y = 0;
		waypointId = "0"; //$NON-NLS-1$
		this.projections = projections;
	}
	
	public AddWaypointDialog(Shell parentShell, double y, double x, String waypointId, Projection[] projections) {
		this(parentShell, projections);
		
		this.x = x;
		this.y = y;
		this.waypointId = waypointId;
	}
	
	public PatrolWaypoint getWaypoint(){
		return newWaypoint;
	}
	
	@Override
	protected void okPressed() {
		newWaypoint = new PatrolWaypoint();
		Waypoint wp = new Waypoint();
		wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
		wp.setConservationArea(SmartDB.getCurrentConservationArea());
		newWaypoint.setWaypoint(wp);
		
		newWaypoint.getWaypoint().setId(txtWaypointId.getText());
		newWaypoint.getWaypoint().setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
		newWaypoint.getWaypoint().setConservationArea(SmartDB.getCurrentConservationArea());
		try{
			//reproject
			CoordinateReferenceSystem sourceCrs = ReprojectUtils.stringToCrs( ((Projection)((IStructuredSelection)lstProjections.getSelection()).getFirstElement()).getDefinition() );
			Point point = gf.createPoint(new Coordinate(Double.parseDouble(txtX.getText()),Double.parseDouble(txtY.getText())));
			Point p = (Point) JTS.transform(point, CRS.findMathTransform(sourceCrs, SmartDB.DATABASE_CRS));

			newWaypoint.getWaypoint().setRawX(p.getX());
			newWaypoint.getWaypoint().setRawY(p.getY());
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.AddWaypointDialog_Error_SavingWaypoint + ex.getLocalizedMessage(), ex);
			return;
		}
		super.okPressed();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
				
		Composite waypointComp = new Composite(parent, SWT.NONE);
		waypointComp.setLayout(new GridLayout(2, false));
		waypointComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Label lbl = new Label(waypointComp, SWT.NONE);
		lbl.setText(Messages.AddWaypointDialog_Projection_Label);

		
		lstProjections = new ComboViewer(waypointComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 100;
		lstProjections.getControl().setLayoutData(gd);
		lstProjections.setLabelProvider(ProjectionLabelProvider.getInstance());
		lstProjections.setContentProvider(ArrayContentProvider.getInstance());
		lstProjections.setInput(projections);
		currentProjection = HibernateManager.getCurrentViewProjection();
		if (currentProjection == null) {
			currentProjection = projections.length > 0 ? projections[0] : null;
			for (int i = 0; i < projections.length; i ++){
				if (projections[i].getIsDefault() ){
					currentProjection = projections[i];
					break;
				}
			}
		}
		if (currentProjection != null) {
			lstProjections.setSelection(new StructuredSelection(currentProjection));			
			try{
				Point point = gf.createPoint(new Coordinate(x,y));
				Point np = (Point)JTS.transform(point, CRS.findMathTransform(SmartDB.DATABASE_CRS, 
						ReprojectUtils.stringToCrs(currentProjection.getDefinition())));
				x = np.getX();
				y = np.getY();
			}catch (Exception ex){
				
			}
		}
		lstProjections.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				transformInput();
				currentProjection = (Projection)((IStructuredSelection)lstProjections.getSelection()).getFirstElement();
				validate();
			}
		});
		
		lbl = new Label(waypointComp, SWT.NONE);
		lbl.setText(Messages.AddWaypointDialog_WaypointId_Label );
		
		ModifyListener validation = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		
		txtWaypointId = new Text(waypointComp, SWT.BORDER);
		txtWaypointId.setText(waypointId);
		txtWaypointId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWaypointId.addModifyListener(validation);
		txtWaypointId.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {}
			
			@Override
			public void focusGained(FocusEvent e) {
				txtWaypointId.selectAll();
			}
		});
		
		lbl = new Label(waypointComp, SWT.NONE);
		lbl.setText(Messages.AddWaypointDialog_X_Label );
		txtX = new Text(waypointComp, SWT.BORDER);
		txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if(x != 0)txtX.setText(String.valueOf(x));
		txtX.addModifyListener(validation);
		txtX.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {}
			
			@Override
			public void focusGained(FocusEvent e) {
				txtX.selectAll();
			}
		});
		
		lbl = new Label(waypointComp, SWT.NONE);
		lbl.setText(Messages.AddWaypointDialog_Y_Label );
		txtY = new Text(waypointComp, SWT.BORDER);
		txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if(y != 0)txtY.setText(String.valueOf(y));
		txtY.addModifyListener(validation);
		txtY.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {}
			
			@Override
			public void focusGained(FocusEvent e) {
				txtY.selectAll();
			}
		});
		
		Link lnkMap  = new Link(waypointComp, SWT.NONE);
		lnkMap.setText("<a>" + Messages.AddWaypointDialog_SelectOnMap + "</a>");  //$NON-NLS-1$ //$NON-NLS-2$
		lnkMap.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectOnMap();
			}
		});
		lnkMap.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false,2,1));
		
		setMessage(Messages.AddWaypointDialog_DialogMessage);
		setTitle(Messages.AddWaypointDialog_DialogTitle);
		super.getShell().setText(Messages.AddWaypointDialog_DialogTitle);
		return parent;
	}
	
	private void transformInput() {
		try {
			//reproject
			CoordinateReferenceSystem sourceCrs = ReprojectUtils.stringToCrs( ((Projection)((IStructuredSelection)lstProjections.getSelection()).getFirstElement()).getDefinition());
			Point point = gf.createPoint(new Coordinate(Double.parseDouble(txtX.getText()),Double.parseDouble(txtY.getText())));
			Point p = (Point) JTS.transform(point, CRS.findMathTransform(ReprojectUtils.stringToCrs(currentProjection.getDefinition()), sourceCrs));

			txtX.setText(String.valueOf(p.getX()));
			txtY.setText(String.valueOf(p.getY()));
		} catch (Exception ex) {
			//nothing
		}
	}

	private void selectOnMap(){
		SelectPointOnMapDialog md = new SelectPointOnMapDialog(txtX.getShell());
		
		try{
			CoordinateReferenceSystem target = ReprojectUtils.stringToCrs(currentProjection.getDefinition());
		
			try {
				Double x = Double.parseDouble(txtX.getText());
				Double y = Double.parseDouble(txtY.getText());
				
				Coordinate c2 = ReprojectUtils.reproject(x, y, target, SmartDB.DATABASE_CRS);
				if (c2 != null){
					md.setInitPoint(c2.x,c2.y);
				}
			}catch (Exception ex) {
				//eatme
			}
			if (md.open() == SelectPointOnMapDialog.OK){
				if (md.getPoint() != null){
					Coordinate c2 = ReprojectUtils.reproject(md.getPoint().getX(), md.getPoint().getY(), SmartDB.DATABASE_CRS, target);
					txtX.setText(String.valueOf(c2.getX()));
					txtY.setText(String.valueOf(c2.getY()));
					
				}
			}
		}catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(ex.getMessage(),  ex);
		}
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control ctr = super.createContents(parent);
		validate();
		return ctr;
	}
	
	private void validate(){
		String error = findError();
		setErrorMessage(error);
		getButton(OK).setEnabled(error == null);
	}
	private String findError(){
		CoordinateReferenceSystem sourceCrs = null;
		if (lstProjections.getSelection().isEmpty()){
			return Messages.AddWaypointDialog_Error_NoProjection;
		}
		try{
			sourceCrs = ReprojectUtils.stringToCrs( ((Projection)((IStructuredSelection)lstProjections.getSelection()).getFirstElement()).getDefinition()); 
		}catch (Exception ex){
			return Messages.AddWaypointDialog_Error_CouldNotParseCrs + ex.getLocalizedMessage();
		}
		
		if (txtWaypointId.getText().trim().length() == 0){
			return Messages.AddWaypointDialog_Error_NoId;
		}
		try{
			Integer.parseInt(txtWaypointId.getText());
		}catch (NumberFormatException ex){
			return Messages.AddWaypointDialog_Error_InvalidId;
		}
		
		if (this.txtX.getText().trim().length() == 0){
			return Messages.AddWaypointDialog_Error_NoXValue;
		}

		try{
			Double.parseDouble(txtX.getText());
		}catch (NumberFormatException ex){
			return Messages.AddWaypointDialog_Error_InvalidXValue;
		}
		
		if (this.txtY.getText().trim().length() == 0){
			return Messages.AddWaypointDialog_Error_NoYValue;
		}
		try{
			Double.parseDouble(txtY.getText());
		}catch (NumberFormatException ex){
			return Messages.AddWaypointDialog_Error_InvalidYValue;
		}
		
		try{
			Point point = gf.createPoint(new Coordinate(x,y));
			JTS.transform(point, CRS.findMathTransform(sourceCrs, SmartDB.DATABASE_CRS));
		}catch (Exception ex){
			return Messages.AddWaypointDialog_Error_InvalidXY + ex.getLocalizedMessage();
		}
		
		return null;
	}
}
