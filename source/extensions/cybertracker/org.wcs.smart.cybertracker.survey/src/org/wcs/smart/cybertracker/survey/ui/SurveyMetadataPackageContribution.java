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

import java.net.URI;
import java.net.URL;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.EmployeeTeam;
import org.wcs.smart.ca.EmployeeTeamMember;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldUuidValue;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.model.MissionMetadataField;
import org.wcs.smart.cybertracker.survey.model.SurveyCtPackage;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.NamedIconItemLabelProvider;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Patrol metadata contribution
 * 
 * @author Emily
 *
 */
public class SurveyMetadataPackageContribution implements IPackageUiContribution {


	private static final String CUSTOMKEY = "ISCUSTOM"; //$NON-NLS-1$
	
	private Button btnCmt, btnMembers, btnLeader, btnCmtrq, btnMembersrq, btnLeaderrq;
	private ComboViewer cmbLeader;
	private Text txtComment;
	
	private HashMap<MissionAttribute, Object[]> customAttributes;
	
	private CheckboxTableViewer lstEmployees;
	private Listener onModified;
	
	private ICtPackage ctpackage;
	private boolean fireEvents = true;
	private Font boldFont;
	private Runnable onInitilized;
	private Composite core;
	
	@Inject private IEclipseContext context;

	

	private SurveyDesign current = null;
	
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
	
