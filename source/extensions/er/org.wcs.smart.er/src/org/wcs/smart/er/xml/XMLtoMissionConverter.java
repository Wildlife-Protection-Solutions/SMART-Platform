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
package org.wcs.smart.er.xml;

import java.io.File;
import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.er.xml.model.missions.MembersType;
import org.wcs.smart.er.xml.model.missions.MissionDayType;
import org.wcs.smart.er.xml.model.missions.MissionPropertyValuesType;
import org.wcs.smart.er.xml.model.missions.MissionType;
import org.wcs.smart.er.xml.model.missions.SurveyWaypointsType;
import org.wcs.smart.er.xml.model.missions.TracksType;
import org.wcs.smart.er.xml.model.missions.WaypointObservationAttributeType;
import org.wcs.smart.er.xml.model.missions.WaypointObservationType;
import org.wcs.smart.er.xml.model.missions.WaypointType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * Converts an xml mission file to a
 * mission object.
 * 
 * @author Emily, Jeff
 * @since 1.0.0, 4.0
 */
public class XMLtoMissionConverter {
	
	private Session session;
	private ConservationArea ca;

	private Mission mission;
	private List<String> warnings = new ArrayList<String>();
	
	private File attachmentLocation = null;
	
	
	public XMLtoMissionConverter(){
		
	}
	
	/**
	 * @return any warnings generated during the import process
	 */
	public List<String> getWarnings(){
		return warnings;
	}
	
	/**
	 * @return the imported mission
	 */
	public Mission getImportedMission(){
		return mission;
	}
	
	
	/**
	 * Imports a mission from an xml object.
	 * <p>
	 * Use getImportedPatrol() to retrieve the imported
	 * mission object.
	 * </p>
	 * <p>User getWarings() to retrieve any warnings
	 * that occurred during the import process.
	 * </p> 
	 * @param xml
	 * @param session
	 * @param ca
	 * @param attachmentLocation
	 * @throws Exception
	 */
	public void fromXml(MissionType xml, boolean keepIDs, Session session, ConservationArea ca, File attachmentLocation) throws Exception {
		this.session = session;
		this.ca = ca;
		this.attachmentLocation = attachmentLocation;
		
		mission = new Mission();

		mission.setComment(xml.getComment());
		
		XMLGregorianCalendar temp = xml.getStartDate();
		if(temp != null){
			mission.setStartDate(temp.toGregorianCalendar().getTime());
		}else{
			mission.setStartDate(null);
		}
		temp = xml.getEndDate();
		if(temp != null){
			mission.setEndDate(temp.toGregorianCalendar().getTime());
		}else{
			mission.setEndDate(null);
		}
		
		if(keepIDs){
			mission.setId(xml.getId());
		}else{
			mission.setId(SurveyHibernateManager.generateMissionId(session));
		}
		
		setMembersAndLeader(mission, xml);
		createAndSetSurvey(mission, xml);
		mission.setMissionDays(new ArrayList<MissionDay>());
		
		setMissionPropertyValues(mission, xml);
		for (MissionDayType mdtxml : xml.getDays()){
			MissionDay md = new MissionDay();
			md.setDate(mdtxml.getDate().toGregorianCalendar().getTime());
			md.setStartTime( new Time(mdtxml.getStartTime().toGregorianCalendar().getTime().getTime()) );
			md.setEndTime( new Time(mdtxml.getEndTime().toGregorianCalendar().getTime().getTime()) );
			md.setRestMinutes(mdtxml.getRestMinutes());
			md.setMission(mission);
			
			mission.getMissionDays().add(md);
			setTracks(md, mdtxml);

			//must have created and set the Tracks before calling setwaypoints. 
			setWaypoints(md, mdtxml);
		}
	}

	private void setTracks(MissionDay m, MissionDayType xml) {
		for(TracksType xmlMt : xml.getTracks()){
			MissionTrack mt = new MissionTrack();
			
			mt.setGeom(xmlMt.getGeom());
			mt.setId(xmlMt.getId());

			mt.setMissionDay(m);
			mt.setSamplingUnit(findSamplingUnit(xmlMt.getSamplingUnitId() ,session));
			
			m.getTracks().add(mt);
		}
		
	}

	private void setMissionPropertyValues(Mission m, MissionType xml) {
		for (MissionPropertyValuesType xmlMpv : xml.getMissionPropertyValues()){
			MissionPropertyValue mpv = new MissionPropertyValue();
			
			mpv.setMission(m);
			mpv.setMissionAttribute(findMissionAttribute(xmlMpv.getMissionAttributeKeyId()) );
			mpv.setNumberValue(xmlMpv.getNumberValue());
			mpv.setStringValue(xmlMpv.getStringValue());
			
			mpv.setAttributeListItem( getMissionListItem(xmlMpv.getListElementKeyId()) );
			
			m.getMissionPropertyValues().add(mpv);
		}		
	}

