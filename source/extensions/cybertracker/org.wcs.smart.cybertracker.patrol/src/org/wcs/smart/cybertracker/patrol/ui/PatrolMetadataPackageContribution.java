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

import java.text.Collator;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.EmployeeTeam;
import org.wcs.smart.ca.EmployeeTeamMember;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
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
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Patrol metadata contribution
 * 
 * @author Emily
 *
 */
public class PatrolMetadataPackageContribution implements IPackageUiContribution {

	private Button btnTT, btnArmed, btnTeam, btnStation, btnMandate, btnObj, btnCmt, btnMembers, btnLeader, btnPilot, btnArmedYes, btnArmedNo;
	
	private Button btnTTrq, btnArmedrq, btnTeamrq, btnStationrq, btnMandaterq, btnObjrq, btnCmtrq, btnMembersrq, btnLeaderrq, btnPilotrq;
	
	private ComboViewer cmbTt, cmbTeam, cmbStation, cmbMandate, cmbLeader, cmbPilot;
	private Text txtObj, txtComment;
	
	private HashMap<PatrolAttribute, Object[]> customAttributes;
	
	private CheckboxTableViewer lstEmployees;
	private Listener onModified;
	private Runnable onInitilized;
	
	private ICtPackage ctpackage;
	private boolean fireEvents = true;
	
