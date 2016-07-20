package org.wcs.smart.connect.dataqueue.cybertracker.patrol;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.hibernate.Session;
import org.wcs.smart.connect.dataqueue.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.ui.PatrolFilteredComboViewer;
import org.wcs.smart.ui.SmartLabelProvider;

public class PatrolDialog extends TitleAreaDialog {

	private HashMap<UUID, Patrol> patrols;
	
	private HashMap<UUID, UiData> uiItems;
	
	private Session session;
	
	public PatrolDialog(Shell parentShell, HashMap<UUID, Patrol> patrols, Session session) {
		super(parentShell);
		this.patrols = patrols;
		this.session = session;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	public void okPressed(){
	
		//validate();
		try{
			for (Entry<UUID, UiData> e : uiItems.entrySet()){
				if (e.getValue().btnExisting.getSelection()){
					mergePatrol(e.getKey(), patrols.get(e.getValue()), e.getValue().cmbPatrol.getSelection());
				}else{
					createNewPatrol(e.getKey(), patrols.get(e.getKey()));
				}
			}
		}catch (Exception ex){
			ex.printStackTrace();
			MessageDialog.openWarning(getShell(), "Error", "Error saving results");
			super.cancelPressed();
			return;
		}
		super.okPressed();
	}
	
	private void mergePatrol(UUID ctUuid, Patrol newPatrol, Patrol addToPatrol) throws Exception{
		if (newPatrol.getPatrolType().equals(addToPatrol.getPatrolType())){
			System.out.println("Cannot merge patrols of different types");
		}
		
		PatrolLeg toAdd= newPatrol.getFirstLeg();
		addToPatrol.getLegs().add(toAdd);
		toAdd.setPatrol(addToPatrol);
		
//		updateReferences(newPatrol);
		PatrolHibernateManager.savePatrol(newPatrol, session, true);
		
	}
	
//	private void updateReferences(Patrol p){
//		for (PatrolLeg pl : p.getLegs()){
//			for (PatrolLegDay day : pl.getPatrolLegDays()){
//				for (PatrolWaypoint wp : day.getWaypoints()){
//					for (WaypointObservation wo : wp.getWaypoint().getObservations()){
//						wo.setWaypoint(wp.getWaypoint());
//					}
//				}
//			}
//		}
//	}

	private void createNewPatrol(UUID ctUuid, Patrol newPatrol) throws Exception{
		newPatrol.setConservationArea(SmartDB.getCurrentConservationArea());
		newPatrol.setStartDate(newPatrol.getFirstLeg().getStartDate());
		newPatrol.setEndDate(newPatrol.getFirstLeg().getEndDate());
		
		newPatrol.setId(PatrolHibernateManager.generatePatrolId(newPatrol, session));
		if (newPatrol.getPatrolType() == null){
			if (newPatrol.getFirstLeg().getType() != null){
				newPatrol.setPatrolType(newPatrol.getFirstLeg().getType().getPatrolType());
			}else{
				//TODO: error
				//no patrol type 
			}
		}
//		updateReferences(newPatrol);
		PatrolHibernateManager.savePatrol(newPatrol, session, true);
		
		CtPatrolLink link = new CtPatrolLink();
		link.setCtUuid(ctUuid);
		link.setPatrolLeg(newPatrol.getFirstLeg());
		
		session.save(link);
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		// add controls to composite as necessary
		
		ScrolledComposite scroll = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite main = new Composite(scroll, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		
		
		uiItems = new HashMap<UUID, PatrolDialog.UiData>();
		
		for (Entry<UUID, Patrol> e : patrols.entrySet()){
			Label l = new Label(main, SWT.NONE);
			StringBuilder lbl = new StringBuilder();
			Patrol p = e.getValue();
			lbl.append(p.getStartDate() == null ? "NO START DATE " : p.getStartDate().toString());
			lbl.append( " ");
			lbl.append(p.getPatrolType() == null ? " No Patrol Type " : e.getValue().getPatrolType());
			lbl.append( " ");
			lbl.append(p.getFirstLeg() == null || p.getFirstLeg().getType() == null ? " No transport type" : p.getFirstLeg().getType().getName());
			lbl.append( " ");
			lbl.append(p.getFirstLeg() == null || p.getFirstLeg().getLeader() == null ? "No Leader" : SmartLabelProvider.getShortLabel(p.getFirstLeg().getLeader().getMember()));
			
			l.setText(lbl.toString());
			Composite op= new Composite(main, SWT.NONE);
			op.setLayout(new GridLayout(3, false));
			Button btnNew = new Button(op, SWT.RADIO);
			btnNew.setText("Create New Patrol");
			btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
			btnNew.setSelection(true);
			
			Button btnExisting = new Button(op, SWT.RADIO);
			btnExisting.setText("Add To Existing Patrol");
			btnExisting.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
			btnExisting.setSelection(false);
			
			PatrolFilteredComboViewer viewer = new PatrolFilteredComboViewer(op);
			viewer.setEnabled(false);
			
			final Patrol imported = e.getValue();
			SelectionListener listener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					viewer.setEnabled(btnExisting.getSelection());
//					validate(viewer, imported);
				}
			};
			
			btnNew.addSelectionListener(listener);
			btnExisting.addSelectionListener(listener);
			
			uiItems.put(e.getKey(), new UiData(btnNew, btnExisting, viewer));
			Label spacer = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
			spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		}
		scroll.setContent(main);
		scroll.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setTitle("Import CyberTracker Data");
		setMessage("The following CyberTracker data needs to be identified as new patrols or new legs");
		getShell().setText("CyberTracker Data Import");
		return composite;

	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
	private class UiData{
		Button btnNew;
		Button btnExisting;
		PatrolFilteredComboViewer cmbPatrol;
		
		public UiData(Button btnNew, Button btnExisting, PatrolFilteredComboViewer cmbPatrol){
			this.btnNew = btnNew;
			this.btnExisting = btnExisting;
			this.cmbPatrol = cmbPatrol;
		}
		
	}
}
