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
package org.wcs.smart.patrol.internal.ui.properties;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.PatrolOptions;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Property page for editing patrol options
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolOptionsPropertyPage extends AbstractPropertyJHeaderDialog {


	
	private PatrolOptions patrolOption = null;
	private Text txtEditTime;
	private Button btnTrackDistanceDirection;
	/**
	 * @param parent
	 * @param title
	 */
	public PatrolOptionsPropertyPage() {
		super(Display.getCurrent().getActiveShell(), "Patrol Options");
		
		patrolOption = PatrolHibernateManager.getPatrolOptions(ca, getSession());
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
		
		Group g = new Group(container, SWT.NONE);
		g.setText("Distance and Direction");
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(g, SWT.WRAP);
		lbl.setText("Allow users to additionally record the Distance and Direction of each waypoint.  These values will be added to the patrol observation table.");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 350;
		
		
		
		btnTrackDistanceDirection = new Button(g, SWT.CHECK);
		btnTrackDistanceDirection.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		btnTrackDistanceDirection.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
		});
		lbl = new Label(g, SWT.NONE);
		lbl.setText("Record Distance and Direction");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		lbl = new Label(container, SWT.NONE);  //spacer
		
		g = new Group(container, SWT.NONE);
		g.setText("Edit Options");
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lbl = new Label(g, SWT.WRAP);
		lbl.setText("Number of days after which the patrol data are no longer editable.  A value of -1 specifies that the patrol data will always be editable.");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 350;
		
		final ControlDecoration cdEditTime =createDecoration(lbl);
		cdEditTime.hide();
		
		
		txtEditTime = new Text(g, SWT.BORDER);
		txtEditTime.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		((GridData)txtEditTime.getLayoutData()).widthHint = 30;
		txtEditTime.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setChangesMade(true);
				try{
					Integer.parseInt(txtEditTime.getText());
					cdEditTime.hide();
				}catch (Exception ex){
					cdEditTime.show();
					cdEditTime.setDescriptionText("Invalid integer");
				}
			}
		});
		lbl = new Label(g, SWT.NONE);
		lbl.setText("Days");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		//init values
		if (patrolOption != null){
			btnTrackDistanceDirection.setSelection(patrolOption.getTrackDistanceDirection());
			if (patrolOption.getEditTime() != null){
				txtEditTime.setText(patrolOption.getEditTime().toString());
			}else{
				txtEditTime.setText("-1");
			}
		}
		
		setMessage("Manage patrol options.");
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
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		if (patrolOption == null){
			return false;
		}
		patrolOption.setTrackDistanceDirection(btnTrackDistanceDirection.getSelection());
		int edittime = -1;
		try{
			edittime = Integer.parseInt(txtEditTime.getText());
		}catch (NumberFormatException ex){
			SmartPatrolPlugIn.displayLog("Invalid edit time.  Must be an integer.", ex);
			return false;
		}
		if (edittime < -1){
			SmartPatrolPlugIn.displayLog("Edit time must be >= -1", null);
			return false;
		}
		patrolOption.setEditTime(edittime);
		
		Session s = getSession();
		s.beginTransaction();
		try{
			s.saveOrUpdate(patrolOption);
			s.getTransaction().commit();
			setChangesMade(false);
			return true;
		}catch (Exception ex){
			s.getTransaction().rollback();
			s.close();
			SmartPatrolPlugIn.displayLog("Could not save updates to patrol options. " + ex.getMessage(), ex);
		}
		return false;
	}
	
	
}
