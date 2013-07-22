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

import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S.A;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * Common smart importing logic.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class SmartImporter {

	private List<String> warnings = new ArrayList<String>();
	
	protected void initLegData(PatrolLeg leg, CyberTrackerPatrol ctPatrol) {
		leg.setType(ctPatrol.getPatrolTransportType());
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
	}
	
	protected void addObservations(PatrolLeg leg, S s, Map<String, E> eMap, Session session) {
		PatrolLegDay legDay = findOrAddLegDay(leg, s);
		if (legDay == null)
			return;
		
		Waypoint wp = findOrAddWaypoint(legDay, s);
		addObservations(wp, s, eMap, session);
	}

	protected void addObservations(Waypoint wp, S s, Map<String, E> eMap, Session session) {
		List<A> aList = fetchObservationRecords(s, eMap);
		if (aList.isEmpty())
			return;
		Category category = fetchCategory(aList, eMap, session);
		if (category == null)
			return;
		WaypointObservation obs = new WaypointObservation();
		obs.setWaypoint(wp);
		obs.setCategory(category);
		obs.setAttributes(fetchAttributes(obs, aList, eMap, session));
		
		wp.getObservations().add(obs);
	}

	private List<A> fetchObservationRecords(S s, Map<String, E> eMap) {
		List<A> result = new ArrayList<A>();
		//must have at least one attribute
		for (A a : s.getA()) {
			E e = eMap.get(a.getI());
			if (ElementsUtil.ATTRIBUTE_ELEMENT_TAG.equals(e.getTag1()) || ElementsUtil.CATEGORY_ELEMENT_TAG.equals(e.getTag1()))
				result.add(a);
		}
		return result;
	}

	private List<WaypointObservationAttribute> fetchAttributes(WaypointObservation obs, List<A> aList, Map<String, E> eMap, Session session) {
		List<WaypointObservationAttribute> result = new ArrayList<WaypointObservationAttribute>();
		for (A a : aList) {
			if (a.getV() == null)
				continue;
			
			E e = eMap.get(a.getI());
			if (!ElementsUtil.ATTRIBUTE_ELEMENT_TAG.equals(e.getTag1()))
				continue;
			
			String tag0 = e.getTag0();
			if (tag0 == null || tag0.isEmpty()) {
				addWarning(MessageFormat.format(Messages.SmartImporter_Warn_AttributeTag0_Missing, e.getN()));
				continue;
			}

			Attribute attr = CyberTrackerHibernateManager.fetchByUuid(Attribute.class, e.getTag0(), session);
			if (attr == null) {
				addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoAttributeInDatamodel, e.getN(), e.getTag0()));
				continue;
			}

			WaypointObservationAttribute wpoa = new WaypointObservationAttribute();
			wpoa.setObservation(obs);
			wpoa.setAttribute(attr);
			switch (attr.getType()) {
			case NUMERIC:
				wpoa.setNumberValue(Double.valueOf(a.getV()));
				break;
			case TEXT:
				wpoa.setStringValue(a.getV());
				break;
			case LIST:
			{
				E eLst = eMap.get(a.getV());
				AttributeListItem item = CyberTrackerHibernateManager.fetchByUuid(AttributeListItem.class, eLst.getTag0(), session);
				if (item == null) {
					addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoListAttrItemInDatamodel, e.getN(), eLst.getN(), eLst.getTag0()));
					continue;
				}
				wpoa.setAttributeListItem(item);
				break;
			}
			case TREE:
			{
				E eTr = eMap.get(a.getV());
				AttributeTreeNode item = CyberTrackerHibernateManager.fetchByUuid(AttributeTreeNode.class, eTr.getTag0(), session);
				if (item == null) {
					addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoTreeAttrItemInDatamodel, e.getN(), eTr.getN(), eTr.getTag0()));
					continue;
				}
				wpoa.setAttributeTreeNode(item);
				break;
			}
			case BOOLEAN:
			{
				E eBool = eMap.get(a.getV());
				Double value = null;
				if (ElementsUtil.BOOL_TRUE.equals(eBool.getTag0())) {
					value = 1.0;
				} else if (ElementsUtil.BOOL_FALSE.equals(eBool.getTag0())) {
					value = 0.0;
				}
				wpoa.setNumberValue(value);
				break;
			}
			default:
				break;
			}
			result.add(wpoa);
		}	
		return result;
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

	private PatrolLegDay findOrAddLegDay(PatrolLeg leg, S s) {
		//TODO: update time for PatrolLegDay
		Date date = null;
		for (A a : s.getA()) {
			if (ICyberTrackerConstants.DATE.equals(a.getI())) {
				date = toDate(a.getV());
				break;
			}
		}
		
		if (date == null)
			return null;

		for (PatrolLegDay pld : leg.getPatrolLegDays()) {
			if (pld.getDate().equals(date))
				return pld;
		}
		
		PatrolLegDay pld = new PatrolLegDay();
		pld.setPatrolLeg(leg);
		pld.setDate(date);
		
		leg.getPatrolLegDays().add(pld);
		return pld;
	}

	protected Waypoint findOrAddWaypoint(PatrolLegDay pld, S s) {
		if (pld.getWaypoints() == null)
			pld.setWaypoints(new ArrayList<Waypoint>());
		
		//TODO: when to add multiple observations to the same waypoint? x&y are the same?
		Waypoint wp = new Waypoint();
		wp.setObservations(new ArrayList<WaypointObservation>());
		wp.setPatrolLegDay(pld);
		wp.setId(pld.getWaypoints().size()+1);
		wp.setX(0);
		wp.setY(0);
		for (A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.TIME.equals(i)) {
				wp.setTime(Time.valueOf(a.getV()));
			} else if (ICyberTrackerConstants.LATITUDE.equals(i)) {
				wp.setY(Double.valueOf(a.getV()));
			} else if (ICyberTrackerConstants.LONGITUDE.equals(i)) {
				wp.setX(Double.valueOf(a.getV()));
			}
		}
		
		pld.getWaypoints().add(wp);
		return wp;
	}
	
	protected Date toDate(String strDate) {
		if (strDate == null)
			return null;
		DateFormat formatter = new SimpleDateFormat(ICyberTrackerConstants.CT_DATE_FORMAT);
		try {
			return formatter.parse(strDate);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected void displayWarnings() {
		if (getWarnings() != null && getWarnings().size() > 0) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					WarningDialog wdialog = new WarningDialog(Display.getDefault().getActiveShell(), Messages.SmartImporter_WarnDialog_Title, Messages.SmartImporter_WarnDialog_Message, getWarnings());
					wdialog.open();
				}
			});
		}
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

}
