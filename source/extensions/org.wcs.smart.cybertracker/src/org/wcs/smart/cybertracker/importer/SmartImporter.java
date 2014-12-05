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
package org.wcs.smart.cybertracker.importer;

import java.io.File;
import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.PatrolScreensUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol.ImportError;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol.PatrolMeta;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S.A;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Common smart importing logic.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class SmartImporter {
	
	private static final int WARN_WP_TIME_FRAME = 10; //in minutes

	private List<String> warnings = new ArrayList<String>();

	public static DateFormat createCyberTrackerDateFormatter() {
		DateFormat formatter = new SimpleDateFormat(ICyberTrackerConstants.CT_DATE_FORMAT); //will this always work or CT might provide different format depending on locale settings?
		return formatter;
	}

	public static Date combine(Date date, Time time) {
		if (date == null)
			return time;
		if (time == null)
			return date;
		Calendar timeCalendar = Calendar.getInstance();
		timeCalendar.setTime(time);
		Calendar dateCalendar = Calendar.getInstance();
		dateCalendar.setTime(date);
		dateCalendar.add(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
		dateCalendar.add(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
		dateCalendar.add(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));
		return dateCalendar.getTime();
	}

	/**
	 * Returns list of coordinates recorded during specified period of time.
	 * Assumes that source list is sorted by z coordinate (date+time).
	 * @param list
	 * @param from
	 * @param to
	 * @return
	 */
	public static List<Coordinate> listPart(final List<Coordinate> list, Date from, Date to) {
		Coordinate fromC = new Coordinate(0, 0, from.getTime() - 0.5);
		Coordinate toC = new Coordinate(0, 0, to.getTime() + 0.5);
		Comparator<Coordinate> cmp = new CoordinateZComparator();
		int low = binaryCut(list, fromC, cmp) + 1;
		int high = binaryCut(list, toC, cmp) + 1;
		return new ArrayList<Coordinate>(list.subList(low, high));
	}

	/**
	 * Returns max index of an element smaller or equal to the key
	 */
    private static <T> int binaryCut(List<? extends T> l, T key, Comparator<? super T> c) {
        int low = 0;
        int high = l.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = l.get(mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return low-1;  // key not found
    }
	
	protected boolean fixTransportError(final CyberTrackerPatrol ctPatrol) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				Session session = HibernateManager.openSession();
				try {
					List<PatrolTransportType> types = PatrolHibernateManager.getActivePatrolTransporationTypes(SmartDB.getCurrentConservationArea(), session, ctPatrol.getPatrolType());
					List<ImportError> trProblem = ctPatrol.getProblems().get(PatrolMeta.TRANSPORT);
					String message = trProblem != null && !trProblem.isEmpty() ? trProblem.get(0).getMessage() : null;
					TransportSelectorDialog selectorDialog = new TransportSelectorDialog(Display.getDefault().getActiveShell(), types, message);
					if (selectorDialog.open() != IDialogConstants.OK_ID) {
						return;
					}
					ctPatrol.setPatrolTransportType(selectorDialog.getSelectedTransportType());
				} catch (final Exception e) {
					session.getTransaction().rollback();
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.SmartImporter_Transport_Load_Error, e);
						}
					});
				}
				finally {
					if (session.getTransaction().isActive()){
						session.getTransaction().rollback();
					}
					session.close();
				}
			}
		});
		return ctPatrol.getPatrolTransportType() != null;
	}
	
	protected String getPatrolIdentifier(CyberTrackerPatrol ctPatrol){
		return DateFormat.getDateTimeInstance().format(ctPatrol.getStartDate()) + "  [" + ctPatrol.getCtTransport() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
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
		leg.setStartDate(ctPatrol.getStartDate());
		leg.setEndDate(ctPatrol.getEndDate());
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
		
		List<Coordinate> timerTrackList = ctPatrol.getTimerTrackList();
		if (timerTrackList == null || timerTrackList.isEmpty())
			return;
		for (PatrolLegDay pld : leg.getPatrolLegDays()) {
			Date from = combine(pld.getDate(), pld.getStartTime());
			Date to = combine(pld.getDate(), pld.getEndTime());
			List<Coordinate> coordinates = listPart(timerTrackList, from, to);
			Track track = PatrolUtils.convertToTrack(coordinates);
			if (track != null) {
				track.setPatrolLegDay(pld);
				pld.setTrack(track);
			}
		}
	
	}
	
	protected void addObservations(PatrolLeg leg, S s, Map<String, E> eMap, Session session) {
		PatrolLegDay legDay = findLegDay(leg, s);
		if (legDay == null)
			return;
		
		Waypoint wp = findOrAddWaypoint(legDay, s, eMap);
		addObservations(wp, s, eMap, session);
		addAttachments(wp, s, eMap);
	}

	protected void addObservations(Waypoint wp, S s, Map<String, E> eMap, Session session) {
		List<List<A>> aList = splitObservationRecords(s, eMap);
		if (aList.isEmpty())
			return;
		List<A> catList = aList.get(0);
		if (catList.isEmpty())
			return;
		
		Category category = fetchCategory(catList, eMap, session);
		if (category == null)
			return;
		
		for (int i = 1; i < aList.size(); i++) {
			List<A> attrList = aList.get(i);
			WaypointObservation obs = new WaypointObservation();
			obs.setWaypoint(wp);
			obs.setCategory(category);
			obs.setAttributes(fetchAttributes(obs, attrList, eMap, session));
			
			wp.getObservations().add(obs);
		}
	}

	/**
	 * @param s
	 * @param eMap
	 * @return First list is a list of categories, all the rest are list of attributes
	 */
	private List<List<A>> splitObservationRecords(S s, Map<String, E> eMap) {
		List<A> categories = new ArrayList<A>();
		Map<Integer, List<A>> attributes = new HashMap<Integer, List<A>>();
		List<A> preMsAtts = null;
		boolean processMultiselect = true;
		A defaultValue = null;
		for (A a : s.getA()) {
			E e = eMap.get(a.getI());
			if (e == null)
				continue; //skip invalid records
			
			if (ElementsUtil.CATEGORY_ELEMENT_TAG.equals(e.getTag1())) {
				categories.add(a);
			} else if (ElementsUtil.ATTRIBUTE_ELEMENT_TAG.equals(e.getTag1())) {
				Integer tag2 = e.getTag2() != null ? Integer.valueOf(e.getTag2()) : 0;
				List<A> aList = attributes.get(tag2);
				if (aList == null) {
					aList = new ArrayList<A>();
					attributes.put(tag2, aList);
				}
				aList.add(a);
			} else if (ElementsUtil.DEFAULT_VALUES_ELEMENT_TAG.equals(e.getTag1())) {
				defaultValue = a;
			} else if (ElementsUtil.MULISELECT_ELEMENT_TAG.equals(e.getTag1())) {
				if (processMultiselect) {
					processMultiselect = false;
					preMsAtts = attributes.remove(0); //all previously enter values MUST be here
					if (preMsAtts == null) {
						preMsAtts = Collections.emptyList();
					}
					if (!attributes.isEmpty()) {
						//development validation; MUST never enter here
						throw new IllegalStateException("Mapping to entry different from #0 present before multi-select occured."); //$NON-NLS-1$
					}
				}
				Integer tag2 = e.getTag2() != null ? Integer.valueOf(e.getTag2()) : 0;
				List<A> aList = attributes.get(tag2);
				if (aList == null) {
					aList = new ArrayList<A>();
					aList.addAll(preMsAtts); //add all previously entered values as they should be applied to all items selected in multi-select
					attributes.put(tag2, aList);
				}
				//create fake record as if it is result element for listAttribute with listAttributeValue
				A fakeA = new A();
				fakeA.setN("List Attribute for item " + a.getN()); //will never be displayed //$NON-NLS-1$
				fakeA.setI(e.getTag3());
				fakeA.setV(a.getI());
				fakeA.setValue(""); //$NON-NLS-1$
				aList.add(fakeA);
				if (e.getTag4() != null && !e.getTag4().isEmpty()) {
					//also need to add numberAttribute if this is multiselect+number (reference to numAttr in tag4, value in a.getV())
					fakeA = new A();
					fakeA.setN("Number Attribute for item " + a.getN()); //will never be displayed //$NON-NLS-1$
					fakeA.setI(e.getTag4());
					fakeA.setV(a.getV());
					fakeA.setValue(""); //$NON-NLS-1$
					aList.add(fakeA);
				}
			}
		}
		
		List<List<A>> result = new ArrayList<List<A>>();
		result.add(categories);
		for (Integer i : attributes.keySet()) {
			List<A> aList = attributes.get(i);
			if (defaultValue != null) {
				aList.add(defaultValue);
			}
			result.add(aList);
		}
		return result;
	}
	
	private List<WaypointObservationAttribute> fetchAttributes(WaypointObservation obs, List<A> aList, Map<String, E> eMap, Session session) {
		List<WaypointObservationAttribute> result = new ArrayList<WaypointObservationAttribute>();
		for (A a : aList) {
			if (a.getV() == null)
				continue;
			
			E e = eMap.get(a.getI());

			if (ElementsUtil.DEFAULT_VALUES_ELEMENT_TAG.equals(e.getTag1())) {
				//handling default values
				String[] ctIdArray = a.getV().split(ICyberTrackerConstants.ATTRIBUTE_DEFAULT_VALUES_SEPATATOR);
				for (String ctid : ctIdArray) {
					E de = eMap.get(ctid);
					WaypointObservationAttribute wpoa = createWaypointObservationAttribute(de, de.getTag2(), eMap, session);
					if (wpoa == null)
						continue;
					wpoa.setObservation(obs);
					result.add(wpoa);
				}
				continue;
			}
				
			if (!ElementsUtil.ATTRIBUTE_ELEMENT_TAG.equals(e.getTag1()))
				continue;

			WaypointObservationAttribute wpoa = createWaypointObservationAttribute(e, a.getV(), eMap, session);
			if (wpoa == null)
				continue;
			wpoa.setObservation(obs);
			result.add(wpoa);
		}	
		return result;
	}

	private WaypointObservationAttribute createWaypointObservationAttribute(E e, String av, Map<String, E> eMap, Session session) {
		String tag0 = e.getTag0();
		if (tag0 == null || tag0.isEmpty()) {
			addWarning(MessageFormat.format(Messages.SmartImporter_Warn_AttributeTag0_Missing, e.getN()));
			return null;
		}

		Attribute attr = CyberTrackerHibernateManager.fetchByUuid(Attribute.class, tag0, session);
		if (attr == null) {
			addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoAttributeInDatamodel, e.getN(), tag0));
			return null;
		}

		WaypointObservationAttribute wpoa = new WaypointObservationAttribute();
		wpoa.setAttribute(attr);
		switch (attr.getType()) {
		case NUMERIC:
			wpoa.setNumberValue(Double.valueOf(av));
			break;
		case TEXT:
			wpoa.setStringValue(av);
			break;
		case LIST:
		{
			E eLst = eMap.get(av);
			AttributeListItem item = CyberTrackerHibernateManager.fetchByUuid(AttributeListItem.class, eLst.getTag0(), session);
			if (item == null && !CyberTrackerHibernateManager.isEmptyTag0(eLst.getTag0())) {
				addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoListAttrItemInDatamodel, e.getN(), eLst.getN(), eLst.getTag0()));
				return null;
			}
			wpoa.setAttributeListItem(item);
			break;
		}
		case TREE:
		{
			E eTr = eMap.get(av);
			AttributeTreeNode item = CyberTrackerHibernateManager.fetchByUuid(AttributeTreeNode.class, eTr.getTag0(), session);
			if (item == null && !CyberTrackerHibernateManager.isEmptyTag0(eTr.getTag0())) {
				addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoTreeAttrItemInDatamodel, e.getN(), eTr.getN(), eTr.getTag0()));
				return null;
			}
			wpoa.setAttributeTreeNode(item);
			break;
		}
		case BOOLEAN:
		{
			E eBool = eMap.get(av);
			Double value = null;
			if (ElementsUtil.BOOL_TRUE.equals(eBool.getTag0())) {
				value = 1.0;
			} else if (ElementsUtil.BOOL_FALSE.equals(eBool.getTag0())) {
				value = 0.0;
			}
			wpoa.setNumberValue(value);
			break;
		}
		}
		return wpoa;
	}
	
	private Category fetchCategory(List<A> aList, Map<String, E> eMap, Session session) {
		String v = null;
		int vIndex = -1;
		for (A a : aList) {
			E iE = eMap.get(a.getI());
			if (iE != null && ElementsUtil.CATEGORY_ELEMENT_TAG.equals(iE.getTag1())) {
				int index = Integer.valueOf(iE.getTag0());
				if (index > vIndex) {
					vIndex = index;
					v = a.getV();
				}
			}
		}
		
		if (v == null)
			return null;
		
		E e = eMap.get(v);
		if (e == null) {
			addWarning(MessageFormat.format(Messages.SmartImporter_Warn_ElementNotDefined, v));
			return null;
		}
		String tag0 = e.getTag0();
		if (tag0 == null || tag0.isEmpty()) {
			addWarning(MessageFormat.format(Messages.SmartImporter_Warn_CategoryTag0_Missing, e.getN()));
			return null;
		}
		
		Category category = CyberTrackerHibernateManager.fetchByUuid(Category.class, e.getTag0(), session);
		if (category == null)
			addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoCategoryInDatamodel, e.getN(), e.getTag0()));

		return category;
	}

	/**
	 * All leg days must create at this point.
	 * They must be created as part of initLegData(...) logic
	 */
	private PatrolLegDay findLegDay(PatrolLeg leg, S s) {
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
		wp.setObservations(new ArrayList<WaypointObservation>());
		wp.setId(pld.getWaypoints().size()+1);
		wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
		wp.setConservationArea(SmartDB.getCurrentConservationArea());
		wp.setX(0);
		wp.setY(0);
		for (A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.TIME.equals(i)) {
				Time t = Time.valueOf(a.getV());
				wp.setDateTime(SmartUtils.combineDateTime(pld.getDate(), t));
			} else if (ICyberTrackerConstants.LATITUDE.equals(i)) {
				wp.setY(Double.valueOf(a.getV()));
			} else if (ICyberTrackerConstants.LONGITUDE.equals(i)) {
				wp.setX(Double.valueOf(a.getV()));
			} else if (PatrolScreensUtil.RESULT_NEW_WAYPOINT.equals(a.getN())) {
				E e = eMap.get(a.getV());
				newWp = "true".equals(e.getTag0()); //$NON-NLS-1$
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
			
			long delta = Math.abs(wp.getDateTime().getTime() - lastWp.getWaypoint().getDateTime().getTime());
			if (delta > WARN_WP_TIME_FRAME * 60 * 1000) {
				addWarning(MessageFormat.format(Messages.SmartImporter_Warn_AddToWaypointTimeframe, lastWp.getId(), WARN_WP_TIME_FRAME));
			}
		}
		return lastWp.getWaypoint();
	}
	
	protected Date toDate(String strDate) {
		if (strDate == null)
			return null;
		DateFormat formatter = createCyberTrackerDateFormatter();
		try {
			return formatter.parse(strDate);
		} catch (ParseException e) {
			CyberTrackerPlugIn.log(e.getMessage(), e);
			return null;
		}
	}

	private void addAttachments(Waypoint wp, S s, Map<String, E> eMap) {
		String mediaFolder = null;
		try {
			mediaFolder = PdaUtil.getCTMediaFolder();
		} catch (Exception ex) {
			CyberTrackerPlugIn.log("Could not determine CyberTracker ExportMedia folder", ex); //$NON-NLS-1$
		}
		for (S.A a : s.getA()) {
			if (isPhotoI(a.getI(), eMap)) {
				if (mediaFolder == null) {
					addWarning(MessageFormat.format(Messages.SmartImporter_Warn_ExportMedia_UnknownFolder, a.getV()));
					continue;
				}
				File file = new File(mediaFolder + a.getV());
				if (!file.exists()) {
					addWarning(MessageFormat.format(Messages.SmartImporter_Warn_ExportMedia_FileNotFound, mediaFolder, a.getV()));
					continue;
				}
				if (wp.getAttachments() == null) {
					wp.setAttachments(new ArrayList<WaypointAttachment>());
				}
				WaypointAttachment attachment = new WaypointAttachment();
				attachment.setWaypoint(wp);
				attachment.setFilename(a.getV());
				attachment.setCopyFromLocation(file);
				wp.getAttachments().add(attachment);
			}
		}
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
	
	public List<String> getWarnings() {
		return warnings;
	}
	
	protected void addWarning(String warning) {
		warnings.add(warning);
	}
	
	protected void clearWarning() {
		warnings.clear();
	}
	
	private boolean isPhotoI(String i, Map<String, E> eMap) {
		if (i != null) {
			if (ICyberTrackerConstants.PHOTO.equals(i))
				return true;
			E e = eMap.get(i);
			if (e != null) {
				return e.getN().startsWith(PatrolScreensUtil.RESULT_PHOTO);
			}
		}
		return false;
	}

	public static class CoordinateZComparator implements Comparator<Coordinate> {
		@Override
		public int compare(Coordinate o1, Coordinate o2) {
			return ((Double) o1.z).compareTo((Double) o2.z);
		}
	}

}