	private void createAndSetSurvey(Mission m, MissionType xml) {
		Survey survey = new Survey();
		
		XMLGregorianCalendar temp = xml.getSurvey().getEndDate();
		if(temp != null){
			survey.setEndDate(temp.toGregorianCalendar().getTime());
		}else{
			survey.setEndDate(null);
		}
		temp = xml.getSurvey().getStartDate();
		if(temp != null){
			survey.setStartDate(temp.toGregorianCalendar().getTime());
		}else{
			survey.setStartDate(null);
		}
		
		survey.setId(xml.getSurvey().getId());
		
		ArrayList<Mission> missionList = new ArrayList<Mission>();
		missionList.add(m);
		survey.setMissions(missionList);
		
		survey.setSurveyDesign(findSurveyDesignByKey(xml.getSurvey().getSurveyDesignKeyId()) );
	
		m.setSurvey(survey);
	}

	private void setMembersAndLeader(Mission m, MissionType xml) {
		m.setMembers(new ArrayList<MissionMember>());
		
		for(MembersType xmlMember : xml.getMembers()){
			MissionMember member = new MissionMember();
			
			Employee employee = findEmployeeByIdAndName(xmlMember);
			if(employee == null){
				employee = findEmployeeByName(xmlMember);
			}
			member.setMember(employee);
			member.setIsLeader(xmlMember.isLeader());
			member.setMission(m);
			
			if(xmlMember.isLeader() == true){
				m.setLeader(member);
			}
			m.getMembers().add(member);
		}
		
	}

	private void setWaypoints(MissionDay m, MissionDayType xml) {
		for(SurveyWaypointsType xmlWp : xml.getSurveyWaypoints()){
			SurveyWaypoint swp = new SurveyWaypoint();
			
			Waypoint wp = convertWaypoint(xmlWp.getWaypoints());
			swp.setWaypoint(wp);
			swp.setMissionDay(m);
			swp.setMissionTrack( getTrackById(m, xmlWp.getMissionTrackId()) );
			swp.setSamplingUnit(findSamplingUnit(xmlWp.getSamplingUnitId() ,session));

			m.getWaypoints().add(swp);
		}		
	}

	private MissionTrack getTrackById(MissionDay m, String missionTrackId) {
		for(MissionTrack mt : m.getTracks()){
			if(mt.getId().equals(missionTrackId)){
				return mt;
			}
		}
		return null;
	}

