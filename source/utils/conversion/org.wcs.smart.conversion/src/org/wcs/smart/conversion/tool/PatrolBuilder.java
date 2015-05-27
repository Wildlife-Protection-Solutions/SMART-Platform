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
package org.wcs.smart.conversion.tool;

import java.sql.SQLException;
import java.sql.Time;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.model.ExtraAttribute;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeType;
import org.wcs.smart.conversion.model.MappedAttributeValue;
import org.wcs.smart.conversion.model.MappedCategory;
import org.wcs.smart.conversion.tag.TagA;
import org.wcs.smart.conversion.tag.TagS;
import org.wcs.smart.conversion.tag.TagT;
import org.wcs.smart.conversion.util.CoordinateUtil;
import org.wcs.smart.conversion.util.Ct2AttributeTypeUtil;
import org.wcs.smart.conversion.util.SmartUtil;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.patrol.xml.model.LabelType;
import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolMemberType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.TrackType;
import org.wcs.smart.patrol.xml.model.WaypointObservationAttributeType;
import org.wcs.smart.patrol.xml.model.WaypointObservationType;
import org.wcs.smart.patrol.xml.model.WaypointType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKBWriter;

public class PatrolBuilder extends AbstractBuilder {
	
//	private MatchSession session;
//	private Ct2SmartLookup lookup;
//	private DataModelLookup dmLookup;
//	private TeamMembersParser membersParser = new TeamMembersParser();
//	private ElementsLookup elLookup;

	public PatrolBuilder(MatchSession session, DataModelLookup dmLookup) throws SQLException {
		super(session, dmLookup);
//		this.session = session;
//		this.dmLookup = dmLookup;
//		lookup = new Ct2SmartLookup(session.getSmartMapping());
//		elLookup = new ElementsLookup(session.getConnection());
	}

