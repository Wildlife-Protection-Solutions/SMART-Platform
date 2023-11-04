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
package org.wcs.smart.cybertracker.patrol.importer;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.cybertracker.ImageProcessor;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.AbstractSmartImporter;
import org.wcs.smart.cybertracker.importer.ImportWarningDialog;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.ImportError;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S.A;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol.PatrolMeta;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.EmployeeSelectorDialog;

/**
 * Common logic for importing patrols and patrol legs.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public abstract class AbstractPatrolImporter extends AbstractSmartImporter {

	protected boolean fixTransportError(final CyberTrackerPatrol ctPatrol) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				
				try (Session session = HibernateManager.openSession()){
					List<PatrolTransportType> types = PatrolHibernateManager.getActivePatrolTransporationTypes(SmartDB.getCurrentConservationArea(), session);
					List<ImportError> trProblem = ctPatrol.getProblems().get(PatrolMeta.TRANSPORT);
					String message = trProblem != null && !trProblem.isEmpty() ? trProblem.get(0).getMessage() : null;
					TransportSelectorDialog selectorDialog = new TransportSelectorDialog(Display.getDefault().getActiveShell(), types, message);
					if (selectorDialog.open() != IDialogConstants.OK_ID) {
						return;
					}
					ctPatrol.setPatrolTransportType(selectorDialog.getSelectedTransportType());
				} catch (final Exception e) {
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							SmartPlugIn.displayLog(Messages.SmartImporter_Transport_Load_Error, e);
						}
					});
				}
			}
		});
		return ctPatrol.getPatrolTransportType() != null;
	}
	
	protected String getPatrolIdentifier(CyberTrackerPatrol ctPatrol){
		return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(ctPatrol.getStartDate()) + "  [" + ctPatrol.getCtTransport() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private boolean checkEmployees(final PatrolLeg leg, final CyberTrackerPatrol ctPatrol){
		if (leg.getMembers().size() == 0){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), 
							Messages.SmartImporter_ImportErrorDialogTitle, 
							MessageFormat.format(Messages.SmartImporter_NoEmployeesErrorMessage, 
								new Object[]{getPatrolIdentifier(ctPatrol)}));
				}				
			});
			return false;
		}
		return true;
	}
	
	protected boolean fixLeaderError(final PatrolLeg leg, final CyberTrackerPatrol ctPatrol, final Session session){
		if (!checkEmployees(leg, ctPatrol)){
			return false;
		}
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				EmployeeSelectorDialog dialog = new EmployeeSelectorDialog(
						Display.getDefault().getActiveShell(), 
						MessageFormat.format(Messages.SmartImporter_LeaderTitle, getPatrolIdentifier(ctPatrol)),
						MessageFormat.format(Messages.SmartImporter_SelectLeaderMessage, new Object[]{ctPatrol.getCtLeader() }),
						EmployeeSelectorDialog.Type.LEADER, leg);
				dialog.open();
			}});
		return leg.getLeader() != null;
	}


	protected boolean fixPilotError(final PatrolLeg leg, final CyberTrackerPatrol ctPatrol, final Session session){
		if (!checkEmployees(leg, ctPatrol)){
			return false;
		}
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				EmployeeSelectorDialog dialog = new EmployeeSelectorDialog(
						Display.getDefault().getActiveShell(), 
						MessageFormat.format(Messages.SmartImporter_PilotTitle, getPatrolIdentifier(ctPatrol)),
						MessageFormat.format(Messages.SmartImporter_SelectPilotTitle, new Object[]{ctPatrol.getPilot()}), 
						EmployeeSelectorDialog.Type.PILOT, leg);
				dialog.open();
			}});
		return leg.getPilot() != null;
	}
	
	protected void initLegData(PatrolLeg leg, CyberTrackerPatrol ctPatrol, Session session) {
		if (ctPatrol.getPatrolTransportType() != null) {
			leg.setType(ctPatrol.getPatrolTransportType());
		}
		leg.setStartDate(ctPatrol.getStartDate().toLocalDate());
		leg.setEndDate(ctPatrol.getEndDate().toLocalDate());
		leg.setMandate(ctPatrol.getMandate());
		List<PatrolLegMember> legMembers = new ArrayList<PatrolLegMember>();
		for (Employee e : ctPatrol.getMembers()) {
			PatrolLegMember plm = new PatrolLegMember();
			plm.setPatrolLeg(leg);
			plm.setMember(e);
			plm.setIsLeader(e.equals(ctPatrol.getLeader()));
			plm.setIsPilot(e.equals(ctPatrol.getPilot()));
			legMembers.add(plm);
		}
		leg.setMembers(legMembers);
		
		leg.createLegDays(session);
		leg.getPatrolLegDays().get(0).setStartTime(ctPatrol.getStartDate().toLocalTime());
		leg.getPatrolLegDays().get(leg.getPatrolLegDays().size() - 1).setEndTime(ctPatrol.getEndDate().toLocalTime());
		
		List<Coordinate> timerTrackList = ctPatrol.getTimerTrackList();
		if (timerTrackList != null && !timerTrackList.isEmpty()) {
			for (PatrolLegDay pld : leg.getPatrolLegDays()) {
				LocalDateTime from = pld.getDate().atTime(pld.getStartTime());
				LocalDateTime to = pld.getDate().atTime(pld.getEndTime());
				
				List<Coordinate> coordinates = listPart(timerTrackList, from, to);
				Track track = PatrolUtils.convertToTrack(coordinates);
				if (track != null) {
					track.setPatrolLegDay(pld);
					pld.setTrack(track);
				}
			}
		}
	}
	
	protected void addObservations(PatrolLeg leg, S s, Map<String, E> eMap, Session session) {
		PatrolLegDay legDay = findLegDay(leg, s);
		if (legDay == null)
			return;
		
		Waypoint wp = findOrAddWaypoint(legDay, s, eMap);
		addObservations(wp, s, eMap, session);
		String prefix = getFilenameDateFormat().format(legDay.getDate()) + "_Leg_"+leg.getId(); //$NON-NLS-1$
		addAttachments(wp, s, eMap, prefix, session);
	}

	/**
	 * All leg days must create at this point.
	 * They must be created as part of initLegData(...) logic
	 */
	private PatrolLegDay findLegDay(PatrolLeg leg, S s) {
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
		
		for (PatrolLegDay pld : leg.getPatrolLegDays()) {
			if (pld.getDate().equals(date)) {
				return pld;
			}
		}
		return null;
	}

	protected Waypoint findOrAddWaypoint(PatrolLegDay pld, S s, Map<String, E> eMap) {
		if (pld.getWaypoints() == null)
			pld.setWaypoints(new ArrayList<PatrolWaypoint>());

		boolean newWp = true;

		PatrolWaypoint pwp = new PatrolWaypoint();
		Waypoint wp = new Waypoint();
		wp.setObservationGroups(new ArrayList<>());
		wp.setId(String.valueOf(pld.getWaypoints().size()+1));
		wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
		wp.setConservationArea(SmartDB.getCurrentConservationArea());
		wp.setRawX(0);
		wp.setRawY(0);
		for (A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.TIME.equals(i)) {
				LocalTime t = LocalTime.parse(a.getV());
				wp.setDateTime(pld.getDate().atTime(t));
			} else if (ICyberTrackerConstants.LATITUDE.equals(i)) {
				wp.setRawY(Double.valueOf(a.getV()));
			} else if (ICyberTrackerConstants.LONGITUDE.equals(i)) {
				wp.setRawX(Double.valueOf(a.getV()));
			} else if (ScreensUtil.RESULT_NEW_WAYPOINT.equals(a.getN())) {
				E e = eMap.get(a.getV());
				newWp = ElementsUtil.BOOL_TRUE.equals(e.getTag0());
			}
		}

		pwp.setWaypoint(wp);
		pwp.setPatrolLegDay(pld);
		if (newWp) {
			pld.getWaypoints().add(pwp);
			return wp;
		}
		
		//below is "Add To Last Waypoint" case
		if (pld.getWaypoints().isEmpty()) {
			addWarning(Messages.SmartImporter_Warn_WrongFirstWaypoint);
			pld.getWaypoints().add(pwp);
			return wp;
		}
		
		PatrolWaypoint lastWp = pld.getWaypoints().get(pld.getWaypoints().size()-1);
		if (wp.getDateTime() != null) {
			if (lastWp.getWaypoint().getDateTime() == null)
				lastWp.getWaypoint().setDateTime(wp.getDateTime());
			
			long delta = ChronoUnit.MILLIS.between(wp.getDateTime(), lastWp.getWaypoint().getDateTime());
			if (delta > WARN_WP_TIME_FRAME * 60 * 1000) {
				addWarning(MessageFormat.format(Messages.SmartImporter_Warn_AddToWaypointTimeframe, lastWp.getWaypoint().getId(), WARN_WP_TIME_FRAME));
			}
		}
		return lastWp.getWaypoint();
	}

	/**
	 * Displays warnings dialog if warnings present and returns if user choose to proceed with import
	 * @return
	 */
	protected boolean displayWarnings(final CyberTrackerPatrol ctPatrol) {
		final boolean[] isOk = {true};
		if (getWarnings() != null && getWarnings().size() > 0) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					ImportWarningDialog wdialog = new ImportWarningDialog(Display.getDefault().getActiveShell(), 
							Messages.SmartImporter_WarnDialog_Title, 
							MessageFormat.format(Messages.SmartImporter_WarnDialog_Message, getPatrolIdentifier(ctPatrol)), 
							getWarnings());
					isOk[0] = wdialog.open() == IDialogConstants.OK_ID;
				}
			});
		}
		return isOk[0];
	}

	/**
	 * Process all images;  This resizes the images as defined by the Cybertracker properties 
	 * for the conservation area.
	 * 
	 * @param legs the legs to process images for
	 * @param session
	 */
	protected void processImages(List<PatrolLeg> legs, Session session){
		if (legs.size() == 0);
		ConservationArea ca = legs.get(0).getPatrol().getConservationArea();
		CyberTrackerPropertiesOption opResize = CtJsonUtil.getImageResizeOption(ca, session);
		
		if (opResize == null || opResize.getStringValue().equalsIgnoreCase(CyberTrackerPropertiesOption.ImageResizeOption.NONE.name())) return;
		
		
		List<ISmartAttachment> attachments = new ArrayList<>();
		for (PatrolLeg l : legs){
			for (PatrolLegDay pld : l.getPatrolLegDays()){
				if (pld.getWaypoints() == null) continue;
				for (PatrolWaypoint pw : pld.getWaypoints()){
					if (pw.getWaypoint().getAttachments() == null) continue;
					for (WaypointAttachment attachment : pw.getWaypoint().getAttachments()){
						attachments.add(attachment);
					}
					if (pw.getWaypoint().getAllObservations() != null){
						for (WaypointObservation wo : pw.getWaypoint().getAllObservations()){
							if (wo.getAttachments() == null) continue;
							for (ObservationAttachment attachment : wo.getAttachments()){
								attachments.add(attachment);
							}
						}		
					}
				}
			}
		}
		double maxsizebytes = CtJsonUtil.getImageMaxSizeOption(ca, session) * 1048576l;
		if (opResize.getStringValue().equalsIgnoreCase(CyberTrackerPropertiesOption.ImageResizeOption.AUTO.name())){
			//attempt to resize image automatically
			int[] size = CtJsonUtil.getImageAutoResizeSizeOption(ca, session);		
			for (ISmartAttachment attachment : attachments){
				if (attachment.getCopyFromLocation().toAbsolutePath().toFile().length() >= maxsizebytes){
					ImageProcessor.INSTANCE.processAttachment(attachment,size[0], size[1]);
				}
			}	
		}
	}
}
