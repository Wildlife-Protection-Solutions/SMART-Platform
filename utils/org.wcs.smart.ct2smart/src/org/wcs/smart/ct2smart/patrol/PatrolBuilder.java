package org.wcs.smart.ct2smart.patrol;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeValue;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.ct2smart.xml.parser.TagA;
import org.wcs.smart.ct2smart.xml.parser.TagS;
import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.WaypointObservationAttributeType;
import org.wcs.smart.patrol.xml.model.WaypointObservationType;
import org.wcs.smart.patrol.xml.model.WaypointType;

public class PatrolBuilder {

	private static final DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
	
	private Ct2SmartLookup lookup;
	
	public PatrolBuilder(Ct2Smart ct2Smart) {
		lookup = new Ct2SmartLookup(ct2Smart);
	}

	public PatrolType createPatrol(List<TagS> sights, String dateStr) throws DatatypeConfigurationException, ParseException {
		XMLGregorianCalendar date = toXmlDate(df.parse(dateStr));

		PatrolType patrol = new PatrolType();
		patrol.setIsArmed(false);
		patrol.setStartDate(date);
		patrol.setEndDate(date);
		
		PatrolLegType leg = new PatrolLegType();
		patrol.getLegs().add(leg);
		leg.setId(String.valueOf(patrol.getLegs().size()));
		leg.setStartDate(date);
		leg.setEndDate(date);
		
		PatrolLegDayType legDay = new PatrolLegDayType();
		leg.getDays().add(legDay);
		legDay.setDate(date);

		for (TagS s : sights) {
			WaypointType wp = new WaypointType();
			legDay.getWaypoints().add(wp);
			wp.setId(legDay.getWaypoints().size());

			String defaultCategoryKey = null; //TODO: implement fetching
			WaypointObservationType obs = new WaypointObservationType();
			wp.getObservations().add(obs);
			obs.setCategoryKey(defaultCategoryKey); 

			for (TagA a : s) {
				Ct2Attribute cta = lookup.findAttribute(a.getI());
				//TODO: special cases for not default category
				if (cta == null || cta.getType() == null) {
					//attribute is unmapped; treat the same as ignore
					//TOOD: warning?
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
					default:
						break;
					}
			}
		}

		return patrol;
	}
	
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
	
}
