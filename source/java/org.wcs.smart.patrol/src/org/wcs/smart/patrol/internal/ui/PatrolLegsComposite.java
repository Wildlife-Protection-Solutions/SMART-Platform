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

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.patrol.IPatrolEditContribution;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.PatrolLegStartDateComparator;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.editpatrol.EditPatrolDateLegsDialog;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Patrol item composite that modifies the patrol legs.  Allows users
 * to add, remove, and change patrol legs.
 * 
 * added merge leg option - Nov 2016 - Jeff
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolLegsComposite extends PatrolItemComposite{

	private DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
	
	private static final String START_INFO_LABEL = Messages.PatrolLegsComposite_PatrolStart_Label;
	private static final String END_INFO_LABEL = Messages.PatrolLegsComposite_PatrolEnd_Label;
	
	private Label lblDateInfo;
	private Label lblNewPatrol;
	private PatrolLegTable patrolLegViewer;
	private Patrol patrol;
	private Button btnMoveToNewPatrol; 
	
	private ArrayList<PatrolLeg> legs;
	
	private List<PatrolTransportType> typeOps ;
	private List<PatrolMandate> mandateOps ; 
	private List<Employee> allEmployes; 
	
	private LocalDate patrolStartDate;
	private LocalDate patrolEndDate;
	private boolean canEditDates = false;
	
	private Session session;
	
	private Link lnkEditDate;
	private Composite main;
	
	private ArrayList<Patrol> newPatrols = new ArrayList<>();
	private Map<PatrolLeg, PatrolLeg> newPatrolsLegMapping = new HashMap<>();

	private static final PatrolWaypointSource PATROL_WP_SRC = (PatrolWaypointSource) SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class)
			.getSource(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
	/**
	 * Creates a new patrol legs composite
	 * @param canEditDates true if the patrol dates can be changed, false if only legs can be modified
	 * @param currentSession - database session
	 */
	public PatrolLegsComposite(boolean canEditDates) {
		this.canEditDates = canEditDates;
	}
	
	/**
	 * 
	 * @return the initial size for the dialog or null
	 * if use default
	 */
	public Point getInitialSize(){
		try{
			int width = (int) (getShell().getBounds().width * 0.8);
			if (width < 500){
				width = 575;
			}
			int height = (int) (getShell().getBounds().height * 0.6);
			if (height < 350){
				height = 350;
			}
			return new Point(width, height);
		}catch (Exception ex){}
		return null;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	@Override
	public Composite createComponent(Composite parent, int style) {
		main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));

		if (canEditDates){
			Composite tmp = new Composite(main, SWT.NONE);
			tmp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
			tmp.setLayout(new GridLayout(2, false));
			((GridLayout)tmp.getLayout()).marginWidth = 0;
			((GridLayout)tmp.getLayout()).marginHeight = 0;
			lblDateInfo = new Label(tmp, SWT.NONE);
			lblDateInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			lnkEditDate = new Link(tmp, SWT.NONE);
			lnkEditDate.setText("<a>" + DialogConstants.EDIT_LINK_TEXT + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			lnkEditDate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			lnkEditDate.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					EditPatrolDateLegsDialog dialog = new EditPatrolDateLegsDialog(getShell(), patrolStartDate, patrolEndDate);
					if (dialog.open() == Window.OK){
						patrolStartDate = dialog.getStartDate();
						patrolEndDate = dialog.getEndDate();
						updateDateText();
						fireChangeListeners();
					}
				}
			});
		}else{
			lblDateInfo = new Label(main, SWT.NONE);
			lblDateInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
		patrolLegViewer = new PatrolLegTable();
		patrolLegViewer.createTable(main);

		
		Menu mnu = new Menu(patrolLegViewer.getTable().getControl());
		
		MenuItem miEditLeg = new MenuItem(mnu, SWT.PUSH);
		miEditLeg.setText(Messages.PatrolLegsComposite_EditLeg_Button);
		miEditLeg.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEditLeg.addListener(SWT.Selection, e->editLeg());
		miEditLeg.setEnabled(false);
		
		MenuItem miMergeLeg = new MenuItem(mnu, SWT.PUSH);
		miMergeLeg.setText(Messages.PatrolLegsComposite_MergeLegs);
		miMergeLeg.addListener(SWT.Selection, e->mergeLegs());
		miMergeLeg.setEnabled(false);
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem miDeleteLeg = new MenuItem(mnu, SWT.PUSH);
		miDeleteLeg.setText(Messages.PatrolLegsComposite_RemoveLeg_Button);
		miDeleteLeg.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDeleteLeg.addListener(SWT.Selection, e->deleteLeg());
		miDeleteLeg.setEnabled(false);
		
		patrolLegViewer.getTable().getControl().setMenu(mnu);
		
		Composite buttonPanel = new Composite(main, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(7, false));
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		
		final Button btnChangeTransport = new Button(buttonPanel, SWT.PUSH);
		btnChangeTransport.setText(Messages.PatrolLegsComposite_ChangeTransport_Button);
		btnChangeTransport.setSelection(false);
		btnChangeTransport.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnChangeTransport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolLeg pl =  (PatrolLeg)patrolLegViewer.getSelection().getFirstElement();
				if (pl == null){
					return;
				}
				PatrolTransportChangeDialog leaderDialg = new PatrolTransportChangeDialog(getShell(), 
						pl, legs, session);
				if (leaderDialg.open() == Window.OK){
					sortAndRefresh();
					fireChangeListeners();
				}
			}

		});
		
		final Button btnAddLeg = new Button(buttonPanel, SWT.PUSH);
		btnAddLeg.setText(Messages.PatrolLegsComposite_ChangeLeader_Button);
		btnAddLeg.setSelection(false);
		btnAddLeg.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAddLeg.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolLeg pl =  (PatrolLeg)patrolLegViewer.getSelection().getFirstElement();
				if (pl == null){
					return;
				}
				PatrolLegLeaderChangeDialog leaderDialg = new PatrolLegLeaderChangeDialog(getShell(),
						pl, legs);
				if (leaderDialg.open() == Window.OK){
					sortAndRefresh();
					fireChangeListeners();
				}
			}

		});
		
		
		final Button btnSplit = new Button(buttonPanel, SWT.PUSH);
		btnSplit.setText(Messages.PatrolLegsComposite_SplitPatrol_Button);
		btnSplit.setSelection(false);
		btnSplit.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnSplit.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolLeg pl =  (PatrolLeg)patrolLegViewer.getSelection().getFirstElement();
				if (pl == null){
					return;
				}
				PatrolLegSplitDialog splitDialog = new PatrolLegSplitDialog(getShell(), pl,
						typeOps, legs);
				if (splitDialog.open() == Window.OK){
					sortAndRefresh();
					fireChangeListeners();
				}
			}

		});
		
		final Button btnRemoveLeg = new Button(buttonPanel, SWT.PUSH);
		btnRemoveLeg.setText(Messages.PatrolLegsComposite_RemoveLeg_Button);
		btnRemoveLeg.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnRemoveLeg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				deleteLeg();
			}
		});
		
		final Button btnEditLeg = new Button(buttonPanel, SWT.PUSH);
		btnEditLeg.setText(Messages.PatrolLegsComposite_EditLeg_Button);
		btnEditLeg.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEditLeg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				editLeg();
			}
		});

		
		final Button btnmergeLegs = new Button(buttonPanel, SWT.PUSH);
		btnmergeLegs.setText(Messages.PatrolLegsComposite_MergeLegs);
		btnmergeLegs.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnmergeLegs.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				mergeLegs();
			}
		});

		if (canEditDates){
			btnMoveToNewPatrol = new Button(buttonPanel, SWT.PUSH);
			btnMoveToNewPatrol.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			btnMoveToNewPatrol.setText(Messages.PatrolLegsComposite_0);
			btnMoveToNewPatrol.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					IStructuredSelection selection = (IStructuredSelection)patrolLegViewer.getSelection();
					ArrayList<PatrolLeg> toBeMoved = new ArrayList<PatrolLeg>(); 
					Iterator<?> i = selection.iterator();
				    while ( i.hasNext() ) {
				    	toBeMoved.add( ((PatrolLeg)i.next()) );
				    }
				    MovePatrolLegDialog movePatrolLegDialog = new MovePatrolLegDialog(getShell(), toBeMoved);
					if (movePatrolLegDialog.open() == Window.OK){
						for(PatrolLeg pld : toBeMoved){
							legs.remove(pld);
						}
						sortAndRefresh();
						fireChangeListeners();
						//add the new patrol of a list of new ones so we can save it later (users can use the option multiple times beforing saving, we want to save them all).
						if(movePatrolLegDialog.getNewPatrol() != null) {
							newPatrols.add(movePatrolLegDialog.getNewPatrol());
							newPatrolsLegMapping.putAll(movePatrolLegDialog.getLegMapping());
						}
						lblNewPatrol.setText(Messages.PatrolLegsComposite_1);
					}
				}
			});
		}

		lblNewPatrol = new Label(main, SWT.NONE);
		lblNewPatrol.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		patrolLegViewer.getTable().addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				int numSelected =  ((IStructuredSelection)patrolLegViewer.getSelection()).size();
				btnChangeTransport.setEnabled(numSelected == 1);
				btnAddLeg.setEnabled(numSelected == 1);
				btnSplit.setEnabled(numSelected == 1);
				btnRemoveLeg.setEnabled((numSelected == 1) && legs.size() > 1);
				miDeleteLeg.setEnabled((numSelected == 1) && legs.size() > 1);
				btnEditLeg.setEnabled(numSelected == 1);
				miEditLeg.setEnabled(numSelected == 1);
				btnmergeLegs.setEnabled(numSelected > 1);
				miMergeLeg.setEnabled(numSelected > 1);
				if (btnMoveToNewPatrol != null) btnMoveToNewPatrol.setEnabled(numSelected > 0 && legs.size() > 1 && numSelected != legs.size());
			}
		});
		
		btnAddLeg.setEnabled(false );
		btnSplit.setEnabled(false );
		btnRemoveLeg.setEnabled(false );
		btnEditLeg.setEnabled( false );
		btnChangeTransport.setEnabled(false);
		btnmergeLegs.setEnabled(false);
		miEditLeg.setEnabled(false);
		miDeleteLeg.setEnabled(false);
		if (btnMoveToNewPatrol != null) btnMoveToNewPatrol.setEnabled(false);
		
		return main;
	}

	private void updateDateText() {
		lblDateInfo.setText(START_INFO_LABEL + ": " + dateFormatter.format(patrolStartDate) + "  " + END_INFO_LABEL + ": " + dateFormatter.format(patrolEndDate) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		lblDateInfo.getParent().getParent().layout(true, true);
	}
	
	private void mergeLegs() {
		IStructuredSelection selection = (IStructuredSelection)patrolLegViewer.getSelection();
		ArrayList<PatrolLeg> toBeMerged = new ArrayList<PatrolLeg>(); 
		Iterator<?> i = selection.iterator();
	    while ( i.hasNext() ) {
	    	toBeMerged.add( ((PatrolLeg)i.next()) );
	    }
		MergePatrolLegDialog patrolLegDialog = new MergePatrolLegDialog(getShell(), toBeMerged, typeOps, mandateOps);
		if (patrolLegDialog.open() == Window.OK){
			patrolLegDialog.getNewLeg().setPatrol(patrol);
			legs.add(patrolLegDialog.getNewLeg());
			for(PatrolLeg pld : toBeMerged){
				legs.remove(pld);
			}
			sortAndRefresh();
			fireChangeListeners();
		}
	}
	
	private void editLeg() {
		PatrolLeg toEdit = (PatrolLeg)((IStructuredSelection)patrolLegViewer.getSelection()).getFirstElement();
		EditPatrolLegDialog patrolLegDialog = new EditPatrolLegDialog(getShell(), toEdit, allEmployes, typeOps, mandateOps, patrolStartDate, patrolEndDate);
		if (patrolLegDialog.open() == Window.OK){
			sortAndRefresh();
			fireChangeListeners();
		}
	}
	
	private void deleteLeg() {
		if (MessageDialog.openConfirm(getShell(), Messages.PatrolLegsComposite_DeleteLeg_ConfirmDialog_Title, Messages.PatrolLegsComposite_DeleteLeg_ConfirmDialog_Message)){
			removeLeg();
			LocalDate[] dates = PatrolUtils.calculateDateRange(legs);
			patrolStartDate = dates[0];
			patrolEndDate = dates[1];
			updateDateText();
			PatrolUtils.createLegDaysForMissingDays(patrol, dates[0], dates[1], legs);
			sortAndRefresh();
			fireChangeListeners();
		}
	}
	
	private Shell getShell(){
		return main.getShell();
	}

	private void sortAndRefresh(){
		Collections.sort(legs, new PatrolLegStartDateComparator());
		patrolLegViewer.showPilotColum(isPilotColumnRequired());
		patrolLegViewer.refresh();
		patrolLegViewer.getTable().setSelection(null);
	}

	private boolean isPilotColumnRequired() {
		for (PatrolLeg leg : legs) {
			if (leg.getType().getPatrolType().getRequiresPilot()) {
				return true;
			}
		}
		return false;
	}
	
	private void removeLeg(){
		PatrolLeg toDelete = (PatrolLeg)((IStructuredSelection)patrolLegViewer.getSelection()).getFirstElement();
		toDelete.setPatrol(null);
		legs.remove(toDelete);
		sortAndRefresh();
		fireChangeListeners();
		
	}
	
	/**
	 * Clones all the existing patrol legs, and updates the ui components
	 * to contain the data provided by the patrol.
	 * 
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	@Override
	public void setValues(Patrol p, Session session) {
		this.session = session;
		if (p.getUuid() != null) {
			this.patrol = session.get(Patrol.class,p.getUuid());
		}else {
			this.patrol = p;
		}
		
		this.legs = new ArrayList<PatrolLeg>();
		
		//clone the legs
		for (PatrolLeg leg : p.getLegs()){
			leg.getPatrolLegDays().forEach(pd->Hibernate.initialize(pd.getWaypoints()));
			PatrolLeg tmpLeg = clonePatrolLeg(leg);
			
			//start time
			LocalDateTime date = null;
			if (leg.getPatrolLegDays().size() > 0){
				date = LocalDateTime.of(leg.getStartDate(), leg.getPatrolLegDays().get(0).getStartTime());	
			}else{
				date = leg.getStartDate().atStartOfDay();
			}
			tmpLeg.setStartDate(date.toLocalDate());
			tmpLeg.getPatrolLegDays().get(0).setStartTime(date.toLocalTime());
			
			//end time
			if (leg.getPatrolLegDays().size() > 0){
				date = LocalDateTime.of(leg.getEndDate(), leg.getPatrolLegDays().get(leg.getPatrolLegDays().size() - 1).getEndTime());
			}else{
				date = leg.getStartDate().atTime(LocalTime.MAX);
			}
			
			tmpLeg.setEndDate(date.toLocalDate());
			leg.getPatrolLegDays().get(leg.getPatrolLegDays().size() - 1).setEndTime(date.toLocalTime());
			
			this.legs.add(tmpLeg);
		}
		
		
		try {
			PatrolHibernateManager.computeAttachmentLocations(this.patrol, session);
		} catch (Exception e) {
			SmartPatrolPlugIn.displayLog(e.getMessage(), e);
		}
		
		session.beginTransaction();
		try{
			typeOps = PatrolHibernateManager.getActivePatrolTransporationTypes(patrol.getConservationArea(), session);
			mandateOps = PatrolHibernateManager.getActiveMandates(patrol.getConservationArea(), session);
			allEmployes = PatrolHibernateManager.getActiveEmployees(patrol.getConservationArea(), session);
			session.getTransaction().rollback();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.PatrolLegsComposite_Error_LoadingPatrolTypes1, ex);
			session.getTransaction().rollback();
		}
		patrolLegViewer.setInput(legs);
		
		this.patrolStartDate = LocalDate.from(patrol.getStartDate());
		this.patrolEndDate = LocalDate.from(patrol.getEndDate());
		updateDateText();
		
		patrolLegViewer.showPilotColum(patrol.hasPilot());
		sortAndRefresh();
	}

	private PatrolLeg clonePatrolLeg(PatrolLeg leg){
		PatrolLeg tmpLeg = leg.simpleClone();
		
		tmpLeg.setPatrolLegDays(new ArrayList<PatrolLegDay>());
		if (leg.getPatrolLegDays() != null && leg.getPatrolLegDays().size() > 0){
			//Clone Leg Days as well, now that we can merge we need more details on these.
			for (PatrolLegDay pld : leg.getPatrolLegDays()){
				PatrolLegDay clone = pld.clone();
				clone.setWaypoints(pld.getWaypoints()); //This is kinda bad as the waypoints are now associated with both clone and pld, but the only times 'clone' is saved we create new PatrolWaypoints objects, and the the pld is then deleted, so I think that makes it OK, maybe. Blame Jeff if not.
				clone.setPatrolLeg(tmpLeg);
				tmpLeg.getPatrolLegDays().add(clone);
			}
		}
		
		//uuid
		tmpLeg.setUuid(leg.getUuid());
		return tmpLeg;
	}
	
	private void doflush(Session session) {
		if (session.getTransaction().isActive()) session.flush();
	}
	private void doPersist(Session session, Object o) {
		if (session.getTransaction().isActive()) session.persist(o);
	}
	private void doRemove(Session session, Object o) {
		if (session.getTransaction().isActive()) session.remove(o);
	}
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public boolean updatePatrol(Patrol p, Session session) {
		boolean isNew = p.getUuid() == null;

		Set<UUID> existingWaypoints = new HashSet<>();
		Set<PatrolLeg> movedLegs = new HashSet<>();
		for (Patrol newp : newPatrols) {
			movedLegs.addAll(newp.getLegs());
		}
		
		try {
			if (!isNew) PatrolHibernateManager.computeAttachmentLocations(p, session);
			
			//track all existing waypoints so we can delete the ones
			//that don't get moved around
			for (PatrolLeg pl : p.getLegs()) {
				for (PatrolLegDay pld : pl.getPatrolLegDays()) {
					if (pld.getWaypoints() == null) continue;
					for (PatrolWaypoint pw : pld.getWaypoints()) {
						existingWaypoints.add(pw.getWaypoint().getUuid());
					}
				}
			}
		} catch (Exception e) {
			SmartPatrolPlugIn.displayLog(e.getMessage(), e);
			return false;
		}
		
		if (this.canEditDates){
			p.setStartDate(patrolStartDate);
			p.setEndDate(patrolEndDate);
		}

		doflush(session);
		
		
		List<PatrolLegDay> toDeleteDay = new ArrayList<>();
		
		if (!isNew) {
			//save the new patrol legs; doing this here allows other information
			//(links) to get copied over before items are deleted
			//waypoints are dealt with after all the other stuff is dealt with
			saveNewPatrolLegs(session);
		}
		
		//keep track of link between days and waypoints - we do this
		//so attachments don't get deleted when we delete patrol leg days
		//we delete the waypoints separately: see comments further down
		Map<PatrolLegDay, List<PatrolWaypoint>> oldWaypoints = new HashMap<>();
		for (PatrolLeg current : p.getLegs()) {
			for (PatrolLegDay currentday : current.getPatrolLegDays()) {
				if (currentday.getWaypoints() != null)
					oldWaypoints.put(currentday, new ArrayList<>(currentday.getWaypoints()));	
			}
		}
		
		//create leg objects from new configuration data
		List<PatrolLeg> newlegs = new ArrayList<>();
		for (PatrolLeg leg : legs) {
			PatrolLeg clone = new PatrolLeg();
			
			List<PatrolLegDay> legDeletes = new ArrayList<>();
			if (leg.getUuid() != null) {
				clone = session.get(PatrolLeg.class, leg.getUuid());
				//we are going to recreate the patrol leg days 
				for (PatrolLegDay pld : clone.getPatrolLegDays()) {
					
					//delete and clear these so attachments don't get remove
					//The WaypointAttachmentInterceptor is used for this session
					//which will delete any attachments associated with a patrol leg day
					//when the patrol leg day is removed
					//we don't want that as the waypoints may be assigned back to
					//a patrol leg day further down
					if (pld.getWaypoints() != null) {
						for (PatrolWaypoint pw : pld.getWaypoints()) {
							doRemove(session, pw);
						}
						pld.getWaypoints().clear();
					}
					toDeleteDay.add(pld);
					legDeletes.add(pld);
				}
				clone.getPatrolLegDays().clear();
				doflush(session);
			}else {
				
				clone.setPatrolLegDays(new ArrayList<>());
			}
			newlegs.add(clone);
			
			//clone.setUuid(leg.getUuid());
			clone.setEndDate(leg.getEndDate());
			clone.setStartDate(leg.getStartDate());
			clone.setId(leg.getId());
			clone.setType(leg.getType());
			clone.setMandate(leg.getMandate());
			
			clone.setPatrol(p);
			
			if (clone.getUuid() == null) {
				doPersist(session, clone);
				doflush(session);
				
				clone.setMembers(new ArrayList<>());
				for (PatrolLegMember m : leg.getMembers()) {
					PatrolLegMember cc = new PatrolLegMember();
					cc.setIsLeader(m.getIsLeader());
					cc.setIsPilot(m.getIsPilot());
					cc.setPatrolLeg(clone);
					cc.setMember(m.getMember());
					doPersist(session, cc);
					clone.getMembers().add(cc);					
				}
			}else {
				//merge members
				Set<Employee> currentMembers = new HashSet<>();
				for (PatrolLegMember m : clone.getMembers()) currentMembers.add(m.getMember());
				for (PatrolLegMember m : leg.getMembers()) {
					if (!currentMembers.contains(m.getMember())) {
						PatrolLegMember newMember = new PatrolLegMember();
						newMember.setMember(m.getMember());
						newMember.setPatrolLeg(clone);
						newMember.setIsLeader(m.getIsLeader());
						newMember.setIsPilot(m.getIsPilot());
						clone.getMembers().add(newMember);
					}
				}
				currentMembers.clear();
				for (PatrolLegMember m : leg.getMembers()) currentMembers.add(m.getMember());
				for (PatrolLegMember m : clone.getMembers()) {
					if (!currentMembers.contains(m.getMember())) {
						doRemove(session, m);
					}
				}
				
			}

			//create and persist leg days
			clone.createLegDays(session);
			
			for (PatrolLegDay pld : clone.getPatrolLegDays()) doPersist(session, pld);
			

			//configure leg day information and attempt to copy over waypoints
			//to new leg days
			
			//configure start times
			clone.getPatrolLegDays().get(0).setStartTime(leg.getPatrolLegDays().get(0).getStartTime());
			clone.getPatrolLegDays().get(clone.getPatrolLegDays().size()-1).setEndTime(leg.getPatrolLegDays().get(leg.getPatrolLegDays().size() - 1).getEndTime());
			
			//try to match rest minutes
			for (PatrolLegDay d : leg.getPatrolLegDays()) {
				
				for (PatrolLegDay current : clone.getPatrolLegDays()) {
					if (current.getDate().equals(d.getDate())) {
						current.setRestMinutes(d.getRestMinutes());
						break;
					}
				}
			} 
			
			for (PatrolLegDay current : clone.getPatrolLegDays()) {
				if (current.getEndTime().equals(LocalTime.MAX)) {
					current.setEndTime(LocalTime.of(23, 59, 59));
				}
			}
			doflush(session);
			
			
			//lets try to re-assign any waypoints to the correct day
			for (PatrolLegDay pd : legDeletes) {
				PatrolLegDay found = null;
				for(PatrolLegDay x : clone.getPatrolLegDays()){
					if (x.getDate().equals(pd.getDate())){
						found = x;
						break;
					}
				}
				
				if (found != null) {
					//don't process again later
					toDeleteDay.remove(pd);
					
					//move waypoints
					if (found.getWaypoints() == null) found.setWaypoints(new ArrayList<>());
					
					for (PatrolWaypoint pw : oldWaypoints.get(pd)) {
						PatrolWaypoint pwclone = new PatrolWaypoint();
						pwclone.setWaypoint(pw.getWaypoint());
						pwclone.setPatrolLegDay(found);
						found.getWaypoints().add(pwclone);
						doPersist(session, pwclone);
					}
					
					found.setTracks(new ArrayList<>());
					if (pd.getTracks() != null) {
						for (Track t : pd.getTracks()) {
							Track tclone = new Track();
							tclone.setDistance(t.getDistance());
							tclone.setGeom(t.getGeom());
							tclone.setPatrolLegDay(found);
							found.getTracks().add(tclone);
							doPersist(session, tclone);
						}
					}
				}
			}
			doflush(session);	
		}
				
		
		List<PatrolLeg> toDeleteLeg = new ArrayList<>();
		//list of legs to attempt to move waypoints out of before we delete
		List<PatrolLeg> toMoveLeg = new ArrayList<>();
		for (PatrolLeg existing : p.getLegs()) {
			PatrolLeg match = null;
			for (PatrolLeg newleg : newlegs) {
				if (existing.getUuid() != null && existing.getUuid().equals(newleg.getUuid())) {
					match = newleg;
					break;
				}
			}
			if (match != null) continue;
			
			//there is no new leg so flag this for deletion
			toDeleteLeg.add(existing);
			
			//check if the waypoints are part of the a new patrol
			//if they are we don't want to move the waypoints to another leg
			//in this patrol
			for (Entry<PatrolLeg, PatrolLeg> v : newPatrolsLegMapping.entrySet()) {
				if(existing.getUuid() != null &&  v.getValue().getUuid().equals(existing.getUuid())) {
					match = v.getKey();
					break;
				}				
			}
			if (match == null) {
				toMoveLeg.add(existing);
			}			
		}
		
		//attempt to move waypoints to appropriate day
		List<PatrolLegDay> toTryMove = new ArrayList<>(toDeleteDay);
		for (PatrolLeg pl : toMoveLeg) toTryMove.addAll(pl.getPatrolLegDays());
		for (PatrolLegDay delete : toTryMove) {
			//attempt to move waypoint to new day
			
			PatrolLegDay moveto = null;
			for(PatrolLeg pl : newlegs) {	
				for (PatrolLegDay d : pl.getPatrolLegDays()) {
					if (d.getDate().equals(delete.getDate())) {
						moveto = d;
						break;
					}
				}
				if (moveto != null) break;
			}
			if (moveto != null) {
				if (moveto.getWaypoints() == null) moveto.setWaypoints(new ArrayList<>());
				if (oldWaypoints.get(delete) != null) {
			
					for(PatrolWaypoint pw : oldWaypoints.get(delete)) {
						
						PatrolWaypoint pw2 = new PatrolWaypoint();
						pw2.setWaypoint(pw.getWaypoint());
						pw2.setPatrolLegDay(moveto);
						doPersist(session, pw2);
						moveto.getWaypoints().add(pw2);
					}	
					doflush(session);
				}
				
				if (moveto.getTracks() == null) moveto.setTracks(new ArrayList<>());
				
				if (delete.getTracks() != null) {
					for (Track t : delete.getTracks()) {
						Track tclone = new Track();
						tclone.setPatrolLegDay(moveto);
						tclone.setDistance(t.getDistance());
						tclone.setGeom(t.getGeom());
						doPersist(session, tclone);
						moveto.getTracks().add(tclone);
					}
					doflush(session);
				}
				
			}
		}
		
		for (PatrolLeg delete : toDeleteLeg) {
			//for attachments; see comments above
			for (PatrolLegDay pld : delete.getPatrolLegDays()) {
				if (pld.getWaypoints() == null) continue;
				for (PatrolWaypoint pw : pld.getWaypoints()) {
					doRemove(session, pw);
				}
				pld.getWaypoints().clear();
			}
			if (delete.getUuid() != null) doPersist(session, delete);
		}
		p.getLegs().removeAll(toDeleteLeg);
		
		
		doflush(session);
		
		//clean up attachments and orphaned waypoints
		//any waypoints do longer attached to a patrol need to deleted
		for (PatrolLeg pl : newlegs) {
			for (PatrolLegDay pld : pl.getPatrolLegDays()) {
				if (pld.getWaypoints() == null) continue;
				for(PatrolWaypoint pw : pld.getWaypoints()) {
					existingWaypoints.remove(pw.getWaypoint().getUuid());
				}
			}
		}
		if (isNew) {
			p.getLegs().addAll(newlegs);
		}
				
		if (!isNew) {
			saveNewPatrolWaypoints(session, existingWaypoints);
		}
		
		//any remaining waypoints were not moved or removed so delete this existing
		for (UUID u : existingWaypoints) {
			doRemove(session, session.get(Waypoint.class, u));
		}
		doflush(session);

		if (p.getUuid() == null) {
			p.recalculateType(session);
		}else {
			//evict patrol object and reload to ensure legs 
			//are recent before recalculating type
			//#3630
			session.evict(p);
			session.get(Patrol.class, p.getUuid()).recalculateType(session);
		}
		
		return true;			
	}

	private void saveNewPatrolLegs(Session session) {
		List<IPatrolEditContribution> contributions = IPatrolEditContribution.findContributions();
		for(Patrol p2 : newPatrols){
			//deal with any legs moved to new patrols
			//if we made brand new patrols, save them and copy any plan and intel links there were

			p2.createLegDays(session);
			
			doPersist(session, p2);		
			
			//save the waypoints, they are not cascaded like everything else.
			for(PatrolLeg pl : p2.getLegs()){
				PatrolLeg existing = null;
				//search the map here by entry as the 
				//uuids for the patrol leg get set above in the doPersist
				//setting which sets the uuid and invalids the map
				for (Entry<PatrolLeg, PatrolLeg> entry : newPatrolsLegMapping.entrySet()) {
					if (entry.getKey().equals(pl)) {
						existing = entry.getValue();
						break;
					}
				}
				for (IPatrolEditContribution c : contributions) {
					c.mergePatrolMovePatrolLeg(existing, pl, session);
				}
			}
		}
	}
	private void saveNewPatrolWaypoints(Session session, Set<UUID> existingWaypoints) {
		for(Patrol p2 : newPatrols){
			//save the waypoints, they are not cascaded like everything else.
			for(PatrolLeg pl : p2.getLegs()){
				for(PatrolLegDay pld : pl.getPatrolLegDays()) {
					if (pld.getWaypoints() == null) continue;
					
					for(PatrolWaypoint pwp : pld.getWaypoints()) {
						
						existingWaypoints.remove(pwp.getWaypoint().getUuid());
						
						if (session.getTransaction().isActive()) pwp = session.merge(pwp);
						
						//re-create attachments as files need to move
						List<WaypointAttachment> newAttachments = new ArrayList<>();
						for (WaypointAttachment ws : pwp.getWaypoint().getAttachments()) {
							WaypointAttachment clone = new WaypointAttachment();
							clone.setCopyFromLocation(ws.getAttachmentFile());
							clone.setFilename(ws.getFilename());
							clone.setWaypoint(pwp.getWaypoint());
							clone.computeFileLocation(Paths.get(p2.getConservationArea().getFileDataStoreLocation())
									.resolve(PATROL_WP_SRC.getDatastoreFileLocation(p2))
									.resolve(clone.getFilename()));
							newAttachments.add(clone);
							doPersist(session,clone);
						}
						for (WaypointAttachment ws : pwp.getWaypoint().getAttachments()) {
							WaypointAttachment delete = session.get(WaypointAttachment.class, ws.getUuid());
							delete.getWaypoint().getAttachments().remove(delete);
							doRemove(session, delete);
						}
						pwp.getWaypoint().getAttachments().clear();
						pwp.getWaypoint().getAttachments().addAll(newAttachments);
							
						//do the same thing for observation attachments
						for (WaypointObservationGroup group : pwp.getWaypoint().getObservationGroups()) {
							for (WaypointObservation wo : group.getObservations()) {
								List<ObservationAttachment> newAttachments2 = new ArrayList<>();
								for (ObservationAttachment ws : wo.getAttachments()) {
									ObservationAttachment clone = new ObservationAttachment();
									clone.setCopyFromLocation(ws.getAttachmentFile());
									clone.setFilename(ws.getFilename());
									clone.setObservation(wo);
									clone.computeFileLocation(Paths.get(p2.getConservationArea().getFileDataStoreLocation())
											.resolve(PATROL_WP_SRC.getDatastoreFileLocation(p2))
											.resolve(clone.getFilename()));
									newAttachments2.add(clone);
									
									doPersist(session, clone);
								}
								for (ObservationAttachment ws : wo.getAttachments()) {
									ObservationAttachment delete = session.get(ObservationAttachment.class, ws.getUuid());
									delete.getObservation().getAttachments().remove(delete);
									doRemove(session, delete);										
								}
								wo.getAttachments().clear();
								wo.getAttachments().addAll(newAttachments2);
							}
						}
						doflush(session);
						
					}
				}
			}
			
			//copy the intel and plan links to the new patrol as well
			//do this by running the contribution from each plug-in that has links to patrols (intel and plan at the time of writing)
			for(IPatrolEditContribution c : IPatrolEditContribution.findContributions()){
				c.splitPatrol(session, patrol, p2);
			}
			
			lblNewPatrol.setText(Messages.PatrolLegsComposite_2);
		}
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return Messages.PatrolLegsComposite_Title;
	}

	public int getLegCount(){
		if (legs == null){ return 0; }
		return legs.size();
	}
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getErrorMessage()
	 */
	@Override
	public String getErrorMessage(){
		
		/* ensure each patrol leg between the patrol start and end */
		for (Iterator<?> iterator = legs.iterator(); iterator.hasNext();) {
			PatrolLeg legA = (PatrolLeg) iterator.next();
			
			if (legA.getStartDate().isAfter(patrolEndDate)){
				return MessageFormat.format(Messages.PatrolLegsComposite_LegError_A, new Object[]{legA.getId()});
			}
			if (legA.getStartDate().isBefore(patrolStartDate)){
				return MessageFormat.format(Messages.PatrolLegsComposite_LegError_B, new Object[]{legA.getId()});
			}
			
			if (legA.getEndDate().isAfter(patrolEndDate)){
				return MessageFormat.format(Messages.PatrolLegsComposite_LegError_C, new Object[]{legA.getId()});
			}
			if (legA.getEndDate().isBefore(patrolStartDate)){
				return MessageFormat.format(Messages.PatrolLegsComposite_LegError_D, new Object[]{legA.getId()});
			}
		}
		
		/* Ensure that members are not in two places at the same time */
		for (Iterator<?> iterator = legs.iterator(); iterator.hasNext();) {
			PatrolLeg legA = (PatrolLeg) iterator.next();
		
			for (Iterator<?> iterator2 = legs.iterator(); iterator2.hasNext();) {
				PatrolLeg legB = (PatrolLeg) iterator2.next();
				if (legA.equals(legB)){
					continue;
				}else if(! ( (legB.getEndDate().isBefore(legA.getStartDate()) || legB.getEndDate().isEqual(legA.getStartDate())) ||
						(legB.getStartDate().isAfter(legA.getEndDate()) ||legB.getStartDate().isEqual(legA.getEndDate())   ) )){
					
					//legs overlap ensure members don't
					HashSet<Employee> bMembers = new HashSet<Employee>();
					for (PatrolLegMember member : legB.getMembers()){
						bMembers.add(member.getMember());
					}
					for (PatrolLegMember member : legA.getMembers()){
						if (bMembers.contains(member.getMember())){
							return MessageFormat.format(Messages.PatrolLegsComposite_LegError_E, 
								new Object[]{SmartLabelProvider.getFullLabel(member.getMember()), legA.getId(), legB.getId() });
						}
					}
				}
			}
		}
		
		/* ensure there is at least one leg for each day in the patrol */
		LocalDate working = LocalDate.from(patrolStartDate);
		while (working.isBefore(patrolEndDate)|| working.isEqual(patrolEndDate)){
			boolean found = false;
			for (Iterator<?> iterator = legs.iterator(); iterator.hasNext();) {
				PatrolLeg leg = (PatrolLeg) iterator.next();
				
				if ( (working.isBefore(leg.getEndDate()) || working.isEqual(leg.getEndDate())) && 
						(working.isAfter(leg.getStartDate()) ||working.isEqual(leg.getStartDate())) ){
					found = true;
					break;
				}
				
			}
			if (!found){
				return MessageFormat.format(Messages.PatrolLegsComposite_Error_MissingLegDay,
						new Object[]{ DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(working) });
			}		
			working = ChronoUnit.DAYS.addTo(working, 1);
		}		
		return null;		
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_DATES_LEG;
	}
}

