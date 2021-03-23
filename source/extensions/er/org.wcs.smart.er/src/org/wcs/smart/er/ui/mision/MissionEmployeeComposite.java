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
package org.wcs.smart.er.ui.mision;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.EmployeeTeam;
import org.wcs.smart.common.control.MultipleSelectComposite;
import org.wcs.smart.common.control.MultipleSelectComposite.IListChanged;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.ui.EmployeeLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Mission members composite.
 * 
 * @author Emily
 *
 */
public class MissionEmployeeComposite extends MissionComposite {

	private MultipleSelectComposite<Employee> composite;
	private ComboViewer leaderViewer = null;
	private Composite warnComp;
	private Label errorlbl;
	private HashSet<Employee> observers ;
	
	public MissionEmployeeComposite() {
	}
	
	
	@Override
	public Control createControl(Composite parent) {

		Composite c = new Composite(parent, SWT.NONE);
		
		c.setLayout(new GridLayout(1, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		warnComp = new Composite(c, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = gl.marginHeight = 0;
		warnComp.setLayout(gl);
		warnComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Label l = new Label(warnComp, SWT.NONE);
		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		errorlbl = new Label(warnComp, SWT.NONE);
		errorlbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		warnComp.setVisible(false);
		
		composite = new EmployeeSelectComposite(c, SWT.NONE);
		composite.setLabelProvider(EmployeeLabelProvider.getInstance());
		composite.setLabelAllText(Messages.MissionEmployeeComposite_AllEmployeesLabel);
		composite.setLabelSelectedText(Messages.MissionEmployeeComposite_MissionMemberLabel);
		composite.setItemComparator(new Comparator<Employee>() {
			@Override
			public int compare(Employee o1, Employee o2) {
				return Collator.getInstance().compare(
						SmartLabelProvider.getFullLabel(o1), 
						SmartLabelProvider.getFullLabel(o2));
			}
		});
		composite.addSelectionChangedListener(new IListChanged<Employee>() {
			@Override
			public void listChanged(List<Employee> items) {
				updateLeaderInput(items);
				fireChangeListeners();
			}
		});
		
		Composite leaderCmp = new Composite(c, SWT.NONE);
		leaderCmp.setLayout(new GridLayout(2, false));
		leaderCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lbl = new Label(leaderCmp, SWT.NONE);
		lbl.setText(Messages.MissionEmployeeComposite_MissionLeader);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		leaderViewer = new ComboViewer(leaderCmp, SWT.DROP_DOWN | SWT.READ_ONLY);
		leaderViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		leaderViewer.setContentProvider(new ArrayContentProvider());
		leaderViewer.setLabelProvider(EmployeeLabelProvider.getInstance());
		leaderViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();	
			}
		});
		
		return c;
	}

	
	protected void updateLeaderInput(List<Employee> items) {
		IStructuredSelection selection = (IStructuredSelection)leaderViewer.getSelection();
		Object element = null;
		if (selection != null) {
			element = selection.getFirstElement();
		}
		leaderViewer.setInput(items.toArray());
		leaderViewer.setSelection(items.contains(element) ? new StructuredSelection(element) : null);
	}


