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

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Track;
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

	private int lsIndex;
	private LineString lineString; //lineString for editing
	
	public PatrolTrackPointDialog(Shell parentShell, Track t, boolean canEdit) {
		this(parentShell, t, 0, canEdit);
	}

	/**
	 * @param parentShell parent shell
	 * @param t the track to display
	 * @param lsIndex index of a linestring to edit
	 * @param canEdit if the track can be editted
	 */
	public PatrolTrackPointDialog(Shell parentShell, Track t, int lsIndex, boolean canEdit) {
		super(parentShell, canEdit);
		this.track = t;
		this.lsIndex = lsIndex;
		
		try{
			lineString = track.getLineStrings().get(lsIndex);
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(ex.getMessage(), ex);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
		Button btn = createButton(parent, IDialogConstants.OK_ID, "Apply", true);
		btn.setEnabled(false);
	}
	
	@Override
	protected TimeZone getTrackZTimezone() {
		return Track.ZTIMEZONE;
	}

	@Override
	protected UUID getEditTrackUUid() {
		return track.getUuid();
	}

	@Override
	public LineString getEditTrackLineString() {
		return lineString;
	}

	@Override
	protected void setEditTrackLineString(LineString ls) {
		lineString = ls;
	}

	@Override
	/*
	 * Saves changes to track
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed(){
		try{
			//save then close
			List<LineString> lineStrings = new ArrayList<>(track.getLineStrings());
			if (lineString != null) {
				lineStrings.set(lsIndex, lineString);
			} else {
				lineStrings.remove(lsIndex);
			}
			if (lineStrings.isEmpty()) {
				//delete track
				track.getPatrolLegDay().setTrack(null);
				track.setPatrolLegDay(null);
			} else {
				track.setLineStrings(lineStrings);
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(ex.getMessage(), ex);
			return;
		}
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		setModified(false);
	}
	
}
