/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.incident.xml.model.v23;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import org.geotools.geometry.jts.WKBReader;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.LocalSignatureTypeManager;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Attribute.GeometrySource;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.GeometryAttributeValue;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IIncidentProvider;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.xml.IXmlToIncidentConverter;
import org.wcs.smart.incident.xml.IncidentToXml;
import org.wcs.smart.incident.xml.IncidentXmlManager;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Converts an xml incident file to a
 * waypoint object
 * 
 * @author Emily
 * @since 8.0.0
 */
public class XmlToIncident implements IXmlToIncidentConverter{
	
	private Session session;
	private ConservationArea ca;

	private Waypoint incident;
	private List<String> warnings = new ArrayList<String>();
	
	private Path attachmentLocation = null;
	
	
	public XmlToIncident(){
		
	}
	
	private WaypointType readIncident(Path file) throws JAXBException, IOException{
		try(InputStream is = Files.newInputStream(file)){
			JAXBContext context = JAXBContext.newInstance("org.wcs.smart.incident.xml.model.v23"); //$NON-NLS-1$
			Unmarshaller un = context.createUnmarshaller();	
			@SuppressWarnings("unchecked")
			JAXBElement<WaypointType> o = (JAXBElement<WaypointType>) un.unmarshal(is);
			WaypointType x = o.getValue();
			return x;
		}
	}

	/**
	 * @return any warnings generated during the import process
	 */
	public List<String> getWarnings(){
		return warnings;
	}
	
