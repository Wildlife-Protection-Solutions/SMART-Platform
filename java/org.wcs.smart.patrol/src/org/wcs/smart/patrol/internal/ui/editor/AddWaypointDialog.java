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

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.ui.ProjectionLabelProvider;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Dialog for adding a new waypoint.
 * @author Emily
 * @since 1.0.0
 */
public class AddWaypointDialog extends TitleAreaDialog{

	private static final GeometryFactory gf = new GeometryFactory();
	private Text txtWaypointId;
	private Text txtX;
	private Text txtY;
	private ComboViewer lstProjections;
	private double y;
	private double x;
	private int waypointId;
	private Projection[] projections;
	
	private Waypoint newWaypoint;
	
	public AddWaypointDialog(Shell parentShell, Projection[] projections) {
		super(parentShell);
		x = 0;
		y = 0;
		waypointId = 0;
		this.projections = projections;
	}
	
	public AddWaypointDialog(Shell parentShell, double y, double x, int waypointId, Projection[] projections) {
		this(parentShell, projections);
		
		this.x = x;
		this.y = y;
		this.waypointId = waypointId;
	}
	
	public Waypoint getWaypoint(){
		return newWaypoint;
	}
	
	@Override
	protected void okPressed() {
		newWaypoint = new Waypoint();
		newWaypoint.setId(Integer.parseInt(txtWaypointId.getText()));
		try{
			//reproject
			CoordinateReferenceSystem sourceCrs = ((Projection)((IStructuredSelection)lstProjections.getSelection()).getFirstElement()).getCrs();
			Point point = gf.createPoint(new Coordinate(Double.parseDouble(txtX.getText()),Double.parseDouble(txtY.getText())));
			Point p = (Point) JTS.transform(point, CRS.findMathTransform(sourceCrs, SmartDB.DATABASE_CRS));

			newWaypoint.setX(p.getX());
			newWaypoint.setY(p.getY());
		}catch (Exception ex){
			SmartPlugIn.displayLog(getShell(), "Error saving waypoint.\n\n" + ex.getMessage(), ex);
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
		
		parent.setLayout(new GridLayout(1, false));
		
		Composite waypointComp = new Composite(parent, SWT.NONE);
		waypointComp.setLayout(new GridLayout(2, false));
		waypointComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Label lbl = new Label(waypointComp, SWT.NONE);
		lbl.setText("Projection:");

		
		lstProjections = new ComboViewer(waypointComp, SWT.DROP_DOWN);
		lstProjections.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstProjections.setLabelProvider(ProjectionLabelProvider.getInstance());
		lstProjections.setContentProvider(ArrayContentProvider.getInstance());
		lstProjections.setInput(projections);
		Projection defaultProj = projections.length > 0 ? projections[0] : null;
		for (int i = 0; i < projections.length; i ++){
			if (projections[i].getIsDefault() ){
				defaultProj = projections[i];
				break;
			}
		}
		if (defaultProj != null){
			lstProjections.setSelection(new StructuredSelection(defaultProj));			
			try{
				Point point = gf.createPoint(new Coordinate(x,y));
				Point np = (Point)JTS.transform(point, CRS.findMathTransform(SmartDB.DATABASE_CRS, defaultProj.getCrs()));
				x = np.getX();
				y = np.getY();
			}catch (Exception ex){
				
			}
		}
		lstProjections.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		lbl = new Label(waypointComp, SWT.NONE);
		lbl.setText("Waypoint Id:" );
		
		ModifyListener validation = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		
		txtWaypointId = new Text(waypointComp, SWT.BORDER);
		txtWaypointId.setText(String.valueOf(waypointId));
		txtWaypointId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWaypointId.addModifyListener(validation);
		
		lbl = new Label(waypointComp, SWT.NONE);
		lbl.setText("X Coordinate:" );
		txtX = new Text(waypointComp, SWT.BORDER);
		txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if(x != 0)txtX.setText(String.valueOf(x));
		txtX.addModifyListener(validation);
		
		
		lbl = new Label(waypointComp, SWT.NONE);
		lbl.setText("Y Coordinate:" );
		txtY = new Text(waypointComp, SWT.BORDER);
		txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if(y != 0)txtY.setText(String.valueOf(y));
		txtY.addModifyListener(validation);
		
		
		setMessage("Add a new waypoint.");
		super.getShell().setText("Add Waypoints");
		return parent;
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
		double x = -1;
		double y = -1;
		if (lstProjections.getSelection().isEmpty()){
			return "Projection must be selected.";
		}
		try{
			sourceCrs = ((Projection)((IStructuredSelection)lstProjections.getSelection()).getFirstElement()).getCrs(); 
		}catch (Exception ex){
			return "Could not parse CRS: " + ex.getMessage();
		}
		
		if (txtWaypointId.getText().trim().length() == 0){
			return "Waypoint id must be specified.";
		}
		try{
			Integer.parseInt(txtWaypointId.getText());
		}catch (NumberFormatException ex){
			return "Invalid waypoint id.  The waypoint id must be an integer.";
		}
		
		if (this.txtX.getText().trim().length() == 0){
			return "X Coordinate  must be specified.";
		}

		try{
			x = Double.parseDouble(txtX.getText());
//			if (e < -180 || e > 180){
//				return "Longitude must be between -180 and 180";
//			}
		}catch (NumberFormatException ex){
			return "Invalid x coordinate value.";
		}
		
		if (this.txtY.getText().trim().length() == 0){
			return "Y Coordinate must be specified.";
		}
		try{
			y = Double.parseDouble(txtY.getText());
//			if (n < -90 || n > 90){
//				return "Latitude must be between -90 and 90";
//			}
		}catch (NumberFormatException ex){
			return "Invalid y coordinate value.";
		}
		
		try{
			Point point = gf.createPoint(new Coordinate(x,y));
			JTS.transform(point, CRS.findMathTransform(sourceCrs, SmartDB.DATABASE_CRS));
		}catch (Exception ex){
			return "Invalid x,y values. " + ex.getMessage();
		}
		
		return null;
	}
}
