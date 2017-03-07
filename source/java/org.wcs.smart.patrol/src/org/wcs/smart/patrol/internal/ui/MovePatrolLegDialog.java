package org.wcs.smart.patrol.internal.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;

public class MovePatrolLegDialog extends TitleAreaDialog{


	private List<PatrolLeg> legsToMove;
	private Patrol newPatrol;
	private Text txtPatrolId;
	private Patrol originalPatrol;
	private Session s;
	
	public MovePatrolLegDialog(Shell parentShell, ArrayList<PatrolLeg> patrolLegs, Session s) {
		super(parentShell);
		this.legsToMove = patrolLegs;
		originalPatrol = legsToMove.get(0).getPatrol();
		this.s = s;
	}
	

	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Label lbl;
		parent = (Composite) super.createDialogArea(parent);
		
		Composite patrolIdComp = new Composite(parent, SWT.NONE);
		patrolIdComp.setLayout(new GridLayout(2, false));
		patrolIdComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lbl = new Label(patrolIdComp, SWT.NONE);
		lbl.setText("New Patrol ID");
		txtPatrolId = new Text(patrolIdComp, SWT.BORDER);
		txtPatrolId.setTextLimit(PatrolLeg.ID_MAX_SIZE);
		txtPatrolId.setText("(split from)" + legsToMove.get(0).getPatrol().getId() );
		txtPatrolId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtPatrolId.setTextLimit(32);
	
		setMessage("Select a Patrol ID for the New Patrol.");
		getShell().setText("Split Legs into a New Patrol");
		setTitle("Create New Patrol");
		return parent;
	}
	
	
	
	/**
	 * Create the new Patrol and assign the legs to it.
	 */
	@Override
	protected void okPressed() {
		//Make a new Patrol to put everything into:
		newPatrol = originalPatrol.simpleClone();
		newPatrol.setLegs(new ArrayList<PatrolLeg>());
		newPatrol.setId(txtPatrolId.getText());
		Date e;
		Date start;

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 2200);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		start = cal.getTime(); 

		cal.set(Calendar.YEAR, 1900);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		e = cal.getTime();
		for(PatrolLeg  pl : legsToMove){
			if(pl.getStartDate().before(start))start = pl.getStartDate();
			if(pl.getEndDate().after(e))e = pl.getEndDate();
		}
		newPatrol.setStartDate(start);
		newPatrol.setEndDate(e);
		
		for(PatrolLeg pl : legsToMove){
			PatrolLeg legClone = pl.simpleClone();
			legClone.setPatrolLegDays(new ArrayList<PatrolLegDay>());
			if (pl.getPatrolLegDays() != null && pl.getPatrolLegDays().size() > 0){
				//Clone Leg Days as well
				for (PatrolLegDay pld : pl.getPatrolLegDays()){
					PatrolLegDay clone = pld.clone();
					
					ArrayList<PatrolWaypoint> allWaypoints = new ArrayList<PatrolWaypoint>();
					
					for(PatrolWaypoint wp : pld.getWaypoints()){
						PatrolWaypoint pw = new PatrolWaypoint();
						pw.setPatrolLegDay(clone);
						pw.setWaypoint(wp.getWaypoint());
						allWaypoints.add(pw);
					}
					clone.setWaypoints(allWaypoints); 
					clone.setPatrolLeg(legClone);
					legClone.getPatrolLegDays().add(clone);
				}
			}
			legClone.setPatrol(newPatrol);
			newPatrol.getLegs().add(legClone);
		}
		
		super.okPressed();
	}

	
	/**
	 * return the Patrol that is created when the dialog's 'ok' button is pressed.
	 */
	public Patrol getNewPatrol() {
		return newPatrol;
	}


}