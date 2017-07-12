/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.patrol.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.qa.patrol.internal.messages"; //$NON-NLS-1$
	public static String DeletePatrolTrackAction_ActionName;
	public static String DeletePatrolTrackAction_DeleteConfirmMessage;
	public static String DeletePatrolTrackAction_DeleteDialogTitle;
	public static String DeletePatrolTrackAction_DeletedMsg;
	public static String DeletePatrolTrackAction_DeleteError;
	public static String DeletePatrolTrackAction_TrackNotFoundError;
	public static String DeletePatrolWaypointAction_ActionName;
	public static String DeletePatrolWaypointAction_ConfirmDeleteMsg;
	public static String DeletePatrolWaypointAction_DeleteDialogTitle;
	public static String DeletePatrolWaypointAction_DeletedMsg;
	public static String DeletePatrolWaypointAction_DeleteError;
	public static String DeletePatrolWaypointAction_DeleteErrorNotFound;
	public static String EditTrackAction_ActionName;
	public static String EditTrackAction_ModifiedMsg;
	public static String EditTrackAction_NotFoundDialogMsg;
	public static String EditTrackAction_NotFoundDialogTitle;
	public static String EditTrackAction_ParseError;
	public static String EditTrackAction_TrackNotFoundMsg;
	public static String EditWaypointAction_ActionFixMessage;
	public static String EditWaypointAction_ActionName;
	public static String OpenSourcePatrolAction_ActionName;
	public static String OpenSourcePatrolAction_NotFoundDialogTitle;
	public static String OpenSourcePatrolAction_TrackNotFound;
	public static String OpenSourcePatrolAction_WpNotFound;
	public static String PatrolEditWaypointDialog_interpolateToolTooltip;
	public static String PatrolEditWaypointDialog_WpNotFound;
	public static String PatrolLabelProvider_ErrorValidatingPatrolTrack;
	public static String PatrolLabelProvider_InvalidMaxSpeed;
	public static String PatrolLabelProvider_LegLabel;
	public static String PatrolLabelProvider_LoadingDataMsg;
	public static String PatrolLabelProvider_MaxSpeedDescription;
	public static String PatrolLabelProvider_MaxSpeedLbl;
	public static String PatrolLabelProvider_MaxSpeedName;
	public static String PatrolLabelProvider_PatrolProviderName;
	public static String PatrolLabelProvider_PatrolTypes;
	public static String PatrolLabelProvider_SpeedUnits;
	public static String PatrolLabelProvider_TrackNotFound;
	public static String PatrolLabelProvider_TrackSpeedExceeded;
	public static String PatrolLabelProvider_WaypointDataProviderName;
	public static String PatrolLabelProvider_WaypointIdLbl;
	public static String PatrolLabelProvider_WaypointNotFound;
	public static String PatrolLabelProvider_WpSpeedExceeded;
	public static String PatrolSpeedParameterCollector_DbError;
	public static String PatrolSpeedParameterCollector_InvalidMaxSpeedValue;
	public static String PatrolSpeedParameterCollector_TransportTypeRequired;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
