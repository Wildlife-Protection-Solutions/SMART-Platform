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
package org.wcs.smart.observation.ui;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.util.GeometryUtils;

/**
 * Property page for editing patrol options
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ObservationOptionsPropertyPage extends AbstractPropertyJHeaderDialog {

	private ObservationOptions patrolOption = null;
	private Text txtEditTime;
	private ControlDecoration cdEditTime;
	private Button btnTrackDistanceDirection;
	private Button btnTrackObserver;
	
	private String errorEditTimeMessage = MessageFormat.format(Messages.PatrolOptionsPropertyPage_Error_EditTimeInvalid, -1, Short.MAX_VALUE);
	
	/**
	 * @param parent
	 * @param title
	 */
	public ObservationOptionsPropertyPage(Shell parent) {
		super(parent, Messages.PatrolOptionsPropertyPage_DialogTitle);
		try(Session s = HibernateManager.openSession()){
			patrolOption = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), s);
		}
	}
	

	@Override
	public boolean  close(){
		return super.close();
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartUiUtils.createHeaderLabel(container, Messages.ObservationOptionsPropertyPage_DistanceBearingOpLabel);
		
		Composite g = new Composite(container, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
//		Label lbl = new Label(g, SWT.WRAP);
//		lbl.setText(Messages.PatrolOptionsPropertyPage_DistanceDirection_DescLabel1);
//		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
//		((GridData)lbl.getLayoutData()).widthHint = 350;
		
		Link lbl1 = new Link(g, SWT.WRAP);
		lbl1.setText(Messages.PatrolOptionsPropertyPage_DistanceDirection_DescLabel1);
		lbl1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl1.getLayoutData()).widthHint = 350;
		lbl1.addListener(SWT.Selection, e->{
			
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.ObservationOptionsPropertyPage_ComputationDetails);
			sb.append("\n\n"); //$NON-NLS-1$
			sb.append("d = distance (meters)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("b = bearing (degrees)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("RADIUS = " + GeometryUtils.EARTH_RADIUS); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("dR = d / RADIUS"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("rb = toRadians(b)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("ry = toRadians(y)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("rx = toRadians(x)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("rprjy = asin( sin(ry) * cos(dR) + cos(ry) * sin(dR) * cos(rb) ) "); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("rprjx = rx + atan2( Math.sin(rb) * sin(dR) * cos(ry), cos(dR) - sin(ry) * sin(rprjy) )"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("prj_x = toDegrees(rprjx)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("prj_y = toDegrees(rprjy)"); //$NON-NLS-1$
			sb.append("\n\n"); //$NON-NLS-1$
			
			MessageDialog md = new MessageDialog(getShell(), Messages.ObservationOptionsPropertyPage_DistanceBearingTitle, null, null, MessageDialog.NONE, 0, IDialogConstants.OK_LABEL) {
				@Override
			    protected Control createCustomArea(Composite parent) {
					parent.getParent().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
					parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
					
			    	Text txtWarnings = new Text(parent, SWT.MULTI | SWT.BORDER);
			    	txtWarnings.setEditable(false);
			    	txtWarnings.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			    	GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			    	txtWarnings.setLayoutData(gd);
			    	
			    	txtWarnings.setText(sb.toString());
			        return txtWarnings;
			    }
			};
			md.open();
		});
		
		btnTrackDistanceDirection = new Button(g, SWT.CHECK | SWT.WRAP);
		btnTrackDistanceDirection.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnTrackDistanceDirection.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
		});
		btnTrackDistanceDirection.setText(Messages.PatrolOptionsPropertyPage_RecordDistanceDirectory_Op);
		
		SmartUiUtils.createHeaderLabel(container, Messages.ObservationOptionsPropertyPage_ObserverOp);

		g = new Composite(container, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(g, SWT.WRAP);
		lbl.setText(Messages.ObservationOptionsPropertyPage_ObserverDescription);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 350;
		
		btnTrackObserver = new Button(g, SWT.CHECK);
		btnTrackObserver.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnTrackObserver.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
		});
		btnTrackObserver.setText(Messages.ObservationOptionsPropertyPage_ObserverLabel);
		
		
		lbl = new Label(container, SWT.NONE);  //spacer
		
		SmartUiUtils.createHeaderLabel(container, Messages.PatrolOptionsPropertyPage_PatrolEditOptions_Label);
		
		g = new Composite(container, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lbl = new Label(g, SWT.WRAP);
		lbl.setText(Messages.PatrolOptionsPropertyPage_PatrolEditOptions_DescLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 350;
		
		cdEditTime = createDecoration(lbl);
		cdEditTime.setDescriptionText(errorEditTimeMessage);
		cdEditTime.hide();
		
		txtEditTime = new Text(g, SWT.BORDER);
		txtEditTime.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		((GridData)txtEditTime.getLayoutData()).widthHint = 30;
		txtEditTime.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setChangesMade(true);
			}
		});
		lbl = new Label(g, SWT.NONE);
		lbl.setText(Messages.PatrolOptionsPropertyPage_PatrolEditOptions_DaysLabel);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		//init values
		if (patrolOption != null){
			btnTrackDistanceDirection.setSelection(patrolOption.getTrackDistanceDirection());
			btnTrackObserver.setSelection(patrolOption.getTrackObserver());
			
			if (patrolOption.getEditTime() != null){
				txtEditTime.setText(patrolOption.getEditTime().toString());
			}else{
				txtEditTime.setText("-1"); //$NON-NLS-1$
			}
		}
		
		setTitle(Messages.PatrolOptionsPropertyPage_PageName);
		setMessage(Messages.PatrolOptionsPropertyPage_DialogMessage);
		setChangesMade(false);
		return container;
	}

	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}

	protected void validate() {
		if (!isEditTimeValid()) {
			cdEditTime.show();
			setErrorMessage(errorEditTimeMessage);
			if (getButton(IDialogConstants.OK_ID) != null) {
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			}
			return;
		}
		cdEditTime.hide();
		setErrorMessage(null);
	}
	
	private boolean isEditTimeValid() {
		try {
			int edittime = Integer.parseInt(txtEditTime.getText());
			if (edittime < -1 || edittime > Short.MAX_VALUE) {
				return false;
			} else {
				return true;
			}
		} catch (Exception ex) {
			return false;
		}

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
		if (patrolOption == null){
			return false;
		}
		if (!isEditTimeValid()) {
			//this should be impossible to be here, but added just in case
			ObservationPlugIn.displayLog(errorEditTimeMessage, null);
			return false;
		}
		patrolOption.setTrackDistanceDirection(btnTrackDistanceDirection.getSelection());
		patrolOption.setTrackObserver(btnTrackObserver.getSelection());
		patrolOption.setEditTime(Integer.parseInt(txtEditTime.getText()));
	
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try{
				s.saveOrUpdate(patrolOption);
				s.getTransaction().commit();
				setChangesMade(false);
				
				//fire event for options modified
				WaypointEventManager.getInstance().waypointOptionsModified();
				
				return true;
			}catch (Exception ex){
				s.getTransaction().rollback();
				ObservationPlugIn.displayLog(Messages.PatrolOptionsPropertyPage_Error_CouldNotSave + ex.getLocalizedMessage(), ex);
			}
		}
		return false;
	}
	
	
}
