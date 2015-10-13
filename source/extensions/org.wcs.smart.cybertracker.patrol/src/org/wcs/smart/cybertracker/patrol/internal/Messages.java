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
package org.wcs.smart.cybertracker.patrol.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.cybertracker.patrol.internal.messages"; //$NON-NLS-1$
	public static String CyberTrackerExportHandler_ErrDialog_Title;
	public static String DataModelWrapper_Dropdown_Label;
	public static String PatrolCTExportDialog_ConfigurableModel;
	public static String PatrolCTExportDialog_LoadConfModels_Error;
	public static String PatrolScreens_Begin;
	public static String PatrolScreens_Begin_GPSRequiredMessage;
	public static String PatrolScreens_Begin_Title;
	public static String PatrolScreens_Comments;
	public static String PatrolScreens_Confirm;
	public static String PatrolScreens_ConfirmMessage;
	public static String PatrolScreens_EndPatrol;
	public static String PatrolScreens_Exit_Title;
	public static String PatrolScreens_ExitCyberTracker;
	public static String PatrolScreens_IsArmed;
	public static String PatrolScreens_Leader;
	public static String PatrolScreens_Mandate;
	public static String PatrolScreens_Members;
	public static String PatrolScreens_NewObservation;
	public static String PatrolScreens_NextTask;
	public static String PatrolScreens_Objective;
	public static String PatrolScreens_PatrolType;
	public static String PatrolScreens_Paused;
	public static String PatrolScreens_PausePatrol;
	public static String PatrolScreens_Pilot;
	public static String PatrolScreens_ResumePatrol;
	public static String PatrolScreens_Start_Title;
	public static String PatrolScreens_StartPatrol;
	public static String PatrolScreens_Station;
	public static String PatrolScreens_Team;
	public static String PatrolScreens_Transport;
	public static String PatrolScreensUtil_Error_Meta_Leader;
	public static String PatrolScreensUtil_Error_Meta_Mandate;
	public static String PatrolScreensUtil_Error_Meta_Pilot;
	public static String PatrolScreensUtil_Error_Meta_Station;
	public static String PatrolScreensUtil_Error_Meta_Team;
	public static String PatrolScreensUtil_Error_Meta_Transport;
	public static String PatrolScreensUtil_Error_TransportNotSet;
	public static String PatrolScreensUtil_Error_TypesNotSet;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
