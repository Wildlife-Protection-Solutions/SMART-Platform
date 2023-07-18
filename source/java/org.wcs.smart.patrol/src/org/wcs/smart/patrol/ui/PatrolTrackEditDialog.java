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
package org.wcs.smart.patrol.ui;

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.editor.PartolTracksComposite;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.map.TracksComposite.ITracksCompositeListener;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing patrol track.
 * 
 * @author elitvin
 * @since 6.0.0
 */
public class PatrolTrackEditDialog extends SmartStyledTitleDialog {

	private boolean isChanged = false;
	
	private PatrolLegDay patrolLegDay;
	private PartolTracksComposite cmp;
	private boolean canEdit;

	private Track trackBackup;
	
	public PatrolTrackEditDialog(Shell shell, PatrolLegDay patrolLegDay, boolean canEdit) {
		super(shell);
		this.patrolLegDay = patrolLegDay;
		this.canEdit = canEdit;
		trackBackup = backupTrack(patrolLegDay);
	}

	private Track backupTrack(PatrolLegDay patrolLegDay) {
		if (patrolLegDay.getTrack() == null) {
			return null;
		}
		try {
			Track t = new Track();
			t.setLineStrings(patrolLegDay.getTrack().getLineStrings());
			return t;
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(ex.getMessage(), ex);
		}
		return null;
	}

	private void restoreTrack(PatrolLegDay patrolLegDay, Track track) {
		if (track == null) {
			return;
		}
		try{
			patrolLegDay.getTrack().setLineStrings(track.getLineStrings());
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(ex.getMessage(), ex);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);

		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				String title = MessageFormat.format(Messages.PatrolTrackEditDialog_Title, 
						DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(patrolLegDay.getDate()));
				setTitle(title);
				getShell().setText(title);
				setMessage(Messages.PatrolTrackEditDialog_Message);
	
				cmp = new PartolTracksComposite(comp, patrolLegDay, canEdit);
				cmp.addChangeListener(new ITracksCompositeListener() {
					@Override
					public void compositeModified() {
						if (getButton(IDialogConstants.OK_ID) == null) return;
						getButton(IDialogConstants.OK_ID).setEnabled(true);
						isChanged = true;
					}
				});
				return comp;
			} finally {
				session.getTransaction().rollback();
			}
		}
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		Button ok = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		ok.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}

	protected void okPressed() {
		saveChanges();
	}
	
	public boolean isChanged(){
		return this.isChanged;
	}
	
	public boolean close() {
		if (isChanged) {
			MessageDialog md = new MessageDialog(getShell(), 
					Messages.PatrolTrackEditDialog_ConfirmCloseDialog_Title, 
					null, 
					Messages.PatrolTrackEditDialog_ConfirmCloseDialog_Message, MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			int ret = md.open();
			switch (ret) {
			case 0: //yes
				if (!saveChanges()){
					return false;
				}else{
					setReturnCode(IDialogConstants.OK_ID);
				}
				break;
			case 1: //no
				restoreTrack(patrolLegDay, trackBackup);
				break;
			case 2: //cancel
				return false;
			default: //should never happen
				return false;
			}
		}
		return super.close();
	}

	public boolean saveChanges() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				PatrolLegDay ref = session.getReference(patrolLegDay);
				
				if (patrolLegDay.getTrack().getGeom() == null) {
					ref.getTracks().clear();
					patrolLegDay.setTrack(null);
				} else {
					
					if (ref.getTrack() == null) {
						Track t = new Track();
						t.setPatrolLegDay(ref);
						session.persist(t);
						patrolLegDay.getTrack().setUuid(t.getUuid());
					}
					ref.getTrack().setDistance(patrolLegDay.getTrack().getDistance());
					ref.getTrack().setGeom(patrolLegDay.getTrack().getGeom());
				}
				
				session.getTransaction().commit();
				
			} catch (Exception ex) {
				SmartPlugIn.displayLog(Messages.PatrolTrackEditDialog_SaveError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				return false;
			}
		}

		trackBackup = backupTrack(patrolLegDay);
		isChanged = false;
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_TRACKS, patrolLegDay);
		return true;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	@Override
	public Point getInitialSize(){
		return new Point(1000, 800);
	}
	
}
