package org.wcs.smart.qa.ui.configure.create;

import java.util.Locale;

import org.apache.commons.jxpath.ri.axes.ParentContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.InternalExtensionManager;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.model.QaRoutineParameter;
import org.wcs.smart.qa.ui.configure.IParameterCollector;
import org.wcs.smart.qa.ui.configure.RoutinesListDialog;
import org.wcs.smart.ui.properties.DialogConstants;

public class EditRoutineDialog extends TitleAreaDialog{

	private QaRoutine routine;
	
	private Label lblType2;
	private Text txtName;
	private Text txtDescription;
	private ControlDecoration cdName;
	private Button btnAuto;
	
	private Composite parameterComp;
	private IParameterCollector collector;
	private ScrolledComposite sc ;
	
	public EditRoutineDialog(Shell parentShell, QaRoutine r) {
		super(parentShell);
		this.routine = r;
	}

	private void initControls(){
		Session s = HibernateManager.openSession();
		try{
			this.routine = (QaRoutine) s.get(QaRoutine.class, routine.getUuid());
			if (this.routine == null){
				return;
			}
			this.routine.getNames().size();
			for (QaRoutineParameter p : routine.getParameters()){
				p.getParameterId();
			}
		}finally{
			s.close();
		}
		txtName.setText(routine.getName());
		if (routine.getDescription() != null){
			txtDescription.setText(routine.getDescription());
		}
		btnAuto.setSelection(routine.getAutoCheck());
		lblType2.setText(routine.getRoutineType().getName(Locale.getDefault()));
		
		collector = InternalExtensionManager.INSTANCE.newParameterCollector(routine.getRoutineTypeId());
		if (collector == null){
			Label l = new Label(parameterComp, SWT.NONE);
			l.setText("No Parameters required for this QA Routine");
		}else{
			collector.createUi(parameterComp);
			collector.initUi(routine);
			collector.addListener(e->validate());
		}
		
		parameterComp.layout(true);
		sc.setMinSize(parameterComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		validate();
	}
	
	@Override
	protected void okPressed(){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			QaRoutine r = (QaRoutine)s.get(QaRoutine.class, routine.getUuid());
			String name = txtName.getText().trim();
			r.setName(name);
			r.updateName(SmartDB.getCurrentLanguage(), name);
			if (txtDescription.getText().trim().isEmpty()){
				r.setDescription(null);
			}else{
				r.setDescription(txtDescription.getText().trim());
			}
			r.setAutoCheck(btnAuto.getSelection());
			
			if (collector != null){
				collector.updateParameters(r);
			}
			
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			QaPlugIn.displayLog("Error saving changes to QA Routine: " + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		super.okPressed();
		super.okPressed();
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		Composite top = new Composite(composite, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		
		Label lblType = new Label(top, SWT.NONE);
		lblType.setText(RoutinesListDialog.RoutineColumn.TYPE.guiName + ":");
		
		lblType2 = new Label(top, SWT.NONE);
		lblType2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblName = new Label(top, SWT.NONE);
		lblName.setText(RoutinesListDialog.RoutineColumn.NAME.guiName + ":"); //$NON-NLS-1$
		lblName.setToolTipText(RoutinesListDialog.RoutineColumn.NAME.tooltip);
		
		txtName = new Text(top, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.setTextLimit(org.wcs.smart.ca.Label.MAX_LENGTH);
		txtName.addListener(SWT.Modify, e->validate());
		
		cdName = new ControlDecoration(txtName, SWT.TOP | SWT.LEFT);
		cdName.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdName.hide();
		
		Label lblAuto = new Label(top, SWT.NONE);
		lblAuto.setText(RoutinesListDialog.RoutineColumn.AUTO.guiName + ":"); //$NON-NLS-1$
		lblAuto.setToolTipText(RoutinesListDialog.RoutineColumn.AUTO.tooltip);

		btnAuto = new Button(top, SWT.CHECK);
		btnAuto.addListener(SWT.Modify, e->validate());
		
		Label lblDesc = new Label(top, SWT.NONE);
		lblDesc.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		lblDesc.setText(RoutinesListDialog.RoutineColumn.DESC.guiName + ":"); //$NON-NLS-1$
		lblDesc.setToolTipText(RoutinesListDialog.RoutineColumn.DESC.tooltip);
		
		txtDescription = new Text(top, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL );
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtDescription.setTextLimit(QaRoutine.MAX_DESC_LENGTH);
		((GridData)txtDescription.getLayoutData()).heightHint = 60;
		txtDescription.addListener(SWT.Modify, e->validate());
		
		Label l = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		sc = new ScrolledComposite(composite, SWT.V_SCROLL );
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		parameterComp = new Composite(sc, SWT.NONE);
		parameterComp.setLayout(new GridLayout());
		parameterComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		sc.setContent(parameterComp);
		
		
		getShell().setText("Edit QA Routine");
		setTitle(routine.getName());
		setMessage("Modify the QA Routine details");
		
		initControls();
		
		return composite;
	}
	
	
	private void validate(){
		boolean ok = true; 
		if (txtName.getText().trim().length() == 0){
			ok = false;
			cdName.setDescriptionText("Name required");
			cdName.show();
		}else{
			cdName.hide();
		}
		Button btn = getButton(IDialogConstants.OK_ID);
		if (collector != null && !collector.isValid()){
			ok = false;
		}
		if (btn != null)btn.setEnabled(ok);
		 
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
