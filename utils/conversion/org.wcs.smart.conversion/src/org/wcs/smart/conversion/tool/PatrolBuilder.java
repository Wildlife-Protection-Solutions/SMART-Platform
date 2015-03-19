package org.wcs.smart.conversion.tool;

import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.geotools.referencing.GeodeticCalculator;
import org.wcs.smart.conversion.lookup.Ct2SmartLookup;
import org.wcs.smart.conversion.lookup.Ct2SmartLookup.Ct2AttributeValuePair;
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

public class PatrolBuilder {
	
	private static final String LANGUAGE_CODE = "en"; //$NON-NLS-1$

	private static final DateFormat df_dot = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
	private static final DateFormat df = new SimpleDateFormat("MM/dd/yyyy"); //$NON-NLS-1$
	
//	private MatchSession session;
	private Ct2SmartLookup lookup;
	private DataModelLookup dmLookup;
	private TeamMembersParser membersParser = new TeamMembersParser();
//	private ElementsLookup elLookup;

	public PatrolBuilder(MatchSession session, DataModelLookup dmLookup) throws SQLException {
//		this.session = session;
		this.dmLookup = dmLookup;
		lookup = new Ct2SmartLookup(session.getSmartMapping());
//		elLookup = new ElementsLookup(session.getConnection());
	}

