package org.wcs.smart.cybertracker.survey.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.wcs.smart.cybertracker.survey.internal.messages"; //$NON-NLS-1$
	public static String CyberTrackerSurvey_Err_SurveyDesignMissing;
	public static String CyberTrackerSurvey_Err_SurveyDesignNotFound;
	public static String CyberTrackerSurvey_Warn_Leader;
	public static String CyberTrackerSurvey_Warn_Member;
	public static String CyberTrackerSurvey_Warn_MissionPropertyListItem;
	public static String CyberTrackerSurvey_Warn_MissionPropertyNotFound;
	public static String CyberTrackerSurvey_Warn_MissionPropertyNotInDesign;
	public static String CyberTrackerSurvey_Warn_UnsupportedAttribute;
	public static String SurveyCTExportDialog_LoadSurveyDesigns_Error;
	public static String SurveyCTExportDialog_SurveyDesign;
	public static String SurveyScreensUtil_BeginSurvey;
	public static String SurveyScreensUtil_Comments;
	public static String SurveyScreensUtil_EndSurvey;
	public static String SurveyScreensUtil_EndSurveyMessage;
	public static String SurveyScreensUtil_Error_InvalidMissionPropertyType;
	public static String SurveyScreensUtil_Error_Leader;
	public static String SurveyScreensUtil_ErrorDialog_Title;
	public static String SurveyScreensUtil_Leader;
	public static String SurveyScreensUtil_NewObservation;
	public static String SurveyScreensUtil_NewSamplingUnit;
	public static String SurveyScreensUtil_NoSamplingUnit;
	public static String SurveyScreensUtil_PausedSurveyTitle;
	public static String SurveyScreensUtil_PauseSurvey;
	public static String SurveyScreensUtil_ResumeSurvey;
	public static String SurveyScreensUtil_SamplingUnit;
	public static String SurveyScreensUtil_StartSurvey;
	public static String SurveyScreensUtil_StartSurveyTitle;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
