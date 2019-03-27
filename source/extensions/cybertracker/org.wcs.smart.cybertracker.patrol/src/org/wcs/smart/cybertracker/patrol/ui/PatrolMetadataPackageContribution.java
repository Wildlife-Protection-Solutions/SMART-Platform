/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Patrol metadata contribution
 * 
 * @author Emily
 *
 */
public class PatrolMetadataPackageContribution implements IPackageUiContribution {

	private Button btnTT, btnArmed, btnTeam, btnStation, btnMandate, btnObj, btnCmt, btnMembers, btnLeader, btnPilot, btnArmedYes, btnArmedNo;
	private ComboViewer cmbTt, cmbTeam, cmbStation, cmbMandate, cmbLeader, cmbPilot;
	private Text txtObj, txtComment;
	
	private CheckboxTableViewer lstEmployees;
	private Listener onModified;
	
	private void fireChanged() {
		if (onModified != null) onModified.handleEvent(new Event());
	}
	
	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified) {
		
		this.onModified = onModified;
		
		Composite outer = new Composite(parent, SWT.BORDER);
		outer.setLayout(new GridLayout());
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight = 0;
		((GridLayout)outer.getLayout()).verticalSpacing = 0;
		
		Composite headerComp = new Composite(outer, SWT.NONE);
		headerComp.setLayout(new GridLayout(4, false));
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label col1 = new Label(headerComp, SWT.NONE);
		col1.setText("Field");
		col1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

		Label col2 = new Label(headerComp, SWT.NONE);
		col2.setText("Show Option");
		col2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label col3 = new Label(headerComp, SWT.NONE);
		col3.setText("Default Value");
		col3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		ScrolledComposite table = new ScrolledComposite(outer, SWT.V_SCROLL);
		table.setExpandHorizontal(true);
		table.setExpandVertical(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite core = new Composite(table, SWT.NONE);
		core.setLayout(new GridLayout(4, false));
		table.setContent(core);
		
		Label c1 = new Label(core, SWT.NONE);
		c1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)c1.getLayoutData()).heightHint = 0;
		
		Label c2 = new Label(core, SWT.NONE);
		c2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)c2.getLayoutData()).heightHint = 0;
		
		Label c3 = new Label(core, SWT.NONE);
		c3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)c3.getLayoutData()).heightHint = 0;
		
		Label c4 = new Label(core, SWT.NONE);
		c4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)c4.getLayoutData()).heightHint = 0;
		
		// transport type
		Object[] d = createComboViewerSection("Transport Type", core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.MIXED_PATROL_ICON));
		btnTT = (Button) d[0];
		cmbTt = (ComboViewer) d[1];
		
		// armed
		Label l = new Label(core, SWT.NONE);
		l.setImage(SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ARMED_ICON));
				
		l = new Label(core, SWT.NONE);
		l.setText("Is Armed");
		
		btnArmed = new Button(core, SWT.CHECK);
		btnArmed.setSelection(true);
		btnArmed.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		Composite cmp = new Composite(core, SWT.NONE);
		cmp.setLayout(new GridLayout(2, false));
		
		btnArmedYes = new Button(cmp, SWT.RADIO);
		btnArmedYes.setText("yes");
		btnArmedYes.setEnabled(false);
		btnArmedNo = new Button(cmp, SWT.RADIO);
		btnArmedNo.setText("no");
		btnArmedNo.setSelection(true);
		btnArmedNo.setEnabled(false);
		btnArmed.addListener(SWT.Selection, e->{
			btnArmedYes.setEnabled(!btnArmed.getSelection());
			btnArmedNo.setEnabled(!btnArmed.getSelection());
			fireChanged();
		});
		btnArmedYes.addListener(SWT.Selection, e->fireChanged());
		btnArmedNo.addListener(SWT.Selection, e->fireChanged());
		
		// team
		d = createComboViewerSection("Team", core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_TEAM_ICON));
		btnTeam = (Button) d[0];
		cmbTeam = (ComboViewer) d[1];
		
		// station
		d = createComboViewerSection("Station", core, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.STATION_ICON));
		btnStation = (Button) d[0];
		cmbStation = (ComboViewer) d[1];
		
		// mandate
		d = createComboViewerSection("Patrol Mandate", core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MANDATE_ICON));
		btnMandate = (Button) d[0];
		cmbMandate = (ComboViewer) d[1];
				
		// objective
		d = createTextSection("Patol Objective", core, null);
		btnObj = (Button) d[0];
		txtObj = (Text) d[1];
		
		// comment
		d = createTextSection("Patol Comment", core, null);
		btnObj = (Button) d[0];
		txtComment = (Text) d[1];		
		
		
		LabelProvider employeeLblProvider = new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Employee) return SmartLabelProvider.getShortLabel((Employee)element);
				return super.getText(element);
			}
		};
		
		// employess
		l = new Label(core, SWT.NONE);
		l.setImage(SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MEMBER_ICON));
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		l = new Label(core, SWT.NONE);
		l.setText("Members");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		btnMembers = new Button(core, SWT.CHECK);
		btnMembers.setSelection(true);
		btnMembers.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));

		lstEmployees = CheckboxTableViewer.newCheckList(core, SWT.BORDER);
		lstEmployees.setContentProvider(ArrayContentProvider.getInstance());
		lstEmployees.setLabelProvider(employeeLblProvider);
		lstEmployees.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lstEmployees.getControl().setEnabled(false);
		((GridData)lstEmployees.getControl().getLayoutData()).heightHint = 80;
		
		// leader
		d = createComboViewerSection("Patrol Leader", core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_LEADER_ICON));
		btnLeader = (Button) d[0];
		cmbLeader = (ComboViewer) d[1];
		cmbLeader.setLabelProvider(employeeLblProvider);
		btnLeader.setEnabled(false);
		cmbLeader.getControl().setEnabled(false);
		// pilot
		d = createComboViewerSection("Patrol Pilot", core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_PILOT_ICON));
		btnPilot = (Button) d[0];
		cmbPilot = (ComboViewer) d[1];
		cmbPilot.setLabelProvider(employeeLblProvider);
		btnPilot.setEnabled(false);
		cmbPilot.getControl().setEnabled(false);
		
		btnMembers.addListener(SWT.Selection, e->{
			lstEmployees.getControl().setEnabled(!btnMembers.getSelection());
			
			if (btnMembers.getSelection()) {
				btnLeader.setEnabled(false);
				btnPilot.setEnabled(false);
				cmbLeader.getControl().setEnabled(false);
				cmbPilot.getControl().setEnabled(false);
			}else {
				btnLeader.setEnabled(true);
				btnPilot.setEnabled(true);
				cmbLeader.getControl().setEnabled(!btnLeader.getSelection());
				cmbPilot.getControl().setEnabled(!btnPilot.getSelection());
			}
			fireChanged();
		});
		lstEmployees.addCheckStateListener(event->{
			List<Employee> items = new ArrayList<>();
			for (Object o : lstEmployees.getCheckedElements()) {
				if (o instanceof Employee) items.add((Employee) o);
			}
			cmbLeader.setInput(items);
			cmbPilot.setInput(items);
			if (cmbLeader.getStructuredSelection().isEmpty()) cmbLeader.setSelection(new StructuredSelection(items.get(0)));
			if (cmbPilot.getStructuredSelection().isEmpty()) cmbPilot.setSelection(new StructuredSelection(items.get(0)));
		});
		
		table.setMinSize(core.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		core.layout(true);
		((GridData)col1.getLayoutData()).widthHint = c1.getSize().x + c2.getSize().x + 5;
		((GridData)c3.getLayoutData()).widthHint = col2.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
	
		loadValues.schedule();
		
		return outer;
	}
	
	private Object[] createComboViewerSection(String sectionName, Composite parent, Image icon) {
		Composite g = parent;
		
		Label l = new Label(g, SWT.NONE);
		l.setImage(icon);
		
		l = new Label(g, SWT.NONE);
		l.setText(sectionName);

		Button btn = new Button(g, SWT.CHECK);
		btn.setSelection(true);
		btn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		ComboViewer cmb = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmb.setContentProvider(ArrayContentProvider.getInstance());
		cmb.setLabelProvider(new NamedItemLabelProvider());
		cmb.setInput(new String[] { DialogConstants.LOADING_TEXT });
		cmb.getControl().setEnabled(false);
		cmb.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btn.addListener(SWT.Selection, e-> {
			cmb.getControl().setEnabled(!btn.getSelection());
			fireChanged();
		});
		
		cmb.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		cmb.addSelectionChangedListener(e->fireChanged());
		
		return new Object[] {btn, cmb};
	}
	
	private Object[] createTextSection(String sectionName, Composite parent, Image icon) {
		Composite g = parent;
		
		Label l = new Label(g, SWT.NONE);
		l.setImage(icon);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		l = new Label(g, SWT.NONE);
		l.setText(sectionName);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

		Button btn = new Button(g, SWT.CHECK);
		btn.setSelection(true);
		btn.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));

		Text txt = new Text(g, SWT.BORDER);
		txt.setEnabled(false);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txt.getLayoutData()).heightHint = 40;
		
		btn.addListener(SWT.Selection, e-> {
			txt.setEnabled(!btn.getSelection());
			fireChanged();
		});
		
		txt.addListener(SWT.Modify, e->fireChanged());
		
		return new Object[] {btn, txt};
	}
	@Override
	public String isValid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public boolean isTab() { return true; }
	
	@Override
	public String getTabName() { return "Patrol Metadata"; }
	
	Job loadValues = new Job("loading metadata values") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			List<PatrolTransportType> types = new ArrayList<>();
			List<Team> teams = new ArrayList<>();
			List<Station> stations = new ArrayList<>();
			List<PatrolMandate> mandates = new ArrayList<>();

			List<Employee> employees = new ArrayList<>();
			
			try(Session s = HibernateManager.openSession()){
				
				types.addAll(QueryFactory.buildQuery(s, PatrolTransportType.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"isActive", true})
						.list());
			
				teams.addAll(QueryFactory.buildQuery(s, Team.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"isActive", true}).list());
				
				stations.addAll(QueryFactory.buildQuery(s, Station.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"isActive", true}).list());
				
				mandates.addAll(QueryFactory.buildQuery(s, PatrolMandate.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"isActive", true}).list());
				
				employees.addAll(QueryFactory.buildQuery(s, Employee.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"endEmploymentDate", null}).list());
				
			}
			
			Display.getDefault().syncExec(()->{
				cmbTt.setInput(types);
				cmbTt.setSelection(new StructuredSelection(types.get(0)));
				
				cmbTeam.setInput(teams);
				cmbTeam.setSelection(new StructuredSelection(teams.get(0)));
				
				cmbStation.setInput(stations);
				cmbStation.setSelection(new StructuredSelection(stations.get(0)));
				
				cmbMandate.setInput(mandates);
				cmbMandate.setSelection(new StructuredSelection(mandates.get(0)));
				
				cmbPilot.setInput(new ArrayList<>());
				cmbLeader.setInput(new ArrayList<>());
				
				lstEmployees.setInput(employees);
			});
			
			
			return Status.OK_STATUS;
		}
		
	};
}
