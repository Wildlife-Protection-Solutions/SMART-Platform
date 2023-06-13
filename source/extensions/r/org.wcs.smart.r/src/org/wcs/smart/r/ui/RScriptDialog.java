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
package org.wcs.smart.r.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashSet;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.RScriptInterceptor;
import org.wcs.smart.r.RScriptManager;
import org.wcs.smart.r.internal.Messages;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing r script details
 * 
 * @author Emily
 *
 */
public class RScriptDialog extends SmartStyledTitleDialog{

	private RScript script;
	
	private Text txtDefaultParameters;
	private Text txtName;
	
	private Text txtFile;
	
	public RScriptDialog(Shell parentShell, RScript script) {
		super(parentShell);
		this.script = script;
	}
	
	public RScriptDialog(Shell parentShell) {
		this(parentShell, null);
		
		script = new RScript();
		script.setNames(new HashSet<>());
		script.setConservationArea(SmartDB.getCurrentConservationArea());
		script.setCreator(SmartDB.getCurrentEmployee());
		script.setName(""); //$NON-NLS-1$
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		if (script.getUuid() != null) {
			try(Session s = HibernateManager.openSession()){
				script = s.get(RScript.class, script.getUuid());
				script.getNames().forEach(e->e.getValue());
			}
		}
		
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (this.script.getUuid() == null) {
			//new script we need to get a file
			Label l = new Label(parent, SWT.NONE);
			l.setText(Messages.RScriptDialog_ScriptFileName);
			
			txtFile = new Text(parent, SWT.BORDER);
			txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			txtFile.addListener(SWT.Modify, e->validate());
			
			Button btnBrowse = new Button(parent, SWT.PUSH);
			btnBrowse.setText("..."); //$NON-NLS-1$
			btnBrowse.addListener(SWT.Selection, e->{
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setText(Messages.RScriptDialog_ImportTitle);
				fd.setFilterExtensions(new String[] {"*.R;*.RData", "*.txt", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				fd.setFilterNames(new String[] {Messages.RScriptDialog_RScripts, Messages.RScriptDialog_TextFiles, Messages.RScriptDialog_AllFiles});
				String fileName = fd.open();
				
				if (fileName == null) return;
				txtFile.setText(fileName);
				
				Path p = Paths.get(txtFile.getText());
				
				String scriptname = p.getFileName().toString();
				int index = scriptname.lastIndexOf('.');
				if (index > 0) {
					scriptname = scriptname.substring(0, index);
				}
				if (txtName.getText().trim().isEmpty()) {
					txtName.setText(scriptname);
				}
			});
			
			l = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		}
		
		Label l = new Label(parent, SWT.NONE);
		l.setText(Messages.RScriptDialog_NameLabel);
		
		txtName = new Text(parent, SWT.BORDER);
		txtName.setText(script.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtName.getLayoutData()).widthHint = 200;
		txtName.addListener(SWT.Modify, e->validate());
		
		Link link = new Link(parent,  SWT.NONE);
		link.setText("<a>" + Messages.RScriptDialog_TranslateOp + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		link.addListener(SWT.Selection, evt->{
			TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(getShell(), script);
			if (dialog.open() == Window.OK) {
				txtName.setText(script.getName());
				validate();
			}
		});
		
		if (this.script.getUuid() != null) {
			l = new Label(parent, SWT.NONE);
			l.setText(Messages.RScriptDialog_FileName);
			
			Text txtFile = new Text(parent, SWT.BORDER);
			txtFile.setText(script.getFilename());
			txtFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtFile.getLayoutData()).widthHint = 200;
			txtFile.setEditable(false);
			
			Composite temp = new Composite(parent, SWT.NONE);
			temp.setLayout(new GridLayout(2, false));
			((GridLayout)temp.getLayout()).marginWidth = 0;
			((GridLayout)temp.getLayout()).marginHeight = 0;
			
			link = new Link(temp,  SWT.NONE);
			link.setText("<a>" + Messages.RScriptDialog_EditOp + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
			link.setToolTipText(Messages.RScriptDialog_edittooltip);
			link.addListener(SWT.Selection, evt->{
				AttachmentUtil.launch( RScriptManager.INSTANCE.getScriptPath( this.script ) );
			});
			
			link = new Link(temp,  SWT.NONE);
			link.setText("<a>" + Messages.RScriptDialog_ShowOp + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
			link.setToolTipText(Messages.RScriptDialog_showtooltip);
			link.addListener(SWT.Selection, evt->{
				AttachmentUtil.launch( RScriptManager.INSTANCE.getScriptPath( this.script ).getParent() );
			});
		}
		
		
		l = new Label(parent, SWT.NONE);
		l.setText(Messages.RScriptDialog_DefaultParams);
		l.setToolTipText(Messages.RScriptDialog_ParamInfo);
		l.setLayoutData(new GridData(SWT.TOP, SWT.FILL, false, false, 3, 1));
		
		txtDefaultParameters = new Text(parent, SWT.BORDER | SWT.WRAP  | SWT.V_SCROLL);
		txtDefaultParameters.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		((GridData)txtDefaultParameters.getLayoutData()).heightHint = 100;
		txtDefaultParameters.addListener(SWT.Modify, e->validate());
		if (script.getDefaultParameters() != null) txtDefaultParameters.setText(script.getDefaultParameters());
			
		setTitle(Messages.RScriptDialog_Title);
		getShell().setText(Messages.RScriptDialog_Title);
		setMessage(Messages.RScriptDialog_Message);
		
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
			setErrorMessage(MessageFormat.format(Messages.RScriptDialog_NameError, org.wcs.smart.ca.Label.MAX_LENGTH));
			enableOk(false);
			return;
		}
		String param = txtDefaultParameters.getText();
		if (param.trim().length() > RScript.MAX_DEFAULT_PARAM_SIZE) {
			setErrorMessage(MessageFormat.format(Messages.RScriptDialog_ParamError, RScript.MAX_DEFAULT_PARAM_SIZE));
			enableOk(false);
			return;
		}
		
		if (txtFile != null) {
			if (txtFile.getText().trim().isEmpty()) {
				setErrorMessage(Messages.RScriptDialog_ScriptRequired);
				enableOk(false);
				return;	
			}
			Path p = Paths.get(txtFile.getText());
			if (!Files.exists(p)) {
				setErrorMessage(MessageFormat.format(Messages.RScriptDialog_FileNotFound, p.toString()));
			}
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
			MessageDialog md = new MessageDialog(getShell(), Messages.RScriptDialog_SaveTitle, null, Messages.RScriptDialog_SaveMessage, MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			int ret = md.open();
			if (ret == 2){
				//cancel
				return;
			}else if (ret == 0){
				//yes
				okPressed();	
				return;
			}
		}
		super.cancelPressed();
	}
	
	@Override
	public void okPressed() {
		validate();
		if (getErrorMessage() != null) return;
		
		script.setName(txtName.getText().trim());
		script.updateName(SmartDB.getCurrentLanguage(),txtName.getText().trim());
		script.setDefaultParameters(txtDefaultParameters.getText());
		if (script.getUuid() == null) {
			script.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), txtName.getText().trim());
			
			Path p = Paths.get(txtFile.getText());
			script.setImportFile(p);
		}
		try(Session session = HibernateManager.openSession(new RScriptInterceptor())){
			session.beginTransaction();
			boolean isNew = script.getUuid() == null;
			try {
				HibernateManager.saveOrMerge(session, script);
				session.getTransaction().commit();
			}catch (Exception ex) {
				if (isNew) script.setUuid(null);
				RPlugIn.displayLog(MessageFormat.format(Messages.RScriptDialog_SaveError, ex.getMessage()), ex);
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
