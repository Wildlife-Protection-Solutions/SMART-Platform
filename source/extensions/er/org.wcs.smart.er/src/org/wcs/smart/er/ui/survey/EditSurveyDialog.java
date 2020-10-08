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
package org.wcs.smart.er.ui.survey;

import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

/**
 * New survey dialog.
 * 
 * @author Emily
 *
 */
public class EditSurveyDialog extends SmartStyledTitleDialog{

	private Text txtDesign;
	private Text txtId;
	private ControlDecoration cdId;
	private String canEdit;
	private Session session;
	
	private Survey toEdit;
	
	public EditSurveyDialog(Shell parentShell, 
			UUID toEdit) {
		super(parentShell);
		this.session = HibernateManager.openSession();
		this.toEdit = (Survey) session.load(Survey.class, toEdit);
	}
	
	@Override
	public boolean close(){
		try {
			if (session.isOpen())
				session.close();
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		}
		return super.close();
	}

	/**
	 * Saves the new survey then closes the dialog
	 */
	@Override
	protected void okPressed() {
		if (canEdit == null){
			if (save()){
				super.okPressed();
			}
		}else{
			super.okPressed();
		}
	}
	
	protected void createButtonsForButtonBar(Composite parent){
		if (canEdit == null){
			super.createButtonsForButtonBar(parent);
			getButton(IDialogConstants.OK_ID).setText(DialogConstants.SAVE_TEXT);	
		}else{
			super.createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		}
		validate();
	}
	
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(2, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (canEdit != null){
			Composite warning = new Composite(part, SWT.NONE);
			warning.setLayout(new GridLayout(2, false));
			warning.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			Label lblImage = new Label(warning, SWT.NONE);
			Image x = SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON);
			lblImage.setImage(x);
			Label lblWarning = new Label(warning, SWT.NONE);
			lblWarning.setText(MessageFormat.format(Messages.EditSurveyDialog_CannotEdit, new Object[]{canEdit})) ;
		}
		
		Label l = new Label(part, SWT.NONE);
		l.setText(Messages.EditSurveyDialog_DesignLabel);
		
		txtDesign = new Text(part, SWT.BORDER);
		txtDesign.setEnabled(false);
		txtDesign.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		l = new Label(part, SWT.NONE);
		l.setText(Messages.EditSurveyDialog_IdLabel);
		
		txtId = new Text(part, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtId.setEditable(canEdit == null);
		txtId.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});
		cdId = createDecoration(txtId);
		
		setTitle(toEdit.getId());
		getShell().setText(Messages.EditSurveyDialog_ShellTitle);
		setMessage(Messages.EditSurveyDialog_Message);
		
		initControls();
		
		
		return parent;
	}
	
	private ControlDecoration createDecoration(Control parent){
		ControlDecoration cd = new ControlDecoration(parent, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	

	
	private boolean save(){

		toEdit.setId(txtId.getText());
		
		session.beginTransaction();
		try{
			session.save(toEdit);
			session.getTransaction().commit();
			
			//close session
			session.close();
			
			//fire event
			try{
				SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, toEdit);
			}catch (Exception ex){
				EcologicalRecordsPlugIn.displayLog(ex.getMessage(), ex);	
			}
			
			return true;
		}catch (Exception ex){
			try{
				session.getTransaction().rollback();
			}catch (Exception ex2){
				EcologicalRecordsPlugIn.log(ex2.getMessage(), ex2);
			}
			EcologicalRecordsPlugIn.displayLog(Messages.EditSurveyDialog_Error + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}
		return false;
	}
	
	private void validate(){
		boolean error = false;
		if (!SmartUtils.isSimpleString(txtId.getText(), RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Survey.ID_MAX_LENGTH)){
			cdId.setDescriptionText(MessageFormat.format(Messages.EditSurveyDialog_InvalidId, new Object[]{Survey.ID_MAX_LENGTH, RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
			cdId.show();
			error = true;
		}else{
			cdId.hide();
		}
		
	
		if (getButton(OK) != null){
			getButton(OK).setEnabled(!error);
		}
	}
	
	private void initControls(){
		txtDesign.setText(toEdit.getSurveyDesign().getName());
		txtId.setText(toEdit.getId());		
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