	public PatrolType createPatrol(List<TagS> sList, List<TagT> tList, String id) throws DatatypeConfigurationException, ParseException {
			
		LineString line = createTrack(tList);
		
		TrackType track = null;
		if (line != null) {
			track = new TrackType();
			track.setDistance(distanceInMeters(line) / 1000.0);
			WKBWriter writer = new WKBWriter(3);
			track.setGeom(encodeHex(writer.write(line)));
		}
		
		PatrolType patrol = new PatrolType();
		patrol.setId(id);
		patrol.setIsArmed(false);
		patrol.setPatrolType("GROUND");
		
		PatrolLegType leg = new PatrolLegType();
		patrol.getLegs().add(leg);
		leg.setId(String.valueOf(patrol.getLegs().size()));
		LabelType transportType = new LabelType();
		transportType.setLanguageCode(LANGUAGE_CODE);
		transportType.setValue("Foot");
		leg.setTransportType(transportType);
//		PatrolMemberType member = new PatrolMemberType();
//		leg.getMembers().add(member);
//		member.setIsLeader(true);
//		member.setIsPilot(false);
//		member.setEmployeeId("SMART5");
//		member.setFamilyName("Gordon");
//		member.setGivenName("Rawlston");
		
		PatrolLegDayType legDay = new PatrolLegDayType();
		leg.getDays().add(legDay);
		legDay.setTrack(track);
		legDay.setRestMinutes(0.0);

		XMLGregorianCalendar xmlDate = null;
		XMLGregorianCalendar xmlStartTime = null;
		XMLGregorianCalendar xmlEndTime = null;
		Date wpDate = null;
		Time wpTime = null;
		
		Set<String> members = new HashSet<String>();
		LabelType mandate = null;
		String comment = ""; //$NON-NLS-1$
		
				
		for (TagS s : sList) {
			WaypointType wp = new WaypointType();
			legDay.getWaypoints().add(wp);
			wp.setId(legDay.getWaypoints().size());

			MappedCategory defaultCategory = getDefaultCategory(s);
			boolean ignoreCategory = defaultCategory != null && Boolean.TRUE.equals(defaultCategory.isIgnore());
			String defaultCategoryKey = defaultCategory != null ? defaultCategory.getCategoryKey() : null;
			WaypointObservationType defObs = new WaypointObservationType();
			defObs.setCategoryKey(defaultCategoryKey);
			for (ExtraAttribute ea : defaultCategory.getExtraAttribute()) {
				WaypointObservationAttributeType obsAttr = ea2woa(ea);
				if (obsAttr != null) {
					defObs.getAttributes().add(obsAttr);
				}
			}
			if (!ignoreCategory) {
				wp.getObservations().add(defObs);
			}

			for (TagA a : s) {
				MappedAttribute cta = lookup.findAttribute(a.getI());
				if (cta == null || cta.getType() == null) {
					//attribute is unmapped; treat the same as ignore
					System.out.println("Warning: No mapping for attribute: " + a.getN());
					continue;
				}
				WaypointObservationType obs;
				//determine if we use default observation or special
				if (cta.getCategoryKey() == null) {
					obs = defObs;
				} else {
					//special cases for not default category
					obs = new WaypointObservationType();
					wp.getObservations().add(obs);
					obs.setCategoryKey(cta.getCategoryKey());
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
							obsAttr.setDValue(Double.valueOf(a.getV()));
							obs.getAttributes().add(obsAttr);
						} catch (NumberFormatException e) {
							System.err.println("Failed to convert to double. Attribute: " + a.getN() + " value: '" + a.getV() + "'. Attribute skipped.");
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
					case META_DATE:
						wpDate = parseDate(a.getV());
						xmlDate = toXmlDate(wpDate);
						break;
					case META_TIME: {
						wpTime = Time.valueOf(a.getV());
						XMLGregorianCalendar xmlTime = toXmlTime(wpTime);
						wp.setTime(xmlTime);
						if (xmlStartTime != null) {
							if (xmlTime.compare(xmlStartTime) == DatatypeConstants.LESSER)
								xmlStartTime = xmlTime;
						} else {
							xmlStartTime = xmlTime;
						}
						
						if (xmlEndTime != null) {
							if (xmlTime.compare(xmlEndTime) == DatatypeConstants.GREATER)
								xmlEndTime = xmlTime;
						} else {
							xmlEndTime = xmlTime;
						}
						break;
					}
					case META_LON:
						wp.setX(Double.valueOf(a.getV()));
						break;
					case META_LAT:
						wp.setY(Double.valueOf(a.getV()));
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
							mandate.setLanguageCode(LANGUAGE_CODE);
							mandate.setValue(v);
						} else if (!v.equals(mandate.getValue())) {
							System.out.println("WARN: Two different mandates in one patrol (" + patrol.getId() + "): " + mandate.getValue() + " and " + v);
						}
						break;
					}
					case META_MEMBERS:
						members.addAll(membersParser.parseMembers(a.getV()));
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
					case IGNORE:
					case META_PATROL:
						break;
				}
				
				if (Ct2AttributeTypeUtil.canMap(cta.getType())) {
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
		}

		//validate/interpolate waypoint coordinates
		List<WaypointType> waypoints = legDay.getWaypoints();
		for (int i = 0; i < waypoints.size(); i++) {
			WaypointType wp = waypoints.get(i);

			if (wp.getX() != null && wp.getY() != null)
				continue;
			
			if (track != null) {
				System.out.println(MessageFormat.format("INFO: Coordinate problem in patrol {0} waypoint {1}. Intepolating coordinates from track.", patrol.getId(), wp.getId()));
				Coordinate c = CoordinateUtil.interpolate(line, combine(wpDate, wpTime));
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
		
		for (String fullName : members) {
			PatrolMemberType member = toMember(fullName);
			leg.getMembers().add(member);
		}
		if (!leg.getMembers().isEmpty()) {
			leg.getMembers().get(0).setIsLeader(true);
		}

		patrol.setMandate(mandate);

		patrol.setStartDate(xmlDate);
		patrol.setEndDate(xmlDate);
		patrol.setComment(comment);

		leg.setStartDate(xmlDate);
		leg.setEndDate(xmlDate);

		legDay.setDate(xmlDate);
		legDay.setStartTime(xmlStartTime);
		legDay.setEndTime(xmlEndTime);
		
		return patrol;
	}

	private boolean isSameValue(WaypointObservationAttributeType a1, WaypointObservationAttributeType a2) {
		return isSame(a1.getItemKey(), a2.getItemKey()) && isSame(a1.getDValue(), a2.getDValue()) && isSame(a1.getSValue(), a2.getSValue());
	}

	private boolean isSame(Object o1, Object o2) {
		if (o1 == null) 
			return o2 == null;
		return o1.equals(o2);
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
		AttributeType dmAttr = dmLookup.getAttribute(ea.getAttributeKey());
		if (dmAttr == null) {
			System.err.println("ERROR: Extra attribute was not added. No attribute found in datamodel with key: " + ea.getAttributeKey());
			return null;
		}
		String type = dmAttr.getType();
		if ("LIST".equals(type) || "TREE".equals(type)) { //$NON-NLS-1$ //$NON-NLS-2$
			obsAttr.setItemKey(ea.getValueKey());

		} else if ("TEXT".equals(type)) { //$NON-NLS-1$
			obsAttr.setSValue(ea.getValueKey());

		} else if ("NUMERIC".equals(type)) { //$NON-NLS-1$
			try {
				obsAttr.setDValue(Double.valueOf(ea.getValueKey()));
			} catch (NumberFormatException e) {
				System.err.println("ERROR: Failed to convert extra attribute value to double. DM key: " + ea.getAttributeKey());
				return null;
			}
			
		} else if ("BOOLEAN".equals(type)) { //$NON-NLS-1$
			obsAttr.setBValue("True".equals(ea.getValueKey()));
		
		} else {
			System.err.println("ERROR: Unsupported type for extra attribute:" + type);
			return null;
		}
		return obsAttr;
	}
	
	private LineString createTrack(List<TagT> tList) throws ParseException {
		if (tList == null) {
			return null;
		}
		
		List<Coordinate> coordinates = new ArrayList<Coordinate>();
		Date date;
		Time time;
		double x, y;
		for (TagT t : tList) {
			date = parseDate(t.getDate());
			time = Time.valueOf(t.getTime());
			y = Double.valueOf(t.getLatitude());
			x = Double.valueOf(t.getLongitude());
			coordinates.add(new Coordinate(x, y, combine(date, time).getTime()));
		}
		return CoordinateUtil.buildLineString(coordinates);
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
					coordinates.add(new Coordinate(x, y, combine(date, time).getTime()));
				}
				LineString line = CoordinateUtil.buildLineString(coordinates);
				
				TrackType track = null;
				if (line != null) {
					track = new TrackType();
					track.setDistance(distanceInMeters(line) / 1000.0);
					WKBWriter writer = new WKBWriter(3);
					track.setGeom(encodeHex(writer.write(line)));
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
	
	private MappedCategory getDefaultCategory(TagS s) {
		List<Ct2AttributeValuePair> data = new ArrayList<Ct2AttributeValuePair>();
		for (TagA a : s) {
			MappedAttribute cta = lookup.findAttribute(a.getI());
			if (cta != null && MappedAttributeType.CATEGORY.equals(cta.getType())) {
				Ct2AttributeValuePair pair = new Ct2AttributeValuePair();
				pair.attribute = cta;
				pair.value = a.getV();
				data.add(pair);
			}
		}
		MappedCategory c = lookup.findCategory(data);
		if (c == null) {
			String info = "";
			for (Ct2AttributeValuePair pair : data) {
				info += "\nattribute: " + Ct2AttributeTypeUtil.getN(pair.attribute) + " " + pair.attribute.getI();
				MappedAttribute value = lookup.findAttribute(pair.value);
				info += "  value: ";
				info += value != null ? Ct2AttributeTypeUtil.getN(value) : "null";
				info += " " + pair.value;
			}
			System.err.println("ERROR: No category defined for following items: " + info);
		}
		return c;
	}

	private Date parseDate(String dateStr) throws ParseException {
		if (dateStr == null || dateStr.isEmpty()) {
			return null;
		} else if (dateStr.matches("[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{4}")) { //$NON-NLS-1$
			return df_dot.parse(dateStr);
		} else if (dateStr.matches("[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}")) { //$NON-NLS-1$
			return df.parse(dateStr);
		}
		System.err.println("Cannot parse date: " + dateStr);
		throw new ParseException(dateStr, 0);
	}
	
	//copy from SmartUtil
	private XMLGregorianCalendar toXmlDate(Date d) throws DatatypeConfigurationException {
		if (d == null) {
			return null;
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(d);
		
		XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		xgc.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setHour(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setMinute(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setSecond(DatatypeConstants.FIELD_UNDEFINED);
		
		return xgc;
	}

	//copy from PatrolToXmlConverter
	private XMLGregorianCalendar toXmlTime(Date d) throws DatatypeConfigurationException{
		GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
		cal.setTime(d);
		
		XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		xgc.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setYear(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setMonth(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setDay(DatatypeConstants.FIELD_UNDEFINED);
		
		return xgc;
	}

	private static Date combine(XMLGregorianCalendar xmlDate, XMLGregorianCalendar xmlTime) {
		Date date = xmlDate != null ? xmlDate.toGregorianCalendar().getTime() : null;
		Date time = xmlTime != null ? xmlTime.toGregorianCalendar().getTime() : null;
		return combine(date, time);
	}
	
	//copy from SmartImporter
	private static Date combine(Date date, Date time) {
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

	//copy from GeometryUtils
	private static double distanceInMeters(LineString ls) {
		GeodeticCalculator cal = new GeodeticCalculator();
		double distance = 0;
		for (int i = 1; i < ls.getCoordinates().length; i ++){
			cal.setStartingGeographicPoint(ls.getCoordinateN(i-1).x, ls.getCoordinateN(i-1).y);
			cal.setDestinationGeographicPoint(ls.getCoordinateN(i).x, ls.getCoordinateN(i).y);
			
			distance +=cal.getOrthodromicDistance();
		}
		return distance;
	}

	//copy from SmartUtils
	private static String encodeHex(byte[] data) {
		if (data == null) return ""; //$NON-NLS-1$
		char[] toDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		int l = data.length;
		char[] out = new char[l << 1];
		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
			out[j++] = toDigits[0x0F & data[i]];
		}
		return new String(out);
	}

}
