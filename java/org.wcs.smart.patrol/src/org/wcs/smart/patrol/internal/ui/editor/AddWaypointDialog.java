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
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
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
	private Projection currentProjection;
	
	private PatrolWaypoint newWaypoint;
	
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
		
		newWaypoint.getWaypoint().setId(Integer.parseInt(txtWaypointId.getText()));
		newWaypoint.getWaypoint().setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
		newWaypoint.getWaypoint().setConservationArea(SmartDB.getCurrentConservationArea());
		try{
			//reproject
			CoordinateReferenceSystem sourceCrs = ((Projection)((IStructuredSelection)lstProjections.getSelection()).getFirstElement()).getCrs();
			Point point = gf.createPoint(new Coordinate(Double.parseDouble(txtX.getText()),Double.parseDouble(txtY.getText())));
			Point p = (Point) JTS.transform(point, CRS.findMathTransform(sourceCrs, SmartDB.DATABASE_CRS));

			newWaypoint.getWaypoint().setX(p.getX());
			newWaypoint.getWaypoint().setY(p.getY());
		}catch (Exception ex){
			SmartPlugIn.displayLog(getShell(), Messages.AddWaypointDialog_Error_SavingWaypoint + ex.getLocalizedMessage(), ex);
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
		currentProjection = ObservationHibernateManager.getCurrentViewProjection();
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
				Point np = (Point)JTS.transform(point, CRS.findMathTransform(SmartDB.DATABASE_CRS, currentProjection.getCrs()));
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
		txtWaypointId.setText(String.valueOf(waypointId));
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
		
		setMessage(Messages.AddWaypointDialog_DialogMessage);
		setTitle(Messages.AddWaypointDialog_DialogTitle);
		super.getShell().setText(Messages.AddWaypointDialog_DialogTitle);
		return parent;
	}
	
	private void transformInput() {
		try {
			//reproject
			CoordinateReferenceSystem sourceCrs = ((Projection)((IStructuredSelection)lstProjections.getSelection()).getFirstElement()).getCrs();
			Point point = gf.createPoint(new Coordinate(Double.parseDouble(txtX.getText()),Double.parseDouble(txtY.getText())));
			Point p = (Point) JTS.transform(point, CRS.findMathTransform(currentProjection.getCrs(), sourceCrs));

			txtX.setText(String.valueOf(p.getX()));
			txtY.setText(String.valueOf(p.getY()));
		} catch (Exception ex) {
			//nothing
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
			sourceCrs = ((Projection)((IStructuredSelection)lstProjections.getSelection()).getFirstElement()).getCrs(); 
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
