/*
 * Copyright (C) 2026 Wildlife Conservation Society
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
package org.wcs.smart.er.internal;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.MissionMergeDialog;
import org.wcs.smart.er.ui.handlers.EditSurveyElementHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SharedUtils;

/**
 * Tools for merging a collection of missions into a new mission
 */
public class MissionMerger {

	public void doMergeMissions(Shell shell, Set<UUID> uuids) {
		
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
		
		try {
			dialog.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						mergeMissions(uuids, shell, monitor);
					}catch (OperationCanceledException ex) {
						EcologicalRecordsPlugIn.displayLog(Messages.MissionMerger_Cancelled, ex);
						
					} catch (Exception e) {
						EcologicalRecordsPlugIn.displayLog(MessageFormat.format(Messages.MissionMerger_MergeError, e.getMessage()), e);
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			EcologicalRecordsPlugIn.displayLog(MessageFormat.format(Messages.MissionMerger_MergeError, e.getMessage()), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void mergeMissions(Set<UUID> uuids, Shell shell, IProgressMonitor monitor) throws Exception {
		
		SubMonitor sub = SubMonitor.convert(monitor);
		sub.beginTask(Messages.MissionMerger_TaskName, 5);
		
		MissionMergeMetadata metadata = new MissionMergeMetadata();
		
		try(Session session = HibernateManager.openSession()){
		
			//validate missions
			List<Mission> missions = new ArrayList<>();
			for (UUID muuid : uuids) {
				Mission m = session.get(Mission.class, muuid);
				if (m != null) missions.add(m);
			}
			
			if (missions.size() <= 1) {
				throw new Exception(Messages.MissionMerger_MissionsRequired);
			}
			
			//ensure they are all part of the same design; allow different surveys
			Set<SurveyDesign> designs = new HashSet<>();
			missions.forEach(m->designs.add(m.getSurvey().getSurveyDesign()));
			
			if (designs.size() > 1) {
				throw new Exception(Messages.MissionMerger_SurveyDesignError);
			}
			
			//need to pick all the metadata values for the mission
			//survey ops
			Set<Survey> surveyops = new HashSet<>();
			StringJoiner comments = new StringJoiner("\n"); //$NON-NLS-1$
			Set<Employee> members = new HashSet<>();
			Map<MissionAttribute, List<MissionPropertyValue>> props = new HashMap<>();
			for (MissionProperty p : designs.iterator().next().getMissionProperties()) {
				props.put(p.getAttribute(), new ArrayList<>());
			}
			List<String> ids = new ArrayList<>();
			
			Employee leader = null;
			LocalDate startDate = null;
			LocalDate endDate = null;
			
			for (Mission m : missions) {
				surveyops.add(m.getSurvey());
				ids.add(m.getId());
				
				if (startDate == null || startDate.isAfter(m.getStartDate())) {
					startDate = m.getStartDate();
				} 
				if (endDate == null || endDate.isBefore(m.getEndDate())) {
					endDate = m.getEndDate();
				}
				if (m.getComment() != null && !m.getComment().isBlank()) comments.add(m.getComment());
				
				if (leader == null) leader = m.getLeader().getMember();
				m.getMembers().forEach(e->members.add(e.getMember()));
				
				for (MissionPropertyValue p : m.getMissionPropertyValues()) {
					if (props.get(p.getMissionAttribute()) == null) continue; //not supported by this survey design
					props.get(p.getMissionAttribute()).add(p);					
				}
			}
			
			metadata.setMissionIdOps(ids);
			metadata.setMissionDates(startDate, endDate);
			metadata.setSurveyOps(new ArrayList<>(surveyops));
			metadata.setEmployeeOps(new ArrayList<>(members), leader);
			metadata.setAttributeOps(props);
			metadata.setComment(comments.toString());
			if (metadata.getComment().length() > Mission.MAX_LENGTH_COMMENT) {
				metadata.setComment(metadata.getComment().substring(0, Mission.MAX_LENGTH_COMMENT));
			}
			
			final boolean[] isok = {false};
			
			long fdays = ChronoUnit.DAYS.between(metadata.getStartDate(), metadata.getEndDate());
			if (fdays> Mission.MAX_MISSION_LENGTH_DAYS) {
				throw new Exception(MessageFormat.format(Messages.MissionMerger_MaxDays, fdays, Mission.MAX_MISSION_LENGTH_DAYS));
			}
			
			Display.getDefault().syncExec(()->{				
				if (fdays > Mission.WARN_MISSION_LENGTH_DAYS) {
					boolean ok = MessageDialog.openQuestion(shell, Messages.MissionMerger_ConfirmTitle, MessageFormat.format(Messages.MissionMerger_MaxDays2, fdays));
					if (!ok) {
						isok[0] = false;
						return;
					}
				}
				
				MissionMergeDialog dialog = new MissionMergeDialog(shell, metadata);
				isok[0] = (dialog.open() == Window.OK);			
			});
			
			if (!isok[0]) return;
			
			sub.split(1);
		}
		
		Object[] results = doMerge(uuids, metadata, sub.split(4));
		Mission newMission = (Mission) results[0];
		List<Mission> removedMissions = (List<Mission>) results[1];

		//fire events
		SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_ADDED, newMission);
		removedMissions.forEach(m->SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_DELETED, m));
		
		Display.getDefault().asyncExec(()->EditSurveyElementHandler.editMission(shell, newMission.getUuid(), newMission.getId()));
	}
	
	private Object[] doMerge(Set<UUID> missionuuids, MissionMergeMetadata metadata, IProgressMonitor monitor) throws Exception {
		
		SubMonitor sub = SubMonitor.convert(monitor);
		sub.beginTask(Messages.MissionMerger_TaskName, missionuuids.size() + 1);
		
		//merge all missions into new mission
		Mission newMission = new Mission();
		newMission.setSurvey(metadata.getSurvey());
		newMission.setId(metadata.getMissionId());
		
		newMission.setComment(metadata.getComment());
		newMission.setStartDate(metadata.getStartDate());
		newMission.setEndDate(metadata.getEndDate());
		
		newMission.setMembers(new ArrayList<>());
		metadata.getEmployeeOps().forEach(m -> {
			MissionMember mm = new MissionMember();
			mm.setMember(m);
			mm.setMission(newMission);
			mm.setIsLeader(m.equals(metadata.getLeader()));
			newMission.getMembers().add(mm);
		
		});
		
		newMission.setMissionPropertyValues(new ArrayList<>());
		metadata.getAttributeValues().forEach(av->{
			MissionPropertyValue v = new MissionPropertyValue();
			v.setMission(newMission);
			v.setMissionAttribute(av.getMissionAttribute());
			v.setValue(av.getValue());
			newMission.getMissionPropertyValues().add(v);			
		});
		
		
		newMission.setMissionDays(new ArrayList<>());
		
		LocalDate currentDay = metadata.getStartDate();
		while(currentDay.isBefore(metadata.getEndDate()) || currentDay.equals(metadata.getEndDate())) {
			
			MissionDay md = new MissionDay();
			md.setMission(newMission);
			md.setDate(currentDay);
			md.setTracks(new ArrayList<>());
			md.setWaypoints(new ArrayList<>());
			md.setStartTime(LocalTime.MAX);
			md.setRestMinutes(0);
			md.setEndTime(SharedUtils.END_OF_DAY);
			newMission.getMissionDays().add(md);
			currentDay = currentDay.plusDays(1);
		}
		sub.split(1);
		
		List<Mission> removedMissions = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			
			try {
				//save new mission
				session.persist(newMission);
				session.flush();
				
				for(UUID olduuid : missionuuids) {
					Mission old = session.get(Mission.class, olduuid);
					sub.setTaskName(MessageFormat.format(Messages.MissionMerger_processing, old.getId()));
					
					for (MissionDay oldmd : old.getMissionDays()) {
						MissionDay mergeto = null;
						
						for (MissionDay newmd : newMission.getMissionDays()) {
							if (newmd.getDate().equals(oldmd.getDate())) {
								mergeto = newmd;
								break;
							}
						}
						
						if (mergeto == null) {
							throw new Exception(Messages.MissionMerger_MissingDay + oldmd.getDate().toString());
						}
						
						//deal with tracks
						Map<MissionTrack, MissionTrack> old2new = new HashMap<>();
						for(MissionTrack oldtrack : oldmd.getTracks()) {
							MissionTrack newtrack = new MissionTrack();
							newtrack.setGeom(oldtrack.getGeom());
							newtrack.setId(oldtrack.getId());
							newtrack.setMissionDay(mergeto);
							newtrack.setSamplingUnit(oldtrack.getSamplingUnit());
							session.persist(newtrack);
							
							mergeto.getTracks().add(newtrack);							
							old2new.put(oldtrack, newtrack);
						}
						oldmd.getTracks().forEach(t->session.remove(t));
						oldmd.getTracks().clear();
						
						//merge waypoint
						for (SurveyWaypoint oldwp : new ArrayList<>(oldmd.getWaypoints())) {
							SurveyWaypoint newwp = new SurveyWaypoint();
							newwp.setMissionDay(mergeto);
							newwp.setSamplingUnit(oldwp.getSamplingUnit());
							newwp.setWaypoint(oldwp.getWaypoint());
							newwp.setMissionTrack(old2new.get(oldwp.getMissionTrack()));
							
							//SurveyWaypoint primary key is wp_uuid
							//so we have to remove the exisiting one
							//before we can add a new one
							oldwp.getMissionDay().getWaypoints().remove(oldwp);
							session.remove(oldwp);							
							session.flush();
							
							session.persist(newwp);							
							mergeto.getWaypoints().add(newwp);
							
						}
						
						if (mergeto.getRestMinutes() < oldmd.getRestMinutes()) {
							mergeto.setRestMinutes(oldmd.getRestMinutes());
						}										
					}
					old.getMissionDays().forEach(md->session.remove(md));
					old.getMissionDays().clear();
					session.remove(old);
					session.flush();
					removedMissions.add(old);
					
					sub.split(1);
				}
				
				//compute start/end for each day
				for(MissionDay md : newMission.getMissionDays()) {
					
					LocalTime start = null;
					LocalTime end = null;
					
					
					//update time from tracks if required
					for (MissionTrack track : md.getTracks()) {
						LineString ls = track.getLineString();
						LocalTime lsstart = SharedUtils.toLocalDateTime(ls.getCoordinateN(0)).toLocalTime();
						LocalTime lsend= SharedUtils.toLocalDateTime(ls.getCoordinateN(ls.getNumPoints()-1)).toLocalTime();
						
						
						if (start == null || lsstart.isBefore(start)) {
							start = lsstart;
						}
						if (end == null || lsend.isAfter(end)) {
							end = lsend;
						}
					}
					
					for (SurveyWaypoint newwp : md.getWaypoints()) {					
						if (start == null || newwp.getWaypoint().getDateTime().toLocalTime().isBefore(start)) {
							start = newwp.getWaypoint().getDateTime().toLocalTime();
						}
						if (end == null || newwp.getWaypoint().getDateTime().toLocalTime().isAfter(end)) {
							end = newwp.getWaypoint().getDateTime().toLocalTime();
						}
					}
					if (start != null) md.setStartTime(start);
					if (end != null) md.setEndTime(end);
					
				}
				
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
			

		}
		return new Object[] {newMission, removedMissions};
	}
}
