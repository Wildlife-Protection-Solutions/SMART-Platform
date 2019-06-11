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
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
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
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldUuidValue;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.PatrolCtPackage;
import org.wcs.smart.cybertracker.patrol.model.PatrolMetadataField;
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
	
	private ICtPackage ctpackage;
	private boolean fireEvents = true;
	
	private void fireChanged() {
		if (!fireEvents) return;
		if (onModified != null) onModified.handleEvent(new Event());
	}
	
	@Override
	public boolean isTab() { 
		return true; 
	}
	
	@Override
	public String getTabName() { 
		return Messages.PatrolMetadataPackageContribution_TabName; 
	}
	
	
	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified) {
		this.ctpackage = ctpackage;
		this.onModified = onModified;
		
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight = 0;
		((GridLayout)outer.getLayout()).verticalSpacing = 0;
		
		Composite headerComp = new Composite(outer, SWT.NONE);
		headerComp.setLayout(new GridLayout(4, false));
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		WidgetElement.setCSSClass(headerComp, SmartUiUtils.HEADER_CLASS);

		Label col1 = new Label(headerComp, SWT.NONE);
		col1.setText(Messages.PatrolMetadataPackageContribution_FieldLabel);
		col1.setToolTipText(Messages.PatrolMetadataPackageContribution_FieldTooltip);
		col1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

		Label col2 = new Label(headerComp, SWT.NONE);
		col2.setText(Messages.PatrolMetadataPackageContribution_UserSelectedLabel);
		col2.setToolTipText(Messages.PatrolMetadataPackageContribution_UserSelectedTooltip);
		col2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label col3 = new Label(headerComp, SWT.NONE);
		col3.setText(Messages.PatrolMetadataPackageContribution_FixedLabel);
		col3.setToolTipText(Messages.PatrolMetadataPackageContribution_FixedTooltip);
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
		Object[] d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_TransportTypeLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.MIXED_PATROL_ICON));
		btnTT = (Button) d[0];
		cmbTt = (ComboViewer) d[1];
		
		// armed
		Label l = new Label(core, SWT.NONE);
		l.setImage(SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ARMED_ICON));
				
		l = new Label(core, SWT.NONE);
		l.setText(Messages.PatrolMetadataPackageContribution_ArmedLabel);
		
		btnArmed = new Button(core, SWT.CHECK);
		btnArmed.setSelection(true);
		btnArmed.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		Composite cmp = new Composite(core, SWT.NONE);
		cmp.setLayout(new GridLayout(2, false));
		
		btnArmedYes = new Button(cmp, SWT.RADIO);
		btnArmedYes.setText(Messages.PatrolMetadataPackageContribution_YesOption);
		btnArmedYes.setEnabled(false);
		btnArmedNo = new Button(cmp, SWT.RADIO);
		btnArmedNo.setText(Messages.PatrolMetadataPackageContribution_NoOption);
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
		d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_TeamLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_TEAM_ICON));
		btnTeam = (Button) d[0];
		cmbTeam = (ComboViewer) d[1];
		
		// station
		d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_StationLabel, core, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.STATION_ICON));
		btnStation = (Button) d[0];
		cmbStation = (ComboViewer) d[1];
		
		// mandate
		d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_MandateLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MANDATE_ICON));
		btnMandate = (Button) d[0];
		cmbMandate = (ComboViewer) d[1];
				
		// objective
		d = createTextSection(Messages.PatrolMetadataPackageContribution_ObjectiveLabel, core, null);
		btnObj = (Button) d[0];
		txtObj = (Text) d[1];
		
		// comment
		d = createTextSection(Messages.PatrolMetadataPackageContribution_CommentLabel, core, null);
		btnCmt = (Button) d[0];
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
		l.setText(Messages.PatrolMetadataPackageContribution_MembersLabel);
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
		d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_LeaderLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_LEADER_ICON));
		btnLeader = (Button) d[0];
		cmbLeader = (ComboViewer) d[1];
		cmbLeader.setLabelProvider(employeeLblProvider);
		btnLeader.setEnabled(false);
		cmbLeader.getControl().setEnabled(false);
		// pilot
		d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_PilotLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_PILOT_ICON));
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
			updateLeaderPilotLists();
			fireChanged();
		});
		
		table.setMinSize(core.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		core.layout(true);
		((GridData)col1.getLayoutData()).widthHint = c1.getSize().x + c2.getSize().x + 5;
		((GridData)c3.getLayoutData()).widthHint = col2.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
	
		loadValues.schedule();
		
		return outer;
	}
	
	private void updateLeaderPilotLists() {
		List<Employee> items = new ArrayList<>();
		for (Object o : lstEmployees.getCheckedElements()) {
			if (o instanceof Employee) items.add((Employee) o);
		}
		cmbLeader.setInput(items);
		cmbPilot.setInput(items);
		if (cmbLeader.getStructuredSelection().isEmpty() && items.size() > 0) cmbLeader.setSelection(new StructuredSelection(items.get(0)));
		if (cmbPilot.getStructuredSelection().isEmpty() && items.size() > 0) cmbPilot.setSelection(new StructuredSelection(items.get(0)));
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
		if (!btnMembers.getSelection() && lstEmployees.getCheckedElements().length == 0) {
			return Messages.PatrolMetadataPackageContribution_MemberRequired;
		}
		
		if (!btnMembers.getSelection() && !btnLeader.getSelection() && cmbLeader.getStructuredSelection().isEmpty()) {
			return Messages.PatrolMetadataPackageContribution_LeaderRequired;
		}
		
		if (!btnMandate.getSelection() && cmbMandate.getStructuredSelection().isEmpty()) {
			return Messages.PatrolMetadataPackageContribution_MandateRequired;
		}
		if (!btnTT.getSelection() && cmbTt.getStructuredSelection().isEmpty()) {
			return Messages.PatrolMetadataPackageContribution_TransportRequired;
		}

		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage) {		
		PatrolCtPackage ppackage = (PatrolCtPackage)ctpackage;
		if (ppackage.getMetadataValues() == null) ppackage.setMetadataValues(new ArrayList<>());
		
		HashMap<String, MetadataFieldValue> map = new HashMap<>();
		for (MetadataFieldValue v : ppackage.getMetadataValues()) {
			map.put(v.getMetadataKey(), v);
		}
		
		MetadataFieldValue v = findMetadataValue(map, PatrolMetadataField.ARMED,  btnArmed, ppackage);
		v.setBooleanValue(btnArmedYes.getSelection());
		
		v = findMetadataValue(map, PatrolMetadataField.COMMENT,  btnCmt, ppackage);
		v.setStringValue(txtComment.getText().strip());
		
		v = findMetadataValue(map, PatrolMetadataField.LEADER,  btnLeader, ppackage);
		if (cmbLeader.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			v.setUuidValue( ((Employee)cmbLeader.getStructuredSelection().getFirstElement()).getUuid() );
		}
		
		v = findMetadataValue(map, PatrolMetadataField.MANDATE,  btnMandate, ppackage);
		if (cmbMandate.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			v.setUuidValue( ((PatrolMandate)cmbMandate.getStructuredSelection().getFirstElement()).getUuid() );
		}
		
		v = findMetadataValue(map, PatrolMetadataField.MEMBERS,  btnMembers, ppackage);
		if (v.getUuidList() == null) v.setUuidList(new ArrayList<>());
		v.getUuidList().clear();
		
		for (Object o : lstEmployees.getCheckedElements()) {
			if (o instanceof Employee) {
				MetadataFieldUuidValue uuidValue = new MetadataFieldUuidValue();
				uuidValue.setMetadata(v);
				uuidValue.setUuidValue(((Employee) o).getUuid());
				v.getUuidList().add(uuidValue);
			}
		}
		
		v = findMetadataValue(map, PatrolMetadataField.OBJECTIVE,  btnObj, ppackage);
		v.setStringValue(txtObj.getText());
		
		v = findMetadataValue(map, PatrolMetadataField.PILOT,  btnPilot, ppackage);
		if (cmbPilot.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			v.setUuidValue( ((Employee)cmbPilot.getStructuredSelection().getFirstElement()).getUuid() );
		}
		
		v = findMetadataValue(map, PatrolMetadataField.STATION,  btnStation, ppackage);
		if (cmbStation.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			if (cmbStation.getStructuredSelection().getFirstElement() instanceof Station) {
				v.setUuidValue( ((Station)cmbStation.getStructuredSelection().getFirstElement()).getUuid() );
			}else {
				v.setUuidValue( null );
			}
		}
		
		v = findMetadataValue(map, PatrolMetadataField.TEAM,  btnTeam, ppackage);
		if (cmbTeam.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			if (cmbTeam.getStructuredSelection().getFirstElement() instanceof Team) {
				v.setUuidValue( ((Team)cmbTeam.getStructuredSelection().getFirstElement()).getUuid() );
			}else {
				v.setUuidValue( null );
			}
		}
		
		v = findMetadataValue(map, PatrolMetadataField.TRANSPORT,  btnTT, ppackage);
		if (cmbTt.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			v.setUuidValue( ((PatrolTransportType)cmbTt.getStructuredSelection().getFirstElement()).getUuid() );
		}
	}
	
	private MetadataFieldValue findMetadataValue(HashMap<String, MetadataFieldValue> items, PatrolMetadataField field, Button btnVisible, PatrolCtPackage ppackage) {
		MetadataFieldValue v = items.get(field.name());
		if (v == null) {
			v = new MetadataFieldValue();
			v.setCtPackage((AbstractCtPackage)ppackage);
			v.setConservationArea(ppackage.getConservationArea());
			v.setMetadataKey(field.name());
			ppackage.getMetadataValues().add(v);
		}
		v.setVisible(btnVisible.getSelection());
		return v;
	}
		
	private Job loadValues = new Job("loading metadata values") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			List<PatrolTransportType> types = new ArrayList<>();
			List<Object> teams = new ArrayList<>();
			List<Object> stations = new ArrayList<>();
			List<PatrolMandate> mandates = new ArrayList<>();

			List<Employee> employees = new ArrayList<>();
			
			List<MetadataFieldValue> metadataValues = new ArrayList<>();
			if ( ((PatrolCtPackage)ctpackage).getMetadataValues() != null) {
				metadataValues.addAll(((PatrolCtPackage)ctpackage).getMetadataValues());
			}
			
			try(Session s = HibernateManager.openSession()){
				
				types.addAll(QueryFactory.buildQuery(s, PatrolTransportType.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"isActive", true}) //$NON-NLS-1$
						.list());
			
				teams.addAll(QueryFactory.buildQuery(s, Team.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"isActive", true}).list()); //$NON-NLS-1$
				
				stations.addAll(QueryFactory.buildQuery(s, Station.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"isActive", true}).list()); //$NON-NLS-1$
				
				mandates.addAll(QueryFactory.buildQuery(s, PatrolMandate.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"isActive", true}).list()); //$NON-NLS-1$
				
				employees.addAll(QueryFactory.buildQuery(s, Employee.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"endEmploymentDate", null}).list()); //$NON-NLS-1$
			
			}
			
			teams.add(0, Messages.PatrolMetadataPackageContribution_NoneOption);
			stations.add(0, Messages.PatrolMetadataPackageContribution_NoneOption);
			Display.getDefault().syncExec(()->{
				try {
					fireEvents = false;
				
					cmbTt.setInput(types);
					if (!types.isEmpty()) cmbTt.setSelection(new StructuredSelection(types.get(0)));
					
					cmbTeam.setInput(teams);
					if (!teams.isEmpty()) cmbTeam.setSelection(new StructuredSelection(teams.get(0)));
					
					cmbStation.setInput(stations);
					if (!stations.isEmpty()) cmbStation.setSelection(new StructuredSelection(stations.get(0)));
					
					cmbMandate.setInput(mandates);
					if (!mandates.isEmpty()) cmbMandate.setSelection(new StructuredSelection(mandates.get(0)));
					
					cmbPilot.setInput(new ArrayList<>());
					cmbLeader.setInput(new ArrayList<>());
					
					lstEmployees.setInput(employees);
					
					//configure default values
					for (MetadataFieldValue v : metadataValues) {
						//populate members first
						 if (v.getMetadataKey().equals(PatrolMetadataField.MEMBERS.name())) {
							btnMembers.setSelection(v.isVisible());
							for (MetadataFieldUuidValue item : v.getUuidList()) {
								Employee e = new Employee();
								e.setUuid(item.getUuidValue());
								lstEmployees.setChecked(e, true);
							}
						}
					}
					updateLeaderPilotLists();
					for (MetadataFieldValue v : metadataValues) {
						if (v.getMetadataKey().equals(PatrolMetadataField.ARMED.name())) {
							btnArmed.setSelection(v.isVisible());
							btnArmedYes.setSelection(v.getBooleanValue());
							btnArmedNo.setSelection(!v.getBooleanValue());
						}else if (v.getMetadataKey().equals(PatrolMetadataField.COMMENT.name())) {
							btnCmt.setSelection(v.isVisible());
							txtComment.setText(v.getStringValue());
						}else if (v.getMetadataKey().equals(PatrolMetadataField.LEADER.name())) {
							btnLeader.setSelection(v.isVisible());
							Employee temp = new Employee();
							temp.setUuid(v.getUuidValue());
							cmbLeader.setSelection(new StructuredSelection(temp));
						}else if(v.getMetadataKey().equals(PatrolMetadataField.MANDATE.name())){
							btnMandate.setSelection(v.isVisible());
							PatrolMandate temp = new PatrolMandate();
							temp.setUuid(v.getUuidValue());
							cmbMandate.setSelection(new StructuredSelection(temp));
						}else if (v.getMetadataKey().equals(PatrolMetadataField.OBJECTIVE.name())) {
							btnObj.setSelection(v.isVisible());
							txtObj.setText(v.getStringValue());
						}else if (v.getMetadataKey().equals(PatrolMetadataField.PILOT.name())) {
							btnPilot.setSelection(v.isVisible());
							Employee temp = new Employee();
							temp.setUuid(v.getUuidValue());
							cmbPilot.setSelection(new StructuredSelection(temp));
						}else if(v.getMetadataKey().equals(PatrolMetadataField.STATION.name())) {
							btnStation.setSelection(v.isVisible());
							Station temp = new Station();
							temp.setUuid(v.getUuidValue());
							cmbStation.setSelection(new StructuredSelection(temp));
						}else if(v.getMetadataKey().equals(PatrolMetadataField.TEAM.name())) {
							btnTeam.setSelection(v.isVisible());
							Team temp = new Team();
							temp.setUuid(v.getUuidValue());
							cmbTeam.setSelection(new StructuredSelection(temp));
						}else if(v.getMetadataKey().equals(PatrolMetadataField.TRANSPORT.name())) {
							btnTT.setSelection(v.isVisible());
							PatrolTransportType temp = new PatrolTransportType();
							temp.setUuid(v.getUuidValue());
							cmbTt.setSelection(new StructuredSelection(temp));
						}
					}
					btnArmed.notifyListeners(SWT.Selection, new Event());
					btnCmt.notifyListeners(SWT.Selection, new Event());
					btnMandate.notifyListeners(SWT.Selection, new Event());
					btnMembers.notifyListeners(SWT.Selection, new Event());
					btnObj.notifyListeners(SWT.Selection, new Event());
					btnStation.notifyListeners(SWT.Selection, new Event());
					btnTeam.notifyListeners(SWT.Selection, new Event());
					btnTT.notifyListeners(SWT.Selection, new Event());
	
					btnLeader.notifyListeners(SWT.Selection, new Event());
					btnPilot.notifyListeners(SWT.Selection, new Event());
				}finally {
					fireEvents = true;
				}
			});
			
			
			return Status.OK_STATUS;
		}
		
	};
}