	public PatrolType createPatrol(List<TagS> sList, List<TagT> tList, String id) throws DatatypeConfigurationException, ParseException {
		
		LineStringBuilder lineBuilder = new LineStringBuilder();
		LineString line = lineBuilder.createLineString(tList);
		
		TrackType track = null;
		if (line != null) {
			track = new TrackType();
			track.setDistance(SmartUtil.distanceInMeters(line) / 1000.0);
			WKBWriter writer = new WKBWriter(3);
			track.setGeom(SmartUtil.encodeHex(writer.write(line)));
		}
		
		PatrolType patrol = new PatrolType();
		patrol.setId(id);
		patrol.setIsArmed(false);
		patrol.setPatrolType("GROUND");
		
		PatrolLegType leg = new PatrolLegType();
		patrol.getLegs().add(leg);
		leg.setId(String.valueOf(patrol.getLegs().size()));
		LabelType transportType = new LabelType();
		transportType.setLanguageCode(getLanguageCode());
		transportType.setValue("Foot");
		leg.setTransportType(transportType);
		
		Set<String> members = new HashSet<String>();
		LabelType mandate = null;
		String comment = ""; //$NON-NLS-1$
		
		int wpId = 0;
		for (TagS s : sList) {
			WaypointType wp = new WaypointType();
			wp.setId(wpId);
			wpId++;

			MappedCategory defaultCategory = getDefaultCategory(s);
			boolean ignoreCategory = defaultCategory == null || Boolean.TRUE.equals(defaultCategory.isIgnore());
			WaypointObservationType defObs = new WaypointObservationType();
			if (!ignoreCategory) {
				defObs.setCategoryKey(defaultCategory.getCategoryKey());
				for (ExtraAttribute ea : defaultCategory.getExtraAttribute()) {
					WaypointObservationAttributeType obsAttr = ea2woa(ea);
					if (obsAttr != null) {
						defObs.getAttributes().add(obsAttr);
					}
				}
				wp.getObservations().add(defObs);
			}

			XMLGregorianCalendar xmlDate = null;
			XMLGregorianCalendar xmlTime = null;
			
			for (TagA a : s) {
				MappedAttribute cta = getLookup().findAttribute(a.getI());
				if (cta == null || cta.getType() == null) {
					//attribute is unmapped; treat the same as ignore
					System.out.println("Warning: No mapping for attribute: " + a.getN());
					continue;
				}
				WaypointObservationType obs;
				//determine if we use default observation or special
				if (cta.getCategoryKey() == null) {
					obs = defObs;
				} else if (!a.isEmptyV()) {
					//special cases for not default category
					//proceed only if the value is not empty
					obs = new WaypointObservationType();
					wp.getObservations().add(obs);
					obs.setCategoryKey(cta.getCategoryKey());
				} else {
					continue;
				}

				switch (cta.getType()) {
					case BOOL: {
						WaypointObservationAttributeType obsAttr = new WaypointObservationAttributeType();
						obs.getAttributes().add(obsAttr);
						obsAttr.setAttributeKey(cta.getMapTo());
						obsAttr.setBValue("True".equals(a.getV())); //$NON-NLS-1$
						break;
					}
					case NUMERIC: {
						try {
							WaypointObservationAttributeType obsAttr = new WaypointObservationAttributeType();
							obsAttr.setAttributeKey(cta.getMapTo());
							if (!a.isEmptyV()) {
								obsAttr.setDValue(Double.valueOf(a.getV()));
								obs.getAttributes().add(obsAttr);
							}
						} catch (NumberFormatException e) {
							System.err.println(MessageFormat.format("Failed to convert to double. Attribute: {0} value: {1} mission_id: {2}. Attribute skipped.", a.getN(), a.getV(), patrol.getId()));
							//throw e;
						}
						break;
					}
					case TEXT: {
						WaypointObservationAttributeType obsAttr = new WaypointObservationAttributeType();
						obs.getAttributes().add(obsAttr);
						obsAttr.setAttributeKey(cta.getMapTo());
						obsAttr.setSValue(a.getV());
						break;
					}
					case REF_BOOL:
					case REF: {
						WaypointObservationAttributeType obsAttr = new WaypointObservationAttributeType();
						obsAttr.setAttributeKey(cta.getMapTo());
						for (MappedAttributeValue val : cta.getMappedAttributeValue()) {
							if (a.getV().equals(val.getI()) && !Boolean.TRUE.equals(val.isIgnore())) {
								if (MappedAttributeType.REF.equals(cta.getType())) {
									obsAttr.setItemKey(val.getMapTo());
								} else {
									//REF_BOOL case
									obsAttr.setBValue("True".equals(val.getMapTo())); //$NON-NLS-1$
								}
								obs.getAttributes().add(obsAttr);
								break;
							}
						}
						break;
					}
					case WP_DATE: {
						Date wpDate = getDateTimeParser().parseDate(a.getV());
						xmlDate = SmartUtil.toXmlDate(wpDate);
						break;
					}
					case WP_TIME: {
						Time wpTime = getDateTimeParser().parseTime(a.getV());
						xmlTime = SmartUtil.toXmlTime(wpTime);
						wp.setTime(xmlTime);
						break;
					}
					case WP_LON:
						if (!a.isEmptyV()) {
							wp.setX(Double.valueOf(a.getV()));
						} else {
							System.out.println(MessageFormat.format("WARN: Empty logitude in patrol {0} waypint {1}", patrol.getId(), wp.getId()));
						}
						break;
					case WP_LAT:
						if (!a.isEmptyV()) {
							wp.setY(Double.valueOf(a.getV()));
						} else {
							System.out.println(MessageFormat.format("WARN: Empty latitude in patrol {0} waypint {1}", patrol.getId(), wp.getId()));
						}
						break;
					case META_MANDATE: {
						String v = a.getV();
						for (MappedAttributeValue val : cta.getMappedAttributeValue()) {
							if (v.equals(val.getI())) {
								v = Ct2AttributeTypeUtil.getN(val);
								break;
							}
						}
						if (mandate == null) {
							mandate = new LabelType();
							mandate.setLanguageCode(getLanguageCode());
							mandate.setValue(v);
						} else if (!v.equals(mandate.getValue())) {
							System.out.println("WARN: Two different mandates in one patrol (" + patrol.getId() + "): " + mandate.getValue() + " and " + v);
						}
						break;
					}
					case META_MEMBERS:
						members.addAll(getMembersParser().parseMembers(a.getV()));
						break;
					case META_COMMENT:
						if (ignoreCategory && obs == defObs) {
							break;
						}
						if (!comment.isEmpty()) {
							comment += "\n"; //$NON-NLS-1$
						}
						String v = a.getV();
//						if (CsvMatchFileBuilder.isCtId(v)) {
//							String vEl = elLookup.getN(v);
//							if (vEl != null) {
//								v = vEl;
//							}
//						}
						comment += "Waypoint ID=" + String.valueOf(wp.getId()) + ": " + a.getN() + " = " + v;
						break;
					case WP_COMMENT:
						if (!a.isEmptyV()) {
							wp.setComment(wp.getComment() != null ? wp.getComment() + "\n" + a.getV() : a.getV()); //$NON-NLS-1$
						}
						break;
					case TRANSECT_ID:
					case TRANSECT_START_LAT:
					case TRANSECT_START_LON:
					case TRANSECT_END_LAT:
					case TRANSECT_END_LON:
						System.out.println("WARN: Transect mapping presents in patrol generation, patrols do not have transects");
						break;
					case IGNORE:
					case META_OBJECT_ID:
					case CATEGORY:
						break;
				}
				
				if (Ct2AttributeTypeUtil.canMap(cta.getType()) && !a.isEmptyV()) {
					for (ExtraAttribute ea : cta.getExtraAttribute()) {
						WaypointObservationAttributeType obsAttr = ea2woa(ea);
						if (obsAttr != null) {
							obs.getAttributes().add(obsAttr);
						}
					}
				}
			}
			
			//validate that we have unique attributes for each observation
			for (WaypointObservationType obs : wp.getObservations()) {
				Map<String, WaypointObservationAttributeType> key2Attr = new HashMap<String, WaypointObservationAttributeType>();
				List<WaypointObservationAttributeType> duplicates = new ArrayList<WaypointObservationAttributeType>();
				for (WaypointObservationAttributeType attr : obs.getAttributes()) {
					WaypointObservationAttributeType prevAttr = key2Attr.get(attr.getAttributeKey());
					if (prevAttr == null) {
						key2Attr.put(attr.getAttributeKey(), attr);
					} else {
						//attr duplicates prevAttr
						if (isSameValue(attr, prevAttr)) {
							System.out.println(MessageFormat.format("INFO: Duplicate attributes in patrol {0} waypoint {1} with key ''{2}''. Values (item, double, string): {3}, {4}, {5}. Duplicate was dropped out.", patrol.getId(), wp.getId(), attr.getAttributeKey(), attr.getItemKey(), attr.getDValue(), attr.getSValue()));
							duplicates.add(attr);
						} else if (isDetailedKeyValue(attr, prevAttr)) {
							System.out.println(MessageFormat.format("INFO: Similar attributes in patrol {0} waypoint {1} with key ''{2}''. Values1 (item, double, string): {3}, {4}, {5}. Values2 (item, double, string): {6}, {7}, {8}. Only more detailed value will be used.", patrol.getId(), wp.getId(), attr.getAttributeKey(), prevAttr.getItemKey(), prevAttr.getDValue(), prevAttr.getSValue(), attr.getItemKey(), attr.getDValue(), attr.getSValue()));
							duplicates.add(attr);
							if (attr.getItemKey().length() > prevAttr.getItemKey().length()) {
								prevAttr.setItemKey(attr.getItemKey());
							}
						} else {
							System.out.println(MessageFormat.format("WARN: Two diffent attributes in patrol {0} waypoint {1} with key ''{2}''. Values1 (item, double, string): {3}, {4}, {5}. Values2 (item, double, string): {6}, {7}, {8}. Warning will appear on import", patrol.getId(), wp.getId(), attr.getAttributeKey(), prevAttr.getItemKey(), prevAttr.getDValue(), prevAttr.getSValue(), attr.getItemKey(), attr.getDValue(), attr.getSValue()));
						}
						
					}
				}
				obs.getAttributes().removeAll(duplicates);
			}
			
			if (xmlDate != null) {
				PatrolLegDayType legDay = getLegDay(leg, xmlDate);
				legDay.getWaypoints().add(wp);
				addTimeRecord(legDay, xmlTime);
			} else {
				System.out.println(MessageFormat.format("WARN: No date is defined for one of the rows for patrol {0}. Data will be skipped.", patrol.getId()));
			}
			
		}

		//validate/interpolate waypoint coordinates
		for (PatrolLegDayType legDay : leg.getDays()) {
			List<WaypointType> waypoints = legDay.getWaypoints();
			for (int i = 0; i < waypoints.size(); i++) {
				WaypointType wp = waypoints.get(i);

				if (wp.getX() != null && wp.getY() != null)
					continue;
				
				if (track != null) {
					System.out.println(MessageFormat.format("INFO: Coordinate problem in patrol {0} waypoint {1}. Intepolating coordinates from track.", patrol.getId(), wp.getId()));
					Coordinate c = CoordinateUtil.interpolate(line, SmartUtil.combine(legDay.getDate(), wp.getTime()));
					if (c != null) {
						if (wp.getX() == null)
							wp.setX(c.x);
						if (wp.getY() == null)
							wp.setY(c.y);
					}
				}

				if (wp.getX() == null || wp.getY() == null) {
					if (i > 0) {
						System.out.println(MessageFormat.format("INFO: Coordinate problem in patrol {0} waypoint {1}. Using previous waypoint coordinates.", patrol.getId(), wp.getId()));
						WaypointType prevWp = waypoints.get(i-1);
						if (wp.getX() == null)
							wp.setX(prevWp.getX());
						if (wp.getY() == null)
							wp.setY(prevWp.getY());
					} else {
						System.out.println(MessageFormat.format("INFO: Coordinate problem in patrol {0} waypoint {1}. Checking if there are any waypoints with coordinates in this patrol.", patrol.getId(), wp.getId()));
						for (int j = i+1; j < waypoints.size(); j++) {
							WaypointType nextWp = waypoints.get(j);
							if (nextWp.getX() != null && nextWp.getY() != null) {
								if (wp.getX() == null)
									wp.setX(nextWp.getX());
								if (wp.getY() == null)
									wp.setY(nextWp.getY());
							}
						}
						if (wp.getX() == null || wp.getY() == null) {
							System.err.println(MessageFormat.format("ERROR: Coordinate problem in patrol {0} waypoint {1}. Importing this patrol will cause error in SMART.", patrol.getId(), wp.getId()));
							patrol.setId("[ERROR-xy]"+patrol.getId());
							break;
						}			
					}
				}
			}
		}
		
		
		for (String fullName : members) {
			PatrolMemberType member = toMember(fullName);
			leg.getMembers().add(member);
		}
		if (!leg.getMembers().isEmpty()) {
			leg.getMembers().get(0).setIsLeader(true);
		}

		patrol.setMandate(mandate);
		patrol.setComment(comment);
		
		Collections.sort(leg.getDays(), new Comparator<PatrolLegDayType>() {
			@Override
			public int compare(PatrolLegDayType d1, PatrolLegDayType d2) {
				return d1.getDate().compare(d2.getDate());
			}
		});
		
		XMLGregorianCalendar xmlStartTime = leg.getDays().get(0).getDate();
		XMLGregorianCalendar xmlEndTime = leg.getDays().get(leg.getDays().size()-1).getDate();

		patrol.setStartDate(xmlStartTime);
		patrol.setEndDate(xmlEndTime);

		leg.setStartDate(xmlStartTime);
		leg.setEndDate(xmlEndTime);

		return patrol;
	}

