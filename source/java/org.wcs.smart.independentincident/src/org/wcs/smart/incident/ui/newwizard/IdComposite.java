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

public class IdComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.id";
	
	private Text txtId;
	
	@Override
	public String validate() {
		if (txtId.getText().trim().isEmpty()){
			return "Incident ID must be provided.";
		}
		try{
			Integer.parseInt(txtId.getText());
		}catch (Exception ex){
			return "Incident ID must be an integer.";
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout(2, false));
		
		Label l = new Label(item, SWT.NONE);
		l.setText("Incident ID:");
		
		txtId = new Text(item, SWT.BORDER);
		txtId.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		return item;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		int id = Integer.parseInt(txtId.getText());
		incident.setId(id);
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		txtId.setText(String.valueOf(incident.getId()));
	}
	
	@Override
	public String getName() {
		return "Incident ID";
	}

	@Override
	public String getDescription() {
		return "The incident identifier.  Must be an integer.";
	}

}
