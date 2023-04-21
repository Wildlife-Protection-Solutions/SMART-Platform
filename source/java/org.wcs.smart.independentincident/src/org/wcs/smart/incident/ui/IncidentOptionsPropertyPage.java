/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.incident.ui;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.IncidentPropertyManager;
import org.wcs.smart.incident.IntegrateIncidentSource;
import org.wcs.smart.incident.IntegratePatrolIncidentSource;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.patrol.IncidentToPatrolProcessorJob;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Property page for editing incident processing options
 * 
 * @author Emily
 * @since 1.0.0
 */
public class IncidentOptionsPropertyPage extends AbstractPropertyJHeaderDialog {

	private Text txtDistance;
	private ControlDecoration cdDistance;
	private Text txtMaxTime;
	private ControlDecoration cdMaxTime;
	
	/**
	 * @param parent
	 * @param title
	 */
	public IncidentOptionsPropertyPage(Shell parent) {
		super(parent, Messages.IncidentOptionsPropertyPage_Title );
		
		
	}
		
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartUiUtils.createHeaderLabel(container, Messages.IncidentOptionsPropertyPage_IntegrateSectionTitle);
		
		Label l = new Label(container, SWT.WRAP);
		l.setText(MessageFormat.format(
				Messages.IncidentOptionsPropertyPage_IntegrateInfo, 
				IncidentManager.getInstance().getIncidentProvider(IntegratePatrolIncidentSource.KEY).getName(), 
				IncidentManager.getInstance().getIncidentProvider(IntegrateIncidentSource.KEY).getName()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).widthHint = 350;
		
		Button btnRunNow = new Button(container, SWT.NONE);
		btnRunNow.setText(Messages.IncidentOptionsPropertyPage_RunNowButton);
		btnRunNow.setBackground(btnRunNow.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnRunNow.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RUN_ICON));
		btnRunNow.setToolTipText(Messages.IncidentOptionsPropertyPage_RunNowButtonTooltip);
		btnRunNow.addListener(SWT.Selection, e->IncidentToPatrolProcessorJob.getInstance().schedule());
		
		SmartUiUtils.createSubHeaderLabel(container, Messages.IncidentOptionsPropertyPage_MaxDistanceOp);

		//The incident must be within the maximum distance of the track points found based on the incident time. An option also exists to convert incidents to simple {1} if not patrol is found after X days.
		Composite inner = new Composite(container, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		l = new Label(inner, SWT.WRAP);
		l.setText(Messages.IncidentOptionsPropertyPage_MaxDistanceInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).widthHint = 350;
		
		
		txtDistance = new Text(inner, SWT.BORDER);
		txtDistance.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtDistance.getLayoutData()).widthHint = 80;
		cdDistance = createDecoration(txtDistance);
		txtDistance.addListener(SWT.Modify, e->setChangesMade(true));
		
		l = new Label(inner, SWT.NONE);
		l.setText(Messages.IncidentOptionsPropertyPage_Units);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)l.getLayoutData()).horizontalIndent = 5;
		
		SmartUiUtils.createSubHeaderLabel(container, MessageFormat.format(Messages.IncidentOptionsPropertyPage_ConvertToIncidentTypeTitle, IncidentManager.getInstance().getIncidentProvider(IntegrateIncidentSource.KEY).getName()));
		
		inner = new Composite(container, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(inner, SWT.WRAP);
		l.setText(MessageFormat.format(
				Messages.IncidentOptionsPropertyPage_ConvertToIncidentTypeMessage, 				 
				IncidentManager.getInstance().getIncidentProvider(IntegrateIncidentSource.KEY).getName()));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).widthHint = 350;
		
		
		txtMaxTime = new Text(inner, SWT.BORDER);
		txtMaxTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtMaxTime.getLayoutData()).widthHint = 80;
		cdMaxTime = createDecoration(txtMaxTime);
		txtMaxTime.addListener(SWT.Modify, e->setChangesMade(true));
		
		l = new Label(inner, SWT.NONE);
		l.setText(Messages.IncidentOptionsPropertyPage_days);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)l.getLayoutData()).horizontalIndent = 5;
		
		try(Session session = HibernateManager.openSession()){
			txtDistance.setText(String.valueOf(IncidentPropertyManager.INSTANCE.getSetting(session, SmartDB.getCurrentConservationArea(), IncidentPropertyManager.IncidentProperty.INTEGRATE_TO_PATROL_DISTANCE)));
			txtMaxTime.setText(String.valueOf(IncidentPropertyManager.INSTANCE.getSetting(session, SmartDB.getCurrentConservationArea(), IncidentPropertyManager.IncidentProperty.INTEGRATE_TO_PATROL_EXPIRE)));
		}
				
		setTitle(Messages.IncidentOptionsPropertyPage_Title);
		setMessage(Messages.IncidentOptionsPropertyPage_DialogMessage);
		setChangesMade(false);
		return container;
	}

	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.RIGHT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}

	protected void validate() {
		boolean ok = true;
		setErrorMessage(null);
		
		try{
			Integer x = Integer.parseInt(txtDistance.getText());
			if (x < 0) throw new Exception();
			cdDistance.hide();
		}catch (Exception ex) {
			ok = false;
			cdDistance.setDescriptionText(Messages.IncidentOptionsPropertyPage_distanceValidationMessage);
			setErrorMessage(Messages.IncidentOptionsPropertyPage_DistanceError);
			cdDistance.show();
		}
		
		try{
			Integer x = Integer.parseInt(txtMaxTime.getText());
			if (x < 0 && x != -1) throw new Exception();
			cdMaxTime.hide();
		}catch (Exception ex) {
			ok = false;
			cdMaxTime.setDescriptionText(Messages.IncidentOptionsPropertyPage_timeValidationMessage);
			setErrorMessage(Messages.IncidentOptionsPropertyPage_TimeError);
			cdMaxTime.show();
		}
		
		Button btnok = getButton(IDialogConstants.OK_ID);
		if (btnok != null) btnok.setEnabled(super.changesMade && ok);
	}
	
	
	@Override
	protected void setChangesMade(boolean ischanged) {
		super.setChangesMade(ischanged);
		validate();
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		if (getErrorMessage() != null) return false;
		
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try{
				
				IncidentPropertyManager.INSTANCE.updateSetting(s, SmartDB.getCurrentConservationArea(), IncidentPropertyManager.IncidentProperty.INTEGRATE_TO_PATROL_DISTANCE, Integer.valueOf(txtDistance.getText()));
				IncidentPropertyManager.INSTANCE.updateSetting(s, SmartDB.getCurrentConservationArea(), IncidentPropertyManager.IncidentProperty.INTEGRATE_TO_PATROL_EXPIRE, Integer.valueOf(txtMaxTime.getText()));
				s.getTransaction().commit();

				setChangesMade(false);				
			}catch (Exception ex){
				s.getTransaction().rollback();
				IncidentPlugIn.displayLog(Messages.IncidentOptionsPropertyPage_SaveError + ex.getLocalizedMessage(), ex);
			}
		}
		return false;
	}
	
	
}