	/**
	 * @return the imported waypoint
	 *
	 */
	public Waypoint getImportedIncident(){
		return incident;
	}
	
	
	/**
	 * Imports a waypoint from an xml object.
	 * <p>
	 * Use getImportedWaypoint() to retrieve the imported
	 * incident object.
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
	public void fromXml(Path file, 
			Session session, ConservationArea ca, 
			Path attachmentLocation) throws Exception {
		
		WaypointType xml = readIncident(file);
		
		this.session = session;
		this.ca = ca;
		this.attachmentLocation = attachmentLocation;
		
		incident= new Waypoint();
		
		IIncidentProvider provider = IncidentManager.getInstance().getIncidentProvider(xml.getType());
		if (provider == null) throw new Exception(MessageFormat.format(Messages.XmlToIncident_TypeNotSupported, xml.getType()));
		incident.setSourceId(provider.getWaypointSourceKey());
		
		incident.setConservationArea(ca);
		incident.setComment(xml.getComment());
		incident.setDirection(xml.getDirection());
		incident.setDistance(xml.getDistance());
		incident.setId(xml.getId());
		incident.setDateTime( LocalDateTime.parse(xml.getDateTime(), DateTimeFormatter.ofPattern(IncidentToXml.DATE_FORMAT_STR)));
		incident.setRawX(xml.getX());
		incident.setRawY(xml.getY());
		
		if (attachmentLocation != null){
			if (xml.getAttachments().size() > 0){
				incident.setAttachments(new ArrayList<WaypointAttachment>());
				for ( AttachmentType attType : xml.getAttachments()){
					WaypointAttachment att = new WaypointAttachment();
					
					String filename = attType.getFilename();
					
					if (attType.getSignatureTypeKey() != null && !attType.getSignatureTypeKey().trim().isEmpty()) {
						SignatureType stype = LocalSignatureTypeManager.INSTANCE.findType(attType.getSignatureTypeKey(), ca, session);
						if (stype == null) {
							warnings.add(MessageFormat.format(Messages.XmlToIncident_SignatureTypeNotFound,attType.getSignatureTypeKey()));
						}else {
							att.setSignatureType(stype);
						}
					}
					
					Path f = attachmentLocation.toAbsolutePath()
							.resolve(IncidentXmlManager.ATTACHMENT_DIR_NAME)
							.resolve(filename);
					
					if (!Files.exists(f)){
						warnings.add(MessageFormat.format(
								Messages.XmlToIncident_AttachmentNotImported, new Object[]{ filename, f.toAbsolutePath().toString()}));
								
					}else{
						att.setCopyFromLocation(f);
						att.setFilename(filename);
					
						incident.getAttachments().add(att);
						att.setWaypoint(incident);
					}
				}
			}
		}
		
		incident.setObservationGroups(new ArrayList<>());
		for(WaypointObservationGroupType xmlgroup : xml.getGroups()) {
			WaypointObservationGroup group = new WaypointObservationGroup();
			group.setWaypoint(incident);
			incident.getObservationGroups().add(group);
			group.setObservations(new ArrayList<>());
			for (WaypointObservationType type : xmlgroup.getObservations()){
				WaypointObservation ob = convertWaypointObservation(type, group);
				if (ob != null){
					group.getObservations().add(ob);
				}
			}
		}
	}
	
	private WaypointObservation convertWaypointObservation(WaypointObservationType xml, 
			WaypointObservationGroup parent ){
		
		WaypointObservation ob = new WaypointObservation();
		ob.setObservationGroup(parent);
		
		if (attachmentLocation != null){
			if (xml.getAttachments().size() > 0){
				ob.setAttachments(new ArrayList<ObservationAttachment>());
				for ( AttachmentType attachment : xml.getAttachments()){
					ObservationAttachment att = new ObservationAttachment();
					String filename = attachment.getFilename();
					
					Path f = attachmentLocation.toAbsolutePath()
							.resolve(IncidentXmlManager.ATTACHMENT_DIR_NAME)
							.resolve(filename);
					
					if (!Files.exists(f)){
						warnings.add(MessageFormat.format(
								Messages.XmlToIncident_AttachmentNotImported, new Object[]{ filename, f.toAbsolutePath().toString()}));
					}else{

						if (attachment.getSignatureTypeKey() != null && !attachment.getSignatureTypeKey().trim().isEmpty()) {
							SignatureType stype = LocalSignatureTypeManager.INSTANCE.findType(attachment.getSignatureTypeKey(), ca, session);
							if (stype == null) {
								warnings.add(MessageFormat.format(Messages.XmlToIncident_SignatureTypeNotFound,attachment.getSignatureTypeKey()));
							}else {
								att.setSignatureType(stype);
							}
						}
						
						att.setCopyFromLocation(f);
						att.setFilename(filename);
						ob.getAttachments().add(att);
						try{
							att.setObservation(ob);
						}catch (Exception ex){
							warnings.add(MessageFormat.format(Messages.XmlToIncident_CouldNotConfigureAttachment, filename, ex.getMessage()));
						}
					}
				}
			}
		}
		if (xml.getObserver() != null){
			
			Employee e = findEmployee(xml.getObserver());
			if (e == null){
				warnings.add(MessageFormat.format(
						Messages.XmlToIncident_ObserverNotFound,
						new Object[]{xml.getObserver().getGivenName(), xml.getObserver().getFamilyName()}));
				
			}else{
				ob.setObserver(e);
			}
			
		}
		Category cat = findCategory(xml.getCategoryKey());
		if (cat == null){
			warnings.add(MessageFormat.format(Messages.XmlToIncident_CategoryNotFound,
					new Object[]{xml.getCategoryKey()}) 
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
								MessageFormat.format(Messages.XmlToIncident_MultpleAttributeValues,
										new Object[]{attribute.getAttribute().getKeyId()})); 
										
					}
				}else{
					warnings.add(Messages.XmlToIncident_DataError);					
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
			warnings.add(MessageFormat.format(Messages.XmlToIncident_AttributeNotFound,
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
					warnings.add(MessageFormat.format(Messages.XmlToIncident_DoubleValueNotFound, new Object[]{type.getAttributeKey()}));
					return null;
				}
				attribute.setNumberValue(type.getDValue());
			}else if (dmAttribute.getType() == AttributeType.TEXT){
				if (type.getSValue() == null){
					warnings.add(MessageFormat.format(Messages.XmlToIncident_StringValueNotFound, new Object[]{type.getAttributeKey()}));
					return null;
				}
				attribute.setStringValue(type.getSValue());	
			}else if (dmAttribute.getType() == AttributeType.DATE){
				if (type.getSValue() == null){
					warnings.add(MessageFormat.format(Messages.XmlToIncident_StringValueNotFound, new Object[]{type.getAttributeKey()}));
					return null;
				}
				if (!Attribute.isValidDateString(type.getSValue())){
					warnings.add(MessageFormat.format(Messages.XmlToIncident_InvalidDateString, new Object[]{type.getSValue(), type.getAttributeKey(), Attribute.DATE_FORMAT}));
					return null;
				}
				attribute.setStringValue(type.getSValue());
			}else if (dmAttribute.getType() == AttributeType.TIME){
				if (type.getSValue() == null){
					warnings.add(MessageFormat.format(Messages.XmlToIncident_StringValueNotFound, new Object[]{type.getAttributeKey()}));
					return null;
				}
				if (!Attribute.isValidTimeString(type.getSValue())){
					warnings.add(MessageFormat.format(Messages.XmlToIncident_InvalidTimeString, new Object[]{type.getSValue(), type.getAttributeKey(), Attribute.TIME_FORMAT}));
					return null;
				}
				attribute.setStringValue(type.getSValue());
			}else if (dmAttribute.getType() == AttributeType.LIST){
				if (type.getItemKey() == null || type.getItemKey().size() != 1){
					warnings.add(MessageFormat.format(Messages.XmlToIncident_NoValueFound, new Object[]{type.getAttributeKey()}));
					return null;
				}	
				AttributeListItem item = findAttributeListItem(type.getItemKey().get(0), dmAttribute);
				if (item == null){
					warnings.add(MessageFormat.format(Messages.XmlToIncident_ListValueNotFound, new Object[]{type.getItemKey(),type.getAttributeKey()}));
					return null;
				}	
				attribute.setAttributeListItem(item);
				
			}else if (dmAttribute.getType() == AttributeType.MLIST){
				if (type.getItemKey() == null || type.getItemKey().isEmpty()){
					warnings.add(MessageFormat.format(Messages.XmlToIncident_NoValueFound, new Object[]{type.getAttributeKey()}));
					return null;
				}
				List<WaypointObservationAttributeList> items = new ArrayList<>();
				for (String s : type.getItemKey()) {
					AttributeListItem li = findAttributeListItem(s, dmAttribute);
					if (li == null){
						warnings.add(MessageFormat.format(Messages.XmlToIncident_ListValueNotFound, new Object[]{s,type.getAttributeKey()}));
					} else {
						WaypointObservationAttributeList item = new WaypointObservationAttributeList();
						item.setAttributeLisItem(li);
						item.setObservationAttribute(attribute);
						items.add(item);
					}
				}
				if (items.isEmpty()) {
					warnings.add(MessageFormat.format(Messages.XmlToIncident_NoValidItemsForMultiList, type.getAttributeKey()));
					return null;
				}else {
					attribute.setAttributeListItems(items);
				}
				
			}else if (dmAttribute.getType() == AttributeType.TREE){
				if (type.getItemKey() == null || type.getItemKey().size() != 1){
					warnings.add(MessageFormat.format(Messages.XmlToIncident_NoValueFound, new Object[]{type.getAttributeKey()}));
					return null;
				}	
				AttributeTreeNode item = findAttributeTreeItem(type.getItemKey().get(0), dmAttribute);
				if (item == null){
					warnings.add(MessageFormat.format(Messages.XmlToIncident_TreeNodeNotFound, new Object[]{type.getItemKey(),type.getAttributeKey()}));
					return null;
				}
				attribute.setAttributeTreeNode(item);
			}else if (dmAttribute.getType().isGeometry()) {
				GeometrySource src = GeometrySource.UNKNOWN;
				try {
					src = GeometrySource.valueOf(type.getSValue());	
				}catch (Exception ex) {
					warnings.add(MessageFormat.format(Messages.XmlToIncident_InvalidSource, type.getSValue(), type.getAttributeKey()));
				}
				
				try {
					byte[] geom = Base64.getDecoder().decode(type.getGeomValue());
					Geometry g = (new WKBReader()).read(geom);
					GeometryAttributeValue value = new GeometryAttributeValue(g, src);
					attribute.setGeometry(value);	
				}catch (Exception ex) {
					warnings.add(MessageFormat.format(Messages.XmlToIncident_InvalidGeometry, type.getAttributeKey()));
				}
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
		String sql = "FROM Attribute WHERE conservationArea = :ca and keyId = :key"; //$NON-NLS-1$
		Query<Attribute> query = session.createQuery(sql, Attribute.class);
		query.setParameter("key", key); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
		List<Attribute> results = query.list();
		if (results.size() == 0){
			return null;
		}else if (results.size() > 1){
			throw new IllegalStateException(Messages.XmlToIncident_TooManyAttributes);
		}else{
			Attribute att = results.get(0);
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
		String sql = "FROM Category WHERE conservationArea = :ca and keyId = :key"; //$NON-NLS-1$
		Query<Category> query = session.createQuery(sql, Category.class);
		query.setParameter("key", bits[bits.length - 1]); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
		List<Category> results = query.list();
		Category found = null;
		for (Iterator<Category> iterator = results.iterator(); iterator.hasNext();) {
			Category options = iterator.next();
			if (options.getHkey().equals(key)){
				found = options;
				break;
			}
		}
		return found;
		
	}
	
	private  Employee findEmployee(EmployeeType type){	
		return HibernateManager.findEmployeeByIdAndName(type.getEmployeeId(), type.getGivenName(), type.getFamilyName(), ca, session);
	}

}