	private void addTimeRecord(PatrolLegDayType legDay, XMLGregorianCalendar xmlTime) {
		if (xmlTime == null) {
			return;
		}
		if (legDay.getStartTime() != null) {
			if (xmlTime.compare(legDay.getStartTime()) == DatatypeConstants.LESSER)
				legDay.setStartTime(xmlTime);
		} else {
			legDay.setStartTime(xmlTime);
		}

		if (legDay.getEndTime() != null) {
			if (xmlTime.compare(legDay.getEndTime()) == DatatypeConstants.GREATER)
				legDay.setEndTime(xmlTime);
		} else {
			legDay.setEndTime(xmlTime);
		}
	}

	private PatrolLegDayType getLegDay(PatrolLegType leg, XMLGregorianCalendar xmlDate) {
		for (PatrolLegDayType legDay : leg.getDays()) {
			if (xmlDate.compare(legDay.getDate()) == DatatypeConstants.EQUAL) {
				return legDay;
			}
		}
		//leg day for this date do not exist so wee need to create one
		PatrolLegDayType legDay = new PatrolLegDayType();
		leg.getDays().add(legDay);
		legDay.setRestMinutes(0.0);
		legDay.setDate(xmlDate);
		return legDay;
	}

	private boolean isSameValue(WaypointObservationAttributeType a1, WaypointObservationAttributeType a2) {
		return isSame(a1.getItemKey(), a2.getItemKey()) && isSame(a1.getDValue(), a2.getDValue()) && isSame(a1.getSValue(), a2.getSValue());
	}

