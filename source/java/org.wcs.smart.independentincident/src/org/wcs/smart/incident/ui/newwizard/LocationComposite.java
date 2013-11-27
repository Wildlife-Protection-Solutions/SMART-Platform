package org.wcs.smart.incident.ui.newwizard;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class LocationComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.location";
	
	private ComboViewer cmbProjection;
	private Text txtX;
	private Text txtY;
	
	@Override
	public String validate() {
		if (txtX.getText().trim().isEmpty()){
			return "X coordinate must be provided.";
		}
		if (txtY.getText().trim().isEmpty()){
			return "Y coordinate must be provided.";
		}
		try{
			Double.parseDouble(txtX.getText());
		}catch (Exception ex){
			return "X value must be a valid number.";
		}
		try{
			Double.parseDouble(txtY.getText());
		}catch (Exception ex){
			return "Y value must be a valid number.";
		}
		
		double x = Double.parseDouble(txtX.getText());
		double y = Double.parseDouble(txtY.getText());
		Projection proj = (Projection) ((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
		try{
			Coordinate z = ReprojectUtils.reproject(x, y, proj.getCrs(), SmartDB.DATABASE_CRS);
		}catch (Exception ex){
			return "Coordinates are not valid. " + ex.getMessage();
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout(2, false));
		
		Label l = new Label(item, SWT.NONE);
		l.setText("Projection:");
		cmbProjection = new ComboViewer(item, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbProjection.setContentProvider(ArrayContentProvider.getInstance());
		cmbProjection.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof Projection){
					return ((Projection)element).getName();
				}
				return super.getText(element);
			}
		});
		cmbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(item, SWT.NONE);
		l.setText("X:");
		txtX = new Text(item, SWT.BORDER);
		txtX.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtX.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(item, SWT.NONE);
		l.setText("Y:");
		txtY = new Text(item, SWT.BORDER);
		txtY.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtY.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		return item;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		double x = Double.parseDouble(txtX.getText());
		double y = Double.parseDouble(txtY.getText());
		Projection proj = (Projection) ((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
		try{
			Coordinate z = ReprojectUtils.reproject(x, y, proj.getCrs(), SmartDB.DATABASE_CRS);
			incident.setX(z.x);
			incident.setY(z.y);
		}catch (Exception ex){
			//this error should be caught in the validate function
			IncidentPlugIn.log("Could not re-project coordinate to database CRS", ex);
		}
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		List<Projection> projs = HibernateManager.getCaProjectionList(session);
		Projection defaultp = null;
		for(Projection p : projs){
			try{
				if (CRS.equalsIgnoreMetadata(p.getCrs(), SmartDB.DATABASE_CRS)){
					defaultp = p;
					break;
				}
			}catch (Exception ex){
				IncidentPlugIn.log("Error parsing projection info", ex);
			}
		}
		if (defaultp == null){
			defaultp = new Projection();
			defaultp.setCrs(SmartDB.DATABASE_CRS);
			projs.add(defaultp);
		}
		cmbProjection.setInput(projs);
		txtX.setText(String.valueOf(incident.getX()));
		txtY.setText(String.valueOf(incident.getY()));
		
		cmbProjection.setSelection(new StructuredSelection(defaultp));
	}
	
	@Override
	public String getName() {
		return "Location";
	}

	@Override
	public String getDescription() {
		return "The incident location. A single point must be selected.";
	}

}
