package org.wcs.smart.ct2smart.patrol;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.geotools.referencing.GeodeticCalculator;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeValue;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.ct2smart.matcher.model.CtCategory;
import org.wcs.smart.ct2smart.matcher.model.ExtraAttribute;
import org.wcs.smart.ct2smart.parser.TeamMembersParser;
import org.wcs.smart.ct2smart.patrol.Ct2SmartLookup.Ct2AttributeValuePair;
import org.wcs.smart.ct2smart.xml.parser.TagA;
import org.wcs.smart.ct2smart.xml.parser.TagS;
import org.wcs.smart.ct2smart.xml.parser.TagT;
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
	
	private static final String LANGUAGE_CODE = "en";

	private static final DateFormat df = new SimpleDateFormat("MM/dd/yyyy"); //$NON-NLS-1$
	
	private Ct2SmartLookup lookup;
	private TeamMembersParser membersParser = new TeamMembersParser();
	
	public PatrolBuilder(Ct2Smart ct2Smart) {
		lookup = new Ct2SmartLookup(ct2Smart);
	}

	public PatrolType createPatrol(List<TagS> sList, List<TagT> tList) throws DatatypeConfigurationException, ParseException {

		LineString line = createTrack(tList);
		
		TrackType track = null;
		if (line != null) {
			track = new TrackType();
			track.setDistance(distanceInMeters(line) / 1000.0);
			WKBWriter writer = new WKBWriter(3);
			track.setGeom(encodeHex(writer.write(line)));
		}
		
		PatrolType patrol = new PatrolType();
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
		
				
		for (TagS s : sList) {
			WaypointType wp = new WaypointType();
			legDay.getWaypoints().add(wp);
			wp.setId(legDay.getWaypoints().size());

			String defaultCategoryKey = getDefaultCategory(s);
			WaypointObservationType defObs = new WaypointObservationType();
			wp.getObservations().add(defObs);
			defObs.setCategoryKey(defaultCategoryKey);
			

			for (TagA a : s) {
				Ct2Attribute cta = lookup.findAttribute(a.getI());
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
						WaypointObservationAttributeType obsAttr = new WaypointObservationAttributeType();
						obs.getAttributes().add(obsAttr);
						obsAttr.setAttributeKey(cta.getMapTo());
						obsAttr.setDValue(Double.valueOf(a.getV()));
						break;
					}
					case TEXT: {
						WaypointObservationAttributeType obsAttr = new WaypointObservationAttributeType();
						obs.getAttributes().add(obsAttr);
						obsAttr.setAttributeKey(cta.getMapTo());
						obsAttr.setSValue(a.getV());
						break;
					}
					case REF: {
						WaypointObservationAttributeType obsAttr = new WaypointObservationAttributeType();
						obs.getAttributes().add(obsAttr);
						obsAttr.setAttributeKey(cta.getMapTo());
						for (Ct2AttributeValue val : cta.getCt2AttributeValue()) {
							if (a.getV().equals(val.getI())) {
								obsAttr.setItemKey(val.getMapTo());
								break;
							}
						}
						break;
					}
					case META_DATE:
						wpDate = df.parse(a.getV());
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
						for (Ct2AttributeValue val : cta.getCt2AttributeValue()) {
							if (v.equals(val.getI())) {
								v = val.getMapTo();
								break;
							}
						}
						if (mandate == null) {
							mandate = new LabelType();
							mandate.setLanguageCode(LANGUAGE_CODE);
							mandate.setValue(v);
						} else if (!v.equals(mandate.getValue())) {
							System.err.println("Two different mandates in one patrol: " + mandate.getValue() + " and " + v);
						}
						break;
					}
					case META_MEMBERS:
						members.addAll(membersParser.parseMembers(a.getV()));
						break;
					case IGNORE:
						break;
				}
				
				for (ExtraAttribute ea : cta.getExtraAttribute()) {
					WaypointObservationAttributeType obsAttr = new WaypointObservationAttributeType();
					obs.getAttributes().add(obsAttr);
					obsAttr.setAttributeKey(ea.getAttributeKey());
					obsAttr.setItemKey(ea.getValueKey());
				}
			}
			
			if (wp.getX() == null || wp.getY() == null) {
				Coordinate c = CoordinateUtil.interpolate(line, combine(wpDate, wpTime));
				if (c != null) {
					if (wp.getX() == null)
						wp.setX(c.x);
					if (wp.getY() == null)
						wp.setY(c.y);
				}
			}
			
		}

		for (String fullName : members) {
			PatrolMemberType member = new PatrolMemberType();
			leg.getMembers().add(member);
			member.setIsLeader(false);
			member.setIsPilot(false);
			String[] parts = fullName.split(" ");
			switch (parts.length) {
			case 2:
				member.setFamilyName(parts[1]);
				member.setGivenName(parts[0]);
				break;
			case 1:
				member.setFamilyName(parts[0]);
				break;

			default:
				break;
			}
		}
		if (!leg.getMembers().isEmpty()) {
			leg.getMembers().get(0).setIsLeader(true);
		}

		patrol.setMandate(mandate);

		patrol.setStartDate(xmlDate);
		patrol.setEndDate(xmlDate);

		leg.setStartDate(xmlDate);
		leg.setEndDate(xmlDate);

		legDay.setDate(xmlDate);
		legDay.setStartTime(xmlStartTime);
		legDay.setEndTime(xmlEndTime);
		
		return patrol;
	}

	private LineString createTrack(List<TagT> tList) throws ParseException {
		List<Coordinate> coordinates = new ArrayList<Coordinate>();
		Date date;
		Time time;
		double x, y;
		for (TagT t : tList) {
			date = df.parse(t.getDate());
			time = Time.valueOf(t.getTime());
			y = Double.valueOf(t.getLatitude());
			x = Double.valueOf(t.getLongitude());
			coordinates.add(new Coordinate(x, y, combine(date, time).getTime()));
		}
		return CoordinateUtil.buildLineString(coordinates);
	}

	private String getDefaultCategory(TagS s) {
		List<Ct2AttributeValuePair> data = new ArrayList<Ct2AttributeValuePair>();
		for (TagA a : s) {
			Ct2Attribute cta = lookup.findAttribute(a.getI());
			if (cta != null && Ct2AttributeType.CATEGORY.equals(cta.getType())) {
				Ct2AttributeValuePair pair = new Ct2AttributeValuePair();
				pair.attribute = cta;
				pair.value = a.getV();
				data.add(pair);
			}
		}
		CtCategory c = lookup.findCategory(data);
		if (c == null) {
			String info = "";
			for (Ct2AttributeValuePair pair : data) {
				info += "\nattribute: " + pair.attribute.getN() + " " + pair.attribute.getI();
				Ct2Attribute value = lookup.findAttribute(pair.value);
				info += "  value: ";
				info += value != null ? value.getN() : "null";
				info += " " + pair.value;
			}
			System.out.println("Warning: No category defined for following items: " + info);
		}
		return c != null ? c.getCategoryKey() : null;
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

	//copy from SmartImporter
	private static Date combine(Date date, Time time) {
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