	private void setImage(Label label, MissionMetadataField field, IconSet iconSet) {
		URI uri = field.getIcon(iconSet);
		if (uri == null) return;
		try {
			URL url = uri.toURL();
			Image img = SmartUtils.getImage(url, IconManager.Size.SMALL.size);
			if (img != null) {
				label.setImage(img);
				label.addListener(SWT.Dispose, e->img.dispose());
			}
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
		}
	}
	
	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified, Runnable onInitilized) {
		this.ctpackage = ctpackage;
		this.onModified = onModified;
		this.onInitilized = onInitilized;
		
		
		IconSet defaultIs = null;
		try(Session session = HibernateManager.openSession()){
			defaultIs = IconManager.INSTANCE.getDefaultIconSet(session, ctpackage.getConservationArea());
		}
		
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
		col0.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label col1 = new Label(headerComp, SWT.NONE);
		col1.setText(Messages.SurveyMetadataPackageContribution_MetadataColumn);
		col1.setToolTipText(Messages.SurveyMetadataPackageContribution_MetadataTooltip);
		col1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		Label col2 = new Label(headerComp, SWT.NONE);
		col2.setText(Messages.SurveyMetadataPackageContribution_UserSelectedLabel);
		col2.setToolTipText(Messages.SurveyMetadataPackageContribution_UserSelectedTooltip);
		col2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label col3 = new Label(headerComp, SWT.NONE);
		col3.setText(Messages.SurveyMetadataPackageContribution_RequiredField);
		col3.setToolTipText(Messages.SurveyMetadataPackageContribution_RequiredFieldTooltip);
		col3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label col4 = new Label(headerComp, SWT.NONE);
		col4.setText(Messages.SurveyMetadataPackageContribution_FixedValueLabel);
		col4.setToolTipText(Messages.SurveyMetadataPackageContribution_FixedValueTooltip);
		col4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ScrolledComposite table = new ScrolledComposite(outer, SWT.V_SCROLL);
		table.setExpandHorizontal(true);
		table.setExpandVertical(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		core = new Composite(table, SWT.NONE);
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
		
		// comment
		Object[] d = createTextSection(Messages.SurveyMetadataPackageContribution_MissionCommentLabel, core, MissionMetadataField.COMMENT, true, defaultIs);
		btnCmt = (Button) d[0];
		btnCmtrq = (Button) d[1];
		btnCmtrq.setSelection(MissionMetadataField.COMMENT.isRequired());
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
		Label l = new Label(core, SWT.NONE);
		setImage(l, MissionMetadataField.MEMBERS, defaultIs);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		l = new Label(core, SWT.NONE);
		l.setText(Messages.SurveyMetadataPackageContribution_MembersLabel);
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
		
		
		Composite cbottom = new Composite(cemp, SWT.NONE);
		cbottom.setLayout(new GridLayout(3, false));
		cbottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnChecked = new Button(cbottom, SWT.CHECK);
		btnChecked.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnChecked.setText(Messages.SurveyMetadataPackageContribution_BtnOnlyChecked);
		btnChecked.addListener(SWT.Selection, e->{
			lstEmployees.getControl().setVisible(false);
			if (btnChecked.getSelection()) lstEmployees.addFilter(filter);
			else lstEmployees.removeFilter(filter);
			lstEmployees.getControl().setVisible(true);
		});
		
		Link lnkAll = new Link(cbottom, SWT.NONE);
		lnkAll.setText("<a>" +  DialogConstants.SELECT_ALL + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnkAll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		lnkAll.addListener(SWT.Selection, e->lstEmployees.setAllChecked(true));
		
		Link lnkNone = new Link(cbottom, SWT.NONE);
		lnkNone.setText("<a>" + DialogConstants.DESELECT_ALL  + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnkNone.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		lnkNone.addListener(SWT.Selection, e->lstEmployees.setAllChecked(false));
		
		Menu mnu = new Menu(lstEmployees.getControl());
		MenuItem miall = new MenuItem(mnu, SWT.DEFAULT);
		miall.setText(DialogConstants.SELECT_ALL);
		miall.addListener(SWT.Selection, e->lstEmployees.setAllChecked(true));
		MenuItem minone = new MenuItem(mnu, SWT.DEFAULT);
		minone.setText(DialogConstants.DESELECT_ALL);
		minone.addListener(SWT.Selection, e->lstEmployees.setAllChecked(false));
		lstEmployees.getControl().setMenu(mnu);
		
		// leader
		d = createComboViewerSection(Messages.SurveyMetadataPackageContribution_LeaderLabel, core, 
				MissionMetadataField.LEADER, false, defaultIs);
		btnLeader = (Button) d[0];
		btnLeaderrq = (Button) d[1];
		cmbLeader = (ComboViewer) d[2];
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
		
		createMetadataAttributes();
		
		table.setMinSize(core.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		core.layout(true);
		((GridData) col0.getLayoutData()).widthHint = 0;
		((GridData) col1.getLayoutData()).widthHint = c2.getSize().x + c1.getSize().x;
		((GridData) col2.getLayoutData()).widthHint = c3.getSize().x;
		((GridData) col3.getLayoutData()).widthHint = c4.getSize().x;
		((GridData) col4.getLayoutData()).widthHint = c5.getSize().x;
				
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
		if (cmbLeader.getStructuredSelection().isEmpty() && items.size() > 0) cmbLeader.setSelection(new StructuredSelection(sorted.get(0)));
	}
	
	private Object[] createComboViewerSection(String sectionName, Composite parent, MissionMetadataField field, boolean isOptional, IconSet defaultIs) {
		Composite g = parent;
		
		Label l = new Label(g, SWT.NONE);
		setImage(l, field, defaultIs);
		
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
		}else {
			btnrq.addListener(SWT.Selection,e->fireChanged());
		}
		
		ComboViewer cmb = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmb.setContentProvider(ArrayContentProvider.getInstance());
		cmb.setLabelProvider(new NamedItemLabelProvider());
		cmb.setInput(new String[] { DialogConstants.LOADING_TEXT });
		cmb.getControl().setEnabled(false);
		cmb.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		btn.addListener(SWT.Selection, e-> {
			cmb.getControl().setEnabled(!btn.getSelection());
			if (isOptional) btnrq.setEnabled(btn.getSelection());
			fireChanged();
		});
		
		cmb.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		cmb.addSelectionChangedListener(e->fireChanged());
		
		return new Object[] {btn,btnrq, cmb};
	}
	
	private Object[] createTextSection(String sectionName, Composite parent, MissionMetadataField field, boolean isOptional, IconSet defaultIs) {
		Composite g = parent;
		
		Label l = new Label(g, SWT.NONE);
		setImage(l, field, defaultIs);
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
		
		if (!isOptional) {
			btnrq.setEnabled(false);
		}else {
			btnrq.addListener(SWT.Selection,e->fireChanged());
		}
		
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
		
		//the survey design may have changed
		fireEvents = false;
		try {
			createMetadataAttributes();
		}finally {
			fireEvents = true;
		}
		
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
			return Messages.SurveyMetadataPackageContribution_EmployeeRequired;
		}
		
		if (!btnMembers.getSelection() && !btnLeader.getSelection() && cmbLeader.getStructuredSelection().isEmpty()) {
			return Messages.SurveyMetadataPackageContribution_LeaderRequired;
		}
		
		if (customAttributes == null) return null;
		
		for (Entry<MissionAttribute, Object[]> item : customAttributes.entrySet()) {
			MissionAttribute pa = item.getKey();
			Button btnVisible = (Button)item.getValue()[0];
			if (!btnVisible.getSelection() && pa.getType() == AttributeType.NUMERIC) {
				Text txt = (Text) item.getValue()[2];
				if (txt.getText().isBlank()) return null;
				try {
					Double.parseDouble(txt.getText());
				}catch (Exception ex) {
					return MessageFormat.format(Messages.SurveyMetadataPackageContribution_InvalidAttributeValue, pa.getName(), txt.getText()); 
				}
			}
		}
		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage, Session session) {
		SurveyCtPackage cpackage = ((SurveyCtPackage)ctpackage);
		if (cpackage.getMetadataValues() == null) cpackage.setMetadataValues(new ArrayList<>());
		
		List<MetadataFieldValue> items = ((SurveyCtPackage)ctpackage).getMetadataValues();
		HashMap<String, MetadataFieldValue> map = new HashMap<>();
		for (MetadataFieldValue v : items) {
			map.put(v.getMetadataKey(), v);
		}
		
		MetadataFieldValue v = findMetadataValue(map, MissionMetadataField.COMMENT,  btnCmt, btnCmtrq, cpackage, session);
		v.setStringValue(txtComment.getText().strip());
		
		v = findMetadataValue(map, MissionMetadataField.LEADER, btnLeader, btnLeaderrq, cpackage, session);
		if (cmbLeader.getStructuredSelection().isEmpty()) {
			v.setUuidValue(null);
		}else {
			v.setUuidValue( ((Employee)cmbLeader.getStructuredSelection().getFirstElement()).getUuid() );
		}
		
		v = findMetadataValue(map, MissionMetadataField.MEMBERS, btnMembers, btnMembersrq, cpackage, session);
		if (v.getUuidList() == null) v.setUuidList(new ArrayList<>());
		v.getUuidList().forEach(e->session.remove(e));
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
		
		Set<String> customKeys = new HashSet<>();
		for (Entry<MissionAttribute, Object[]> custom : customAttributes.entrySet()) {
			Object[] fields = custom.getValue();
			MissionAttribute ma = custom.getKey();
			v = findMetadataValue(map, ma, (Button)fields[0], (Button)fields[1], cpackage, session);
			customKeys.add(MissionMetadataField.generateKey(ma));
			if (ma.getType() == AttributeType.LIST) {
				TableComboViewer cmb = (TableComboViewer) fields[2];
				Object o = cmb.getStructuredSelection().getFirstElement();
				if (o instanceof MissionAttributeListItem) {
					v.setUuidValue(((MissionAttributeListItem)o).getUuid());
				}else {
					v.setUuidValue(null);
				}
			}else if (ma.getType() == AttributeType.TEXT) {
				v.setStringValue(((Text)fields[2]).getText());
			}else if (ma.getType() == AttributeType.NUMERIC) {
				v.setStringValue(((Text)fields[2]).getText());
			}
		}
		//remove no longer used attribute metadata values
		List<MetadataFieldValue> toremove = new ArrayList<>();
		for (MetadataFieldValue mv : cpackage.getMetadataValues()) {
			if (mv.getMetadataKey().startsWith(MissionMetadataField.getCustomAttributePrefix())) {
				if (!customKeys.contains(mv.getMetadataKey())) toremove.add(mv);
			}
		}
		cpackage.getMetadataValues().removeAll(toremove);
	}
	
	private MetadataFieldValue findMetadataValue(HashMap<String, MetadataFieldValue> items, MissionMetadataField field, 
			Button btnVisible, Button btnRequired, SurveyCtPackage cpackage, Session session) {
		MetadataFieldValue v = items.get(field.name());
		if (v == null) {
			v = new MetadataFieldValue();
			v.setCtPackage(cpackage);
			v.setConservationArea(cpackage.getConservationArea());
			v.setMetadataKey(field.name());
			v.setRequired(field.isRequired());
			session.persist(v);
			cpackage.getMetadataValues().add(v);
		}
		v.setVisible(btnVisible.getSelection());
		v.setRequired(btnRequired.getSelection());
		return v;
	}
	
	private MetadataFieldValue findMetadataValue(HashMap<String, MetadataFieldValue> items, 
			MissionAttribute attribute, Button btnVisible, Button btnRequired,
			SurveyCtPackage ppackage, Session session) {
		
		MetadataFieldValue v = items.get(MissionMetadataField.generateKey(attribute));
		if (v == null) {
			v = new MetadataFieldValue();
			v.setCtPackage((AbstractCtPackage)ppackage);
			v.setConservationArea(ppackage.getConservationArea());
			v.setMetadataKey(MissionMetadataField.generateKey(attribute));
			v.setRequired(false);
			session.persist(v);
			ppackage.getMetadataValues().add(v);
		}
		v.setVisible(btnVisible.getSelection());
		v.setRequired(btnRequired.getSelection());
		return v;
	}
	
	private void createMetadataAttributes() {
		
		SurveyDesign ctx = context.get(SurveyDesign.class);
		
		if (ctx == null && current == null) return;
		if (ctx != null && ctx.equals(current)) return;
		
		current = ctx;
		customAttributes = new HashMap<>();
		for (Control kid : core.getChildren()) {
			Object d = kid.getData(CUSTOMKEY);
			if ( d != null && (Boolean)d) kid.dispose();
		}
		
		if (current == null) return;
		
		//add custom patrol attributes
		try (Session session = HibernateManager.openSession()) {

			SurveyDesign sd = session.get(SurveyDesign.class, current.getUuid());
			List<MissionAttribute> attributes = sd.getMissionProperties()
					.stream().map(p->p.getAttribute()).collect(Collectors.toList());
			
			attributes.sort((a, b) -> Collator.getInstance().compare(a.getName(), b.getName()));

			for (MissionAttribute attribute : attributes) {

				Label icon = new Label(core, SWT.NONE);
				icon.setData(CUSTOMKEY, true);
				icon.setBackground(icon.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				if (attribute.getIcon() != null) {
					attribute.getIcon().getFiles().forEach(e->e.computeFileLocation(session));
					Image img = IconManager.INSTANCE.getThumbnail(attribute.getIcon(), IconManager.Size.SMALL);
					if (img != null) {
						icon.setImage(img);
						icon.addListener(SWT.Dispose, e->img.dispose());
					}
				}
				
				Label name = new Label(core, SWT.NONE);
				name.setText(attribute.getName());
				name.setData(CUSTOMKEY, true);
				name.setBackground(name.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				
				Object[] data = new Object[4];

				Button btnSelected = new Button(core, SWT.CHECK);
				btnSelected.setSelection(true);
				btnSelected.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
				btnSelected.setData(CUSTOMKEY, true);
				
				Button btnRequired = new Button(core, SWT.CHECK);
				btnRequired.setSelection(false);
				btnRequired.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
				btnRequired.setData(CUSTOMKEY, true);
				
				data[0] = btnSelected;
				data[1] = btnRequired;

				// only text, number, list
				if (attribute.getType() == AttributeType.TEXT) {
					Text txt = new Text(core, SWT.BORDER);
					txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					txt.setEnabled(false);
					txt.addListener(SWT.Modify, e -> fireChanged());
					txt.setData(CUSTOMKEY, true);
					data[2] = txt;
					btnSelected.addListener(SWT.Selection, e -> {
						txt.setEnabled(!btnSelected.getSelection());
					});
				} else if (attribute.getType() == AttributeType.NUMERIC) {
					Text txt = new Text(core, SWT.BORDER);
					txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					txt.setEnabled(false);
					txt.addListener(SWT.Modify, e -> fireChanged());
					txt.setData(CUSTOMKEY, true);
					data[2] = txt;
					btnSelected.addListener(SWT.Selection, e -> {
						txt.setEnabled(!btnSelected.getSelection());
					});
				} else if (attribute.getType() == AttributeType.LIST) {
					TableComboViewer cmb = new TableComboViewer(core, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
					cmb.getControl().setData(CUSTOMKEY, true);
					data[2] = cmb;
					cmb.setContentProvider(ArrayContentProvider.getInstance());
					cmb.setLabelProvider(new NamedIconItemLabelProvider(IconManager.Size.SMALL));
					List<Object> items = new ArrayList<>();
					items.add(""); //$NON-NLS-1$
					items.addAll(attribute.getAttributeList());
					cmb.setInput(items);
					cmb.getControl().setEnabled(false);
					cmb.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

					cmb.addSelectionChangedListener(e -> fireChanged());

					btnSelected.addListener(SWT.Selection, e -> {
						cmb.getControl().setEnabled(!btnSelected.getSelection());
					});
				} else {
					Label temp = new Label(core, SWT.NONE);
					temp.setData(CUSTOMKEY, true);
					data[2] = null;
				}

				btnSelected.addListener(SWT.Selection, e -> {
					btnRequired.setEnabled(btnSelected.getSelection());
					fireChanged();
				});

				btnRequired.addListener(SWT.Selection, e -> fireChanged());

				customAttributes.put(attribute, data);
			}
		}

		//initialize
		initializeCustomAttributes();
		
		core.layout(true);
		((ScrolledComposite)core.getParent()).setMinSize(core.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	private void initializeCustomAttributes() {
		if (customAttributes == null) return;
		for (MetadataFieldValue v : ctpackage.getMetadataValues()) {
			for (Entry<MissionAttribute, Object[]> custom : customAttributes.entrySet()) {
				
				Object[] fields = custom.getValue();
				MissionAttribute pa = custom.getKey();
				
				String key = MissionMetadataField.generateKey(pa);
				if (!v.getMetadataKey().equalsIgnoreCase(key)) continue;
				
				((Button)fields[0]).setSelection(v.isVisible());
				((Button)fields[1]).setSelection(v.isRequired());
				
				if (pa.getType() == AttributeType.LIST) {
					TableComboViewer cmb = (TableComboViewer) fields[2];
					if (v.getUuidValue() != null) {
						MissionAttributeListItem temp = new MissionAttributeListItem();
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
				eteams.forEach(team->team.getActiveMembers().forEach(e->e.getEmployee().getGivenName()));
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
							btnMembersrq.setSelection(v.isRequired());
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
							btnCmtrq.setSelection(v.isRequired());
							txtComment.setText(v.getStringValue());
						}else if (v.getMetadataKey().equals(MissionMetadataField.LEADER.name())) {
							btnLeader.setSelection(v.isVisible());
							btnLeaderrq.setSelection(v.isRequired());
							Employee temp = new Employee();
							temp.setUuid(v.getUuidValue());
							cmbLeader.setSelection(new StructuredSelection(temp));
						}						
					}
					btnCmt.notifyListeners(SWT.Selection, new Event());
					btnMembers.notifyListeners(SWT.Selection, new Event());
					btnLeader.notifyListeners(SWT.Selection, new Event());
					
					initializeCustomAttributes();
				}finally {
					fireEvents = true;
				}
				onInitilized.run();
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
