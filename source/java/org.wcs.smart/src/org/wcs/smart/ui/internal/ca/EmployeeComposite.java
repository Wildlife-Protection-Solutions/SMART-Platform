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
package org.wcs.smart.ui.internal.ca;

import java.text.Collator;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.EmployeeTeam;
import org.wcs.smart.ca.EmployeeTeamMember;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.SmartUserLevel;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.CheckBoxDropDown;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.user.UserLevelManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for displaying/editing employee details
 * 
 * @author Emily
 *
 */
public class EmployeeComposite extends Composite {

	private static final String MODIFIED_KEY = "modified"; //$NON-NLS-1$
	
	/* ui components */
	protected Text txtGivenName = null;
	protected Text txtFamilyName = null;
	protected Text txtStaffId = null;
	protected Composite composite = null;
	protected Text txtSmartId = null;
	protected Text txtSmartPassword = null;
	protected Button opFemale = null;
	protected Button opMale = null;
	protected Button chBirthDate = null;
	protected DateTime dtBirthDate = null;
	protected DateTime dtEmploymentStart = null;
	protected DateTime dtEmploymentEnd = null;
	protected Text txtSmartPassword2 = null;
	protected ComboViewer cmbViewerAgency = null;
	protected ComboViewer cmbViewerRank = null;
	protected CheckBoxDropDown cmbUserLevel = null;
	
	protected ListViewer lstTeams = null;
	
	private Button chNotActive = null;
	
	private ControlDecoration cdGiveName;
	private ControlDecoration cdFamilyName;
	private ControlDecoration cdStaffId;
	private ControlDecoration cdBirthDate;
	private ControlDecoration cdEmploymentEnd;
	private ControlDecoration cdEmploymentStart;
	private ControlDecoration cdSmartId;
	private ControlDecoration cdSmartPassword;
	private ControlDecoration cdSmartPassword2;
	private ControlDecoration cdUserLevel;

	protected Button chSmartUser = null;
	protected Label lblSmartUser = null;

	private List<Control> smartUserControls = new ArrayList<>();
	
	private List<Agency> agencies;
	
	public static final int AGENCY_RANK = 1 << 1;
	public static final int SMART_USER = 1 << 2;
	public static final int END_DATE = 1 << 3;
	public static final int SMART_USER_LEVEL = 1 << 4;
	public static final int TEAMS = 1 << 5;
	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param localStyle any combination of AGENCY_RANK, SMART_USER, END_DATE, SMART_USER_LEVEL, TEAMS
	 */
	public EmployeeComposite(Composite parent, int localStyle, List<Agency> agencies) {
		super(parent, SWT.NONE);

		this.agencies = agencies;

		createControl(localStyle);
	}

