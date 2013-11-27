package org.wcs.smart.incident.ui.newwizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.observation.model.Waypoint;

public class DistanceDirectionComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.distancedirection";
	
	private Text txtDirection;
	private Text txtDistance;
	
	@Override
	public String validate() {
		if (!txtDirection.getText().trim().isEmpty()){
			try{
				Float.parseFloat(txtDirection.getText());
			}catch (Exception ex){
				return "Direction must be a valid number.";
			}
		}
		if (!txtDistance.getText().trim().isEmpty()){
			try{
				Float.parseFloat(txtDistance.getText());
			}catch (Exception ex){
				return "Distance must be a valid number.";
			}
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout(2, false));
		
		Label l = new Label(item, SWT.NONE);
		l.setText("Distance:");
		
		txtDistance = new Text(item, SWT.BORDER);
		txtDistance.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtDistance.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(item, SWT.NONE);
		l.setText("Direction:");
		
		txtDirection = new Text(item, SWT.BORDER);
		txtDirection.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtDirection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		return item;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		if (!txtDirection.getText().trim().isEmpty()){
			incident.setDirection(Float.parseFloat(txtDirection.getText().trim()));
		}else{
			incident.setDirection(null);
		}
		if (!txtDistance.getText().trim().isEmpty()){
			incident.setDistance(Float.parseFloat(txtDistance.getText().trim()));
		}else{
			incident.setDistance(null);
		}
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		if (incident.getDirection() == null){
			txtDirection.setText("");
		}else{
			txtDirection.setText(String.valueOf(incident.getDirection()));
		}
		if (incident.getDistance() == null){
			txtDistance.setText("");
		}else{
			txtDistance.setText(String.valueOf(incident.getDistance()));
		}
	}
	
	@Override
	public String getName() {
		return "Distance & Direction";
	}

	@Override
	public String getDescription() {
		return "Sets the incident distance and direction.  These fields are optional.";
	}

}
