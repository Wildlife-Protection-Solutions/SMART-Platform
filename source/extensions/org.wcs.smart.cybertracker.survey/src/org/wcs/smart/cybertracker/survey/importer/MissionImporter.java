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
package org.wcs.smart.cybertracker.survey.importer;

import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.AbstractSmartImporter;
import org.wcs.smart.cybertracker.importer.ImportWarningDialog;
import org.wcs.smart.cybertracker.importer.SightsMultiObsUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S.A;
import org.wcs.smart.cybertracker.survey.export.SurveyScreensUtil;
import org.wcs.smart.cybertracker.survey.model.CyberTrackerSurvey;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.er.ui.mision.editor.WaypointAttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.util.SmartUtils;

/**
 * Imports from {@link CyberTrackerSurvey} to {@link Mission} object
 * and saves imported result as new mission in database.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class MissionImporter extends AbstractSmartImporter {

	/**
	 * If survey is provided that it is used as a target survey, otherwise new survey with passed newSurveyId will be created
	 */
	public Mission importData(CyberTrackerSurvey ctSurvey, Mission mission, Survey survey, String newSurveyId) {
		clearWarning();
		
		for (String warning : ctSurvey.getWarnings()) {
			addWarning(warning);
		}
		
		Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor());
		boolean fireSurveyAdded = false;
		try {
			session.beginTransaction();
			if (survey == null) {
				//this mean that user wants a new survey to be created
				survey = createNewSurvey(ctSurvey, newSurveyId);
				session.save(survey);
				fireSurveyAdded = true;
			}
			if (mission == null) {
				//this mean that user wants a new mission to be created
				mission = createNewMission(ctSurvey, survey, session);
			} else {
				mission = (Mission) session.load(Mission.class, mission.getUuid()); //reloading mission object to avoid lazy initialization exception
				//TODO: validate mission!!!
			}
			
			//check if duplicate of existing Mission
			if (!checkDuplicate(ctSurvey, mission, session)){
				return null;
			}
			
			if (mission.getLeader() == null){
				if (!fixLeaderError(mission, ctSurvey, session)){
					return null;
				}
			}
			List<S> sList = SightsMultiObsUtil.convertMultiObs(ctSurvey);
			for (S s : sList) {
				MissionDay mday = findOrAddMissionDay(mission, s);
				Waypoint wp = findOrAddWaypoint(mday, s, ctSurvey.getElementsMap(), session);
				addObservations(wp, s, ctSurvey.getElementsMap(), session);
			}

			if (!displayWarnings(ctSurvey))
				return null;

			SurveyHibernateManager.saveMission(mission, session, true);
			session.getTransaction().commit();
			if (fireSurveyAdded) {
				SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_ADDED, survey);
			}
			SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_ADDED, mission);
			return mission;
		} catch (final Exception e) {
			session.getTransaction().rollback();
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					SmartPlugIn.displayLog("Failed to save mission.", e);
				}
			});
			return null;
		}
		finally {
			session.close();
		}
	}

	private Survey createNewSurvey(CyberTrackerSurvey ctSurvey, String id) {
		Survey survey = new Survey();
		survey.setSurveyDesign(ctSurvey.getSurveyDesign());
		survey.setStartDate(ctSurvey.getStartDate());
		survey.setEndDate(ctSurvey.getEndDate());
		survey.setId(id);
		return survey;
	}

	private Mission createNewMission(CyberTrackerSurvey ctSurvey, Survey survey, Session session) {
		Mission m = new Mission();
		m.setSurvey(survey);
		m.setComment(ctSurvey.getComment());
		m.setStartDate(ctSurvey.getStartDate());
		m.setEndDate(ctSurvey.getEndDate());
		List<MissionMember> members = new ArrayList<MissionMember>();
		for (Employee e : ctSurvey.getMembers()) {
			MissionMember plm = new MissionMember();
			plm.setMission(m);
			plm.setMember(e);
			plm.setIsLeader(e.equals(ctSurvey.getLeader()));
			members.add(plm);
		}
		m.setMembers(members);

		return m;
	}

	protected boolean checkDuplicate(final CyberTrackerSurvey ctSurvey, Mission m, final Session session) {
		//TODO: implement!!!
		return true;
	}
	
	private boolean checkEmployees(final Mission m, final CyberTrackerSurvey ctSurvey){
		if (m.getMembers().size() == 0){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), 
							"Import Error", 
							MessageFormat.format("The CyberTracker survey ({0}) does not have any valid employees and cannot be imported into SMART.", 
								new Object[]{getPatrolIdentifier(ctSurvey)}));
				}				
			});
			return false;
		}
		return true;
	}
	
	protected boolean fixLeaderError(final Mission m, final CyberTrackerSurvey ctSurvey, final Session session){
		if (!checkEmployees(m, ctSurvey)){
			return false;
		}
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				LeaderSelectorDialog dialog = new LeaderSelectorDialog(
						Display.getDefault().getActiveShell(), 
						MessageFormat.format(Messages.SmartImporter_LeaderTitle, getPatrolIdentifier(ctSurvey)),
						MessageFormat.format(Messages.SmartImporter_SelectLeaderMessage, new Object[]{ctSurvey.getCtLeader() }),
						m);
				dialog.open();
			}});
		return m.getLeader() != null;
	}

	private MissionDay findOrAddMissionDay(Mission mission, S s) {
		//TODO: do we need to check for big gaps?
		Date date = null;
		for (A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.DATE.equals(i)) {
				date = toDate(a.getV());
				break;
			}
		}
		
		if (date == null)
			return null;
		
		for (MissionDay pld : mission.getMissionDays()) {
			if (SmartUtils.isSameDate(pld.getDate(), date)) {
				return pld;
			}
		}
		MissionDay mday = new MissionDay();
		mday.setMission(mission);
		mday.setDate(date);
		//TODO: start time/end time; maybe not from 00:00 to 23:59? 
		mday.setStartTime(createTime(0, 0, 0));
		mday.setEndTime(createTime(23, 59, 59));
		mday.setRestMinutes(0);
		mday.setTracks(new ArrayList<MissionTrack>());
		mday.setWaypoints(new ArrayList<SurveyWaypoint>());
		mission.getMissionDays().add(mday);
		return mday;
	}

	private Waypoint findOrAddWaypoint(MissionDay mday, S s, Map<String, E> eMap, Session session) {
		if (mday.getWaypoints() == null)
			mday.setWaypoints(new ArrayList<SurveyWaypoint>());

		boolean newWp = true;

		SurveyWaypoint swp = new SurveyWaypoint();
		Waypoint wp = new Waypoint();
		wp.setObservations(new ArrayList<WaypointObservation>());
		wp.setId(mday.getWaypoints().size()+1);
		wp.setSourceId(SurveyWaypointSource.KEY);
		wp.setConservationArea(SmartDB.getCurrentConservationArea());
		wp.setX(0);
		wp.setY(0);
		for (A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.TIME.equals(i)) {
				Time t = Time.valueOf(a.getV());
				wp.setDateTime(SmartUtils.combineDateTime(mday.getDate(), t));
			} else if (ICyberTrackerConstants.LATITUDE.equals(i)) {
				wp.setY(Double.valueOf(a.getV()));
			} else if (ICyberTrackerConstants.LONGITUDE.equals(i)) {
				wp.setX(Double.valueOf(a.getV()));
			} else if (ScreensUtil.RESULT_NEW_WAYPOINT.equals(a.getN())) {
				E e = eMap.get(a.getV());
				newWp = ElementsUtil.BOOL_TRUE.equals(e.getTag0());
			} else if (SurveyScreensUtil.RESULT_MISSION_SAMPLING_UNIT.equals(a.getN())) {
				E e = eMap.get(a.getV());
				if (e != null) {
					SamplingUnit su = CyberTrackerHibernateManager.fetchByUuid(SamplingUnit.class, e.getTag0(), session);
					swp.setSamplingUnit(su);
				}
			}
		}

		swp.setWaypoint(wp);
		swp.setMissionDay(mday);
		if (newWp) {
			mday.getWaypoints().add(swp);
			return wp;
		}
		
		//below is "Add To Last Waypoint" case
		if (mday.getWaypoints().isEmpty()) {
			addWarning(Messages.SmartImporter_Warn_WrongFirstWaypoint);
			mday.getWaypoints().add(swp);
			return wp;
		}
		
		SurveyWaypoint lastWp = mday.getWaypoints().get(mday.getWaypoints().size()-1);
		if (wp.getDateTime() != null) {
			if (lastWp.getWaypoint().getDateTime() == null)
				lastWp.getWaypoint().setDateTime(wp.getDateTime());
			
			long delta = Math.abs(wp.getDateTime().getTime() - lastWp.getWaypoint().getDateTime().getTime());
			if (delta > WARN_WP_TIME_FRAME * 60 * 1000) {
				addWarning(MessageFormat.format(Messages.SmartImporter_Warn_AddToWaypointTimeframe, lastWp.getId(), WARN_WP_TIME_FRAME));
			}
		}
		return lastWp.getWaypoint();
	}	

	private Time createTime(int hours, int minute, int second){
		Calendar cForProcessing = Calendar.getInstance();
		cForProcessing.setTimeInMillis(0);
		cForProcessing.set(Calendar.HOUR_OF_DAY, hours);
		cForProcessing.set(Calendar.MINUTE, minute);
		cForProcessing.set(Calendar.SECOND, second);
		cForProcessing.set(Calendar.MILLISECOND, 0);
		return new Time(cForProcessing.getTime().getTime());
	}
	
	/**
	 * Displays warnings dialog if warnings present and returns if user choose to proceed with import
	 * @return
	 */
	protected boolean displayWarnings(final CyberTrackerSurvey ctSurvey) {
		final boolean[] isOk = {true};
		if (getWarnings() != null && getWarnings().size() > 0) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					ImportWarningDialog wdialog = new ImportWarningDialog(Display.getDefault().getActiveShell(), 
							"Warning", 
							MessageFormat.format("The following warnings were generated during the import of CyberTracker survey ''{0}'':", getPatrolIdentifier(ctSurvey)), 
							getWarnings());
					isOk[0] = wdialog.open() == IDialogConstants.OK_ID;
				}
			});
		}
		return isOk[0];
	}
	
	protected String getPatrolIdentifier(CyberTrackerSurvey ctSurvey){
		return DateFormat.getDateTimeInstance().format(ctSurvey.getStartDate()) + "  [" + ctSurvey.getSurveyDesignKey() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
