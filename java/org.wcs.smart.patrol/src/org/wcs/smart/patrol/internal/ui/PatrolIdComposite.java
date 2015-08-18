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
package org.wcs.smart.patrol.internal.ui;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for collecting patrol comments.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PatrolIdComposite extends PatrolItemComposite {
	private Text txtPatrolId;
	private ControlDecoration cdPatrolId;

	public PatrolIdComposite() {
		
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.PatrolIdComposite_Id_Label);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		txtPatrolId = new Text(center, SWT.BORDER | style);
		txtPatrolId.setTextLimit(Patrol.MAX_ID_LENGTH);
		txtPatrolId.setText(PatrolHibernateManager.AUTO_GENERATE_TEXT);
		
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = 8;
		data.widthHint = 200;
		txtPatrolId.setLayoutData(data);
		
		cdPatrolId = createDecoration(txtPatrolId);
		cdPatrolId.hide();
		ModifyListener lsn = new ModifyListener() {			
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
				fireChangeListeners();
			}
		};
		txtPatrolId.addModifyListener(lsn);
		
		return main;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(Patrol p, Session session) {
    	if (p.getId() != null){
    		txtPatrolId.setText(p.getId());
    	}
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public boolean updatePatrol(Patrol p, Session session) {
		String newPatrolId = txtPatrolId.getText().trim();
		
		boolean isDup = PatrolHibernateManager.isDuplicateId(newPatrolId, p.getConservationArea(), session);
		if (isDup){
			if (!MessageDialog.openQuestion(txtPatrolId.getDisplay().getActiveShell(), 
					Messages.PatrolIdComposite_WarningDialogTitle, 
					MessageFormat.format(Messages.PatrolIdComposite_DuplicateIdWarning, new Object[]{newPatrolId}))){
				return false;
			}
		}
		if(validate()){
			p.setId(newPatrolId);
			return true;
		}else{
			return false;
		}
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return Messages.PatrolIdComposite_Title;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_ID;
	}
	
	public boolean validate() {
		boolean isValid = true;
		errorMessage = null;
		if (! SmartUtils.isSimpleString(txtPatrolId.getText(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Patrol.MAX_ID_LENGTH) ) {
			cdPatrolId.show();
			errorMessage = MessageFormat.format(Messages.PatrolIdComposite_Error_InvalidId,
					new Object[]{Patrol.MAX_ID_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc});
			cdPatrolId.setDescriptionText(errorMessage);
			isValid = false;
		}else{
			cdPatrolId.hide();
		}
		return isValid;
	}
	
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}


}
