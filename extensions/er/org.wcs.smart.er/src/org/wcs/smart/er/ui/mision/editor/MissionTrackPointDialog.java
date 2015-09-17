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

import java.util.TimeZone;
import java.util.UUID;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.ui.map.TrackPointDialog;

import com.vividsolutions.jts.geom.LineString;

/**
 * Dialog for display tack points for a mission track.
 * <p>The track points are displayed in a simple table.</p>
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionTrackPointDialog extends TrackPointDialog {
	
	private MissionTrack track;
	private MissionTrack editTrack;

	/**
	 * @param parentShell parent shell
	 * @param t the track to display
	 */
	public MissionTrackPointDialog(Shell parentShell, MissionTrack t) throws Exception {
		super(parentShell);
		this.track = t;
		
		editTrack = new MissionTrack();
		editTrack.setLineString(t.getLineString());
		editTrack.setUuid(track.getUuid());
	}

	@Override
	protected TimeZone getTrackZTimezone() {
		return MissionTrack.ZTIMEZONE;
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
			throw new IllegalStateException(ex);
		}
	}

	@Override
	protected void setEditTrackLineString(LineString ls) {
		editTrack.setLineString(ls);
	}

	@Override
	protected void okPressed() {
		Job job = null;
		Mission m = track.getMissionDay().getMission();
		//save then close
		try{
			if (editTrack.getLineString() == null){
				//delete track
				track.getMissionDay().getTracks().remove(track);
				track.setMissionDay(null);
				job = new DeleteMissionTracksJob(track);
			}else{
				track.setLineString(editTrack.getLineString());
				job = new SaveMissionTracksJob(track);
			}
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(Messages.MissionTrackPointDialog_GeometryError, ex);
			
		}

		//save and fire
		try {
			job.schedule();
			job.join();
			SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, m);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			setModified(false);
		}catch (InterruptedException ex){
			throw new IllegalStateException("Save Job Interrupted", ex); //$NON-NLS-1$
		}
	}
	
}
