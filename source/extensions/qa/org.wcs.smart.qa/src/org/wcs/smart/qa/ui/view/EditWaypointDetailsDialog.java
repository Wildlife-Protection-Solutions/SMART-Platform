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
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.ui.ProjectionLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Dialog for editing waypoint details.
 * 
 * @author Emily
 *
 */
public class EditWaypointDetailsDialog extends TitleAreaDialog{

	private UUID wpUuid;
	private Waypoint waypoint; 
	
	private Text txtX;
	private Text txtY;
	private ComboViewer lstProjection;
	private Projection currentProjection = null;
	
	public EditWaypointDetailsDialog(Shell parentShell, UUID wpUuid) {
		super(parentShell);
		this.wpUuid = wpUuid;
	}

	private void initControls(){
		List<Projection> projections = null;
		Session s = HibernateManager.openSession();
		try{
			waypoint = (Waypoint) s.get(Waypoint.class, wpUuid);
			if (waypoint != null){
				waypoint.getX();
				waypoint.getY();
			}
			
			currentProjection = HibernateManager.getCurrentViewProjection(s);
			projections = HibernateManager.getCaProjectionList(s);
			for (Projection p : projections){
				try{
					p.setParsedCoordinateReferenceSystem(ReprojectUtils.stringToCrs(p.getDefinition()));
				}catch (Exception ex){
					//TODO:
					ex.printStackTrace();
				}
			}
		}finally{
			s.close();
		}
		
		if (currentProjection == null){
			currentProjection = new Projection();
			currentProjection.setConservationArea(SmartDB.getCurrentConservationArea());
			currentProjection.setParsedCoordinateReferenceSystem(SmartDB.DATABASE_CRS);
		}
		if (!projections.contains(currentProjection)){
			projections.add(currentProjection);
		}
		lstProjection.setInput(projections);
		lstProjection.setSelection(new StructuredSelection(currentProjection));
		reprojectPoint(waypoint.getX(),  waypoint.getY());
		
		
		setTitle("Waypoint: " + waypoint.getId() + " - " + DateFormat.getDateTimeInstance().format(waypoint.getDateTime()));

	}
	
	private void reprojectPoint(double x, double y){
		Projection toprj = (Projection) ((IStructuredSelection)lstProjection.getSelection()).getFirstElement();
		Coordinate c = new Coordinate(x, y);
		try {
			c = ReprojectUtils.reproject(x,  y, currentProjection.getParsedCoordinateReferenceSystem(), toprj.getParsedCoordinateReferenceSystem());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		txtX.setText(String.valueOf(c.x));
		txtY.setText(String.valueOf(c.y));
	}
	
	@Override
	protected void okPressed(){
		validate();
		if (!getButton(IDialogConstants.OK_ID).isEnabled()) return;	//data error
		
		Coordinate c = new Coordinate(Double.parseDouble(txtX.getText()), Double.parseDouble(txtY.getText()));
		try {
			c = ReprojectUtils.reproject(c.x,  c.y, currentProjection.getParsedCoordinateReferenceSystem(), SmartDB.DATABASE_CRS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();

			Waypoint wp = (Waypoint) s.get(Waypoint.class, wpUuid);
			wp.setX(c.x);
			wp.setY(c.y);
			
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
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lbl = new Label(main, SWT.NONE);
		
		lbl.setText("Projection:");

		
		lstProjection = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 100;
		lstProjection.getControl().setLayoutData(gd);
		lstProjection.setLabelProvider(ProjectionLabelProvider.getInstance());
		lstProjection.setContentProvider(ArrayContentProvider.getInstance());
		lstProjection.setInput(new String[]{DialogConstants.LOADING_TEXT});
		
		lstProjection.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (txtX.getText().isEmpty() || txtY.getText().isEmpty()) return;
				try{
					reprojectPoint(Double.parseDouble(txtX.getText()), Double.parseDouble(txtY.getText()));	
				}catch (Exception ex){
					ex.printStackTrace();
				}
				currentProjection = (Projection) ((IStructuredSelection)lstProjection.getSelection()).getFirstElement();
				validate();
			}
		});
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("X Coordinate:");
		
		ModifyListener validation = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		
		txtX = new Text(main, SWT.BORDER);
		txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtX.addModifyListener(validation);
		txtX.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {}
			
			@Override
			public void focusGained(FocusEvent e) {
				txtX.selectAll();
			}
		});
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Y Coordinate:");
		
		txtY = new Text(main, SWT.BORDER);
		txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtY.addModifyListener(validation);
		txtY.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {}
			
			@Override
			public void focusGained(FocusEvent e) {
				txtY.selectAll();
			}
		});
		
		
		
		
		getShell().setText("Edit Waypoint");
//		setTitle(routine.getName());
		setMessage("Edit waypoint details");
		
		initControls();
		
		
		return composite;
	}
	
	
	private void validate(){
		boolean ok = (findError() == null); 

		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null)btn.setEnabled(ok);
		 
	}
	
	private String findError(){
		CoordinateReferenceSystem sourceCrs = null;
		if (lstProjection.getSelection().isEmpty()){
			return "A projection must be selected";
		}
		try{
			sourceCrs = ReprojectUtils.stringToCrs( ((Projection)((IStructuredSelection)lstProjection.getSelection()).getFirstElement()).getDefinition()); 
		}catch (Exception ex){
			return "Could not parse CRS: " + ex.getLocalizedMessage();
		}
		
		double x = 0;
		double y = 0;
		if (txtX.getText().trim().length() == 0){
			return "X Coordinate value required";
		}
		try{
			x = Double.parseDouble(txtX.getText());
		}catch (NumberFormatException ex){
			return "Invalid X Coordinate value";
		}
		
		if (txtY.getText().trim().length() == 0){
			return "Y Coordinate value required";
		}
		try{
			y = Double.parseDouble(txtY.getText());
		}catch (NumberFormatException ex){
			return "Invalid Y Coordinate value";
		}
		
		try{
			Point point = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(x,y));
			JTS.transform(point, CRS.findMathTransform(sourceCrs, SmartDB.DATABASE_CRS));
		}catch (Exception ex){
			return "(x,y) values do not represent a valid loctation: " + ex.getLocalizedMessage();
		}
		
		return null;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