	public void createControl(int localStyle) {
		setLayout(new GridLayout(2, false));
		((GridLayout)this.getLayout()).marginWidth = 0;
		((GridLayout)this.getLayout()).marginHeight = 0;
		
		KeyListener validate = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				validate();
			}
		};
		Listener dateValidate = new Listener() {
			@Override
			public void handleEvent(Event event) {
				validate();
				
			}
		};
		
		Composite c = SmartUiUtils.createHeaderLabel(this, Messages.EmployeeComposite_DetailsSection);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				
		Label lbl = createLabelField(this, SmartLabelProvider.EMP_ID + ":"); //$NON-NLS-1$
		txtStaffId = createTextField(this, SWT.NONE,
				Employee.MAX_ID_LENGTH, validate);
		lbl.setToolTipText(
				MessageFormat.format(Messages.EmployeeComposite_AutoAssignId_Label, new Object[]{ EmployeeDialog.AUTO_GENERATE }));		
		txtStaffId.setText(EmployeeDialog.AUTO_GENERATE);
		txtStaffId.addKeyListener(validate);
		
		createLabelField(this, SmartLabelProvider.EMP_GIVEN_NAME + ":"); //$NON-NLS-1$
		txtGivenName = createTextField(this, SWT.NONE,
				Employee.MAX_NAME_LENGTH, validate);

		createLabelField(this,SmartLabelProvider.EMP_FAMILY_NAME + ":"); //$NON-NLS-1$
		txtFamilyName = createTextField(this, SWT.NONE,
				Employee.MAX_NAME_LENGTH, validate);
		
		
		createLabelField(this, SmartLabelProvider.EMP_EMPLOYEMENT_DATE + ":"); //$NON-NLS-1$
		dtEmploymentStart = createDateField(this, SWT.BORDER | SWT.DROP_DOWN
				| SWT.LONG | SWT.DATE, dateValidate);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = 8;
		dtEmploymentStart.setLayoutData(data);

		if ((localStyle & END_DATE) == END_DATE){
			Label blank = new Label(this, SWT.NULL);
			blank.setVisible(false);
			
			chNotActive = new Button(this, SWT.CHECK);
			chNotActive.setText(Messages.EmployeeComposite_Inactive_Label);
			chNotActive.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					dtEmploymentEnd.setEnabled(chNotActive.getSelection());
					enableFields(!chNotActive.getSelection());
					validate();
				}
			});
			chNotActive.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false ));
			((GridData)chNotActive.getLayoutData()).horizontalIndent = 8;
			
			createLabelField(this, SmartLabelProvider.EMP_EMPLOYEMENT_ENDDATE + ":"); //$NON-NLS-1$
			dtEmploymentEnd = createDateField(this, SWT.BORDER | SWT.DROP_DOWN
					| SWT.LONG | SWT.DATE, dateValidate);
			data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
			data.horizontalIndent = 8;
			dtEmploymentEnd.setLayoutData(data);

		}
		createLabelField(this, SmartLabelProvider.EMP_BIRTHDATE + ":"); //$NON-NLS-1$
		Composite tempComp = new Composite(this, SWT.NONE);
		tempComp.setLayout(new GridLayout(2, false));
		((GridLayout)tempComp.getLayout()).marginWidth = 0;
		((GridLayout)tempComp.getLayout()).marginHeight = 0;
		tempComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false ));
		((GridData)tempComp.getLayoutData()).horizontalIndent = 8;
		
		chBirthDate = new Button(tempComp, SWT.CHECK);
		chBirthDate.setEnabled(true);
		chBirthDate.setSelection(true);
		
		dtBirthDate = createDateField(tempComp, SWT.BORDER | SWT.DROP_DOWN	| SWT.LONG | SWT.DATE, dateValidate);
		//default the date to something other than today.
		dtBirthDate.setYear(1950);
		dtBirthDate.setMonth(0);
		dtBirthDate.setDay(1);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = 8;
		dtBirthDate.setLayoutData(data);
		
		chBirthDate.addListener(SWT.Selection, e->{
			dtBirthDate.setEnabled(chBirthDate.getSelection());
			validate();
		});
		createLabelField(this, SmartLabelProvider.EMP_GENDER + ":"); //$NON-NLS-1$

		composite = new Composite(this, SWT.NONE);
		composite.setLayout(new RowLayout(SWT.HORIZONTAL));
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = 8;
		composite.setLayoutData(data);

		opMale = new Button(composite, SWT.RADIO);
		opMale.setSelection(true);
		opMale.setText(Messages.EmployeeComposite_Male_Label);
		
		SelectionListener changedGender = new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent event) {
				validate();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent event){
				validate();
			}
		};
		opMale.addSelectionListener(changedGender);
		
		opFemale = new Button(composite, SWT.RADIO);
		opFemale.setText(Messages.EmployeeComposite_Female_Label);
		opFemale.addSelectionListener(changedGender);

		if ((localStyle & AGENCY_RANK) == AGENCY_RANK){
			createLabelField(this, SmartLabelProvider.EMP_AGENCY + ":"); //$NON-NLS-1$
			cmbViewerAgency = new ComboViewer(this, SWT.READ_ONLY | SWT.BORDER);
			cmbViewerAgency.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			cmbViewerAgency.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					return ((Agency)element).getName();
				}
			});
			cmbViewerAgency.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					updateRanks();
					validate();
				}
			});
			List<Agency> temp = new ArrayList<Agency>();
			temp.addAll(agencies);

			Agency ang = new Agency();
			ang.setName(""); //$NON-NLS-1$
			temp.add(0, ang);
			
			cmbViewerAgency.setContentProvider(ArrayContentProvider.getInstance());
			cmbViewerAgency.setInput(temp.toArray(new Agency[temp.size()]));
			cmbViewerAgency.getCombo().select(0);
			
			createLabelField(this, SmartLabelProvider.EMP_RANK + ":"); //$NON-NLS-1$
			cmbViewerRank = new ComboViewer(this, SWT.READ_ONLY | SWT.BORDER);
			cmbViewerRank.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			cmbViewerRank.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					return ((Rank)element).getName();
				}
			});
			cmbViewerRank.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					validate();
				}
			});
			cmbViewerRank.setContentProvider(ArrayContentProvider.getInstance());
			
			updateRanks();
		}
		if ((localStyle & TEAMS) == TEAMS) {
			Label l = createLabelField(this, Messages.EmployeeComposite_TeamsLabel);
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

			Composite teamComp = new Composite(this, SWT.NONE);
			teamComp.setLayout(new GridLayout(2, false));
			((GridLayout)teamComp.getLayout()).marginWidth = 0;
			((GridLayout)teamComp.getLayout()).marginHeight = 0;
			teamComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			lstTeams = new ListViewer(teamComp, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
			lstTeams.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridData)lstTeams.getControl().getLayoutData()).heightHint = 60;
			lstTeams.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof EmployeeTeamMember) return ((EmployeeTeamMember)element).getTeam().getName();
					return super.getText(element);
				}
			});
			lstTeams.setContentProvider(ArrayContentProvider.getInstance());
			lstTeams.setInput(new ArrayList<>());
			
			ToolBar tb = new ToolBar(teamComp, SWT.VERTICAL | SWT.FLAT);
			tb.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			
			ToolItem miAddTeam = new ToolItem(tb, SWT.PUSH);
			miAddTeam.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			miAddTeam.setToolTipText(Messages.EmployeeComposite_addtooltip);
			miAddTeam.addListener(SWT.Selection, e->addTeamMember());
			
			ToolItem miDeleteTeam = new ToolItem(tb, SWT.PUSH);
			miDeleteTeam.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDeleteTeam.setToolTipText(Messages.EmployeeComposite_removetooltip);
			miDeleteTeam.setEnabled(false);
			miDeleteTeam.addListener(SWT.Selection, e->deleteTeamMember());
			
			Menu mnuTeams = new Menu(lstTeams.getControl());
			lstTeams.getControl().setMenu(mnuTeams);
			
			MenuItem addTeam = new MenuItem(mnuTeams, SWT.PUSH);
			addTeam.setText(DialogConstants.ADD_BUTTON_TEXT);
			addTeam.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			addTeam.addListener(SWT.Selection, e->addTeamMember());

			
			MenuItem deleteTeam = new MenuItem(mnuTeams, SWT.PUSH);
			deleteTeam.setText(DialogConstants.DELETE_BUTTON_TEXT);
			deleteTeam.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			deleteTeam.setEnabled(false);
			deleteTeam.addListener(SWT.Selection, e->deleteTeamMember());
			lstTeams.addSelectionChangedListener(e->{
				deleteTeam.setEnabled(!lstTeams.getStructuredSelection().isEmpty());
				miDeleteTeam.setEnabled(!lstTeams.getStructuredSelection().isEmpty());
			});
		}
		
		if ((localStyle & SMART_USER) == SMART_USER){
			
			Composite header = SmartUiUtils.createHeaderLabel(this, Messages.EmployeeComposite_SmartUser_Label);
			header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			Composite smartc = new Composite(this, SWT.NONE );
			smartc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			smartc.setLayout(new GridLayout(2, false));
			((GridLayout)smartc.getLayout()).marginWidth = 0;
			((GridLayout)smartc.getLayout()).marginHeight = 0;
			
			lblSmartUser = createLabelField(smartc, Messages.EmployeeComposite_IsSmartUser_Label + ":"); //$NON-NLS-1$
			
			chSmartUser = new Button(smartc, SWT.CHECK);
			chSmartUser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			((GridData)chSmartUser.getLayoutData()).horizontalIndent = 8;
			chSmartUser.addSelectionListener(new SelectionAdapter() {		
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (chSmartUser.getSelection()){
						enableSmartUser(true);
					}else{
						enableSmartUser(false);
					}
					validate();
				}
			});
			if ((localStyle & SMART_USER_LEVEL) == SMART_USER_LEVEL){
				Label l = createLabelField(smartc, SmartLabelProvider.EMP_SMART_USER_LEVEL + ":"); //$NON-NLS-1$
				smartUserControls.add(l);
				
				cmbUserLevel = new CheckBoxDropDown(smartc);
				smartUserControls.add(cmbUserLevel);
				cmbUserLevel.setLabelProvider(new LabelProvider(){
					@Override
					public String getText(Object element){
						if (element instanceof SmartUserLevel) return ((SmartUserLevel) element).getGuiName(Locale.getDefault());
						return super.getText(element);
					}
				});
				cmbUserLevel.setContentProvider(ArrayContentProvider.getInstance());
				List<SmartUserLevel> levels = new ArrayList<SmartUserLevel>();
				levels.addAll(UserLevelManager.INSTANCE.getUserLevels().values());
				levels.sort((a,b) -> Collator.getInstance().compare(a.getGuiName(Locale.getDefault()), b.getGuiName(Locale.getDefault())));
				cmbUserLevel.setInput(levels);
				cmbUserLevel.addSelectionChangedListener(new ISelectionChangedListener() {
					
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						validate();
					}
				});
				cmbUserLevel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)cmbUserLevel.getLayoutData()).horizontalIndent = 8;
				cdUserLevel = createDecoration(cmbUserLevel);
				
			}
			
			Label l = createLabelField(smartc, SmartLabelProvider.EMP_SMART_USER + ":"); //$NON-NLS-1$
			smartUserControls.add(l);
			txtSmartId = createTextField(smartc, SWT.NONE,
					Employee.MAX_SMART_ID_LENGTH, validate);
			smartUserControls.add(txtSmartId);
			
			l = createLabelField(smartc, Messages.EmployeeComposite_Password_Label);
			smartUserControls.add(l);
			txtSmartPassword = createTextField(smartc, SWT.PASSWORD,
					Employee.MAX_SMART_PASSWORD_LENGTH, validate);
			smartUserControls.add(txtSmartPassword);
			txtSmartPassword.setData(MODIFIED_KEY, Boolean.FALSE);
			txtSmartPassword.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					txtSmartPassword.setData(MODIFIED_KEY, Boolean.TRUE);
				}
			});
			
			l = createLabelField(smartc, Messages.EmployeeComposite_Password2_Label);
			smartUserControls.add(l);
			txtSmartPassword2 = createTextField(smartc, SWT.PASSWORD,
					Employee.MAX_SMART_PASSWORD_LENGTH, validate);
			smartUserControls.add(txtSmartPassword2);
			enableSmartUser(false);
			chSmartUser.setSelection(false);
		}
		
		cdGiveName = createDecoration(txtGivenName);
		cdFamilyName = createDecoration(txtFamilyName);
		cdStaffId = createDecoration(txtStaffId);
		cdBirthDate= createDecoration(dtBirthDate);
		cdEmploymentStart= createDecoration(dtEmploymentStart);
		if (dtEmploymentEnd != null){
			cdEmploymentEnd = createDecoration(dtEmploymentEnd);
		}
		if (txtSmartId != null) cdSmartId = createDecoration(txtSmartId);
		if (txtSmartPassword != null) cdSmartPassword = createDecoration(txtSmartPassword);
		if (txtSmartPassword2 != null) cdSmartPassword2 = createDecoration(txtSmartPassword2);

		validate();
	}
	
	@SuppressWarnings("unchecked")
	private void deleteTeamMember() {
		List<EmployeeTeamMember> items = (List<EmployeeTeamMember>) lstTeams.getInput();
		for (Iterator<?> iterator = lstTeams.getStructuredSelection().iterator(); iterator.hasNext();) {
			EmployeeTeamMember employeeTeamMember = (EmployeeTeamMember) iterator.next();
			items.remove(employeeTeamMember);
		}
		validate();
		lstTeams.refresh();
	}
	
	@SuppressWarnings("unchecked")
	private void addTeamMember() {
		List<EmployeeTeamMember> items = (List<EmployeeTeamMember>) lstTeams.getInput();
		
		Set<EmployeeTeam> current = items.stream().map(t->t.getTeam()).collect(Collectors.toSet());
		
		TeamSelectDialog dialog = new TeamSelectDialog(getShell(), current);
		if (dialog.open() != Window.OK) return;
		
		
		for (EmployeeTeam t : dialog.teams) {
			if (current.contains(t)) continue;
			EmployeeTeamMember etm = new EmployeeTeamMember();
			etm.setTeam(t);
			items.add(etm);
		}
		validate();
		lstTeams.refresh();
	}
	private void updateRanks(){
		IStructuredSelection selection = ((IStructuredSelection) cmbViewerAgency
				.getSelection());
		if (selection.isEmpty()) {
			cmbViewerRank.getCombo().setEnabled(false);
		} else {
			Agency agency = (Agency) selection.getFirstElement();
			if (agency.getUuid() == null) {
				cmbViewerRank.getCombo().setEnabled(false);
			} else {
				cmbViewerRank.getCombo().setEnabled(true);
				List<Rank> ranks = agency.getRanks();

				List<Rank> temp = new ArrayList<Rank>();
				temp.addAll(ranks);
				Collections.sort(temp, new Comparator<Rank>(){
					@Override
					public int compare(Rank o1, Rank o2) {
						return Collator.getInstance().compare(o1.getName(), o2.getName());
					}});
				Rank none = new Rank();
				none.setName(""); //$NON-NLS-1$
				temp.add(0, none);
				cmbViewerRank.setInput(temp.toArray(new Rank[temp.size()]));
				cmbViewerRank.getCombo().select(0);
			}
		}
		cmbViewerRank.refresh();
	}
	private Text createTextField(Composite parent, int style, int maxLength,
			KeyListener validator) {
		Text txt = new Text(parent, SWT.BORDER | style);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = 8;
		txt.setLayoutData(data);
		txt.setTextLimit(maxLength);
		if (validator != null) {
			txt.addKeyListener(validator);
		}
		return txt;
	}

	private DateTime createDateField(Composite parent, int style, Listener dateValidator) {
		DateTime dt = new DateTime(parent, style);
		if (dateValidator != null) {
			dt.addListener(SWT.Selection, dateValidator);
		}
		return dt;
	}

	
	
	/*
	 * creates a label field
	 */
	private Label createLabelField(Composite parent, String text) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lbl.setText(text);
		return lbl;
	}
	


	/**
	 * Validate the input fields
	 * 
	 * @return <code>false</code> if not complete, <code>true</code> otherwise
	 */
	public boolean validate() {

		boolean isComplete = true;
		if (txtGivenName.getText().trim().isEmpty()
				|| ! SmartUtils.isSimpleString(txtGivenName.getText(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Employee.MAX_NAME_LENGTH) ) {
			cdGiveName.show();
			cdGiveName
					.setDescriptionText(
							MessageFormat.format(Messages.EmployeeComposite_Error_InvalidGivenName, new Object[]{Employee.MAX_NAME_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
			isComplete = false;
		}else{
			cdGiveName.hide();
		}
		
		if (txtFamilyName.getText().trim().isEmpty()
				|| ! SmartUtils.isSimpleString(txtFamilyName.getText(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Employee.MAX_NAME_LENGTH)) {
			cdFamilyName.show();
			cdFamilyName
					.setDescriptionText(
							MessageFormat.format(Messages.EmployeeComposite_Error_InvalidFamilyName, new Object[]{Employee.MAX_NAME_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
			isComplete = false;
		}else{
			cdFamilyName.hide();
		}
		
		if (txtStaffId.getText().trim().isEmpty()
				|| ! SmartUtils.isSimpleString(txtStaffId.getText(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Employee.MAX_ID_LENGTH)) {
			cdStaffId.show();
			cdStaffId
					.setDescriptionText(
							MessageFormat.format(Messages.EmployeeComposite_Error_EmployeeId,
									new Object[]{Employee.MAX_ID_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
			isComplete = false;
		}else{
			cdStaffId.hide();
		}
		
		// test for birthdate being at least Employee.MIN_EMPLOYEE_AGE years in the past
		LocalDate min = LocalDate.now().plusYears(Employee.MIN_EMPLOYEE_AGE);
		LocalDate bdate = SmartUtils.toDate(dtBirthDate);
		if(min.isBefore(bdate)){
			cdBirthDate.show();
			cdBirthDate.setDescriptionText(
					MessageFormat.format(
							Messages.EmployeeComposite_Error_InvalidBirthDate,
							new Object[]{Employee.MIN_EMPLOYEE_AGE} ));
			isComplete = false;
		}else{
			cdBirthDate.hide();
		}
		
		// test for startdate being at least Employee.MIN_EMPLOYEE_AGE since birthdate
		min = bdate.plusYears(Employee.MIN_EMPLOYEE_AGE);
		
		LocalDate eStartDate = SmartUtils.toDate(dtEmploymentStart);
		
		if(eStartDate.isBefore(min)){
			cdEmploymentStart.show();
			cdEmploymentStart.setDescriptionText(
					MessageFormat.format(
							Messages.EmployeeComposite_Error_InvalidStartDate ,
							new Object[]{Employee.MIN_EMPLOYEE_AGE}));
			isComplete = false;
		}else{
			cdEmploymentStart.hide();
		}
		
		if (cdEmploymentEnd != null && dtEmploymentEnd != null){
			boolean hide = true;
			if(chNotActive.getSelection()){
				//test for EmploymentEnd being before employment start
				if(SmartUtils.toDate(dtEmploymentEnd).isBefore(SmartUtils.toDate(dtEmploymentStart))){
					cdEmploymentEnd.show();
					cdEmploymentEnd.setDescriptionText(Messages.EmployeeComposite_Error_InvalidEndDate);
					isComplete = false;
					hide = false;
				}
			}
			if (hide){
				cdEmploymentEnd.hide();
			}
		}
		
		
		if (chSmartUser != null && chSmartUser.getSelection()){
			if (txtSmartId.getText().trim().isEmpty()
					||! SmartUtils.isSimpleString(txtSmartId.getText(), SmartUtils.RegExLevel.ALLOWED_CHARS_MED_REGEX, Employee.MAX_SMART_ID_LENGTH, Employee.MIN_SMART_ID_LENGTH)) {
				cdSmartId.show();
				cdSmartId.setDescriptionText(
						MessageFormat.format(
								Messages.EmployeeComposite_Error_InvalidUserId1,
								new Object[]{Employee.MIN_SMART_ID_LENGTH, Employee.MAX_SMART_ID_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_MED_REGEX.textDesc}));
				isComplete = false;
			}else{
				cdSmartId.hide();
			}
			
			if (txtSmartPassword.getText().trim().isEmpty()
					|| txtSmartPassword.getText().length() < Employee.MIN_SMART_PASSWORD_LENGTH || txtSmartPassword.getText().length() > Employee.MAX_SMART_PASSWORD_LENGTH) {
				cdSmartPassword.show();
				cdSmartPassword
						.setDescriptionText(
								MessageFormat.format(
										Messages.EmployeeComposite_Error_InvalidPassword1,
										new Object[]{Employee.MIN_SMART_PASSWORD_LENGTH, Employee.MAX_SMART_PASSWORD_LENGTH}));
				isComplete = false;
			}else{
				cdSmartPassword.hide();
			}
			
			boolean hide = true;
			if (!txtSmartPassword.getText().equals(txtSmartPassword2.getText())) {
				isComplete = false;
				if (!txtSmartPassword.getText().trim().isEmpty()
					|| !txtSmartPassword2.getText().trim().isEmpty()) {
					cdSmartPassword2.show();
					cdSmartPassword2.setDescriptionText(Messages.EmployeeComposite_Error_PasswordsDontMatch);	
					hide = false;
				}
			}
			if (hide){
				cdSmartPassword2.hide();
			}
			
			if (cmbUserLevel != null){
				if (cmbUserLevel.getCheckObjects().size() == 0){
					cdUserLevel.show();
					cdUserLevel.setDescriptionText(Messages.EmployeeComposite_UserLevelRequired);
				}else{
					cdUserLevel.hide();
				}
			}
		}else if (chSmartUser != null){
			cdSmartId.hide();
			cdSmartPassword.hide();
			cdSmartPassword2.hide();
		}
		return isComplete;
	}
	
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	public void initFields(Employee e){
		txtGivenName.setText(e.getGivenName());
		txtFamilyName.setText(e.getFamilyName());
		txtStaffId.setText(e.getId());
				
		opFemale.setSelection(e.getGender() == Employee.DB_FEMALE);
		opMale.setSelection(e.getGender() ==  Employee.DB_MALE);
		
		if (e.getBirthDate() != null) {
			SmartUtils.initDateTimeWidget(dtBirthDate, e.getBirthDate());
			chBirthDate.setSelection(true);
			dtBirthDate.setEnabled(true);
		}else {
			chBirthDate.setSelection(false);
			dtBirthDate.setEnabled(false);
		}
		SmartUtils.initDateTimeWidget(dtEmploymentStart, e.getStartEmploymentDate());
		
		if (chNotActive != null) {
			if (e.getEndEmploymentDate() != null){
				chNotActive.setSelection(true);
				dtEmploymentEnd.setEnabled(true);
				SmartUtils.initDateTimeWidget(dtEmploymentEnd, e.getEndEmploymentDate());
			}else{
				chNotActive.setSelection(false);
				dtEmploymentEnd.setEnabled(false);
			}
		}
		
		if (cmbViewerAgency != null){
			if (e.getAgency() != null){
				cmbViewerAgency.setSelection(new StructuredSelection(e.getAgency()));
			}
			if (e.getRank() != null){
				cmbViewerRank.setSelection(new StructuredSelection(e.getRank()));
			}
			ViewerComparator comp = new ViewerComparator(new Comparator<String>() {
			    @Override
			    public int compare(String arg0, String arg1) {
			        return Collator.getInstance().compare(arg0, arg1);
			    }
			});
			cmbViewerRank.setComparator(comp); 
			cmbViewerAgency.setComparator(comp); 
		}
		
		if (chSmartUser != null){
			if (e.getSmartUserId() != null){
				chSmartUser.setSelection(true);
				
				txtSmartId.setText(e.getSmartUserId());
				txtSmartPassword.setText(e.getSmartPassword());
				txtSmartPassword2.setText(e.getSmartPassword());
				if (cmbUserLevel != null){
					List<SmartUserLevel> levels = new ArrayList<SmartUserLevel>();
					for (String s : e.getSmartUserLevels()){
						SmartUserLevel l = UserLevelManager.INSTANCE.getUserLevel(s);
						if (l != null) levels.add(l);
					}
					cmbUserLevel.setValue(levels);
				}
				enableSmartUser(true);
			}else{
				enableSmartUser(false);
				chSmartUser.setSelection(false);
				txtSmartId.setText(""); //$NON-NLS-1$
				txtSmartPassword.setText(""); //$NON-NLS-1$
				txtSmartPassword2.setText(""); //$NON-NLS-1$ 
				if (cmbUserLevel != null){
					cmbUserLevel.setValue(Collections.emptyList());
				}
			}
			txtSmartPassword.setData(MODIFIED_KEY, false);
		}
		if (lstTeams != null) {
			lstTeams.setInput(new ArrayList<>(e.getEmployeeTeams()));
		}
		
		if (chNotActive != null) enableFields(!chNotActive.getSelection());
		validate();
	}
	private void enableFields(boolean enable){
		Control[] controls = new Control[]{
				this.txtFamilyName,
				this.txtGivenName,
				this.txtStaffId,
				this.txtSmartId,
				this.dtBirthDate,
				this.chBirthDate,
//				this.dtEmploymentEnd,
				this.dtEmploymentStart,
				this.opMale,
				this.opFemale,
				this.chSmartUser,
				this.cmbUserLevel,
				this.cmbViewerAgency.getCombo(),
				this.cmbViewerRank.getCombo(),
				this.lstTeams.getControl()
		};
		for (int i = 0; i < controls.length; i ++){
			if (controls[i] != null){
				controls[i].setEnabled(enable);
			}
		}
		if (chSmartUser != null && enable){
			enableSmartUser(chSmartUser.getSelection());
		}
		if (enable) dtBirthDate.setEnabled(chBirthDate.getSelection());
	}
	
	protected void enableSmartUser(boolean enable){
		for (Control c : smartUserControls) c.setEnabled(enable);
		if (enable){
			validate();
		}
	}
	
	/**
	 * Updates the employee object with the data 
	 * input into the form
	 * 
	 * @param e updates employee object
	 */
	@SuppressWarnings("unchecked")
	public void updateEmploye(Employee e){
		e.setStartEmploymentDate(SmartUtils.toDate(dtEmploymentStart));
		e.setFamilyName(txtFamilyName.getText().trim());
		e.setGender(opFemale.getSelection() ? Employee.DB_FEMALE : Employee.DB_MALE);
		e.setGivenName(txtGivenName.getText().trim());
		e.setId(txtStaffId.getText().trim());
		
		if (chBirthDate.getSelection()) {
			e.setBirthDate(SmartUtils.toDate(dtBirthDate));
		}else {
			e.setBirthDate(null);
		}
		
		if (dtEmploymentEnd != null){
			if (!chNotActive.getSelection()){
				e.setEndEmploymentDate(null);
			}else{
				e.setEndEmploymentDate(SmartUtils.toDate(dtEmploymentEnd));
			}
		}
		
		e.setAgency(null);
		e.setRank(null);
		if (cmbViewerAgency != null){
		
			IStructuredSelection a = (IStructuredSelection) cmbViewerAgency.getSelection();
			if (!a.isEmpty()) {
				Agency ag = (Agency) a.getFirstElement();
				if (ag.getUuid() != null) {
					e.setAgency(ag);
				}
			}

			if (cmbViewerRank.getCombo().isEnabled()) {
				a = (IStructuredSelection) cmbViewerRank.getSelection();
				if (!a.isEmpty()) {
					Rank ag = (Rank) a.getFirstElement();
					if (ag.getUuid() != null) {
						e.setRank(ag);
					}
				}
			}
		}
		
		if (chSmartUser != null){
			if (chSmartUser.getSelection()){
				if ((Boolean)txtSmartPassword.getData(MODIFIED_KEY)){
					//if password was modified then update it otherwise leave it alone
					e.setSmartPassword(HibernateManager.generatePassword(txtSmartPassword.getText()));
				}
				e.setSmartUserId(txtSmartId.getText().trim());
				if (cmbUserLevel != null){
					e.setSmartUserLevel((Collection<SmartUserLevel>) cmbUserLevel.getCheckObjects());
				}else{
					e.setSmartUserLevel(Collections.singleton(UserLevelManager.ADMIN));
				}
			}else{
				e.setSmartUserId(null);
				e.setSmartUserLevelKeys(null);
				e.setSmartPassword(null);
			}
		}
		
		if (lstTeams != null) {
			if (e.getEmployeeTeams() == null) e.setEmployeeTeams(new ArrayList<>());
			List<EmployeeTeamMember> members = (List<EmployeeTeamMember>) lstTeams.getInput();
			
			for (EmployeeTeamMember m : members) {
				if (m.getEmployee() == null) {
					m.setEmployee(e);
					e.getEmployeeTeams().add(m);
				}
			}
			List<EmployeeTeamMember> todelete = new ArrayList<>();
			for(EmployeeTeamMember i : e.getEmployeeTeams()) {
				if (!members.contains(i)) todelete.add(i);
			}
			e.getEmployeeTeams().removeAll(todelete);
		}
	}
	
	/**
	 * The entered smart user id or null if user
	 * cannot enter smart user id.
	 * @return
	 */
	public String getSmartUser(){
		if (txtSmartId == null) return null;
		return this.txtSmartId.getText();
	}
	/**
	 * 
	 * @return true if the user can enter smart username otherwise false
	 */
	public boolean getSmartUserSelected(){
		if (chSmartUser == null) return false;
		return this.chSmartUser.getSelection();
	}
	
	
	private class TeamSelectDialog extends SmartStyledDialog{

		private List<EmployeeTeam> teams;
		private TableViewer tblViewer;
		private Set<EmployeeTeam> existing;
		
		protected TeamSelectDialog(Shell parent, Set<EmployeeTeam> existing) {
			super(parent);
			this.existing = existing;
		}
		
		@Override
		public void okPressed() {
			teams = new ArrayList<>();
			for (Iterator<?> iterator = tblViewer.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object o = (Object) iterator.next();
				if (o instanceof EmployeeTeam) teams.add((EmployeeTeam)o);
			}
			super.okPressed();
		}
		
		@Override
		public Point getInitialSize() {
			return new Point(350, 350);
		}
		
		@Override
		public Control createDialogArea(Composite parent) {
			Composite main = (Composite) super.createDialogArea(parent);
			
			Composite outer = new Composite(main, SWT.NONE);
			outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true ));
			outer.setLayout(new TableColumnLayout());
			
			tblViewer = new TableViewer(outer, SWT.BORDER | SWT.MULTI);
			
			tblViewer.setContentProvider(ArrayContentProvider.getInstance());
			tblViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			TableViewerColumn col1 = new TableViewerColumn(tblViewer, SWT.NONE);
			col1.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof EmployeeTeam) return ((EmployeeTeam)element).getName();
					return super.getText(element);
				}
			});
			((TableColumnLayout)outer.getLayout()).setColumnData(col1.getColumn(), new ColumnWeightData(1));
			
			tblViewer.addDoubleClickListener(evt->okPressed());
			
			Job load = new Job(Messages.EmployeeComposite_loadteamsjob) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<EmployeeTeam> items = new ArrayList<>();
					try(Session session = HibernateManager.openSession()){
						items.addAll(QueryFactory.buildQuery(session, EmployeeTeam.class,
								new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
					}
					items.removeAll(existing);
					
					Display.getDefault().syncExec(()->{
						tblViewer.setInput(items);
					});
					return Status.OK_STATUS;
				}
				
			};
			load.schedule();
			getShell().setText(Messages.EmployeeComposite_ShellTitle);
			return main;
		}
		
	}
}
