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
package org.wcs.smart.patrol.xml;

import java.io.File;
import java.sql.Time;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.SmartUtils;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SimpleList;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointAttachment;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolMemberType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.TrackType;
import org.wcs.smart.patrol.xml.model.WaypointObservationAttributeType;
import org.wcs.smart.patrol.xml.model.WaypointObservationType;
import org.wcs.smart.patrol.xml.model.WaypointType;

/**
 * Converts an xml patrol file to a
 * patrol object.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class XmlToPatrolConverter {
	
	private Session session;
	private ConservationArea ca;

	private Patrol patrol;
	private List<String> warnings = new ArrayList<String>();
	
	private File attachmentLocation = null;
	
	
	public XmlToPatrolConverter(){
		
	}
	
	public List<String> getWarnings(){
		return warnings;
	}
	public Patrol getImportedPatrol(){
		return patrol;
	}
	public void fromXml(PatrolType xml, Session session, ConservationArea ca, File attachmentLocation) throws Exception {
		this.session = session;
		this.ca = ca;
		this.attachmentLocation = attachmentLocation;
		
		patrol = new Patrol();
			
		patrol.setArmed(xml.isIsArmed());
		patrol.setConservationArea(ca);
		patrol.setEndDate(xml.getEndDate().toGregorianCalendar().getTime());
		patrol.setStartDate(xml.getStartDate().toGregorianCalendar().getTime());

		patrol.setPatrolType(org.wcs.smart.patrol.model.PatrolType.Type.valueOf(xml.getPatrolType()));
		patrol.setObjective(xml.getObjective().getDescription());
		patrol.setObjectiveRating(xml.getObjective().getRating());
		
		PatrolMandate m = (PatrolMandate) findValue(xml.getMandate().getLanguageCode(), xml.getMandate().getValue(), "PatrolMandate");
		if (m == null){
			//ERROR
			throw new Exception("The patrol mandate " + xml.getMandate().getValue() + " [" + xml.getMandate().getLanguageCode() + "] could not be found for the current conservation area. ");
		}else{
			patrol.setMandate(m);
		}
		if (xml.getStation() != null){
			Station station = (Station) findValue(xml.getStation().getLanguageCode(), xml.getStation().getValue(), "Station");
			if (station == null){
				warnings.add("Station " + xml.getStation().getValue()+ " [" + xml.getStation().getLanguageCode() + "] could not be found.  The station will not be specified.");			
			}else{
				patrol.setStation(station);
			}
		}
		
		if (xml.getTeam() != null){
			Team team = (Team) findValue(xml.getTeam().getLanguageCode(), xml.getTeam().getValue(), "Team");
			if (team == null){
				warnings.add("Team " + xml.getTeam().getValue()+ " [" + xml.getTeam().getLanguageCode() + "] could not be found.  The team will not be specified.");
			}else{
				patrol.setTeam(team);
			}
		}
		
		patrol.setLegs(new ArrayList<PatrolLeg>());
		for (PatrolLegType legxml : xml.getLegs()){
			patrol.getLegs().add(convertPatrolLeg(legxml, patrol));
		}		
	}
	
	private PatrolLeg convertPatrolLeg(PatrolLegType xml, Patrol parent) throws Exception{
		PatrolLeg leg = new PatrolLeg();
		leg.setEndDate(xml.getEndDate().toGregorianCalendar().getTime());
		leg.setId(xml.getId());
		leg.setPatrol(parent);		
		leg.setStartDate(xml.getStartDate().toGregorianCalendar().getTime());
		
		PatrolTransportType ttype = (PatrolTransportType)findValue(xml.getTransportType().getLanguageCode(), xml.getTransportType().getValue(), "PatrolTransportType");
		if (ttype == null){
			throw new Exception("Patrol transportation type " + xml.getTransportType().getValue() + " [" + xml.getTransportType().getLanguageCode() + "] could not be found.  Please ensure this transportation type exists.");
		}
		boolean found = false;
		List<PatrolTransportType> types =  PatrolHibernateManager.getPatrolTransporationTypes(ca, session, patrol.getPatrolType());
		for (PatrolTransportType t: types){
			if (t.equals(ttype)){
				found = true;
				break;
			}
		}
		if (!found){
			throw new Exception("Invalid patrol transportation type " + xml.getTransportType() + " for patrol type " + patrol.getPatrolType().getGuiName() + ".  Enusre the transportation type exists for the given patrol type in the database.");
		}
		leg.setType(ttype);
		
		//parse members
		leg.setMembers(new ArrayList<PatrolLegMember>());
		for (PatrolMemberType member : xml.getMembers()){
			Employee e = findEmployeeByIdAndName(member);
			if (e == null){
				e = findEmployeeById(member);
				if (e != null){
					warnings.add("Employee with id " + member.getEmployeeId() + " has a different name in the imported data [" + member.getGivenName() + " " + member.getFamilyName() + "] from the name in SMART [" + e.getGivenName() + " " + e.getFamilyName() + "]");
				}else{
					e = findEmployeeByName(member);
					if (e == null){
						warnings.add("Employee " + member.getGivenName() + " " + member.getFamilyName() + " (" + member.getEmployeeId() +") could not be found.  The employee will not be imported.");
					}else{					
						warnings.add("Employee " + member.getGivenName() + " " + member.getFamilyName() + " with id '" + member.getEmployeeId() +"' could not be found.  Employee " + e.getGivenName() + " " + e.getFamilyName() + " with id '" + e.getId() + "' will be used.");
					}
				}
			}
			if (e != null){
				PatrolLegMember mem = new PatrolLegMember();
				mem.setMember(e);
				mem.setPatrolLeg(leg);
				mem.setIsLeader(member.isIsLeader());
				mem.setIsPilot(mem.getIsPilot());
				leg.getMembers().add(mem);
			}
		}
		
		leg.setPatrolLegDays(new ArrayList<PatrolLegDay>());
		for (PatrolLegDayType type : xml.getDays()){
			leg.getPatrolLegDays().add(convertPatrolLegDay(type, leg));
		}
		return leg;
	}
	
	private PatrolLegDay convertPatrolLegDay(PatrolLegDayType xml, PatrolLeg parent) throws Exception{
		PatrolLegDay legday = new PatrolLegDay();
		legday.setDate(xml.getDate().toGregorianCalendar().getTime());
		legday.setEndTime(new Time(xml.getEndTime().toGregorianCalendar().getTime().getTime()));
		legday.setPatrolLeg(parent);
		legday.setRestMinutes((int) xml.getRestMinutes().doubleValue());
		legday.setStartTime(new Time(xml.getStartTime().toGregorianCalendar().getTime().getTime()));
		if (xml.getTrack() != null){
			TrackType txml = xml.getTrack();
			Track track = new Track();
			track.setDistance((float)txml.getDistance().doubleValue());
			track.setGeom( SmartUtils.decodeHex(txml.getGeom()) );
			track.setPatrolLegDay(legday);
			legday.setTrack(track);
		}
		legday.setWaypoints(new ArrayList<Waypoint>());
		for(WaypointType wtype : xml.getWaypoints()){
			Waypoint wp = convertWaypoint(wtype, legday);
			legday.getWaypoints().add(wp);
		}
		
		return legday;
	}
	
	private Waypoint convertWaypoint(WaypointType xml, PatrolLegDay parent){
		Waypoint wp = new Waypoint();
		wp.setComment(xml.getComment());
		wp.setDirection(xml.getDirection());
		wp.setDistance(xml.getDistance());
		wp.setId(xml.getId());
		wp.setPatrolLegDay(parent);
		wp.setTime(new Time(xml.getTime().toGregorianCalendar().getTime().getTime()));
		wp.setX(xml.getX());
		wp.setY(xml.getY());
		if (attachmentLocation != null){
			if (xml.getAttachments().size() > 0){
				wp.setAttachments(new ArrayList<WaypointAttachment>());
				for ( String filename : xml.getAttachments()){
					WaypointAttachment att = new WaypointAttachment();
					File f = new File(attachmentLocation.getAbsoluteFile() + File.separator + PatrolXmlManager.ATTACHMENT_DIR_NAME + File.separator + filename );
					if (!f.exists()){
						warnings.add("Attachment : " + filename + " will not be imported.  File not found '" + f.getAbsolutePath() + "'");
					}else{
						att.setCopyFromLocation(f);
						att.setFilename(filename);
					
						wp.getAttachments().add(att);
						att.setWaypoint(wp);
					}
				}
			}
		}
		
		if (xml.getObservations().size () > 0){
			wp.setObservations(new ArrayList<WaypointObservation>());
			for (WaypointObservationType type : xml.getObservations()){
				WaypointObservation ob = convertWaypointObservation(type, wp);
				if (ob != null){
					wp.getObservations().add(ob);
				}
			}
		}
		return wp;
	}
	
	private WaypointObservation convertWaypointObservation(WaypointObservationType xml, Waypoint parent ){
		WaypointObservation ob = new WaypointObservation();
		ob.setWaypoint(parent);
		Category cat = findCategory(xml.getCategoryKey());
		if (cat == null){
			warnings.add("Observation category " + xml.getCategoryKey() + " for waypoint " + parent.getId() + " on " + DateFormat.getDateInstance().format(parent.getPatrolLegDay().getDate()) + " " + DateFormat.getTimeInstance().format(parent.getTime() + " could not be found. These observations will not be imported."));
			return null;
		}else{
			ob.setCategory(cat);
			ob.setAttributes(new ArrayList<WaypointObservationAttribute>());
			for (WaypointObservationAttributeType tp : xml.getAttributes()){
				WaypointObservationAttribute attribute = convertWaypointObservationAttribute(tp, ob);
				if (attribute != null){
					ob.getAttributes().add(attribute);
				}else{
					warnings.add("Not all data imported for waypoint " + parent.getId() + " on " + DateFormat.getDateInstance().format(parent.getPatrolLegDay().getDate()) + " " + DateFormat.getTimeInstance().format(parent.getTime()) + ". See previous errors");		
				}
			}
		}
		return ob;
	}
	
	
	private WaypointObservationAttribute convertWaypointObservationAttribute(WaypointObservationAttributeType type, WaypointObservation parent){
		WaypointObservationAttribute attribute = new WaypointObservationAttribute();
		attribute.setObservation(parent);
		
		Attribute dmAttribute = findAttribute(type.getAttributeKey(), parent.getCategory());
		if (dmAttribute == null){
			warnings.add("Attribute: " + type.getAttributeKey() + " could not be found for category " + parent.getCategory().getFullKey() + ".  Attribute information will not be imported.");
			return null;
		}else{
			attribute.setAttribute(dmAttribute);
			if (dmAttribute.getType() == AttributeType.BOOLEAN){
				if (type.isBValue()){
					attribute.setNumberValue(1.0);
				}else{
					attribute.setNumberValue(0.0);
				}
				
			}else if (dmAttribute.getType() == AttributeType.NUMERIC){
				if (type.getDValue() == null){
					warnings.add("Attribute: " + type.getAttributeKey() + " has no double value. Attribute information will not be imported.");
					return null;
				}
				attribute.setNumberValue(type.getDValue());
			}else if (dmAttribute.getType() == AttributeType.TEXT){
				if (type.getSValue() == null){
					warnings.add("Attribute: " + type.getAttributeKey() + " has no string value. Attribute information will not be imported.");
					return null;
				}
				attribute.setStringValue(type.getSValue());	
				
			}else if (dmAttribute.getType() == AttributeType.LIST){
				if (type.getItemKey() == null){
					warnings.add("Attribute: " + type.getAttributeKey() + " has no value. Attribute information will not be imported.");
					return null;
				}	
				AttributeListItem item = findAttributeListItem(type.getItemKey(), dmAttribute);
				if (item == null){
					warnings.add("No list item with key " + type.getItemKey() + " for attribute: " + type.getAttributeKey() + ". Attribute information will not be imported.");
					return null;
				}	
				attribute.setAttributeListItem(item);
			}else if (dmAttribute.getType() == AttributeType.TREE){
				if (type.getItemKey() == null){
					warnings.add("Attribute: " + type.getAttributeKey() + " has no value. Attribute information will not be imported.");
					return null;
				}	
				AttributeTreeNode item = findAttributeTreeItem(type.getItemKey(), dmAttribute);
				if (item == null){
					warnings.add("No attribute tree item with key " + type.getItemKey() + " for attribute: " + type.getAttributeKey() + ". Attribute information will not be imported.");
					return null;
				}
				attribute.setAttributeTreeNode(item);
			}
			
		}
		
		return attribute;
	}

	private AttributeTreeNode findAttributeTreeItem(String key, Attribute attribute){
		String bits[] = key.split("\\.");
		
		List<AttributeTreeNode> nodes = attribute.getTree();
		AttributeTreeNode nd = null;
		for (int i = 0; i < bits.length; i ++){
			nd = findTreeNode(bits[i], nodes);
			if (nd == null){
				return null;
			}
			nodes = nd.getChildren();
		}
		return nd;
	}
	
	private AttributeTreeNode findTreeNode (String keypart, List<AttributeTreeNode> nodes){
		for (AttributeTreeNode node : nodes){
			if (node.getKeyId().equals(keypart)){
				return node;
			}
		}
		return null;
	}
	
	private AttributeListItem findAttributeListItem(String key, Attribute attribute){
		for (AttributeListItem item : attribute.getAttributeList()){
			if (item.getKeyId().equals(key)){
				return item;
			}
		}
		return null;
			
	}
	private Attribute findAttribute(String key, Category category){
		String sql = "FROM Attribute WHERE conservationArea = :ca and keyid = :key";
		Query query = session.createQuery(sql);
		query.setParameter("key", key);
		query.setParameter("ca", ca);
		
		List results = query.list();
		if (results.size() == 0){
			return null;
		}else if (results.size() > 1){
			throw new IllegalStateException("There should never be more than one attribute with the same key. Please review the data model.");
		}else{
			Attribute att = (Attribute) results.get(0);
			//ensure attribute exists for category
			for (CategoryAttribute caatt : category.getAttributes()){
				if (caatt.getAttribute().equals(att)){
					//found so all is good
					return att;
				}
			}
		}
		return null;
	}
	
	private Category findCategory(String key){
		String[] bits = key.split("\\.");
		String sql = "FROM Category WHERE conservationArea = :ca and keyid = :key";
		Query query = session.createQuery(sql);
		query.setParameter("key", bits[bits.length - 1]);
		query.setParameter("ca", ca);
		
		List results = query.list();
		Category found = null;
		for (Iterator iterator = results.iterator(); iterator.hasNext();) {
			Category options = (Category) iterator.next();
			if (options.getFullKey().equals(key)){
				found = options;
				break;
			}
		}
		return found;
		
	}
	
	
	private  Employee findEmployeeByIdAndName(PatrolMemberType type){		
		String sql = "FROM Employee WHERE givenName = :given AND familyName = :family AND id = :id AND conservationArea = :ca";
		Query query = session.createQuery(sql);
		query.setParameter("given", type.getGivenName());
		query.setParameter("family", type.getFamilyName());
		query.setParameter("id", type.getEmployeeId());
		query.setParameter("ca", ca);
		
		List results = query.list();
		if (results.size() == 0){
			return null;
		}else{
			return (Employee)results.get(0);
		}
	}
	
	private  Employee findEmployeeById(PatrolMemberType type){		
		String sql = "FROM Employee WHERE id = :id AND conservationArea = :ca";
		Query query = session.createQuery(sql);
		query.setParameter("id", type.getEmployeeId());
		query.setParameter("ca", ca);
		
		List results = query.list();
		if (results.size() == 0){
			return null;
		}else{
			return (Employee)results.get(0);
		}
	}
	
	private  Employee findEmployeeByName(PatrolMemberType type){		
		String sql = "FROM Employee WHERE givenName = :given AND familyName = :family AND conservationArea = :ca";
		Query query = session.createQuery(sql);
		query.setParameter("given", type.getGivenName());
		query.setParameter("family", type.getFamilyName());
		query.setParameter("ca", ca);
		
		List results = query.list();
		if (results.size() == 0){
			return null;
		}else{
			return (Employee)results.get(0);
		}
	}
	
	private SimpleList findValue(String langCode, String value, String objectType){
		
		String sql = "SELECT c FROM Language a, Label b, " + objectType + " c WHERE b.id.language = a.uuid AND b.id.elementuuid = c.uuid and a.code = :cd and b.value = :value and c.conservationArea = :ca ";
		
		Query query = session.createQuery(sql);
		query.setParameter("cd", langCode);
		query.setParameter("value", value);
		query.setParameter("ca", ca);
		
		List results = query.list();
		if (results.size() == 0){
			return null;
		}else{
			return (SimpleList)results.get(0);
		}
		
		
	}
	

}


