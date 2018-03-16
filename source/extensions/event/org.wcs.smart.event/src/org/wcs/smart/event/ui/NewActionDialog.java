package org.wcs.smart.event.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.event.ActionTypeManager;
import org.wcs.smart.event.ActionTypeManagerInternal;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.event.ui.model.IActionParameterCollector;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

public class NewActionDialog extends TitleAreaDialog {

	private ComboViewer cmbActionType;
	private EAction toUpdate;
	private Composite parameterPanel;
	private Text txtId;
	
	public NewActionDialog(Shell parentShell) {
		this(parentShell, null);
	}

	public NewActionDialog(Shell parentShell, EAction toUpdate) {
		super(parentShell);
		this.toUpdate = toUpdate;
	}
	
	@Override
	protected void okPressed() {
		if (toUpdate == null) {
			toUpdate = new EAction();
			toUpdate.setConservationArea(SmartDB.getCurrentConservationArea());
			toUpdate.setParameters(new ArrayList<>());
		}
		
		toUpdate.setId(txtId.getText());
		Object x = cmbActionType.getStructuredSelection().getFirstElement();
		if (!(x instanceof IActionType))  {
			//TODO: msg
			return;
		}
		toUpdate.setActionTypeKey( ((IActionType)x).getKey() );
		IActionParameterCollector paramCollector = (IActionParameterCollector) parameterPanel.getData("COLLECTOR");
		if (paramCollector != null) {
			paramCollector.updateParameters(toUpdate);
		}else {
			toUpdate.getParameters().clear();
		}
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(toUpdate);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				EventPlugIn.log(ex.getMessage(), ex);
				return;
			}
		}
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button saveBtn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		saveBtn.setEnabled(false);
	}
	
	@Override
	public Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true ));
		
		Composite header = new Composite(main, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(header, SWT.NONE);
		l.setText("ID:");
		
		txtId = new Text(header, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtId.getLayoutData()).widthHint = 100;
		txtId.setTextLimit(EAction.MAX_ID_LENGTH);
		txtId.addModifyListener(e->validate());
		
		l = new Label(header, SWT.NONE);
		l.setText("Action Type:");
		
		cmbActionType = new ComboViewer(header, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbActionType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbActionType.setContentProvider(ArrayContentProvider.getInstance());
		cmbActionType.setLabelProvider(new AssetTypeLabelProvider());
		List<IActionType> types = ActionTypeManager.INSTANCE.getActionTypes();
		cmbActionType.setInput(types);
		
		Group g = new Group(main, SWT.NONE);
		g.setText("Action Parameters");
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		parameterPanel = new Composite(g, SWT.NONE);
		parameterPanel.setLayout(new GridLayout());
		parameterPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)parameterPanel.getLayoutData()).heightHint = 300;
		
		if (toUpdate != null) {
			cmbActionType.getControl().setEnabled(false);
			for (IActionType t : types) {
				if (t.getKey().equalsIgnoreCase(toUpdate.getActionTypeKey())) {
					cmbActionType.setSelection(new StructuredSelection(t));
					break;
				}
			}
			changeActionPanelType();
			txtId.setText(toUpdate.getId());
		}else {
			cmbActionType.addSelectionChangedListener(e->changeActionPanelType());
		}
		
		setTitle("New Action");
		getShell().setText("New Action");
		setMessage("Create a new action");
		return parent;
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	private void validate() {
		String id = txtId.getText().trim();
		if (id.isEmpty()) {
			setError("An ID must be validated.");
			return;
		}
		Object x = cmbActionType.getStructuredSelection().getFirstElement();
		if (!(x instanceof IActionType)) {
			setError("A valid action type must be specified.");
			return;
		}
		
		IActionParameterCollector paramCollector = (IActionParameterCollector) parameterPanel.getData("COLLECTOR");
		if (paramCollector != null) {
			String error = paramCollector.validate();
			if (error != null) {
				setError(error);
				return;
			}
		}
		setErrorMessage(null);
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) btn.setEnabled(true);
	}
	
	private void setError(String error) {
		setErrorMessage(error);
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) btn.setEnabled(false);
	}
	
	
	private void changeActionPanelType() {
		for (Control c : parameterPanel.getChildren()) c.dispose();
		
		Object x = cmbActionType.getStructuredSelection().getFirstElement();
		if (!(x instanceof IActionType)) {
			Label l = new Label(parameterPanel, SWT.NONE);
			l.setText("Select a valid action type.");
			parameterPanel.setData("COLLECTOR", null);
		}else {
			IActionParameterCollector paramCollector = ActionTypeManagerInternal.INSTANCE.createParameterCollector((IActionType)x);
			if (paramCollector == null) {
				parameterPanel.setData("COLLECTOR", null);
				Label l = new Label(parameterPanel, SWT.NONE);
				l.setText("No parameters specified for this action type.");
			}else {
				paramCollector.addModifyListener(e->validate());
				Composite inner = paramCollector.createComposite(parameterPanel);
				inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				parameterPanel.setData("COLLECTOR", paramCollector);
				if (toUpdate != null) paramCollector.initParameters(toUpdate);
			}
		}
		parameterPanel.layout();
		validate();
	}
}
