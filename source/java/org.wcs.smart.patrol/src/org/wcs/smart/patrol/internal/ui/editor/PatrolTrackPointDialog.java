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
package org.wcs.smart.patrol.internal.ui.editor;

import java.util.TimeZone;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.SavePatrolPartJob;
import org.wcs.smart.ui.map.TrackPointDialog;

import com.vividsolutions.jts.geom.LineString;

/**
 * Dialog for display tack points for a patrol track.
 * <p>The track points are displayed in a simple table.</p>
 * 
 * @author Emily
 * @author elitvin
 * @since 3.0.0
 */
public class PatrolTrackPointDialog extends TrackPointDialog {

	private Track track;
	private Track editTrack;	//copy of track for editing
	
	/**
	 * @param parentShell parent shell
	 * @param t the track to display
	 */
	public PatrolTrackPointDialog(Shell parentShell, Track t) {
		super(parentShell);
		this.track = t;
		
		editTrack = new Track();
		try{
			editTrack.setLineString(t.getLineString());
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(ex.getMessage(), ex);
		}
		editTrack.setUuid(track.getUuid());
	}

	@Override
	protected TimeZone getTrackZTimezone() {
		return Track.ZTIMEZONE;
	}

	@Override
	protected UUID getEditTrackUUid() {
		return editTrack.getUuid();
	}

	@Override
	protected LineString getEditTrackLineString() {
		try{
			return editTrack.getLineString();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(ex.getMessage(), ex);
			return null;
		}
	}

	@Override
	protected void setEditTrackLineString(LineString ls) {
		editTrack.setLineString(ls);
	}

	@Override
	/*
	 * Saves changes to track
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed(){
		Patrol p = track.getPatrolLegDay().getPatrolLeg().getPatrol();
		PatrolLegDay pld = track.getPatrolLegDay();
		try{
			//save then close
			if (editTrack.getLineString() == null){
				//delete track
				track.getPatrolLegDay().setTrack(null);
				track.setPatrolLegDay(null);
			}else{
				track.setLineString(editTrack.getLineString());
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(ex.getMessage(), ex);
			return;
		}
		//save and fire
		SavePatrolPartJob saveJob = new SavePatrolPartJob(p,pld); 		
		saveJob.schedule();
		try{
			saveJob.join();
			PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_TRACKS, pld);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			setModified(false);
		}catch (InterruptedException ex){
			throw new IllegalStateException("Save Job Interrupted", ex); //$NON-NLS-1$
		}
	}
	
}