	private Font boldFont;
	
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
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified, Runnable onInitilized) {
		this.ctpackage = ctpackage;
		this.onModified = onModified;
		this.onInitilized = onInitilized;
		
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight = 0;
		((GridLayout)outer.getLayout()).verticalSpacing = 0;
		
		Composite headerComp = new Composite(outer, SWT.NONE);
		headerComp.setLayout(new GridLayout(5, false));
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		SmartUiUtils.setCSSClass(headerComp, SmartUiUtils.HEADER_CLASS);
		
		Label col0 = new Label(headerComp, SWT.NONE);
		col0.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		
		Label col1 = new Label(headerComp, SWT.NONE);
		col1.setText(Messages.PatrolMetadataPackageContribution_FieldLabel);
		col1.setToolTipText(Messages.PatrolMetadataPackageContribution_FieldTooltip);
		col1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		Label col2 = new Label(headerComp, SWT.NONE);
		col2.setText(Messages.PatrolMetadataPackageContribution_UserSelectedLabel);
		col2.setToolTipText(Messages.PatrolMetadataPackageContribution_UserSelectedTooltip);
		col2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label col3 = new Label(headerComp, SWT.NONE);
		col3.setText(Messages.PatrolMetadataPackageContribution_RequiredField);
		col3.setToolTipText(Messages.PatrolMetadataPackageContribution_RequiredFieldTooltip);
		col3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label col4 = new Label(headerComp, SWT.NONE);
		col4.setText(Messages.PatrolMetadataPackageContribution_FixedLabel);
		col4.setToolTipText(Messages.PatrolMetadataPackageContribution_FixedTooltip);
		col4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ScrolledComposite table = new ScrolledComposite(outer, SWT.V_SCROLL);
		table.setExpandHorizontal(true);
		table.setExpandVertical(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite core = new Composite(table, SWT.NONE);
		core.setLayout(new GridLayout(5, false));
		table.setContent(core);
		
		Label c1 = new Label(core, SWT.NONE);
		c1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)c1.getLayoutData()).heightHint = 0;
		((GridData)c1.getLayoutData()).widthHint = 32;
		
		Label c2 = new Label(core, SWT.NONE);
		c2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)c2.getLayoutData()).heightHint = 0;
		c2.setText(col1.getText());
		
		Label c3 = new Label(core, SWT.NONE);
		c3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)c3.getLayoutData()).heightHint = 0;
		c3.setText(col2.getText());
		
		Label c4 = new Label(core, SWT.NONE);
		c4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)c4.getLayoutData()).heightHint = 0;
		c4.setText(col3.getText());
		
		Label c5 = new Label(core, SWT.NONE);
		c5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)c5.getLayoutData()).heightHint = 0;
		c5.setText(col4.getText());
		
		// transport type
		Object[] d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_TransportTypeLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_TRANSPORTTYPE_ICON), false);
		btnTT = (Button) d[0];
		btnTTrq = (Button) d[1];
		cmbTt = (ComboViewer) d[2];
		
		// armed
		Label l = new Label(core, SWT.NONE);
		l.setImage(SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ARMED_ICON));
				
		l = new Label(core, SWT.NONE);
		l.setText(Messages.PatrolMetadataPackageContribution_ArmedLabel);
		
		btnArmed = new Button(core, SWT.CHECK);
		btnArmed.setSelection(true);
		btnArmed.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		btnArmedrq = new Button(core, SWT.CHECK);
		btnArmedrq.setSelection(true);
		btnArmedrq.setEnabled(false);
		btnArmedrq.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		
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
		d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_TeamLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_TEAM_ICON), true);
		btnTeam = (Button) d[0];
		btnTeamrq = (Button) d[1];
		btnTeamrq.setSelection(false);
		cmbTeam = (ComboViewer) d[2];
		
		// station
		d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_StationLabel, core, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.STATION_ICON), true);
		btnStation = (Button) d[0];
		btnStationrq = (Button) d[1];
		btnStationrq.setSelection(false);
		cmbStation = (ComboViewer) d[2];
		
		// mandate
		d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_MandateLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MANDATE_ICON), false);
		btnMandate = (Button) d[0];
		btnMandaterq = (Button) d[1];
		cmbMandate = (ComboViewer) d[2];
				
		// objective
		d = createTextSection(Messages.PatrolMetadataPackageContribution_ObjectiveLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_OBJECTIVE_ICON), true);
		btnObj = (Button) d[0];
		btnObjrq = (Button) d[1];
		btnObjrq.setSelection(false);
		txtObj = (Text) d[2];
		
		// comment
		d = createTextSection(Messages.PatrolMetadataPackageContribution_CommentLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_COMMENT_ICON), true);
		btnCmt = (Button) d[0];
		btnCmtrq = (Button) d[1];
		btnCmtrq.setSelection(false);
		txtComment = (Text) d[2];		
		
		
		
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
		l = new Label(core, SWT.NONE);
		l.setImage(SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MEMBER_ICON));
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		l = new Label(core, SWT.NONE);
		l.setText(Messages.PatrolMetadataPackageContribution_MembersLabel);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		btnMembers = new Button(core, SWT.CHECK);
		btnMembers.setSelection(true);
		btnMembers.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		
		btnMembersrq = new Button(core, SWT.CHECK);
		btnMembersrq.setSelection(true);
		btnMembersrq.setEnabled(false);
		btnMembersrq.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));

		Composite cemp = new Composite(core, SWT.NONE);
		cemp.setLayout(new GridLayout());
		((GridLayout)cemp.getLayout()).marginWidth = 0;
		((GridLayout)cemp.getLayout()).marginHeight = 0;
		cemp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
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
		btnChecked.setText(Messages.PatrolMetadataPackageContribution_ShowChecked);
		btnChecked.addListener(SWT.Selection, e->{
			lstEmployees.getControl().setVisible(false);
			if (btnChecked.getSelection()) lstEmployees.addFilter(filter);
			else lstEmployees.removeFilter(filter);
			lstEmployees.getControl().setVisible(true);
		});
		
		// leader
		d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_LeaderLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_LEADER_ICON), false);
		btnLeader = (Button) d[0];
		btnLeaderrq = (Button)d[1];
		cmbLeader = (ComboViewer) d[2];
		cmbLeader.setLabelProvider(employeeLblProvider);
		btnLeader.setEnabled(false);
		cmbLeader.getControl().setEnabled(false);
		// pilot
		d = createComboViewerSection(Messages.PatrolMetadataPackageContribution_PilotLabel, core, SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_PILOT_ICON), false);
		btnPilot = (Button) d[0];
		btnPilotrq = (Button) d[1];		
		cmbPilot = (ComboViewer) d[2];
		cmbPilot.setLabelProvider(employeeLblProvider);
		btnPilot.setEnabled(false);
		cmbPilot.getControl().setEnabled(false);
		
		btnMembers.addListener(SWT.Selection, e->{
			if (btnMembers.getSelection()) {
				elabel.setText(Messages.PatrolMetadataPackageContribution_FilterMsg);
				btnLeader.setEnabled(false);
				btnPilot.setEnabled(false);
				cmbLeader.getControl().setEnabled(false);
				cmbPilot.getControl().setEnabled(false);
			}else {
				elabel.setText(Messages.PatrolMetadataPackageContribution_MemberMsg);
				
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
		
		//add custom patrol attributes
		customAttributes = new HashMap<>();
		try(Session session = HibernateManager.openSession()){
			
			List<PatrolAttribute> attributes = QueryFactory.buildQuery(session, PatrolAttribute.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"isActive", true}).list(); //$NON-NLS-1$
			attributes.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			
			for (PatrolAttribute attribute : attributes) {
				
				//TODO: populate with icon  
				Label icon = new Label(core, SWT.NONE);
				
				Label name = new Label(core, SWT.NONE);
				name.setText(attribute.getName());
				
				Object[] data = new Object[4];
				
				Button btnSelected = new Button(core, SWT.CHECK);
				btnSelected.setSelection(true);
				btnSelected.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
				
				Button btnRequired = new Button(core, SWT.CHECK);
				btnRequired.setSelection(false);
				btnRequired.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
				
				
				data[0] = btnSelected;
				data[1] = btnRequired;
				
				if (attribute.getType() == AttributeType.TEXT) {
					Text txt =  new Text(core, SWT.BORDER);
					txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					txt.setEnabled(false);
					txt.addListener(SWT.Modify, e->fireChanged());
					data[2] = txt;
					btnSelected.addListener(SWT.Selection,e->{
						txt.setEnabled(!btnSelected.getSelection());
					});
				}else if (attribute.getType() == AttributeType.NUMERIC) {
					Text txt =  new Text(core, SWT.BORDER);
					txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					txt.setEnabled(false);
					txt.addListener(SWT.Modify, e->fireChanged());
					data[2] = txt;
					btnSelected.addListener(SWT.Selection,e->{
						txt.setEnabled(!btnSelected.getSelection());
					});
				}else if (attribute.getType() == AttributeType.DATE) {
					DateTime dateTime = new DateTime(core, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG | SWT.DATE);
					dateTime.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
					dateTime.setEnabled(false);
					btnSelected.addListener(SWT.Selection,e->{
						dateTime.setEnabled(!btnSelected.getSelection());
					});
					dateTime.addListener(SWT.Selection, e->fireChanged());
					data[2] = dateTime;
				}else if (attribute.getType() == AttributeType.LIST) {
					ComboViewer cmb = new ComboViewer(core, SWT.DROP_DOWN | SWT.READ_ONLY);
					data[2] = cmb;
					cmb.setContentProvider(ArrayContentProvider.getInstance());
					cmb.setLabelProvider(new NamedItemLabelProvider());
					List<Object> items = new ArrayList<>();
					items.add(""); //$NON-NLS-1$
					items.addAll(attribute.getAttributeList());
					cmb.setInput(items);
					cmb.getControl().setEnabled(false);
					cmb.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					
					cmb.addSelectionChangedListener(e->fireChanged());
					
					btnSelected.addListener(SWT.Selection,e->{
						cmb.getControl().setEnabled(!btnSelected.getSelection());
					});				
					
				}else if (attribute.getType() == AttributeType.BOOLEAN) {
					
					Composite part = new Composite(core, SWT.NONE);
					part.setLayout(new GridLayout(2, false));
					part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					Button btnYes = new Button(part, SWT.RADIO);
					btnYes.setText(Messages.PatrolMetadataPackageContribution_YesOption);
					btnYes.setEnabled(false);
					Button btnNo = new Button(part, SWT.RADIO);
					btnNo.setText(Messages.PatrolMetadataPackageContribution_NoOption);
					btnNo.setSelection(true);
					btnNo.setEnabled(false);
					
					btnYes.addListener(SWT.Selection,e->fireChanged());
					btnNo.addListener(SWT.Selection,e->fireChanged());
					
					btnSelected.addListener(SWT.Selection,e->{
						btnYes.setEnabled(!btnSelected.getSelection());
						btnNo.setEnabled(!btnSelected.getSelection());
					});
					data[2] = btnYes;
					data[3] = btnNo;
				} else {
					new Label(core, SWT.NONE);
					data[2] = null;
				}
				
				
				btnSelected.addListener(SWT.Selection, e-> {
					btnRequired.setEnabled(btnSelected.getSelection());
					fireChanged();
				});
				
				btnRequired.addListener(SWT.Selection,e->fireChanged());

				customAttributes.put(attribute, data);
			}
		}
		
		table.setMinSize(core.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		core.layout(true);
		((GridData)col0.getLayoutData()).widthHint = 0;
		((GridData)col1.getLayoutData()).widthHint = c2.getSize().x + c1.getSize().x;
		((GridData)col2.getLayoutData()).widthHint = c3.getSize().x;
		((GridData)col3.getLayoutData()).widthHint = c4.getSize().x;
		((GridData)col4.getLayoutData()).widthHint = c5.getSize().x;
		
		loadValues.schedule();
		
		return outer;
	}
	
	private void updateLeaderPilotLists() {
		Set<Employee> items = new HashSet<>();
		for (Object o : lstEmployees.getCheckedElements()) {
			if (o instanceof EmployeeTeam) {
				EmployeeTeam team = (EmployeeTeam)o;
				for (EmployeeTeamMember e : team.getActiveMembers()) items.add(e.getEmployee());
			}else if (o instanceof Employee) {
				items.add((Employee) o);
			}
		}
		
		List<Employee> sorted = new ArrayList<>(items);
		
		sorted.sort((a,b)->Collator.getInstance().compare(SmartLabelProvider.getShortLabel(a), SmartLabelProvider.getShortLabel(b)));
		
		cmbLeader.setInput(sorted);
		cmbPilot.setInput(sorted);
		if (cmbLeader.getStructuredSelection().isEmpty() && items.size() > 0) cmbLeader.setSelection(new StructuredSelection(sorted.get(0)));
		if (cmbPilot.getStructuredSelection().isEmpty() && items.size() > 0) cmbPilot.setSelection(new StructuredSelection(sorted.get(0)));
	}
	
	private Object[] createComboViewerSection(String sectionName, Composite parent, Image icon, boolean isOptional) {
		Composite g = parent;
		
		Label l = new Label(g, SWT.NONE);
		l.setImage(icon);
		
		l = new Label(g, SWT.NONE);
		l.setText(sectionName);

		Button btn = new Button(g, SWT.CHECK);
		btn.setSelection(true);
		btn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		Button btnrq = new Button(g, SWT.CHECK);
		btnrq.setSelection(true);
		btnrq.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		if (!isOptional) {
			btnrq.setEnabled(false);
		}
		btnrq.addListener(SWT.Selection,e->fireChanged());
		
		ComboViewer cmb = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmb.setContentProvider(ArrayContentProvider.getInstance());
		cmb.setLabelProvider(new NamedItemLabelProvider());
		cmb.setInput(new String[] { DialogConstants.LOADING_TEXT });
		cmb.getControl().setEnabled(false);
		cmb.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		btn.addListener(SWT.Selection, e-> {
			cmb.getControl().setEnabled(!btn.getSelection());
			if (isOptional) btnrq.setEnabled(btn.getSelection());
			fireChanged();
		});
		
		cmb.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		cmb.addSelectionChangedListener(e->fireChanged());
		
		return new Object[] {btn, btnrq, cmb};
	}
	
	private Object[] createTextSection(String sectionName, Composite parent, Image icon, boolean isOptional) {
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
		
		Button btnrq = new Button(g, SWT.CHECK);
		btnrq.setSelection(true);
		btnrq.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		if (!isOptional) btnrq.setEnabled(false);
		btnrq.addListener(SWT.Selection,e->fireChanged());
		
		Text txt = new Text(g, SWT.BORDER);
		txt.setEnabled(false);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txt.getLayoutData()).heightHint = 40;
		
		btn.addListener(SWT.Selection, e-> {
			txt.setEnabled(!btn.getSelection());
			if (isOptional) btnrq.setEnabled(btn.getSelection());
			fireChanged();
		});
		
		txt.addListener(SWT.Modify, e->fireChanged());
		
		return new Object[] {btn, btnrq, txt};
	}
	
	@Override
	public String isValid() {
		boolean ok = false;
		for (Object type : lstEmployees.getCheckedElements()) {
			if (type instanceof Employee) {
				ok = true;
				break;
			}else if (type instanceof EmployeeTeam) {
				if (!((EmployeeTeam)type).getActiveMembers().isEmpty()) {
					ok = true;
					break;
				}
			}
		}
		if (!ok) {
			return Messages.PatrolMetadataPackageContribution_EmployeeRequired;
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

		for (Entry<PatrolAttribute, Object[]> item : customAttributes.entrySet()) {
			PatrolAttribute pa = item.getKey();
			Button btnVisible = (Button)item.getValue()[0];
			if (!btnVisible.getSelection() && pa.getType() == AttributeType.NUMERIC) {
				Text txt = (Text) item.getValue()[2];
				try {
					Double.parseDouble(txt.getText());
				}catch (Exception ex) {
					return MessageFormat.format(Messages.PatrolMetadataPackageContribution_InvalidDefaultValue, pa.getName(), txt.getText()); 
				}
			}
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
		
		MetadataFieldValue v = findMetadataValue(map, PatrolMetadataField.ARMED, btnArmed, btnArmedrq, ppackage);
		v.setBooleanValue(btnArmedYes.getSelection());
		
		v = findMetadataValue(map, PatrolMetadataField.COMMENT,  btnCmt, btnCmtrq, ppackage);
		v.setStringValue(txtComment.getText().strip());
		
		v = findMetadataValue(map, PatrolMetadataField.LEADER,  btnLeader, btnLeaderrq, ppackage);
		if (cmbLeader.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			v.setUuidValue( ((Employee)cmbLeader.getStructuredSelection().getFirstElement()).getUuid() );
		}
		
		v = findMetadataValue(map, PatrolMetadataField.MANDATE,  btnMandate, btnMandaterq, ppackage);
		if (cmbMandate.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			v.setUuidValue( ((PatrolMandate)cmbMandate.getStructuredSelection().getFirstElement()).getUuid() );
		}
		
		v = findMetadataValue(map, PatrolMetadataField.MEMBERS,  btnMembers, btnMembersrq, ppackage);
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
		
		v = findMetadataValue(map, PatrolMetadataField.OBJECTIVE, btnObj, btnObjrq, ppackage);
		v.setStringValue(txtObj.getText());
		
		v = findMetadataValue(map, PatrolMetadataField.PILOT, btnPilot, btnPilotrq, ppackage);
		if (cmbPilot.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			v.setUuidValue( ((Employee)cmbPilot.getStructuredSelection().getFirstElement()).getUuid() );
		}
		
		v = findMetadataValue(map, PatrolMetadataField.STATION, btnStation, btnStationrq, ppackage);
		if (cmbStation.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			if (cmbStation.getStructuredSelection().getFirstElement() instanceof Station) {
				v.setUuidValue( ((Station)cmbStation.getStructuredSelection().getFirstElement()).getUuid() );
			}else {
				v.setUuidValue( null );
			}
		}
		
		v = findMetadataValue(map, PatrolMetadataField.TEAM, btnTeam, btnTeamrq, ppackage);
		if (cmbTeam.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			if (cmbTeam.getStructuredSelection().getFirstElement() instanceof Team) {
				v.setUuidValue( ((Team)cmbTeam.getStructuredSelection().getFirstElement()).getUuid() );
			}else {
				v.setUuidValue( null );
			}
		}
		
		v = findMetadataValue(map, PatrolMetadataField.TRANSPORT,  btnTT, btnTTrq, ppackage);
		if (cmbTt.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			v.setUuidValue( ((PatrolTransportType)cmbTt.getStructuredSelection().getFirstElement()).getUuid() );
		}
		
		for (Entry<PatrolAttribute, Object[]> custom : customAttributes.entrySet()) {
			Object[] fields = custom.getValue();
			PatrolAttribute pa = custom.getKey();
			v = findMetadataValue(map, pa, (Button)fields[0], (Button)fields[1], ppackage);
			
			if (pa.getType() == AttributeType.BOOLEAN) {
				if (((Button)fields[2]).getSelection()) {
					v.setBooleanValue(Boolean.TRUE);
				}else {
					v.setBooleanValue(Boolean.FALSE);
				}
			}else if (pa.getType() == AttributeType.DATE) {
				DateTime dt = (DateTime) fields[2];
				v.setStringValue(SmartUtils.toDate(dt).toString());
			}else if (pa.getType() == AttributeType.LIST) {
				ComboViewer cmb = (ComboViewer) fields[2];
				Object o = cmb.getStructuredSelection().getFirstElement();
				if (o instanceof PatrolAttributeListItem) {
					v.setUuidValue(((PatrolAttributeListItem)o).getUuid());
				}else {
					v.setUuidValue(null);
				}
			}else if (pa.getType() == AttributeType.TEXT) {
				v.setStringValue(((Text)fields[2]).getText());
			}else if (pa.getType() == AttributeType.NUMERIC) {
				v.setStringValue(((Text)fields[2]).getText());
			}
			
		}
	}
	
	private MetadataFieldValue findMetadataValue(HashMap<String, MetadataFieldValue> items, 
			PatrolMetadataField field, Button btnVisible, Button btnRequired,
			PatrolCtPackage ppackage) {
		
		MetadataFieldValue v = items.get(field.name());
		if (v == null) {
			v = new MetadataFieldValue();
			v.setCtPackage((AbstractCtPackage)ppackage);
			v.setConservationArea(ppackage.getConservationArea());
			v.setMetadataKey(field.name());
			ppackage.getMetadataValues().add(v);
		}
		v.setVisible(btnVisible.getSelection());
		v.setRequired(btnRequired.getSelection());
		return v;
	}

	private MetadataFieldValue findMetadataValue(HashMap<String, MetadataFieldValue> items, 
			PatrolAttribute attribute, Button btnVisible, Button btnRequired,
			PatrolCtPackage ppackage) {
		
		MetadataFieldValue v = items.get(PatrolMetadataField.generateKey(attribute));
		if (v == null) {
			v = new MetadataFieldValue();
			v.setCtPackage((AbstractCtPackage)ppackage);
			v.setConservationArea(ppackage.getConservationArea());
			v.setMetadataKey(PatrolMetadataField.generateKey(attribute));
			ppackage.getMetadataValues().add(v);
		}
		v.setVisible(btnVisible.getSelection());
		v.setRequired(btnRequired.getSelection());
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
			List<EmployeeTeam> eteams = new ArrayList<>();
			
			List<MetadataFieldValue> metadataValues = new ArrayList<>();
			if ( ((PatrolCtPackage)ctpackage).getMetadataValues() != null) {
				metadataValues.addAll(((PatrolCtPackage)ctpackage).getMetadataValues());
			}
			
			try(Session s = HibernateManager.openSession()){
				eteams.addAll(QueryFactory.buildQuery(s, EmployeeTeam.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()})  //$NON-NLS-1$
						.list());
				eteams.forEach(team->team.getActiveMembers().forEach(e->e.getEmployee().getGivenName()));
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
			
			eteams.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			employees.sort((a,b)->Collator.getInstance().compare(SmartLabelProvider.getShortLabel(a), SmartLabelProvider.getShortLabel(b)));
			
			List<Object> alleteams = new ArrayList<>();
			if (!eteams.isEmpty()) {
				alleteams.add(Messages.PatrolMetadataPackageContribution_Teams);
				alleteams.addAll(eteams);
				alleteams.add(Messages.PatrolMetadataPackageContribution_Employees);
			}
			alleteams.addAll(employees);
			
			
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
					

					lstEmployees.setInput(alleteams);
					
					//configure default values
					for (MetadataFieldValue v : metadataValues) {
						//populate members first
						 if (v.getMetadataKey().equals(PatrolMetadataField.MEMBERS.name())) {
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
						if (v.getMetadataKey().equals(PatrolMetadataField.ARMED.name())) {
							btnArmed.setSelection(v.isVisible());
							btnArmedYes.setSelection(v.getBooleanValue());
							btnArmedNo.setSelection(!v.getBooleanValue());
							btnArmedrq.setSelection(true);
						}else if (v.getMetadataKey().equals(PatrolMetadataField.COMMENT.name())) {
							btnCmt.setSelection(v.isVisible());
							btnCmtrq.setSelection(v.isRequired());
							txtComment.setText(v.getStringValue());
						}else if (v.getMetadataKey().equals(PatrolMetadataField.LEADER.name())) {
							btnLeader.setSelection(v.isVisible());
							btnLeaderrq.setSelection(true);
							Employee temp = new Employee();
							temp.setUuid(v.getUuidValue());
							cmbLeader.setSelection(new StructuredSelection(temp));
						}else if(v.getMetadataKey().equals(PatrolMetadataField.MANDATE.name())){
							btnMandate.setSelection(v.isVisible());
							btnMandaterq.setSelection(true);
							PatrolMandate temp = new PatrolMandate();
							temp.setUuid(v.getUuidValue());
							cmbMandate.setSelection(new StructuredSelection(temp));
						}else if (v.getMetadataKey().equals(PatrolMetadataField.OBJECTIVE.name())) {
							btnObj.setSelection(v.isVisible());
							btnObjrq.setSelection(v.isRequired());
							txtObj.setText(v.getStringValue());
						}else if (v.getMetadataKey().equals(PatrolMetadataField.PILOT.name())) {
							btnPilot.setSelection(v.isVisible());
							btnPilotrq.setSelection(true);
							Employee temp = new Employee();
							temp.setUuid(v.getUuidValue());
							cmbPilot.setSelection(new StructuredSelection(temp));
						}else if(v.getMetadataKey().equals(PatrolMetadataField.STATION.name())) {
							btnStation.setSelection(v.isVisible());
							btnStationrq.setSelection(v.isRequired());
							Station temp = new Station();
							temp.setUuid(v.getUuidValue());
							cmbStation.setSelection(new StructuredSelection(temp));
						}else if(v.getMetadataKey().equals(PatrolMetadataField.TEAM.name())) {
							btnTeam.setSelection(v.isVisible());
							btnTeamrq.setSelection(v.isRequired());
							Team temp = new Team();
							temp.setUuid(v.getUuidValue());
							cmbTeam.setSelection(new StructuredSelection(temp));
						}else if(v.getMetadataKey().equals(PatrolMetadataField.TRANSPORT.name())) {
							btnTT.setSelection(v.isVisible());
							btnTTrq.setSelection(true);
							PatrolTransportType temp = new PatrolTransportType();
							temp.setUuid(v.getUuidValue());
							cmbTt.setSelection(new StructuredSelection(temp));
						}else {
							
							for (Entry<PatrolAttribute, Object[]> custom : customAttributes.entrySet()) {
								
								Object[] fields = custom.getValue();
								PatrolAttribute pa = custom.getKey();
								
								String key = PatrolMetadataField.generateKey(pa);
								if (!v.getMetadataKey().equalsIgnoreCase(key)) continue;
								
								((Button)fields[0]).setSelection(v.isVisible());
								((Button)fields[1]).setSelection(v.isRequired());
								
								if (pa.getType() == AttributeType.BOOLEAN) {
									if (v.getBooleanValue() != null) {
										((Button)fields[2]).setSelection(v.getBooleanValue());
										((Button)fields[3]).setSelection(!v.getBooleanValue());
									}
								}else if (pa.getType() == AttributeType.DATE) {
									DateTime dt = (DateTime) fields[2];
									if (v.getStringValue() != null) {
										SmartUtils.initDateTimeWidget(dt, LocalDate.parse(v.getStringValue()));
									}
								}else if (pa.getType() == AttributeType.LIST) {
									ComboViewer cmb = (ComboViewer) fields[2];
									if (v.getUuidValue() != null) {
										PatrolAttributeListItem temp = new PatrolAttributeListItem();
										temp.setUuid(v.getUuidValue());
										cmb.setSelection(new StructuredSelection(temp));
									}else {
										cmb.setSelection(new StructuredSelection("")); //$NON-NLS-1$
									}
								}else if (pa.getType() == AttributeType.TEXT) {
									((Text)fields[2]).setText(v.getStringValue());
								}else if (pa.getType() == AttributeType.NUMERIC) {
									((Text)fields[2]).setText(v.getStringValue());
								}
								
								((Button)fields[0]).notifyListeners(SWT.Selection, new Event());
							}
							
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
				onInitilized.run();
			});
			
			
			return Status.OK_STATUS;
		}
		
	};
}