	@Override
	public void init(Mission mission, Session session) {
		List<Employee> all = HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), session);
		List<Employee> selected = new ArrayList<Employee>();
		Employee leader = null;
		
		if (mission.getMembers() != null){
			for (MissionMember mm : mission.getMembers()){
				selected.add(mm.getMember());
				all.remove(mm.getMember());
				if (mm.getIsLeader()) {
					leader = mm.getMember();
				}
			}
		}
		
		composite.setItemsData(all, selected);
		
		leaderViewer.setInput(selected.toArray());
		if (leader != null) {
			leaderViewer.setSelection(new StructuredSelection(leader));
		}
		
		//find all observers
		observers = new HashSet<Employee>();
		if (mission.getUuid() != null){
			Query<?> q = session.createQuery("SELECT distinct wpo.observer from WaypointObservation wpo, SurveyWaypoint sw WHERE wpo.observationGroup.waypoint = sw.id.waypoint and sw.missionDay.mission = :mission"); //$NON-NLS-1$
			q.setParameter("mission", mission); //$NON-NLS-1$
			for (Object x : q.list()) observers.add((Employee)x);
		}else{
			if (warnComp != null){
				warnComp.dispose();
				warnComp = null;
			}
		}
	}

	@Override
	public void updateDesign(Mission mission) {
		if (mission.getMembers() == null){
			mission.setMembers(new ArrayList<MissionMember>());
		}
		
		List<Employee> copy = composite.getSelectedItemsAsList();
		
//		// remove existing members
		List<MissionMember> toDelete = new ArrayList<MissionMember>();
		for (MissionMember mm: mission.getMembers()){
			if (!copy.contains(mm.getMember())){
				toDelete.add(mm);
			}else{
				copy.remove(mm.getMember());
			}
		}
		
		mission.getMembers().removeAll(toDelete);
		for (MissionMember mm : toDelete){
			mm.setId(null);
		}

		//add new members
		for(Employee e : copy) {
			MissionMember mm = new MissionMember();
			mm.setMember(e);
			mm.setMission(mission);
			mission.getMembers().add(mm);
		}

		
		IStructuredSelection selection = (IStructuredSelection)leaderViewer.getSelection();
		Employee leader = selection != null ? (Employee) selection.getFirstElement() : null;
		for (MissionMember mm : mission.getMembers()) {
			if (mm.getMember().equals(leader)) {
				mission.setLeader(mm);
				break;
			}
		}		
	}

	@Override
	public boolean isValid() {
		
		if (warnComp != null){
			warnComp.setVisible(false);
			List<Employee> selected =  composite.getSelectedItemsAsList();
			if (observers != null){
				for (Employee observer : observers){
					if (!selected.contains(observer)){
						warnComp.setVisible(true);
						errorlbl.setText(MessageFormat.format(Messages.MissionEmployeeComposite_ObserverError, new Object[]{SmartLabelProvider.getFullLabel(observer)}));
						errorlbl.setToolTipText(MessageFormat.format(Messages.MissionEmployeeComposite_ObserverErrorTooltip, new Object[]{SmartLabelProvider.getFullLabel(observer)}));
						warnComp.layout(true);
						return false;
					}
				}
			}
		}
		
		IStructuredSelection selection = (IStructuredSelection)leaderViewer.getSelection();
		if (!(composite.getSelectedItemsAsList().size() > 0 && 
				selection != null && selection.getFirstElement() != null)) {
			return false;
		}

		return true;
	}

	@Override
	public String getTitle() {
		return Messages.MissionEmployeeComposite_Title;
	}

	@Override
	public String getDescription() {
		return Messages.MissionEmployeeComposite_Description;
	}

	private class EmployeeSelectComposite extends MultipleSelectComposite<Employee> {

		private ComboViewer cmbTeams;
		private List<Employee> nonFilteredItems = null;
		
		public EmployeeSelectComposite(Composite parent, int style) {
			super(parent, style);
		}

		protected void contributeToFromLabelSection(Composite parent) {
			cmbTeams = new ComboViewer(new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER));
			cmbTeams.getControl().setToolTipText(Messages.MissionEmployeeComposite_filtertooltip);
			cmbTeams.setContentProvider(ArrayContentProvider.getInstance());
			cmbTeams.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					if (element instanceof EmployeeTeam) return ((EmployeeTeam)element).getName();
					return super.getText(element);
				}
			});
			cmbTeams.getControl().setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
			((GridData)cmbTeams.getControl().getLayoutData()).widthHint = 150;
			cmbTeams.setInput(Collections.singletonList(DialogConstants.LOADING_TEXT));
			// nothing by default
			
			Job j = new Job(Messages.MissionEmployeeComposite_loadingteamsjob) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<Object> teams = new ArrayList<>();
					teams.add(Messages.MissionEmployeeComposite_AllEmployees);
					try(Session session = HibernateManager.openSession()){
						List<EmployeeTeam> items = QueryFactory.buildQuery(session, EmployeeTeam.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
						items.forEach(i->i.getMembers().forEach(m->m.getEmployee().getGivenName()));
						teams.addAll(items);
					}
					Display.getDefault().syncExec(()->{
						cmbTeams.setInput(teams);
						cmbTeams.setSelection(new StructuredSelection(teams.get(0)));
						cmbTeams.getControl().getParent().getParent().layout(true);
					});
					return Status.OK_STATUS;
				}
				
			};
			j.schedule();
			
			cmbTeams.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (nonFilteredItems == null) nonFilteredItems = EmployeeSelectComposite.this.allItems;
					
					Object x = cmbTeams.getStructuredSelection().getFirstElement();
					if (x instanceof EmployeeTeam) {
						List<Employee> choices = ((EmployeeTeam)x).getMembers().stream().map(e->e.getEmployee()).collect(Collectors.toList());
						EmployeeSelectComposite.this.setItemsData(choices, getSelectedItemsAsList());
					}else {
						//all members
						if (nonFilteredItems != null) EmployeeSelectComposite.this.setItemsData(nonFilteredItems, getSelectedItemsAsList());
					}
				}
			});
		}

		protected void createButtonComposite(Composite btnComposite) {
			Button btnAddAll = new Button(btnComposite, SWT.PUSH);
			btnAddAll.setText(Messages.MissionEmployeeComposite_AddAllButton);
			btnAddAll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			btnAddAll.setBackground(btnAddAll.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			btnAddAll.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					List<Employee> items = getSelectedItemsAsList();
					EmployeeSelectComposite.this.allItems.forEach(next->items.add(next));
					 EmployeeSelectComposite.this.setItemsData(EmployeeSelectComposite.this.allItems, items);
				}

			});
			
			super.createButtonComposite(btnComposite);
		}
	}
}
