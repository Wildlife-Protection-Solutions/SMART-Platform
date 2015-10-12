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
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
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
import org.wcs.smart.er.model.MissionPropertyValue;
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
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.TrackUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * Imports from {@link CyberTrackerSurvey} to {@link Mission} object
 * and saves the result in database.
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
		//check if duplicate any of existing mission
		if (!checkDuplicate(ctSurvey, session)){
			return null;
		}

		boolean fireSurveyAdded = false;
		boolean fireMissionAdded = false;
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
				fireMissionAdded = true;

				//import mission properties
				for (MissionPropertyValue mpv : ctSurvey.getMissionProperties()) {
					mpv.setMission(mission);
					mission.getMissionPropertyValues().add(mpv);
				}
			} else {
				mission = (Mission) session.load(Mission.class, mission.getUuid()); //reloading mission object to avoid lazy initialization exception
				validateExistingMission(ctSurvey, mission, session);
			}
			
			if (mission.getLeader() == null){
				if (!fixLeaderError(mission, ctSurvey, session)){
					return null;
				}
			}
			
			//import observations
			List<S> sList = SightsMultiObsUtil.convertMultiObs(ctSurvey);
			for (S s : sList) {
				MissionDay mday = findOrAddMissionDay(mission, s);
				Waypoint wp = findOrAddWaypoint(mday, s, ctSurvey.getElementsMap(), session);
				addObservations(wp, s, ctSurvey.getElementsMap(), session);
			}

			//import tracks
			appendTracks(ctSurvey, mission);

			if (!displayWarnings(ctSurvey))
				return null;

			SurveyHibernateManager.saveMission(mission, session, true);
			session.getTransaction().commit();
			if (fireSurveyAdded) {
				SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_ADDED, survey);
			}
			SurveyEventHandler.getInstance().fireEvent(fireMissionAdded ? EventType.MISSION_ADDED : EventType.MISSION_MODIFIED, mission);
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

	protected boolean checkDuplicate(final CyberTrackerSurvey ctSurvey, final Session session) {
		Criteria c = session.createCriteria(Mission.class);
		c.add(Restrictions.eq("startDate", ctSurvey.getStartDate())); //$NON-NLS-1$
		c.add(Restrictions.eq("endDate", ctSurvey.getEndDate())); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Mission> missions = c.list(); 

		Mission duplicate = null;
		if (missions.size() > 0) {
			//we need a comparator to sort employee no matter how, but fast
			Comparator<Employee> comparator = new Comparator<Employee>() {
				@Override
				public int compare(Employee o1, Employee o2) {
					return o1.getUuid().compareTo(o2.getUuid());
				}
			};
			TreeSet<Employee> membersSet = new TreeSet<Employee>(comparator);
			membersSet.addAll(ctSurvey.getMembers());
			//ensure that it is really a duplicate
			for (Mission m : missions) {
				if (m.getMembers().size() == ctSurvey.getMembers().size()) {
					Set<Employee> copy = new TreeSet<Employee>(membersSet);
					for (MissionMember mm : m.getMembers()) {
						if (!copy.remove(mm.getMember())) {
							break;
						}
					}
					if (copy.isEmpty()) {
						duplicate = m;
						break;
					}
				}
			}
		}
		
		if (duplicate != null) {
			final boolean[] ret = new boolean[]{false};
			final String duplicateId = duplicate.getId(); 
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					ret[0] = MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
						Messages.PatrolImporter_ImportDialogTitle, 
						MessageFormat.format("The CyberTracker survey {0} looks to be a duplicate of the existing SMART survey {1}.  By importing you may potentially be duplicating data.  Do you want to continue?", getMissionIdentifier(ctSurvey), duplicateId)
						);
				}});
			return ret[0];
		}
		
		return true;
	}

	/**
	 * Method is called when we add to existing mission. We heed to check that this operation occurs fine.
	 */
	private void validateExistingMission(CyberTrackerSurvey ctSurvey, Mission mission, Session session) {
		boolean daysChanged = false;
		if (mission.getStartDate().getTime() > ctSurvey.getStartDate().getTime()) {
			if (!isValidTimeDelta(ctSurvey.getEndDate(), mission.getStartDate()))
				addWarning(MessageFormat.format("Existing mission start date ({0}) and new mission day end date ({1}) have a difference of more than a day. This will introduce a time gap in resulting mission.", DateFormat.getDateInstance(DateFormat.MEDIUM).format(mission.getStartDate()), DateFormat.getDateInstance(DateFormat.MEDIUM).format(ctSurvey.getEndDate())));
			mission.setStartDate(ctSurvey.getStartDate());
			daysChanged = true;
		}

		if (mission.getEndDate().getTime() < ctSurvey.getEndDate().getTime()) {
			if (!isValidTimeDelta(mission.getEndDate(), ctSurvey.getStartDate()))
				addWarning(MessageFormat.format("Existing mission end date ({0}) and new mission day start date ({1}) have a difference of more than a day. This will introduce a time gap in resulting mission.", DateFormat.getDateInstance(DateFormat.MEDIUM).format(mission.getEndDate()), DateFormat.getDateInstance(DateFormat.MEDIUM).format(ctSurvey.getStartDate())));
			mission.setEndDate(ctSurvey.getEndDate());
			daysChanged = true;
		}
		
		if (daysChanged) {
			createMissingMissionDays(mission);
		}

		//validate members and add mission people
		Set<Employee> members = new HashSet<Employee>(ctSurvey.getMembers());
		for (MissionMember mm : mission.getMembers()) {
			members.remove(mm.getMember());
		}
		for (Employee e : members) {
			addWarning(MessageFormat.format("Member {0} is not in a list of members in existing mission. Member will be added to the list of members.", SmartLabelProvider.getShortLabel(e)));
			MissionMember plm = new MissionMember();
			plm.setMission(mission);
			plm.setMember(e);
			mission.getMembers().add(plm);
		}
	}

	//ensures that gap between dates is less than a day
	//(in this case we will not have a gap after adding ctSurvey to the mission)
	private boolean isValidTimeDelta(Date from, Date to) {
		from = SharedUtils.getDatePart(from, false);
		to = SharedUtils.getDatePart(to, false);
		long delta = to.getTime() - from.getTime();
		return delta <= 1000 * 60 * 60 * 24; //more that a day
	}

	private void createMissingMissionDays(Mission m) {
		Set<Calendar> existingDays = new HashSet<Calendar>();
		for (MissionDay md : m.getMissionDays()) {
			existingDays.add(SharedUtils.convertDate(md.getDate()));
		}
		
		Calendar calStart = SharedUtils.convertDate(m.getStartDate());
		calStart.set(Calendar.HOUR, 0);
		calStart.set(Calendar.MINUTE, 0);
		calStart.set(Calendar.SECOND, 0);
		calStart.set(Calendar.MILLISECOND, 0);
		
		Calendar calEnd = SharedUtils.convertDate(m.getEndDate());
		while (calStart.before(calEnd) || calStart.equals(calEnd)) {
			if (!existingDays.contains(calStart)) {
				MissionDay md = new MissionDay();
				md.setDate(SharedUtils.getDatePart(calStart.getTime(), false));
				md.setStartTime(createTime(0, 0, 0));
				md.setEndTime(createTime(23, 59, 59));
				md.setRestMinutes(0);
				md.setTracks(new ArrayList<MissionTrack>());
				md.setWaypoints(new ArrayList<SurveyWaypoint>());
				md.setMission(m);
				m.getMissionDays().add(md);
			}
			calStart.add(Calendar.DAY_OF_MONTH, 1);
		}
	}
	
	private boolean checkEmployees(final Mission m, final CyberTrackerSurvey ctSurvey){
		if (m.getMembers().size() == 0){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), 
							"Import Error", 
							MessageFormat.format("The CyberTracker survey ({0}) does not have any valid employees and cannot be imported into SMART.", 
								new Object[]{getMissionIdentifier(ctSurvey)}));
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
						MessageFormat.format(Messages.SmartImporter_LeaderTitle, getMissionIdentifier(ctSurvey)),
						MessageFormat.format(Messages.SmartImporter_SelectLeaderMessage, new Object[]{ctSurvey.getCtLeader() }),
						m);
				dialog.open();
			}});
		return m.getLeader() != null;
	}

	private MissionDay findOrAddMissionDay(Mission mission, S s) {
		//we don't need to check for big gaps as this validation is done by validateExistingMission(...)
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
			if (SharedUtils.isSameDate(pld.getDate(), date)) {
				return pld;
			}
		}
		MissionDay mday = new MissionDay();
		mday.setMission(mission);
		mday.setDate(date);
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

	private void appendTracks(CyberTrackerSurvey ctSurvey, Mission mission) {
		List<Coordinate> timerTrackList = ctSurvey.getTimerTrackList();
		if (timerTrackList != null && !timerTrackList.isEmpty()) {
			for (MissionDay md : mission.getMissionDays()) {
				Date from = combine(md.getDate(), md.getStartTime());
				Date to = combine(md.getDate(), md.getEndTime());
				List<Coordinate> coordinates = listPart(timerTrackList, from, to);
				LineString track = TrackUtil.convertToLineString(coordinates, MissionTrack.ZTIMEZONE);
				if (track != null) {
					MissionTrack t = new MissionTrack();
					md.getTracks().add(t);
					t.setMissionDay(md);
					t.setLineString(track);
					t.setId("Track" + md.getTracks().size());
				}
			}
		}
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
							MessageFormat.format("The following warnings were generated during the import of CyberTracker survey ''{0}'':", getMissionIdentifier(ctSurvey)), 
							getWarnings());
					isOk[0] = wdialog.open() == IDialogConstants.OK_ID;
				}
			});
		}
		return isOk[0];
	}
	
	protected String getMissionIdentifier(CyberTrackerSurvey ctSurvey){
		return DateFormat.getDateTimeInstance().format(ctSurvey.getStartDate()) + "  [" + ctSurvey.getSurveyDesignKey() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