	private Waypoint convertWaypoint(WaypointType xml){
		Waypoint wp = new Waypoint();

		wp.setComment(xml.getComment());
		wp.setDirection(xml.getDirection());
		wp.setDistance(xml.getDistance());
		wp.setId(xml.getId());
		wp.setConservationArea(SmartDB.getCurrentConservationArea());
		wp.setSourceId(SurveyWaypointSource.KEY);

		XMLGregorianCalendar temp = xml.getDateTime();
		if(temp != null){
			wp.setDateTime(temp.toGregorianCalendar().getTime());
		}else{
			wp.setDateTime(null);
		}
		wp.setX(xml.getX());
		wp.setY(xml.getY());
		if (attachmentLocation != null){
			if (xml.getAttachments().size() > 0){
				wp.setAttachments(new ArrayList<WaypointAttachment>());
				for ( String filename : xml.getAttachments()){
					WaypointAttachment att = new WaypointAttachment();
					File f = new File(attachmentLocation.getAbsoluteFile() + File.separator + MissionXmlManager.ATTACHMENT_DIR_NAME + File.separator + filename );
					if (!f.exists()){
						warnings.add(MessageFormat.format(
								Messages.XMLtoMissionConverter_0, new Object[]{ filename, f.getAbsolutePath()}));
								
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
					File f = new File(attachmentLocation.getAbsoluteFile() + File.separator + MissionXmlManager.ATTACHMENT_DIR_NAME + File.separator + filename );
					if (!f.exists()){
						warnings.add(MessageFormat.format(
								Messages.XMLtoMissionConverter_1, new Object[]{ filename, f.getAbsolutePath()}));
					}else{
						att.setCopyFromLocation(f);
						att.setFilename(filename);
						ob.getAttachments().add(att);
						att.setObservation(ob);
					}
				}
			}
		}
		
		Category cat = findCategory(xml.getCategoryKey());
		if (cat == null){
			warnings.add(MessageFormat.format(Messages.XMLtoMissionConverter_2,
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
								MessageFormat.format(Messages.XMLtoMissionConverter_3,
										new Object[]{parent.getId(), DateFormat.getDateTimeInstance().format(parent.getDateTime()), attribute.getAttribute().getKeyId()})); 
										
					}
				}else{
					warnings.add(MessageFormat.format(
						Messages.XMLtoMissionConverter_4, new Object[]{
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
			warnings.add(MessageFormat.format(Messages.XMLtoMissionConverter_5,
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
					warnings.add(MessageFormat.format(Messages.XMLtoMissionConverter_6, new Object[]{type.getAttributeKey()}));
					return null;
				}
				attribute.setNumberValue(type.getDValue());
			}else if (dmAttribute.getType() == AttributeType.TEXT){
				if (type.getSValue() == null){
					warnings.add(MessageFormat.format(Messages.XMLtoMissionConverter_7, new Object[]{type.getAttributeKey()}));
					return null;
				}
				attribute.setStringValue(type.getSValue());	
			}else if (dmAttribute.getType() == AttributeType.DATE){
				if (type.getSValue() == null){
					warnings.add(MessageFormat.format(Messages.XMLtoMissionConverter_8, new Object[]{type.getAttributeKey()}));
					return null;
				}
				if (!Attribute.isValidDateString(type.getSValue())){
					warnings.add(MessageFormat.format(Messages.XMLtoMissionConverter_9, new Object[]{type.getSValue(), type.getAttributeKey(), Attribute.DATE_FORMAT}));
					return null;
				}
				attribute.setStringValue(type.getSValue());
			}else if (dmAttribute.getType() == AttributeType.LIST){
				if (type.getItemKey() == null){
					warnings.add(MessageFormat.format(Messages.XMLtoMissionConverter_10, new Object[]{type.getAttributeKey()}));
					return null;
				}	
				AttributeListItem item = findAttributeListItem(type.getItemKey(), dmAttribute);
				if (item == null){
					warnings.add(MessageFormat.format(Messages.XMLtoMissionConverter_11, new Object[]{type.getItemKey(),type.getAttributeKey()}));
					return null;
				}	
				attribute.setAttributeListItem(item);
			}else if (dmAttribute.getType() == AttributeType.TREE){
				if (type.getItemKey() == null){
					warnings.add(MessageFormat.format(Messages.XMLtoMissionConverter_12, new Object[]{type.getAttributeKey()}));
					return null;
				}	
				AttributeTreeNode item = findAttributeTreeItem(type.getItemKey(), dmAttribute);
				if (item == null){
					warnings.add(MessageFormat.format(Messages.XMLtoMissionConverter_13, new Object[]{type.getItemKey(),type.getAttributeKey()}));
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
		String sql = "FROM Attribute WHERE conservationArea = :ca and keyid = :key"; //$NON-NLS-1$
		Query query = session.createQuery(sql);
		query.setParameter("key", key); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
		List<?> results = query.list();
		if (results.size() == 0){
			return null;
		}else if (results.size() > 1){
			throw new IllegalStateException(Messages.XMLtoMissionConverter_14);
		}else{
			Attribute att = (Attribute) results.get(0);
			//ensure attribute exists for category
			if (findCategoryAttribute(category, att)){
				return att;
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
	
	
	private  Employee findEmployeeByIdAndName(MembersType type){	
		return HibernateManager.findEmployeeByIdAndName(type.getEmployeeId(), type.getGivenName(), type.getFamilyName(), ca, session);
	}
	
	private  Employee findEmployeeByName(MembersType type){
		return HibernateManager.findEmployeeByName(type.getGivenName(), type.getFamilyName(), ca, session);
	}
	
	private SurveyDesign findSurveyDesignByKey(String surveyDesignKeyId) {
		return SurveyHibernateManager.getInstance().getSurveyDesign(surveyDesignKeyId, session);
	}
	
	private MissionAttribute findMissionAttribute(String missionAttributeKeyId) {
		return SurveyHibernateManager.getInstance().getMissionAttributeByKey(missionAttributeKeyId, session);
	}
	
	private MissionAttributeListItem getMissionListItem(String key) {
		return SurveyHibernateManager.getInstance().getMissionAttributeListItenByKey(key, session);
	}
	
	private SamplingUnit findSamplingUnit(String key, Session session) {
		return SurveyHibernateManager.getInstance().getSamplingUnitById(key, session);
	}

}

