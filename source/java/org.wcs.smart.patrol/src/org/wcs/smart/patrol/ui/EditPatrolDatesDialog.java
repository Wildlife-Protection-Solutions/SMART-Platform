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
package org.wcs.smart.patrol.ui;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.type.Type;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.DateComposite;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Dialog for editing patrol dates when opening a patrol that is 
 * too long (creating from CT error).
 * 
 * @author Emily
 *
 */
public class EditPatrolDatesDialog extends SmartStyledTitleDialog{

	private PatrolEditorInput input;
	private DateComposite dateComp;
	
	public EditPatrolDatesDialog(Shell parentShell, PatrolEditorInput input) {
		super(parentShell);
		this.input = input;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		setTitle(MessageFormat.format(Messages.EditPatrolDatesDialog_Title, input.getPatrolId(), 
				DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(input.getStartDate()), 
				DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(input.getEndDate())));
		getShell().setText(Messages.EditPatrolDatesDialog_ShellTitle);
		setMessage(Messages.EditPatrolDatesDialog_Message);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		
		dateComp = new DateComposite();
		final Composite comp = dateComp.createComponent(main, SWT.NONE);
		dateComp.addChangeListener(()-> {
			String errorMessage = dateComp.getErrorMessage();
			if (errorMessage == null){
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}else{
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			}
			setErrorMessage(errorMessage);
		});
		
		Job j = new Job("loading patrol"){ //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Patrol patrol = null;
				try(Session session = HibernateManager.openSession()){
					patrol = (Patrol) session.get(Patrol.class, input.getUuid());
				}
				final Patrol fpatrol = patrol;
				Display.getDefault().syncExec(()->{
					if (fpatrol == null){
						setErrorMessage(Messages.EditPatrolDatesDialog_LoadError);
					}else{
						comp.setEnabled(true);
						dateComp.setValues(fpatrol.getStartDate(), fpatrol.getEndDate());
					}
				});
				
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
		comp.setEnabled(false);
		
		return composite;
	}
	
	
	/**
	 * Notifies that the ok button of this dialog has been pressed.
	 * <p>
	 * The <code>Dialog</code> implementation of this framework method sets
	 * this dialog's return code to <code>Window.OK</code> and closes the
	 * dialog. Subclasses may override.
	 * </p>
	 */
	protected void okPressed() {
		LocalDate startDate = dateComp.getStartDate();
		LocalDate endDate = dateComp.getEndDate();
		
		boolean[] cont = new boolean[] {false};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					cont[0] = updatePatrol(startDate, endDate);
				}
			});
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.EditPatrolDatesDialog_SaveError + ex.getMessage(), ex);
			return;
		}
		if (cont[0]) super.okPressed();
	}

	/*
	 * counter for the number of waypoints delete
	 */
	private int waypointDeleteCnt = 0;
	
	private boolean updatePatrol(LocalDate startDate, LocalDate endDate){
		Patrol patrol = null;
		
		waypointDeleteCnt = 0;
		
		WaypointAttachmentInterceptor interceptor = new WaypointAttachmentInterceptor() {
			private static final long serialVersionUID = 1L;
			
			@Override
			 public void onDelete(Object entity,
			            Serializable id,
			            Object[] state,
			            String[] propertyNames,
			            Type[] types) {
				 super.onDelete(entity, id, state, propertyNames, types);
				 if (entity instanceof Waypoint) {
					 waypointDeleteCnt++;
				 }
			 }			
		};
		
		try(Session session = HibernateManager.openSession(interceptor)){
			
			session.beginTransaction();
			try{
				patrol = (Patrol) session.get(Patrol.class, input.getUuid());
				patrol.setStartDate(startDate);
				patrol.setEndDate(endDate);
				
				List<PatrolLeg> legsToDelete = new ArrayList<PatrolLeg>();
				for (PatrolLeg pl : patrol.getLegs()){
					if (pl.getEndDate().isBefore(startDate)){
						//delete me
						legsToDelete.add(pl);
						continue;
					}
					if (pl.getStartDate().isAfter(endDate)){
						legsToDelete.add(pl);
						continue;
						//delete me
					}
					
					boolean modified = false;
					if (pl.getStartDate().isBefore(startDate)){
						pl.setStartDate(startDate);
						modified = true;
					}
					if (pl.getEndDate().isAfter(endDate)){
						pl.setEndDate(endDate);
						modified = true;
					}
					if (!modified) continue;
					
					//if leg has been modified we need to update the days
					List<PatrolLegDay> toDelete = new ArrayList<PatrolLegDay>();
					for (PatrolLegDay pld : pl.getPatrolLegDays()){
						if (pld.getDate().isBefore(startDate) || pld.getDate().isAfter(endDate)){
							//delete me
							toDelete.add(pld);
						}
					}
					
					pl.getPatrolLegDays().removeAll(toDelete);
					for (PatrolLegDay pld : toDelete){
						pld.setPatrolLeg(null);
						
						//delete waypoints 
						if (pld.getWaypoints() != null) {
							for (PatrolWaypoint pw : pld.getWaypoints()){
								session.delete(pw.getWaypoint());
							}
						}
						session.delete(pld);
						
					}
				}
				
				if (patrol.getLegs().size() == legsToDelete.size()){
					//we don't want to delete all legs
					PatrolLeg keep = legsToDelete.remove(0);
					keep.setStartDate(startDate);
					keep.setEndDate(endDate);
					keep.createLegDays(session);
				}
				
				for (PatrolLeg pl : legsToDelete){
					if (pl.getPatrolLegDays() != null){
						for (PatrolLegDay pld : pl.getPatrolLegDays()){
							if (pld.getWaypoints() != null){
								for (PatrolWaypoint pw : pld.getWaypoints()){
									session.delete(pw.getWaypoint());
								}
							}
						}
					}
					pl.getPatrol().getLegs().remove(pl);
					pl.setPatrol(null);
					session.delete(pl);
				}
				
				if (patrol.getLegs().size() == 1){
					//if there is only one leg, make sure it expands the entire date range
					//and a day exists for each leg
					patrol.getFirstLeg().setStartDate(startDate);
					patrol.getFirstLeg().setEndDate(endDate);
					patrol.getFirstLeg().createLegDays(session);
				}else{
					//ideally here we make sure there are legs for day etc.
					//but for now we'll leave this up to the user.
				}
				session.flush();
				
				if (waypointDeleteCnt > 0) {
					final boolean[] cont = new boolean[] {true};
					Display.getDefault().syncExec(()->{
						if (!MessageDialog.openConfirm(getShell(), Messages.EditPatrolDatesDialog_VerifyDeleteTitle, MessageFormat.format(Messages.EditPatrolDatesDialog_VerifyDeleteMsg, waypointDeleteCnt))){
							cont[0] = false;
						}
					});
					if (!cont[0]) {
						session.getTransaction().rollback();
						return false;
					}
				}
				session.getTransaction().commit();
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.EditPatrolDatesDialog_SaveError + ex.getMessage(), ex);
				try{
					session.getTransaction().rollback();
				}catch (Exception ex1){}
				return false;
			}
		}
		PatrolEventManager.getInstance().patrolSaved(patrol, true);
		return true;
	}
}
