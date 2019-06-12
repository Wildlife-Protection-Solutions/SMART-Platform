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
package org.wcs.smart.cybertracker.survey.ui;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.EmployeeTeam;
import org.wcs.smart.ca.EmployeeTeamMember;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldUuidValue;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.model.MissionMetadataField;
import org.wcs.smart.cybertracker.survey.model.SurveyCtPackage;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Patrol metadata contribution
 * 
 * @author Emily
 *
 */
public class SurveyMetadataPackageContribution implements IPackageUiContribution {

	private Button btnCmt, btnMembers, btnLeader;
	private ComboViewer cmbLeader;
	private Text txtComment;
	
	private CheckboxTableViewer lstEmployees;
	private Listener onModified;
	
	private ICtPackage ctpackage;
	private boolean fireEvents = true;
	private Font boldFont;
	
	@Override
	public boolean isTab() { 
		return true; 
	}
	
	@Override
	public String getTabName() { 
		return Messages.SurveyMetadataPackageContribution_TabName; 
	}
	
	private void fireChanged() {
		if (!fireEvents) return;
		if (onModified != null) onModified.handleEvent(new Event());
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
		col1.setText(Messages.SurveyMetadataPackageContribution_MetadataColumn);
		col1.setToolTipText(Messages.SurveyMetadataPackageContribution_MetadataTooltip);
		col1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

		Label col2 = new Label(headerComp, SWT.NONE);
		col2.setText(Messages.SurveyMetadataPackageContribution_UserSelectedLabel);
		col2.setToolTipText(Messages.SurveyMetadataPackageContribution_UserSelectedTooltip);
		col2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label col3 = new Label(headerComp, SWT.NONE);
		col3.setText(Messages.SurveyMetadataPackageContribution_FixedValueLabel);
		col3.setToolTipText(Messages.SurveyMetadataPackageContribution_FixedValueTooltip);
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
		
		// comment
		Object[] d = createTextSection(Messages.SurveyMetadataPackageContribution_MissionCommentLabel, core, null);
		btnCmt = (Button) d[0];
		txtComment = (Text) d[1];		
		
		
		ColumnLabelProvider employeeLblProvider = new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof EmployeeTeam) return ((EmployeeTeam)element).getName();
				if (element instanceof Employee) return SmartLabelProvider.getShortLabel((Employee)element);
				return super.getText(element);
			}
			
			@Override
			public Font getFont(Object element) {
				if (element instanceof EmployeeTeam || element instanceof Employee) return null;
				return boldFont;
			}
		};
		
		// employess
		Label l = new Label(core, SWT.NONE);
		l.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_MEMBER_ICON));
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		l = new Label(core, SWT.NONE);
		l.setText(Messages.SurveyMetadataPackageContribution_MembersLabel);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		btnMembers = new Button(core, SWT.CHECK);
		btnMembers.setSelection(true);
		btnMembers.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));

		Composite cemp = new Composite(core, SWT.NONE);
		cemp.setLayout(new GridLayout());
		((GridLayout)cemp.getLayout()).marginWidth = 0;
		((GridLayout)cemp.getLayout()).marginHeight = 0;
		cemp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// employees
		Label elabel = new Label(cemp, SWT.WRAP);
		elabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)elabel.getLayoutData()).widthHint = 200;
		
		lstEmployees = CheckboxTableViewer.newCheckList(cemp, SWT.BORDER | SWT.MULTI);
		lstEmployees.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(lstEmployees));
		lstEmployees.setContentProvider(ArrayContentProvider.getInstance());
		lstEmployees.setLabelProvider(employeeLblProvider);
		lstEmployees.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstEmployees.getControl().getLayoutData()).heightHint = 80;
		FontData fd = lstEmployees.getControl().getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(lstEmployees.getControl().getDisplay(), fd);
		lstEmployees.getControl().addListener(SWT.Dispose,e->boldFont.dispose());
		
		ViewerFilter filter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof Employee || element instanceof EmployeeTeam) return lstEmployees.getChecked(element);
				return true;
			}
		};
		Button btnChecked = new Button(cemp, SWT.CHECK);
		btnChecked.setText(Messages.SurveyMetadataPackageContribution_BtnOnlyChecked);
		btnChecked.addListener(SWT.Selection, e->{
			lstEmployees.getControl().setVisible(false);
			if (btnChecked.getSelection()) lstEmployees.addFilter(filter);
			else lstEmployees.removeFilter(filter);
			lstEmployees.getControl().setVisible(true);
		});
		
		// leader
		d = createComboViewerSection(Messages.SurveyMetadataPackageContribution_LeaderLabel, core, EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_LEADER_ICON));
		btnLeader = (Button) d[0];
		cmbLeader = (ComboViewer) d[1];
		cmbLeader.setLabelProvider(employeeLblProvider);
		btnLeader.setEnabled(false);
		cmbLeader.getControl().setEnabled(false);


		btnMembers.addListener(SWT.Selection, e->{
			if (btnMembers.getSelection()) {
				elabel.setText(Messages.SurveyMetadataPackageContribution_FilterMsg);
				btnLeader.setEnabled(false);
				cmbLeader.getControl().setEnabled(false);
			}else {
				elabel.setText(Messages.SurveyMetadataPackageContribution_MembersMsg);				
				btnLeader.setEnabled(true);
				cmbLeader.getControl().setEnabled(!btnLeader.getSelection());
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
		Set<Employee> items = new HashSet<>();
		for (Object o : lstEmployees.getCheckedElements()) {
			if (o instanceof EmployeeTeam) {
				EmployeeTeam team = (EmployeeTeam)o;
				for (EmployeeTeamMember e : team.getMembers()) items.add(e.getEmployee());
			}else if (o instanceof Employee) {
				items.add((Employee) o);
			}
		}
		
		List<Employee> sorted = new ArrayList<>(items);
		
		sorted.sort((a,b)->Collator.getInstance().compare(SmartLabelProvider.getShortLabel(a), SmartLabelProvider.getShortLabel(b)));
		
		cmbLeader.setInput(sorted);
		if (cmbLeader.getStructuredSelection().isEmpty() && items.size() > 0) cmbLeader.setSelection(new StructuredSelection(sorted.get(0)));
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
		boolean ok = false;
		for (Object type : lstEmployees.getCheckedElements()) {
			if (type instanceof Employee) {
				ok = true;
				break;
			}else if (type instanceof EmployeeTeam) {
				if (!((EmployeeTeam)type).getMembers().isEmpty()) {
					ok = true;
					break;
				}
			}
		}
		if (!ok) {
			return Messages.SurveyMetadataPackageContribution_EmployeeRequired;
		}
		
		if (!btnMembers.getSelection() && !btnLeader.getSelection() && cmbLeader.getStructuredSelection().isEmpty()) {
			return Messages.SurveyMetadataPackageContribution_LeaderRequired;
		}
		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage) {
		SurveyCtPackage cpackage = ((SurveyCtPackage)ctpackage);
		if (cpackage.getMetadataValues() == null) cpackage.setMetadataValues(new ArrayList<>());
		
		List<MetadataFieldValue> items = ((SurveyCtPackage)ctpackage).getMetadataValues();
		HashMap<String, MetadataFieldValue> map = new HashMap<>();
		for (MetadataFieldValue v : items) {
			map.put(v.getMetadataKey(), v);
		}
		
		MetadataFieldValue v = findMetadataValue(map, MissionMetadataField.COMMENT,  btnCmt, cpackage);
		v.setStringValue(txtComment.getText().strip());
		
		v = findMetadataValue(map, MissionMetadataField.LEADER,  btnLeader, cpackage);
		if (cmbLeader.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			v.setUuidValue( ((Employee)cmbLeader.getStructuredSelection().getFirstElement()).getUuid() );
		}
		
		v = findMetadataValue(map, MissionMetadataField.MEMBERS,  btnMembers, cpackage);
		if (v.getUuidList() == null) v.setUuidList(new ArrayList<>());
		v.getUuidList().clear();
		
		for (Object o : lstEmployees.getCheckedElements()) {
			if (o instanceof Employee) {
				MetadataFieldUuidValue uuidValue = new MetadataFieldUuidValue();
				uuidValue.setMetadata(v);
				uuidValue.setUuidValue(((Employee) o).getUuid());
				v.getUuidList().add(uuidValue);
			}else if (o instanceof EmployeeTeam) {
				MetadataFieldUuidValue uuidValue = new MetadataFieldUuidValue();
				uuidValue.setMetadata(v);
				uuidValue.setUuidValue(((EmployeeTeam) o).getUuid());
				v.getUuidList().add(uuidValue);
			}
		}
	}
	
	private MetadataFieldValue findMetadataValue(HashMap<String, MetadataFieldValue> items, MissionMetadataField field, Button btnVisible, SurveyCtPackage cpackage) {
		MetadataFieldValue v = items.get(field.name());
		if (v == null) {
			v = new MetadataFieldValue();
			v.setCtPackage(cpackage);
			v.setConservationArea(cpackage.getConservationArea());
			v.setMetadataKey(field.name());
			cpackage.getMetadataValues().add(v);
		}
		v.setVisible(btnVisible.getSelection());
		return v;
	}
	
	private Job loadValues = new Job("loading metadata values") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			List<Employee> employees = new ArrayList<>();
			List<EmployeeTeam> eteams = new ArrayList<>();

			List<MetadataFieldValue> metadataValues = new ArrayList<>();
			if ( ((SurveyCtPackage)ctpackage).getMetadataValues() != null) {
				metadataValues.addAll(((SurveyCtPackage)ctpackage).getMetadataValues());
			}
			try(Session s = HibernateManager.openSession()){
				employees.addAll(QueryFactory.buildQuery(s, Employee.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"endEmploymentDate", null}).list()); //$NON-NLS-1$
				eteams.addAll(QueryFactory.buildQuery(s, EmployeeTeam.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()})  //$NON-NLS-1$
						.list());
				eteams.forEach(team->team.getMembers().forEach(e->e.getEmployee().getGivenName()));
			}
			
			eteams.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			employees.sort((a,b)->Collator.getInstance().compare(SmartLabelProvider.getShortLabel(a), SmartLabelProvider.getShortLabel(b)));
			
			List<Object> alleteams = new ArrayList<>();
			if (!eteams.isEmpty()) {
				alleteams.add(Messages.SurveyMetadataPackageContribution_EmployeeTeams);
				alleteams.addAll(eteams);
				alleteams.add(Messages.SurveyMetadataPackageContribution_Employees);
			}
			alleteams.addAll(employees);
			
			Display.getDefault().syncExec(()->{
				try {
					fireEvents = false;
					
					cmbLeader.setInput(new ArrayList<>());
					lstEmployees.setInput(alleteams);
					
					//configure default values
					for (MetadataFieldValue v : metadataValues) {
						//populate members first
						 if (v.getMetadataKey().equals(MissionMetadataField.MEMBERS.name())) {
							btnMembers.setSelection(v.isVisible());
							for (MetadataFieldUuidValue item : v.getUuidList()) {
								//either an employee or a team
								Employee e = new Employee();
								e.setUuid(item.getUuidValue());
								lstEmployees.setChecked(e, true);
								EmployeeTeam t = new EmployeeTeam();
								t.setUuid(item.getUuidValue());
								lstEmployees.setChecked(t, true);
							}
						}
					}
					updateLeaderPilotLists();
					for (MetadataFieldValue v : metadataValues) {
						if (v.getMetadataKey().equals(MissionMetadataField.COMMENT.name())) {
							btnCmt.setSelection(v.isVisible());
							txtComment.setText(v.getStringValue());
						}else if (v.getMetadataKey().equals(MissionMetadataField.LEADER.name())) {
							btnLeader.setSelection(v.isVisible());
							Employee temp = new Employee();
							temp.setUuid(v.getUuidValue());
							cmbLeader.setSelection(new StructuredSelection(temp));
						}
					}
					btnCmt.notifyListeners(SWT.Selection, new Event());
					btnMembers.notifyListeners(SWT.Selection, new Event());
					btnLeader.notifyListeners(SWT.Selection, new Event());
				}finally {
					fireEvents = true;
				}
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
