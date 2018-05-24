package org.wcs.smart.r.ui;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.DialogConstants;

public class RScriptDialog extends TitleAreaDialog {

	private RScript script;
	
	private Text txtDefaultParameters;
	private Text txtName;
	
	public RScriptDialog(Shell parentShell, RScript script) {
		super(parentShell);
		this.script = script;
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*2));
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		try(Session s = HibernateManager.openSession()){
			script = s.get(RScript.class, script.getUuid());
			script.getNames().forEach(e->e.getValue());
		}
		
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Name:");
		
		txtName = new Text(parent, SWT.BORDER);
		txtName.setText(script.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtName.getLayoutData()).widthHint = 200;
		txtName.addListener(SWT.Modify, e->validate());
		
		Link link = new Link(parent,  SWT.NONE);
		link.setText("<a>" + "translate" + "</a>");
		link.addListener(SWT.Selection, evt->{
			TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(getShell(), script);
			if (dialog.open() == Window.OK) {
				validate();
			}
		});
		
		
		l = new Label(parent, SWT.NONE);
		l.setText("Default Script Parameters:");
		l.setToolTipText("the parameters to sent to the r script along with the output from SMART queries.");
		l.setLayoutData(new GridData(SWT.TOP, SWT.FILL, false, false, 3, 1));
		
		txtDefaultParameters = new Text(parent, SWT.BORDER | SWT.WRAP  | SWT.V_SCROLL);
		txtDefaultParameters.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		txtDefaultParameters.addListener(SWT.Modify, e->validate());
		if (script.getDefaultParameters() != null) txtDefaultParameters.setText(script.getDefaultParameters());
			
		setTitle("R Script");
		getShell().setText("R Script");
		setMessage("Update properties associated with R script");
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btnok = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
		btnok.setEnabled(false);
	}
	
	private void validate() {
		String newName = txtName.getText();
		if (newName.trim().isEmpty() || newName.trim().length() > org.wcs.smart.ca.Label.MAX_LENGTH) {
			setErrorMessage(MessageFormat.format("A name between 1 and {0} characters is required.", org.wcs.smart.ca.Label.MAX_LENGTH));
			enableOk(false);
			return;
		}
		String param = txtDefaultParameters.getText();
		if (param.trim().length() > RScript.MAX_DEFAULT_PARAM_SIZE) {
			setErrorMessage(MessageFormat.format("Default parameters cannot exceed {0} characters.", RScript.MAX_DEFAULT_PARAM_SIZE));
			enableOk(false);
			return;
		}
		enableOk(true);
		setErrorMessage(null);
	}
	
	private void enableOk(boolean enable) {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (ok == null) return;
		ok.setEnabled(enable);
	}
	
	@Override
	public void cancelPressed() {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (ok != null && ok.isEnabled()) {
			MessageDialog md = new MessageDialog(getShell(), "Confirm Save", null, "There are unsaved changes. Would you like to save your changes before closing?", MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			int ret = md.open();
			if (ret == 2){
				//cancel
				return;
			}else if (ret == 0){
				//yes
				okPressed();	
			}
		}
		super.cancelPressed();
	}
	
	@Override
	public void okPressed() {
		validate();
		if (getErrorMessage() != null) return;
		
		script.setName(txtName.getText());
		script.updateName(SmartDB.getCurrentLanguage(),txtName.getText());
		script.setDefaultParameters(txtDefaultParameters.getText());
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(script);
				session.getTransaction().commit();
			}catch (Exception ex) {
				RPlugIn.displayLog(MessageFormat.format("Unable to save changes to R script: {0}", ex.getMessage()), ex);
				session.getTransaction().rollback();
				return;
			}
		}
		super.okPressed();
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
