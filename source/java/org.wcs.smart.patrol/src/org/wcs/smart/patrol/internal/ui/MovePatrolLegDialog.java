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
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
/**
 * Dialog to let you name the new Patrol you create when using the "Split leg into new Patrol" button
 * You can move more than one leg, they all go into a single, new patrol.
 * 
 * @author Jeff
 * @since 5.0.0
 * 
 */


public class MovePatrolLegDialog extends TitleAreaDialog{


	private List<PatrolLeg> legsToMove;
	private Patrol newPatrol;
	private Text txtPatrolId;
	private Patrol originalPatrol;
	
	
	/**
	 * Creates a new move patrol leg dialog 
	 * 
	 * @param parentShell parent shell
	 * @param patrolLeg legs to move into new patrols
	 */
	public MovePatrolLegDialog(Shell parentShell, ArrayList<PatrolLeg> patrolLegs) {
		super(parentShell);
		this.legsToMove = patrolLegs;
		originalPatrol = legsToMove.get(0).getPatrol();
	}
	

	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	/**
	 * Create the dialog elements
	 * 
	 **/
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Label lbl;
		parent = (Composite) super.createDialogArea(parent);
		
		Composite patrolIdComp = new Composite(parent, SWT.NONE);
		patrolIdComp.setLayout(new GridLayout(2, false));
		patrolIdComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lbl = new Label(patrolIdComp, SWT.NONE);
		lbl.setText(Messages.MovePatrolLegDialog_NewPatrolID);
		txtPatrolId = new Text(patrolIdComp, SWT.BORDER);
		txtPatrolId.setTextLimit(PatrolLeg.ID_MAX_SIZE);
		txtPatrolId.setText(Messages.MovePatrolLegDialog_SplitText + legsToMove.get(0).getPatrol().getId() );
		txtPatrolId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtPatrolId.setTextLimit(32);
	
		setMessage(Messages.MovePatrolLegDialog_SelectNewID);
		getShell().setText(Messages.MovePatrolLegDialog_SplitLegsButton);
		setTitle(Messages.MovePatrolLegDialog_CreateNewPatrol);
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