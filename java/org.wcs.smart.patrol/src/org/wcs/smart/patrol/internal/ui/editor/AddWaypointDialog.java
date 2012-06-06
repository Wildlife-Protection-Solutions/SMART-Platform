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
import org.wcs.smart.patrol.model.Waypoint;

/**
 * Dialog for adding a new waypoint.
 * @author Emily
 * @since 1.0.0
 */
public class AddWaypointDialog extends TitleAreaDialog{

	private Text txtWaypointId;
	private Text txtEasting;
	private Text txtNorthing;
	private double y;
	private double x;
	
	private Waypoint newWaypoint;
	public AddWaypointDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public AddWaypointDialog(Shell parentShell, double y, double x) {
		super(parentShell);
		this.x = x;
		this.y =y;
	}
	
	public Waypoint getWaypoint(){
		return newWaypoint;
		
	}
	
	@Override
	protected void okPressed() {
		newWaypoint = new Waypoint();
		newWaypoint.setId(Integer.parseInt(txtWaypointId.getText()));
		newWaypoint.setX(Double.parseDouble(txtEasting.getText()));
		newWaypoint.setY(Double.parseDouble(txtNorthing.getText()));
		super.okPressed();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		
		parent.setLayout(new GridLayout(1, false));
		
		Composite legtype = new Composite(parent, SWT.NONE);
		legtype.setLayout(new GridLayout(2, false));
		legtype.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(legtype, SWT.NONE);
		lbl.setText("Waypoint Id:" );
		
		ModifyListener validation = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		
		txtWaypointId = new Text(legtype, SWT.BORDER);
		txtWaypointId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWaypointId.addModifyListener(validation);
		
		lbl = new Label(legtype, SWT.NONE);
		lbl.setText("Longitude:" );
		txtEasting = new Text(legtype, SWT.BORDER);
		txtEasting.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if(x != 0)txtEasting.setText(String.valueOf(x));
		txtEasting.addModifyListener(validation);
		
		
		lbl = new Label(legtype, SWT.NONE);
		lbl.setText("Latitude:" );
		txtNorthing = new Text(legtype, SWT.BORDER);
		txtNorthing.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if(y != 0)txtNorthing.setText(String.valueOf(y));
		txtNorthing.addModifyListener(validation);
		
		
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
		if (txtWaypointId.getText().trim().length() == 0){
			return "Waypoint id must be specified.";
		}
		try{
			Integer.parseInt(txtWaypointId.getText());
		}catch (NumberFormatException ex){
			return "Invalid waypoint id.  The waypoint id must be an integer.";
		}
		
		if (this.txtEasting.getText().trim().length() == 0){
			return "Longitude must be specified.";
		}
		try{
			double e = Double.parseDouble(txtEasting.getText());
			if (e < -180 || e > 180){
				return "Longitude must be between -180 and 180";
			}
		}catch (NumberFormatException ex){
			return "Invalid longitude value.";
		}
		
		if (this.txtNorthing.getText().trim().length() == 0){
			return "Latitude Coordinate must be specified.";
		}
		try{
			double n = Double.parseDouble(txtNorthing.getText());
			if (n < -90 || n > 90){
				return "Latitude must be between -90 and 90";
			}
		}catch (NumberFormatException ex){
			return "Invalid latitude value.";
		}
		
		
		
		return null;
	}
}
