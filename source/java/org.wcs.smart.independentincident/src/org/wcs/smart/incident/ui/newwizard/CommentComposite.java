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

public class CommentComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.comment";
	
	private Text txtComment;
	
	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout(1, false));
		
		Label l = new Label(item, SWT.NONE);
		l.setText("Comments:");
		
		txtComment = new Text(item, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
		txtComment.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)txtComment.getLayoutData()).widthHint = 150;
		((GridData)txtComment.getLayoutData()).heightHint = 150;
		txtComment.setTextLimit(Waypoint.COMMENT_MAX_LENGTH);
		return item;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		if (txtComment.getText().trim().isEmpty()){
			incident.setComment(null);
		}else{
			incident.setComment(txtComment.getText().trim());
		}
		
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		if (incident.getComment()== null){
			txtComment.setText("");
		}else{
			txtComment.setText(incident.getComment());
		}
	}

	@Override
	public String getName() {
		return "Comments";
	}

	@Override
	public String getDescription() {
		return "Additional comments about the incident";
	}

}