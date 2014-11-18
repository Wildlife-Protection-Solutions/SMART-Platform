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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolMemberType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.TrackType;
import org.wcs.smart.patrol.xml.model.WaypointObservationAttributeType;
import org.wcs.smart.patrol.xml.model.WaypointObservationType;
import org.wcs.smart.patrol.xml.model.WaypointType;
import org.wcs.smart.util.SmartUtils;

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
	
	/**
	 * @return any warnings generated during the import process
	 */
	public List<String> getWarnings(){
		return warnings;
	}
	
	/**
	 * @return the imported patrol
	 */
	public Patrol getImportedPatrol(){
		return patrol;
	}
	
	
	/**
	 * Imports a patrol from an xml object.
	 * <p>
	 * Use getImportedPatrol() to retrieve the imported
	 * patrol object.
	 * </p>
	 * <p>User getWarings() to retireve any warnings
	 * that ocurred during the import process.
	 * </p> 
	 * @param xml
	 * @param session
	 * @param ca
	 * @param attachmentLocation
	 * @throws Exception
	 */
	public void fromXml(PatrolType xml, boolean keepIDs, Session session, ConservationArea ca, File attachmentLocation) throws Exception {
		this.session = session;
		this.ca = ca;
		this.attachmentLocation = attachmentLocation;
		
		patrol = new Patrol();
			
		patrol.setArmed(xml.isIsArmed());
		patrol.setConservationArea(ca);
		patrol.setEndDate(xml.getEndDate().toGregorianCalendar().getTime());
		patrol.setStartDate(xml.getStartDate().toGregorianCalendar().getTime());
		patrol.setComment(xml.getComment());
		if (keepIDs) {
			patrol.setId(xml.getId());
			//validate patrol id
			if (! SmartUtils.isSimpleString(patrol.getId(), 
					SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Patrol.MAX_ID_LENGTH) ) {
				throw new Exception(MessageFormat.format(Messages.XmlToPatrolConverter_InvalidPatrolId,
						patrol.getId(), Patrol.MAX_ID_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc));
			}
				
		}
		
		patrol.setPatrolType(org.wcs.smart.patrol.model.PatrolType.Type.valueOf(xml.getPatrolType().toUpperCase()));
		if (xml.getObjective() != null){
			patrol.setObjective(xml.getObjective().getDescription());
		}

		if (xml.getMandate() != null){
			PatrolMandate m = (PatrolMandate) findValue(xml.getMandate().getLanguageCode(), xml.getMandate().getValue(), "PatrolMandate"); //$NON-NLS-1$
			if (m == null){
				//ERROR
				throw new Exception(
						MessageFormat.format(
								Messages.XmlToPatrolConverter_Error_MandateNotFound,
								new Object[]{xml.getMandate().getValue(), xml.getMandate().getLanguageCode()})
								);
			}else{
				patrol.setMandate(m);
			}
		}else{
			patrol.setMandate(null);
		}
		if (xml.getStation() != null){
			Station station = (Station) findValue(xml.getStation().getLanguageCode(), xml.getStation().getValue(), "Station"); //$NON-NLS-1$
			if (station == null){
				
				warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_StationNoFound, new Object[]{xml.getStation().getValue(),xml.getStation().getLanguageCode()}));			
			}else{
				patrol.setStation(station);
			}
		}
		
		if (xml.getTeam() != null){
			Team team = (Team) findValue(xml.getTeam().getLanguageCode(), xml.getTeam().getValue(), "Team"); //$NON-NLS-1$
			if (team == null){
				warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_TemNotFound, new Object[]{xml.getTeam().getValue(),xml.getTeam().getLanguageCode()}));
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
		
		PatrolTransportType ttype = 
				(PatrolTransportType)findTransportationValue(xml.getTransportType().getLanguageCode(), xml.getTransportType().getValue(), patrol.getPatrolType());
				
		if (ttype == null){
			throw new Exception(MessageFormat.format(
				Messages.XmlToPatrolConverter_Error_TranpsortTypeNotFound, new Object[]{xml.getTransportType().getValue(), xml.getTransportType().getLanguageCode(), patrol.getPatrolType().getGuiName()}));
		}
		boolean found = false;
		
		session.beginTransaction();
		List<PatrolTransportType> types =  PatrolHibernateManager.getPatrolTransporationTypes(ca, session, patrol.getPatrolType());
		session.getTransaction().rollback();
		
		for (PatrolTransportType t: types){
			if (t.equals(ttype)){
				found = true;
				break;
			}
		}
		if (!found){
			throw new Exception(MessageFormat.format(
					Messages.XmlToPatrolConverter_Error_InvalidTransportType, new Object[]{xml.getTransportType().getValue(), xml.getTransportType().getLanguageCode(), patrol.getPatrolType().getGuiName()}));
		}
		leg.setType(ttype);
		
		//parse members
		leg.setMembers(new ArrayList<PatrolLegMember>());
		for (PatrolMemberType member : xml.getMembers()){
			Employee e = findEmployeeByIdAndName(member);
			if (e == null){
				e = findEmployeeById(member);
				if (e != null){
					warnings.add(MessageFormat.format(
							Messages.XmlToPatrolConverter_Warning_EmployeeNameDifferent,
							new Object[]{member.getEmployeeId(), Employee.formatName(member.getGivenName(), member.getFamilyName()),e.getShortLabel()}) 
						);
							
							
				}else{
					e = findEmployeeByName(member);
					if (e == null){
						warnings.add(MessageFormat.format(
								Messages.XmlToPatrolConverter_Warning_EmployeeNotFound,
								new Object[]{Employee.formatName(member.getGivenName(), member.getFamilyName(), member.getEmployeeId())}
						));
					}else{					
						
						warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_EmployeeIdDifferent,
								new Object[]{Employee.formatName(member.getGivenName(), member.getFamilyName(), member.getEmployeeId() ), e.getFullLabel()}
						));
					}
				}
			}
			if (e != null){
				PatrolLegMember mem = new PatrolLegMember();
				mem.setMember(e);
				mem.setPatrolLeg(leg);
				mem.setIsLeader(member.isIsLeader());
				mem.setIsPilot(member.isIsPilot());
				leg.getMembers().add(mem);
			}
		}
		
		leg.setPatrolLegDays(new ArrayList<PatrolLegDay>());
		for (PatrolLegDayType type : xml.getDays()){
			PatrolLegDay pld = convertPatrolLegDay(type, leg);
			

			if(!(pld.getDate().before(leg.getStartDate()) || pld.getDate().after(leg.getEndDate()))){
				//ensure leg day is included in leg date range, if not don't include it
				leg.getPatrolLegDays().add(pld);
			}else{
				warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_DayOutsideLegRange, new Object[]{leg.getId(), pld.getDate(), leg.getStartDate(), leg.getEndDate()}));
			}
			
			
		}
		
		//verify if a leg day exists for each day
		Date start = leg.getStartDate();
		Date end = leg.getEndDate();
		Calendar cal = Calendar.getInstance();
		cal.setTime(start);
		while(!cal.getTime().after(end)){
			boolean dayfound = false;
			for(PatrolLegDay pld : leg.getPatrolLegDays()){
				if (pld.getDate().equals(cal.getTime())){
					dayfound = true;
					break;
				}
			}
			if (!dayfound){
				//create a leg for the missing day
				PatrolLegDay missing = new PatrolLegDay();
				missing.setDate(cal.getTime());
				missing.setEndTime(Time.valueOf("00:00:00")); //$NON-NLS-1$
				missing.setStartTime(missing.getEndTime());
				missing.setPatrolLeg(leg);
				missing.setRestMinutes(0);
				leg.getPatrolLegDays().add(missing);
			}
			cal.add(Calendar.DAY_OF_MONTH, 1);
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
		legday.setWaypoints(new ArrayList<PatrolWaypoint>());
		for(WaypointType wtype : xml.getWaypoints()){
			PatrolWaypoint pwp = new PatrolWaypoint();
			pwp.setPatrolLegDay(legday);
			pwp.setWaypoint(convertWaypoint(wtype, legday));
			legday.getWaypoints().add(pwp);
		}
		
		return legday;
	}
	
	private Waypoint convertWaypoint(WaypointType xml, PatrolLegDay parent){
		Waypoint wp = new Waypoint();
		wp.setComment(xml.getComment());
		wp.setDirection(xml.getDirection());
		wp.setDistance(xml.getDistance());
		wp.setId(xml.getId());
		wp.setConservationArea(parent.getPatrolLeg().getPatrol().getConservationArea());
		wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
		wp.setDateTime(SmartUtils.combineDateTime(parent.getDate(), xml.getTime().toGregorianCalendar().getTime()));
		wp.setX(xml.getX());
		wp.setY(xml.getY());
		if (attachmentLocation != null){
			if (xml.getAttachments().size() > 0){
				wp.setAttachments(new ArrayList<WaypointAttachment>());
				for ( String filename : xml.getAttachments()){
					WaypointAttachment att = new WaypointAttachment();
					File f = new File(attachmentLocation.getAbsoluteFile() + File.separator + PatrolXmlManager.ATTACHMENT_DIR_NAME + File.separator + filename );
					if (!f.exists()){
						warnings.add(MessageFormat.format(
								Messages.XmlToPatrolConverter_Warning_AttachmentFileNotFound, new Object[]{ filename, f.getAbsolutePath()}));
								
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
		
		if (attachmentLocation != null){
			if (xml.getAttachments().size() > 0){
				ob.setAttachments(new ArrayList<ObservationAttachment>());
				for ( String filename : xml.getAttachments()){
					ObservationAttachment att = new ObservationAttachment();
					File f = new File(attachmentLocation.getAbsoluteFile() + File.separator + PatrolXmlManager.ATTACHMENT_DIR_NAME + File.separator + filename );
					if (!f.exists()){
						warnings.add(MessageFormat.format(
								Messages.XmlToPatrolConverter_Warning_AttachmentFileNotFound, new Object[]{ filename, f.getAbsolutePath()}));
					}else{
						att.setCopyFromLocation(f);
						att.setFilename(filename);
						ob.getAttachments().add(att);
						att.setObservation(ob);
					}
				}
			}
		}
		if (xml.getObserver() != null){
			Employee e = findEmployeeByIdAndName(xml.getObserver());
			if (e == null){
				warnings.add(MessageFormat.format(
						Messages.XmlToPatrolConverter_ObserverNotFound,
						new Object[]{xml.getObserver().getGivenName(), xml.getObserver().getFamilyName()}));
			}else{
				ob.setObserver(e);
			}
		}
		
		Category cat = findCategory(xml.getCategoryKey());
		if (cat == null){
			warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_CategoryNotFound,
					new Object[]{xml.getCategoryKey(),parent.getId() ,DateFormat.getDateTimeInstance().format(parent.getDateTime())  }) 
					);
			return null;
		}else{
			ob.setCategory(cat);
			ob.setAttributes(new ArrayList<WaypointObservationAttribute>());
			for (WaypointObservationAttributeType tp : xml.getAttributes()){
				WaypointObservationAttribute attribute = convertWaypointObservationAttribute(tp, ob);
				if (attribute != null){
					// validate to ensure the attribute does not already exist for given observation
					boolean found = false;
					for (WaypointObservationAttribute existing : ob.getAttributes()){
						if (existing.getAttribute().equals(attribute.getAttribute())){
							found = true;
							break;
						}
					}
					if (!found){
						ob.getAttributes().add(attribute);
					}else{
						warnings.add(
								MessageFormat.format(Messages.XmlToPatrolConverter_DuplicateAttributesError,
										new Object[]{parent.getId(), DateFormat.getDateTimeInstance().format(parent.getDateTime()), attribute.getAttribute().getKeyId()})); 
										
					}
				}else{
					warnings.add(MessageFormat.format(
						Messages.XmlToPatrolConverter_Warning_NotAllDataImported, new Object[]{
							parent.getId(),DateFormat.getDateTimeInstance().format(parent.getDateTime()) }) 
					);
					
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
			warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_AttributeNotFound,
					new Object[]{type.getAttributeKey(), parent.getCategory().getHkey()}));
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
					warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_NoDoubleValue, new Object[]{type.getAttributeKey()}));
					return null;
				}
				attribute.setNumberValue(type.getDValue());
			}else if (dmAttribute.getType() == AttributeType.TEXT){
				if (type.getSValue() == null){
					warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_NoStringValue, new Object[]{type.getAttributeKey()}));
					return null;
				}
				attribute.setStringValue(type.getSValue());	
			}else if (dmAttribute.getType() == AttributeType.DATE){
				if (type.getSValue() == null){
					warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_NoStringValue, new Object[]{type.getAttributeKey()}));
					return null;
				}
				if (!Attribute.isValidDateString(type.getSValue())){
					warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_InvalidDateString, new Object[]{type.getSValue(), type.getAttributeKey(), Attribute.DATE_FORMAT}));
					return null;
				}
				attribute.setStringValue(type.getSValue());
			}else if (dmAttribute.getType() == AttributeType.LIST){
				if (type.getItemKey() == null){
					warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_NoListValue, new Object[]{type.getAttributeKey()}));
					return null;
				}	
				AttributeListItem item = findAttributeListItem(type.getItemKey(), dmAttribute);
				if (item == null){
					warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_ListItemNotFound, new Object[]{type.getItemKey(),type.getAttributeKey()}));
					return null;
				}	
				attribute.setAttributeListItem(item);
			}else if (dmAttribute.getType() == AttributeType.TREE){
				if (type.getItemKey() == null){
					warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_NoTreeItem, new Object[]{type.getAttributeKey()}));
					return null;
				}	
				AttributeTreeNode item = findAttributeTreeItem(type.getItemKey(), dmAttribute);
				if (item == null){
					warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_TreeItemNotFound, new Object[]{type.getItemKey(),type.getAttributeKey()}));
					return null;
				}
				attribute.setAttributeTreeNode(item);
			}
			
		}
		
		return attribute;
	}

	private AttributeTreeNode findAttributeTreeItem(String key, Attribute attribute){
		String bits[] = key.split("\\."); //$NON-NLS-1$
		
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
		if (key != null) {
			String sql = "FROM Attribute WHERE conservationArea = :ca and keyid = :key"; //$NON-NLS-1$
			Query query = session.createQuery(sql);
			query.setParameter("key", key); //$NON-NLS-1$
			query.setParameter("ca", ca); //$NON-NLS-1$
			
			List<?> results = query.list();
			if (results.size() == 0){
				return null;
			}else if (results.size() > 1){
				throw new IllegalStateException(Messages.XmlToPatrolConverter_Error_DuplicateAttributes);
			}else{
				Attribute att = (Attribute) results.get(0);
				//ensure attribute exists for category
				if (findCategoryAttribute(category, att)){
					return att;
				}
			}
		}
		return null;
	}
	
	/*
	 * Searches for a given attribute in the category provided or
	 * one of the parent categories.  Well return false if attribute not
	 * found.  True if attribute found.
	 */
	private boolean findCategoryAttribute(Category root, Attribute attribute){
		for (CategoryAttribute att: root.getAttributes()){
			if (att.getAttribute().equals(attribute)){
				return true;
			}
		}
		if (root.getParent() != null){
			return findCategoryAttribute(root.getParent(), attribute);
		}else{
			//attribute not found
			return false;
		}
	}
	
	private Category findCategory(String key){
		String[] bits = key.split("\\."); //$NON-NLS-1$
		String sql = "FROM Category WHERE conservationArea = :ca and keyid = :key"; //$NON-NLS-1$
		Query query = session.createQuery(sql);
		query.setParameter("key", bits[bits.length - 1]); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
		List<?> results = query.list();
		Category found = null;
		for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
			Category options = (Category) iterator.next();
			if (options.getHkey().equals(key)){
				found = options;
				break;
			}
		}
		return found;
		
	}
	
	
	private  Employee findEmployeeByIdAndName(PatrolMemberType type){	
		return HibernateManager.findEmployeeByIdAndName(type.getEmployeeId(), type.getGivenName(), type.getFamilyName(), ca, session);
	}
	
	private  Employee findEmployeeById(PatrolMemberType type){
		return HibernateManager.findEmployeeById(type.getEmployeeId(), ca, session);
	}
	
	private  Employee findEmployeeByName(PatrolMemberType type){
		return HibernateManager.findEmployeeByName(type.getGivenName(), type.getFamilyName(), ca, session);
	}
	
	private NamedItem findValue(String langCode, String value, String objectType){
		
		String sql = "SELECT c FROM Language a, Label b, " + objectType + " c WHERE b.id.language = a.uuid AND b.id.element.uuid = c.uuid and a.code = :cd and b.value = :value and c.conservationArea = :ca "; //$NON-NLS-1$ //$NON-NLS-2$
		
		Query query = session.createQuery(sql);
		query.setParameter("cd", langCode); //$NON-NLS-1$
		query.setParameter("value", value); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
		List<?> results = query.list();
		if (results.size() == 0){
			return null;
		}else if (results.size() > 1){
			warnings.add(MessageFormat.format(Messages.XmlToPatrolConverter_Warning_MultipleOptionsFound, new Object[]{objectType}));
			return (NamedItem)results.get(0);
		}else{
			return (NamedItem)results.get(0);
		}
	}
	
	private NamedItem findTransportationValue(String langCode, String value, org.wcs.smart.patrol.model.PatrolType.Type type){
		
		String sql = "SELECT c FROM Language a, Label b, PatrolTransportType c WHERE b.id.language = a.uuid " + //$NON-NLS-1$
				"AND b.id.element.uuid = c.uuid and a.code = :cd and b.value = :value and c.conservationArea = :ca and " + //$NON-NLS-1$
				"c.patrolType = :patrolType"; //$NON-NLS-1$
		
		Query query = session.createQuery(sql);
		query.setParameter("cd", langCode); //$NON-NLS-1$
		query.setParameter("value", value); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		query.setParameter("patrolType", type); //$NON-NLS-1$
		
		List<?> results = query.list();
		if (results.size() == 0){
			return null;
		}else if (results.size() > 1){
			warnings.add(Messages.XmlToPatrolConverter_Warning_MultipleTransportTypeOptionsFound);
			return (NamedItem)results.get(0);
		}else{
			return (NamedItem)results.get(0);
		}
	}

}