	private boolean isDetailedKeyValue(WaypointObservationAttributeType a1, WaypointObservationAttributeType a2) {
		if (isSame(a1.getDValue(), a2.getDValue()) && isSame(a1.getSValue(), a2.getSValue())) {
			String k1 = a1.getItemKey();
			String k2 = a2.getItemKey();
			if (k1 != null && k2 != null) {
				//<itemKey>biologicalresourceuse.huntingcollectingterrestrialanimals.trapping</itemKey>
				//<itemKey>biologicalresourceuse.huntingcollectingterrestrialanimals</itemKey>
				//we need to use more detailed key and drop the other one
				//valid for trees only
				String[] parts1 = k1.split("\\."); //$NON-NLS-1$
				String[] parts2 = k2.split("\\."); //$NON-NLS-1$
				int size = Math.min(parts1.length, parts2.length);
				for (int i = 0; i < size; i++) {
					if (!parts1[i].equals(parts2[i])) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public static final PatrolMemberType toMember(String fullName) {
		PatrolMemberType member = new PatrolMemberType();
		member.setIsLeader(false);
		member.setIsPilot(false);
		String[] parts = fullName.split(" "); //$NON-NLS-1$
		switch (parts.length) {
		case 2:
			member.setFamilyName(parts[1]);
			member.setGivenName(parts[0]);
			break;
		case 1:
			member.setFamilyName(parts[0]);
			member.setGivenName(parts[0]);
			break;

		default:
			break;
		}
		return member;
	}
	
	private WaypointObservationAttributeType ea2woa(ExtraAttribute ea) {
		WaypointObservationAttributeType obsAttr = new WaypointObservationAttributeType();
		obsAttr.setAttributeKey(ea.getAttributeKey());
		AttributeType dmAttr = getDmLookup().getAttribute(ea.getAttributeKey());
		if (dmAttr == null) {
			System.err.println("ERROR: Extra attribute was not added. No attribute found in datamodel with key: " + ea.getAttributeKey());
			return null;
		}
		String type = dmAttr.getType();
		switch (dmAttr.getType()) {
			case "LIST": //$NON-NLS-1$
			case "TREE": //$NON-NLS-1$	
				obsAttr.setItemKey(ea.getValueKey());
				break;
	
			case "TEXT": //$NON-NLS-1$
				obsAttr.setSValue(ea.getValueKey());
				break;
	
			case "NUMERIC": //$NON-NLS-1$
				try {
					obsAttr.setDValue(Double.valueOf(ea.getValueKey()));
				} catch (NumberFormatException e) {
					System.err.println("ERROR: Failed to convert extra attribute value to double. DM key: " + ea.getAttributeKey());
					return null;
				}
				break;
	
			case "BOOLEAN": //$NON-NLS-1$
				obsAttr.setBValue("True".equals(ea.getValueKey()));
				break;
	
			default:
				System.err.println("ERROR: Unsupported type for extra attribute:" + type);
				return null;
		}

		return obsAttr;
	}
	
	public void buildTracksFromWp(PatrolType p) throws ParseException {
		for (PatrolLegType leg : p.getLegs()) {
			for (PatrolLegDayType legDay : leg.getDays()) {
				List<Coordinate> coordinates = new ArrayList<Coordinate>();
				XMLGregorianCalendar date = legDay.getDate();
				XMLGregorianCalendar time;
				double x, y;
				for (WaypointType wp : legDay.getWaypoints()) {
					time = wp.getTime();
					y = Double.valueOf(wp.getY());
					x = Double.valueOf(wp.getX());
					coordinates.add(new Coordinate(x, y, SmartUtil.combine(date, time).getTime()));
				}
				LineString line = CoordinateUtil.buildLineString(coordinates);
				
				TrackType track = null;
				if (line != null) {
					track = new TrackType();
					track.setDistance(SmartUtil.distanceInMeters(line) / 1000.0);
					WKBWriter writer = new WKBWriter(3);
					track.setGeom(SmartUtil.encodeHex(writer.write(line)));
				}
				legDay.setTrack(track);
			}
		}
	}

	public void removeEmptyWayoints(PatrolType p) throws ParseException {
		for (PatrolLegType leg : p.getLegs()) {
			for (PatrolLegDayType legDay : leg.getDays()) {
				for (Iterator<WaypointType> i = legDay.getWaypoints().iterator(); i.hasNext();) {
					WaypointType wp = i.next();
					if (wp.getObservations().isEmpty()) {
						System.out.println(MessageFormat.format("INFO: Patrol ''{0}'' waypoint ''{1}'' was removed from generated xml because no observation data recorded for this waypoint.", p.getId(), wp.getId()));
						i.remove();
					}
				}
			}
		}
	}
	
}
