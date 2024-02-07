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

import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.ImageProcessor;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.AbstractSmartImporter;
import org.wcs.smart.cybertracker.importer.ImportWarningDialog;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S.A;
import org.wcs.smart.cybertracker.survey.importer.TimeDataContainer.TimeCut;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.model.CyberTrackerSurvey;
import org.wcs.smart.cybertracker.survey.model.SurveyMetadata;
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
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.er.ui.mision.editor.WaypointAttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.TrackUtil;

/**
 * Imports from {@link CyberTrackerSurvey} to {@link Mission} object
 * and saves the result in database.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class MissionImporter extends AbstractSmartImporter {
	
	private SamplingUnit currentSamplingUnit;
	private TimeDataContainer<SamplingUnit> suTimeDataContainer;

	/**
	 * If survey is provided that it is used as a target survey, otherwise new survey with passed newSurveyId will be created
	 */
	public Mission importData(CyberTrackerSurvey ctSurvey, Mission mission, Survey survey, String newSurveyId) {
		clearWarning();
		currentSamplingUnit = ctSurvey.getStartSamplingUnit();
		suTimeDataContainer = new TimeDataContainer<>(currentSamplingUnit);
		
		for (String warning : ctSurvey.getWarnings()) {
			addWarning(warning);
		}

		boolean fireSurveyAdded = false;
		boolean fireMissionAdded = false;
		
		try (Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor())){
			//check if duplicate any of existing mission
			if (!checkDuplicate(ctSurvey, session)){
				return null;
			}
			
			session.beginTransaction();
			try {
				if (survey == null) {
					//this mean that user wants a new survey to be created
					survey = createNewSurvey(ctSurvey, newSurveyId);
					session.persist(survey);
					fireSurveyAdded = true;
				}
				if (mission == null) {
					//this mean that user wants a new mission to be created
					mission = createNewMission(ctSurvey, survey, session);
					fireMissionAdded = true;
					createMissingMissionDays(mission);
	
					//import mission properties
					for (MissionPropertyValue mpv : ctSurvey.getMissionProperties()) {
						mpv.setMission(mission);
						mission.getMissionPropertyValues().add(mpv);
					}
				} else {
					mission = (Mission) session.get(Mission.class, mission.getUuid()); //reloading mission object to avoid lazy initialization exception
					validateExistingMission(ctSurvey, mission, session);
				}
				
				if (mission.getLeader() == null){
					if (!fixLeaderError(mission, ctSurvey, session)){
						return null;
					}
				}
				
				//import observations
				List<S> sList = extractAndPreProcessSights(ctSurvey);
				RestTimeMap restMap = extractRestTime(sList, ctSurvey.getElementsMap());
				sList = restMap.excludePauseS(sList);
				for (S s : sList) {
					MissionDay mday = findOrAddMissionDay(mission, s);
					Waypoint wp = findOrAddWaypoint(mday, s, ctSurvey.getElementsMap(), session);
					if (wp != null) {
						addObservations(wp, s, ctSurvey.getElementsMap(), session);
					}
				}
				for (MissionDay mday : mission.getMissionDays()) {
					mday.setRestMinutes(restMap.getRestMinutes(mday.getDate()));
				}
	
				//import tracks
				appendTracks(ctSurvey, mission);
	
				//process images
				processImages(mission, session);
				
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
						SmartPlugIn.displayLog(Messages.MissionImporter_SaveError, e);
					}
				});
				return null;
			}
		}
	}
	
	protected void addObservations(Waypoint wp, S s, Map<String, E> eMap, Session session) {
		super.addObservations(wp, s, eMap, session);
		String prefix = getFilenameDateFormat().format(wp.getDateTime()); 
		addAttachments(wp, s, eMap, prefix, session);
	}

	private Survey createNewSurvey(CyberTrackerSurvey ctSurvey, String id) {
		Survey survey = new Survey();
		survey.setSurveyDesign(ctSurvey.getSurveyDesign());
		survey.setId(id);
		return survey;
	}

	private Mission createNewMission(CyberTrackerSurvey ctSurvey, Survey survey, Session session) {
		Mission m = new Mission();
		m.setSurvey(survey);
		m.setComment(ctSurvey.getComment());
		m.setStartDate(ctSurvey.getStartDate().toLocalDate());
		m.setEndDate(ctSurvey.getEndDate().toLocalDate());
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
		List<Mission> missions = QueryFactory.buildQuery(session, Mission.class,
				new Object[] {"startDate", ctSurvey.getStartDate()}, //$NON-NLS-1$
				new Object[] {"endDate", ctSurvey.getEndDate()}).getResultList(); //$NON-NLS-1$
		
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
						Messages.MissionImporter_ConfirmDialogTitle, 
						MessageFormat.format(Messages.MissionImporter_Warn_SurveyDuplicate, getMissionIdentifier(ctSurvey), duplicateId)
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
		if (mission.getStartDate().isAfter(ctSurvey.getStartDate().toLocalDate())) {
			if (!isValidTimeDelta(ctSurvey.getEndDate().toLocalDate(), mission.getStartDate()))
				addWarning(MessageFormat.format(Messages.MissionImporter_Warn_TimeGap_Before, 
						DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(mission.getStartDate()), DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(ctSurvey.getEndDate())));
			mission.setStartDate(ctSurvey.getStartDate().toLocalDate());
			daysChanged = true;
		}

		if (mission.getEndDate().isBefore(ctSurvey.getEndDate().toLocalDate())) {
			if (!isValidTimeDelta(mission.getEndDate(), ctSurvey.getStartDate().toLocalDate()))
				addWarning(MessageFormat.format(Messages.MissionImporter_Warn_TimeGap_After, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(mission.getEndDate()), DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(ctSurvey.getStartDate())));
			mission.setEndDate(ctSurvey.getEndDate().toLocalDate());
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
			addWarning(MessageFormat.format(Messages.MissionImporter_Warn_Member, SmartLabelProvider.getShortLabel(e)));
			MissionMember plm = new MissionMember();
			plm.setMission(mission);
			plm.setMember(e);
			mission.getMembers().add(plm);
		}
	}

	//ensures that gap between dates is less than a day
	//(in this case we will not have a gap after adding ctSurvey to the mission)
	private boolean isValidTimeDelta(LocalDate from, LocalDate to) {
		long delta = Math.abs(ChronoUnit.DAYS.between(from, to));
		return delta <= 1;
	}

	private void createMissingMissionDays(Mission m) {
		
		LocalTime stime = LocalTime.MIN;
		LocalTime etime = LocalTime.MAX;
		if (!m.getMissionDays().isEmpty()) {
			stime= m.getMissionDays().get(0).getStartTime();
			etime= m.getMissionDays().get(m.getMissionDays().size() - 1).getEndTime();
		}
		
		Set<LocalDate> existingDays = new HashSet<LocalDate>();
		for (MissionDay md : m.getMissionDays()) {
			existingDays.add(md.getDate());
		}
		
		LocalDate working = m.getStartDate();
		
		while (working.isBefore(m.getEndDate()) || working.isEqual(m.getEndDate())) {
			if (!existingDays.contains(working)) {
				MissionDay md = new MissionDay();
				md.setDate(working);
				md.setStartTime(LocalTime.MIN);
				md.setEndTime(LocalTime.MAX);
				md.setRestMinutes(0);
				md.setTracks(new ArrayList<MissionTrack>());
				md.setWaypoints(new ArrayList<SurveyWaypoint>());
				md.setMission(m);
				m.getMissionDays().add(md);
			}
			working = ChronoUnit.DAYS.addTo(working, 1);
		}
		m.getMissionDays().sort((a,b)->a.getDate().compareTo(b.getDate()));
		m.getMissionDays().get(0).setStartTime(stime);
		m.getMissionDays().get(m.getMissionDays().size() - 1).setStartTime(etime);
	}
	
	private boolean checkEmployees(final Mission m, final CyberTrackerSurvey ctSurvey){
		if (m.getMembers().size() == 0){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), 
							Messages.MissionImporter_ErrorDialog_Title, 
							MessageFormat.format(Messages.MissionImporter_Err_NoEmployees, 
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
						MessageFormat.format(Messages.MissionImporter_LeaderDialog_Ttitle, getMissionIdentifier(ctSurvey)),
						MessageFormat.format(Messages.MissionImporter_LeaderDialog_Message, new Object[]{ctSurvey.getCtLeader() }),
						m);
				dialog.open();
			}});
		return m.getLeader() != null;
	}

	private MissionDay findOrAddMissionDay(Mission mission, S s) {
		//we don't need to check for big gaps as this validation is done by validateExistingMission(...)
		LocalDate date = null;
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
			if (pld.getDate().isEqual(date)) {
				return pld;
			}
		}
		
		
		//NOTE: Ideally this point in code should never be reached because all mission day must already be created. 
		//The only possible way is if date is not between mission start and end date
		addWarning(MessageFormat.format(Messages.MissionImporter_TimeRangeError, date));
		
		LocalTime stime = LocalTime.MIN;
		LocalTime etime = LocalTime.MAX;
		if (!mission.getMissionDays().isEmpty()) {
			stime= mission.getMissionDays().get(0).getStartTime();
			etime= mission.getMissionDays().get(mission.getMissionDays().size() - 1).getEndTime();
		}
		
		MissionDay mday = new MissionDay();
		mday.setMission(mission);
		mday.setDate(date);
		mday.setStartTime(stime);
		mday.setEndTime(etime);
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
		wp.setObservationGroups(new ArrayList<>());
		wp.setId(String.valueOf(mday.getWaypoints().size()+1));
		wp.setSourceId(SurveyWaypointSource.KEY);
		wp.setConservationArea(SmartDB.getCurrentConservationArea());
		wp.setRawX(0);
		wp.setRawY(0);
		for (A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.TIME.equals(i)) {
				LocalTime t = LocalTime.parse(a.getV());
				wp.setDateTime(mday.getDate().atTime(t));
			} else if (ICyberTrackerConstants.LATITUDE.equals(i)) {
				wp.setRawY(Double.valueOf(a.getV()));
			} else if (ICyberTrackerConstants.LONGITUDE.equals(i)) {
				wp.setRawX(Double.valueOf(a.getV()));
			} else if (ScreensUtil.RESULT_NEW_WAYPOINT.equals(a.getN())) {
				E e = eMap.get(a.getV());
				newWp = ElementsUtil.BOOL_TRUE.equals(e.getTag0());
			} else if (SurveyMetadata.JsonKey.MISSION_SAMPLING_UNIT.key.equalsIgnoreCase(a.getN())) {
				E e = eMap.get(a.getV());
				if (e != null) {
					SamplingUnit su = null;
					if (e.getTag0() != null) {
						su = CyberTrackerHibernateManager.fetchByUuid(SamplingUnit.class, e.getTag0(), session);
						if (su != null && !su.getSurveyDesign().getConservationArea().equals(SmartDB.getCurrentConservationArea())){
							//sampling unit is not valid for this conservation area
							su = null;
						}
						if (su == null) {
							addWarning(MessageFormat.format(Messages.MissionImporter_Warn_NoSamplingUnit, e.getN()));
						}
					}
//					if ((su == null && currentSamplingUnit != null) || (su != null && !su.equals(currentSamplingUnit))) {
//						//this is not the same sampling unit
//						currentSamplingUnit = su;
//						suTimeDataContainer.add(currentSamplingUnit, wp.getDateTime()); //time must be already calculated
//					}
					//start new track for sampling unit even if user selected the same sampling unit as it was before! this is done after #1542
					currentSamplingUnit = su;
					suTimeDataContainer.add(currentSamplingUnit, wp.getDateTime()); //time must be already calculated
					return null; //special case: we don't add waypoint when record SamplingUnit change
				}
			}
		}

		swp.setSamplingUnit(currentSamplingUnit);
		swp.setWaypoint(wp);
		swp.setMissionDay(mday);
		if (newWp) {
			mday.getWaypoints().add(swp);
			return wp;
		}
		
		//below is "Add To Last Waypoint" case
		if (mday.getWaypoints().isEmpty()) {
			addWarning(Messages.MissionImporter_Warn_FirstWaypoint);
			mday.getWaypoints().add(swp);
			return wp;
		}
		
		SurveyWaypoint lastWp = mday.getWaypoints().get(mday.getWaypoints().size()-1);
		if (wp.getDateTime() != null) {
			if (lastWp.getWaypoint().getDateTime() == null)
				lastWp.getWaypoint().setDateTime(wp.getDateTime());
			
			long delta = ChronoUnit.MILLIS.between(wp.getDateTime(), lastWp.getWaypoint().getDateTime());
			if (delta > WARN_WP_TIME_FRAME * 60 * 1000) {
				addWarning(MessageFormat.format(Messages.MissionImporter_Warn_WaypointTimeframe, lastWp.getId(), WARN_WP_TIME_FRAME));
			}
		}
		return lastWp.getWaypoint();
	}	

	private void appendTracks(CyberTrackerSurvey ctSurvey, Mission mission) {
		List<Coordinate> timerTrackList = ctSurvey.getTimerTrackList();
		if (timerTrackList != null && !timerTrackList.isEmpty()) {
			for (MissionDay md : mission.getMissionDays()) {
				LocalDateTime from = md.getDate().atTime( md.getStartTime());
				LocalDateTime to = md.getDate().atTime( md.getEndTime());
				List<TimeCut<SamplingUnit>> cuts = suTimeDataContainer.getTimeCuts(from, to);
				for (TimeCut<SamplingUnit> timeCut : cuts) {
					List<Coordinate> coordinates = listPart(timerTrackList, timeCut.getStart(), timeCut.getEnd());
					LineString track = TrackUtil.convertToLineString(coordinates);
					if (track != null) {
						MissionTrack t = new MissionTrack();
						md.getTracks().add(t);
						t.setMissionDay(md);
						t.setLineString(track);
						t.setSamplingUnit(timeCut.getData());
						t.setId(Messages.MissionImporter_TrackPrefix + md.getTracks().size());
					}
					
				}
			}
		}
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
							Messages.MissionImporter_WarnDialog_Title, 
							MessageFormat.format(Messages.MissionImporter_WarnDialog_Message, getMissionIdentifier(ctSurvey)), 
							getWarnings());
					isOk[0] = wdialog.open() == IDialogConstants.OK_ID;
				}
			});
		}
		return isOk[0];
	}
	
	protected String getMissionIdentifier(CyberTrackerSurvey ctSurvey){
		return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(ctSurvey.getStartDate()) + "  [" + ctSurvey.getSurveyDesignKey() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Process all images;  This resizes the images as defined by the Cybertracker properties 
	 * for the conservation area.
	 * 
	 * @param legs the legs to process images for
	 * @param session
	 */
	protected void processImages(Mission mission, Session session){
		if (mission == null) return;
		ConservationArea ca = ((SurveyDesign) session.get(SurveyDesign.class, mission.getSurvey().getSurveyDesign().getUuid())).getConservationArea();
		CyberTrackerPropertiesOption opResize = CtJsonUtil.getImageResizeOption(ca, session);
		
		if (opResize == null || opResize.getStringValue().equalsIgnoreCase(CyberTrackerPropertiesOption.ImageResizeOption.NONE.name())) return;
		
		
		List<ISmartAttachment> attachments = new ArrayList<>();
		for (MissionDay l : mission.getMissionDays()){			
			for (SurveyWaypoint pw : l.getWaypoints()){
				if (pw.getWaypoint().getAttachments() == null) continue;
				for (WaypointAttachment attachment : pw.getWaypoint().getAttachments()){
					attachments.add(attachment);
				}
				
				for (WaypointObservation wo : pw.getWaypoint().getAllObservations()){
					if (wo.getAttachments() == null) continue;
					for (ObservationAttachment attachment : wo.getAttachments()){
						attachments.add(attachment);
					}
				}		
				
			}
		}
		
		//TODO: this needs testing; inparticular the attachment.getCopyFromLocation() option
		double maxsizebytes = CtJsonUtil.getImageMaxSizeOption(ca, session) * 1048576l;
		if (opResize.getStringValue().equalsIgnoreCase(CyberTrackerPropertiesOption.ImageResizeOption.AUTO.name())){
			//attempt to resize image automatically
			int[] size = CtJsonUtil.getImageAutoResizeSizeOption(ca, session);		
			for (ISmartAttachment attachment : attachments){
				//only process new attachments
				if (attachment.getCopyFromLocation() != null && attachment.getCopyFromLocation().toAbsolutePath().toFile().length() >= maxsizebytes){
					Path[] pp = ImageProcessor.INSTANCE.processAttachment(attachment,size[0], size[1]);
					for (Path p : pp) p.toFile().deleteOnExit();
				}
			}	
		}
	}
}
