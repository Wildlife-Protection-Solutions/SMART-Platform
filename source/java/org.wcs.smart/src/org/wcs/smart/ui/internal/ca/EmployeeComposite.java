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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.SmartUserLevel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.CheckBoxDropDown;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.user.UserLevelManager;
import org.wcs.smart.util.SmartUtils;

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
	protected DateTime dtBirthDate = null;
	protected DateTime dtEmploymentStart = null;
	protected DateTime dtEmploymentEnd = null;
	protected Text txtSmartPassword2 = null;
	protected ComboViewer cmbViewerAgency = null;
	protected ComboViewer cmbViewerRank = null;
	protected CheckBoxDropDown cmbUserLevel = null;
	
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

	private Group smartc = null;
	protected Button chSmartUser = null;

	private List<Agency> agencies;
	
	public static final int AGENCY_RANK = 1 << 1;
	public static final int SMART_USER = 1 << 2;
	public static final int END_DATE = 1 << 3;
	public static final int SMART_USER_LEVEL = 1 << 4;
	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param localStyle any combination of AGENCY_RANK, SMART_USER, END_DATE, SMART_USER_LEVEL
	 */
	public EmployeeComposite(Composite parent, int localStyle, List<Agency> agencies) {
		super(parent, SWT.NONE);

		this.agencies = agencies;

		createControl(localStyle);
	}

	public void createControl(int localStyle) {
		setLayout(new GridLayout(2, false));
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
			
			createLabelField(this, SmartLabelProvider.EMP_EMPLOYEMENT_ENDDATE + ":"); //$NON-NLS-1$
			dtEmploymentEnd = createDateField(this, SWT.BORDER | SWT.DROP_DOWN
					| SWT.LONG | SWT.DATE, dateValidate);
			data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
			data.horizontalIndent = 8;
			dtEmploymentEnd.setLayoutData(data);

		}
		createLabelField(this, SmartLabelProvider.EMP_BIRTHDATE + ":"); //$NON-NLS-1$
		dtBirthDate = createDateField(this, SWT.BORDER | SWT.DROP_DOWN	| SWT.LONG | SWT.DATE, dateValidate);
		//default the date to something other than today.
		dtBirthDate.setYear(1950);
		dtBirthDate.setMonth(0);
		dtBirthDate.setDay(1);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = 8;
		dtBirthDate.setLayoutData(data);

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
		if ((localStyle & SMART_USER) == SMART_USER){
			smartc = new Group(this, SWT.NONE );
			smartc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			smartc.setLayout(new GridLayout(2, false));
			
			chSmartUser = new Button(smartc, SWT.CHECK);
			chSmartUser.setText(Messages.EmployeeComposite_IsSmartUser_Label);
			chSmartUser.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2,1));
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
				createLabelField(smartc, SmartLabelProvider.EMP_SMART_USER_LEVEL + ":"); //$NON-NLS-1$
				
				cmbUserLevel = new CheckBoxDropDown(smartc);
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
			
			smartc.setText(Messages.EmployeeComposite_SmartUser_Label);
			createLabelField(smartc, SmartLabelProvider.EMP_SMART_USER + ":"); //$NON-NLS-1$
			txtSmartId = createTextField(smartc, SWT.NONE,
					Employee.MAX_SMART_ID_LENGTH, validate);
	
			
			createLabelField(smartc, Messages.EmployeeComposite_Password_Label);
			txtSmartPassword = createTextField(smartc, SWT.PASSWORD,
					Employee.MAX_SMART_PASSWORD_LENGTH, validate);
			txtSmartPassword.setData(MODIFIED_KEY, Boolean.FALSE);
			txtSmartPassword.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					txtSmartPassword.setData(MODIFIED_KEY, Boolean.TRUE);
				}
			});
			
			createLabelField(smartc, Messages.EmployeeComposite_Password2_Label);
			txtSmartPassword2 = createTextField(smartc, SWT.PASSWORD,
					Employee.MAX_SMART_PASSWORD_LENGTH, validate);
			
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
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1,
				1));
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
		Calendar now = Calendar.getInstance();
		Calendar min = Calendar.getInstance();
		min.set(Calendar.YEAR, now.get(Calendar.YEAR)- Employee.MIN_EMPLOYEE_AGE );
		
		
		Calendar calBirthDate = SmartUtils.getCalendar(dtBirthDate);
		if(min.before(calBirthDate)){
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
		min = Calendar.getInstance();
		min.set(Calendar.YEAR, dtBirthDate.getYear() + Employee.MIN_EMPLOYEE_AGE);
		
		Calendar calStartDate = SmartUtils.getCalendar(dtEmploymentStart);
		if(calStartDate.before(min)){
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
				Calendar start = SmartUtils.getCalendar(dtEmploymentStart);
				Calendar end = SmartUtils.getCalendar(dtEmploymentEnd);
				
				if(end.before(start)){
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
		
		SmartUtils.initDateDateTimeWidget(dtBirthDate, e.getBirthDate());
		SmartUtils.initDateDateTimeWidget(dtEmploymentStart, e.getStartEmploymentDate());
		
		
		if (e.getEndEmploymentDate() != null){
			chNotActive.setSelection(true);
			dtEmploymentEnd.setEnabled(true);
			SmartUtils.initDateDateTimeWidget(dtEmploymentEnd, e.getEndEmploymentDate());
		}else{
			chNotActive.setSelection(false);
			dtEmploymentEnd.setEnabled(false);
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
		
		enableFields(!chNotActive.getSelection());
		validate();
	}
	private void enableFields(boolean enable){
		Control[] controls = new Control[]{
				this.txtFamilyName,
				this.txtGivenName,
				this.txtStaffId,
				this.txtSmartId,
				this.dtBirthDate,
//				this.dtEmploymentEnd,
				this.dtEmploymentStart,
				this.opMale,
				this.opFemale,
				this.chSmartUser,
				this.cmbUserLevel,
				this.cmbViewerAgency.getCombo(),
				this.cmbViewerRank.getCombo()
		};
		for (int i = 0; i < controls.length; i ++){
			if (controls[i] != null){
				controls[i].setEnabled(enable);
			}
		}
		if (chSmartUser != null && enable){
			enableSmartUser(chSmartUser.getSelection());
		}
	}
	
	protected void enableSmartUser(boolean enable){
		txtSmartId.setEditable(enable);

		if (cmbUserLevel != null) cmbUserLevel.setEnabled(enable);
		
		if (txtSmartPassword != null){
			txtSmartPassword.setEnabled(enable);
		}
		if (txtSmartPassword2 != null){
			txtSmartPassword2.setEnabled(enable);
		}
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
	public void updateEmploye(Employee e){

		e.setBirthDate(SmartUtils.getDate(dtBirthDate));
		e.setStartEmploymentDate(SmartUtils.getDate(dtEmploymentStart));
		
		e.setFamilyName(txtFamilyName.getText().trim());
		e.setGender(opFemale.getSelection() ? Employee.DB_FEMALE : Employee.DB_MALE);
		e.setGivenName(txtGivenName.getText().trim());
		
		e.setId(txtStaffId.getText().trim());
		
		if (dtEmploymentEnd != null){
			if (!chNotActive.getSelection()){
				e.setEndEmploymentDate(null);
			}else{
				e.setEndEmploymentDate(SmartUtils.getDate(dtEmploymentEnd));
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
}
