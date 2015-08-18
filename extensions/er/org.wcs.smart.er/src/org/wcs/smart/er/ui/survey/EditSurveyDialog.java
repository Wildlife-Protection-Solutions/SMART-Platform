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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.SurveyPermissionManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

/**
 * New survey dialog.
 * 
 * @author Emily
 *
 */
public class EditSurveyDialog extends TitleAreaDialog{

	private Text txtDesign;
	private Text txtId;
	private ControlDecoration cdId;
	private DateTime startDate;
	private ControlDecoration cdStart;
	private DateTime endDate;
	private ControlDecoration cdEnd;
	private String canEdit;
	private Session session;
	
	private Survey toEdit;
	
	public EditSurveyDialog(Shell parentShell, 
			UUID toEdit) {
		super(parentShell);
		this.session = HibernateManager.openSession();
		this.toEdit = (Survey) session.load(Survey.class, toEdit);
		this.canEdit = SurveyPermissionManager.INSTANCE.canEditSurvey(this.toEdit,
				ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), session));
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
		
		l = new Label(part, SWT.NONE);
		l.setText(Messages.EditSurveyDialog_StartLabel);
		
		SelectionListener listener = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
		};
		startDate = new DateTime(part, SWT.DATE | SWT.LONG | SWT.DROP_DOWN);
		startDate.addSelectionListener(listener);
		startDate.setEnabled(canEdit == null);
		cdStart = createDecoration(startDate);
		
		l = new Label(part, SWT.NONE);
		l.setText(Messages.EditSurveyDialog_EndLabel);
		
		endDate = new DateTime(part, SWT.DATE | SWT.LONG | SWT.DROP_DOWN);
		endDate.addSelectionListener(listener);
		endDate.setEnabled(canEdit == null);
		cdEnd = createDecoration(endDate);
		
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
	
	private void setIcon(ControlDecoration cd, boolean error){
		if (error){
			cd.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		}else{
			cd.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
		}
	}
	
	private boolean save(){
		toEdit.setEndDate(SmartUtils.getDate(endDate));
		toEdit.setStartDate(SmartUtils.getDate(startDate));
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
		
		Date start = SmartUtils.getDate(startDate);
		Date end = SmartUtils.getDate(endDate);
		if (end.before(start)){
			setIcon(cdEnd, true);
			cdEnd.setDescriptionText(Messages.EditSurveyDialog_InvalidStart);
			cdEnd.show();
			error = true;
		}else{
			cdEnd.hide();
		}
		
		cdStart.hide();
		
		SurveyDesign design = (SurveyDesign) toEdit.getSurveyDesign();
		boolean datewarn = false;
		if (design.getStartDate() != null){
			//ensure start date is not before design start date
			//ensure end date is not before design start date
			if (start.before(design.getStartDate())){
				datewarn = true;
			}
			if (end.before(design.getStartDate())){
				datewarn = true;
			}
		}
		if (design.getEndDate() != null){
			//ensure start date is not after end date
			//ensure end date is not after end date
			if (start.after(design.getEndDate())){
				datewarn = true;
			}
			if (end.after(design.getEndDate())){
				datewarn = true;
			}
		}
			
		if (datewarn){
			setIcon(cdEnd, false);
			cdEnd.setDescriptionText(
					MessageFormat.format(Messages.EditSurveyDialog_InvalidRange,
							new Object[]{
								design.getStartDate() == null ? Messages.EditSurveyDialog_UndefinedDate : DateFormat.getDateInstance().format(design.getStartDate()),
										design.getEndDate() == null ? Messages.EditSurveyDialog_UndefinedDate : DateFormat.getDateInstance().format(design.getEndDate())
								}
							));
			cdEnd.show();
		}
		
		//validate missions; all missions must be within survey dates
		for (Mission m : toEdit.getMissions()){
			if (m.getStartDate().before(start) || m.getEndDate().after(end)){
				setIcon(cdEnd, true);
				cdEnd.setDescriptionText(
					MessageFormat.format(Messages.EditSurveyDialog_MissionDateError,
							new Object[]{m.getId(), DateFormat.getDateInstance().format(m.getStartDate()), DateFormat.getDateInstance().format(m.getEndDate())}));
				
				cdEnd.show();
				error = true;
			}
		}
		if (getButton(OK) != null){
			getButton(OK).setEnabled(!error);
		}
	}
	
	private void initControls(){
		txtDesign.setText(toEdit.getSurveyDesign().getName());
		txtId.setText(toEdit.getId());
		
		SmartUtils.initDateDateTimeWidget(startDate, toEdit.getStartDate());
		SmartUtils.initDateDateTimeWidget(endDate, toEdit.getEndDate());
		
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
