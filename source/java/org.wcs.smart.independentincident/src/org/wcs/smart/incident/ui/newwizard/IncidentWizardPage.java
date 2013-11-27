package org.wcs.smart.incident.ui.newwizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.observation.model.Waypoint;

public class IncidentWizardPage extends WizardPage {

	private AbstractIncidentComposite contents;
	private NewIncidentWizard wizard;
	
	public IncidentWizardPage(NewIncidentWizard wizard, AbstractIncidentComposite contents){
		super(contents.getName());
		this.contents = contents;
		this.wizard = wizard;
	}
	
	public boolean canFinish(){
		if (contents instanceof IdComposite ||
				contents instanceof DateTimeComposite ||
				contents instanceof LocationComposite){
			return false;
		}
		return true;
	}
	
	public boolean canFlipToNextPage(){
		return getErrorMessage() == null;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite center = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = 20;
		center.setLayout(gl);
		
		Composite x = contents.createComposite(center);
		x.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		
		contents.addChangeListener(new Listener(){

			@Override
			public void handleEvent(Event event) {
				validate();
			}});
		
		setTitle(contents.getName());
		setMessage(contents.getDescription());
		
		super.setControl(center);
	}

	public String validate(){
		String error = contents.validate();
		if (error == null){
			setErrorMessage(null);
		}else{
			setErrorMessage(error);
		}
		try{
			wizard.getContainer().updateButtons();
		}catch (Exception ex){
			//this may be called before the buttons have been setup to eat this exception
		}
		return error;
	}
	
	public void updateIncident(Waypoint incident){
		contents.updateIncident(incident);
	}
	
	public void initPage(Waypoint incident, Session session){
		contents.initFields(incident, session);
		validate();
	}
}
