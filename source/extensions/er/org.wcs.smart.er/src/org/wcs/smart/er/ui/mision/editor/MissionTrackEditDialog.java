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
package org.wcs.smart.er.ui.mision.editor;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.ui.ISurveyListener;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing mission tracks.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionTrackEditDialog extends TitleAreaDialog {

	private Session session;
	
	private boolean isChanged = false;
	
	private Mission mission;
	private Date date;

	public MissionTrackEditDialog(Shell shell, Mission mission, Date date) {
		super(shell);
		this.date = date;

		session = HibernateManager.openSession();
		session.beginTransaction();
		this.mission = (Mission) session.load(Mission.class, mission.getUuid());
	}

	Mission getMission() {
		return mission;
	}
	
	Date getDate() {
		return date;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);

		String title = MessageFormat.format(Messages.MissionTrackEditDialog_Title, mission.getId(), DateFormat.getDateInstance(DateFormat.MEDIUM).format(date));
		setTitle(title);
		getShell().setText(title);
		setMessage(Messages.MissionTrackEditDialog_Message);

		TracksComposite cmp = new TracksComposite(comp, this);
		cmp.addChangeListener(new ISurveyListener() {
			@Override
			public void compositeModified() {
				if (getButton(IDialogConstants.OK_ID) == null) return;
				getButton(IDialogConstants.OK_ID).setEnabled(true);
				isChanged = true;
			}
		});
		
		return comp;
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		Button ok = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		ok.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}

	protected void okPressed() {
		saveChanges();
	}
	
	public boolean close() {
		if (isChanged) {
			MessageDialog md = new MessageDialog(getShell(), 
					"Edit Mission Tracks", 
					null, 
					"There are unsaved changes.  Would you like to save your changes before closing?", MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			int ret = md.open();
			if (ret == 2) {
				//cancel
				return false;
			}else if (ret == 0){
				//yes
				if (!saveChanges()){
					return false;
				}else{
					setReturnCode(IDialogConstants.OK_ID);
				}
			}
		}
		if (session.getTransaction().isActive()){
			session.getTransaction().rollback();
		}
		session.close();
		
		return super.close();
	}

	private boolean saveChanges() {
		try {
			session.getTransaction().commit();
			isChanged = false;
			
			SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, mission);
			
			//start a new transaction
			session.beginTransaction();
			return true;
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.displayLog("Error saving changes.  Please close dialog and try again." + "\n\n" + ex.getMessage(), ex);
			return false;
		}
	}
	
}
