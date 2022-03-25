/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.asset.data.inout.deployment.xml.v10;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SignatureTypeManager;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.inout.deployment.DeploymentToXml;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetDeploymentDisruption;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointAttachment;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;


/**
 * Imports exported deployment zip file.
 * 
 * @author Emily
 *
 */
public class DeploymentFromXml {

	private ConservationArea ca = SmartDB.getCurrentConservationArea();
	
	public AssetDeployment importDeployment(Path zipFile, Session session, Shell shell, 
			IEventBroker broker, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor);
		
		sub.beginTask(Messages.DeploymentFromXml_LoadAssetTask + zipFile.getFileName().toString(), 10);
		sub.subTask(Messages.DeploymentFromXml_unzipsubtask);
		sub.split(1);
		Path workingDir = null;
		try{
			workingDir = ZipUtil.unzip(zipFile);
		}catch(Exception ex) {
			AssetPlugIn.displayLog(Messages.DeploymentFromXml_unzipError +ex.getMessage(), ex);
			return null;
		}
		try {
			Path deploymentFile = workingDir.resolve(DeploymentToXml.XML_FILE);
			if (!Files.exists(deploymentFile)) {
				AssetPlugIn.displayLog(Messages.DeploymentFromXml_InvalidFile,  null);
				return null;
			}
			sub.subTask(Messages.DeploymentFromXml_ConvertSubtask);
			sub.split(1);
			
			XmlAssetDeployment xml = null;
			try{
				xml = readXmlFile(deploymentFile);
			}catch (Exception ex) {
				AssetPlugIn.displayLog(Messages.DeploymentFromXml_XmlFileError + ex.getMessage(), ex);
				return null;
			}
			
			AssetDeployment deployment = new AssetDeployment();
	
			try {
				Asset asset = findAsset(xml.getAssetKey(), xml.getAssetTypeKey(), session);
				AssetStationLocation location = findStationLocation(xml.getStationId(), xml.getLocationId(), session);
			
				deployment.setAsset(asset);
				deployment.setStationLocation(location);
			}catch (Exception ex) {
				AssetPlugIn.displayLog(ex.getMessage(), ex);
				return null;
			}
			
			LocalDateTime sDate = SmartUtils.toLocalDateTime(xml.getStartDateTime());
			LocalDateTime eDate = null;
			if (xml.getEndDateTime() != null) {
				eDate = SmartUtils.toLocalDateTime(xml.getEndDateTime());
			}
			
			deployment.setAssetWaypoints(new ArrayList<>());
			deployment.setAttributeValues(new ArrayList<>());
			deployment.setDisruptions(new ArrayList<>());
			deployment.setEndDate(eDate);
			deployment.setStartDate(sDate);
			
			
			//ensure deployment does not overlap an existing deployment for this asset
			List<AssetDeployment> deployments = QueryFactory.buildQuery(session, 
					AssetDeployment.class,
					new Object[] {"asset", deployment.getAsset()}).list(); //$NON-NLS-1$
			
			
			LocalDateTime start = sDate;
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime end = (eDate == null ? now : eDate);
			
			boolean overlaps = false;
			for (AssetDeployment other : deployments) {
			
				LocalDateTime startTest = other.getStartDate();
				LocalDateTime endTest = now;
				if (other.getEndDate() != null) endTest = other.getEndDate();
				
				if (!(endTest.isBefore(start) || startTest.isAfter(end))) { 
					overlaps = true;
					break;
				}
			}
			if (overlaps) {
				AssetPlugIn.displayLog(Messages.DeploymentFromXml_OverlappingTimeFrame, null);
				return null;
			}
			
			List<String> warnings = new ArrayList<>();
			
			sub.subTask(Messages.DeploymentFromXml_CovertingAttributesSubTask);
			sub.split(1);
			for (XmlAssetDeploymentAttribute xmla : xml.getAttributes()) {
				try {
					AssetAttribute attribute = findAttribute(xmla.getAttributeKey(), xmla.getAttributeTypeKey(), session);
					
					AssetDeploymentAttributeValue value = new AssetDeploymentAttributeValue();
					value.setAttribute(attribute);
					value.setNumberValue(xmla.getDoubleValue1());
					value.setNumberValue2(xmla.getDoubleValue2());
					if (attribute.getType() == AttributeType.LIST) {
						value.setAttributeListItem(findAttributeListItem(attribute, xmla.getStringValue()));
					}else {
						value.setStringValue(xmla.getStringValue());
					}
					value.setAssetDeployment(deployment);
					deployment.getAttributeValues().add(value);
				}catch (Exception ex) {
					warnings.add(Messages.DeploymentFromXml_NotImportedError + ex.getMessage());
				}
			}
			
			sub.subTask(Messages.DeploymentFromXml_ConvertDisruptionsSubTask);
			sub.split(1);
			for (XmlAssetDeploymentDisruption xmld : xml.getDisruptions()) {
				AssetDeploymentDisruption disruption = new AssetDeploymentDisruption();
				disruption.setComment(xmld.getComment());
				disruption.setStartDate( SmartUtils.toLocalDateTime(xmld.getStartDateTime()) );
				disruption.setEndDate( SmartUtils.toLocalDateTime(xml.getEndDateTime()) );
				disruption.setAssetDeployment(deployment);
				deployment.getDisruptions().add(disruption);
			}
			
			sub.subTask(Messages.DeploymentFromXml_ConvertDeploymentSubTask);
			sub.setWorkRemaining(xml.getWaypoints().size() + 1);
			
			for (XmlWaypoint xmlwp : xml.getWaypoints()) {
				sub.split(1);
				
				AssetWaypoint aw = new AssetWaypoint();
				
				aw.setAssetDeployment(deployment);
				aw.setAttachments(new HashSet<>());
				aw.setIncidentLength(xmlwp.getLength());
				
				try {
					aw.setState(AssetWaypoint.State.valueOf(xmlwp.getState()));
				}catch (Exception ex) {
					aw.setState(AssetWaypoint.State.DIRTY);
				}
				
	
				
				Waypoint wp = new Waypoint();
				wp.setComment(xmlwp.getComment());
				wp.setRawX(xmlwp.getX());
				wp.setRawY(xmlwp.getY());
				wp.setId(xmlwp.getId());
				wp.setConservationArea(ca);
				wp.setDateTime(  SmartUtils.toLocalDateTime(xmlwp.getDateTime()) );
				wp.setObservationGroups(new ArrayList<>());
				wp.setSourceId(AssetWaypointSource.KEY);
				wp.setAttachments(new ArrayList<>());
				
				deployment.getAssetWaypoints().add(aw);
				
				for (XmlAttachmentType xmlattachment : xmlwp.getAttachments()) {
					String filename = xmlattachment.getFilename();
					
					WaypointAttachment attachment = new WaypointAttachment();
					
					if (xmlattachment.getSignatureTypeKey() != null && !xmlattachment.getSignatureTypeKey().trim().isEmpty()) {
						SignatureType stype = SignatureTypeManager.INSTANCE.findType(xmlattachment.getSignatureTypeKey(), ca, session);
						if (stype != null) {
							attachment.setSignatureType(stype);
						}else {
							warnings.add(MessageFormat.format(Messages.DeploymentFromXml_SignatureTypeNotFound,xmlattachment.getSignatureTypeKey()));
						}
					}
					
					Path fname = workingDir.resolve(DeploymentToXml.ATTCHMENT_DIR).resolve(filename);
					if (!Files.exists(fname)) {
						warnings.add(MessageFormat.format(Messages.DeploymentFromXml_WaypointAttachmentNotFound, fname));
						continue;
					}
					attachment.setWaypoint(wp);
					attachment.setFilename(filename);
					attachment.setCopyFromLocation(fname);
					wp.getAttachments().add(attachment);
					
					AssetWaypointAttachment awa = new AssetWaypointAttachment();
					awa.setAssetWaypoint(aw);
					awa.setWaypointAttachment(attachment);
					
					aw.getAttachments().add(awa);
				}
				
				
				for (XmlWaypointObservationGroup xmlg : xmlwp.getObservations()) {
					WaypointObservationGroup group = new WaypointObservationGroup();
					group.setObservations(new ArrayList<>());
					group.setWaypoint(wp);
					
					for (XmlWaypointObservation xmlo : xmlg.getObservations()) {
						
						WaypointObservation wo = new WaypointObservation();
						wo.setAttachments(new ArrayList<>());
						wo.setAttributes(new ArrayList<>());
						try {
							wo.setCategory(findDmCategory(xmlo.getCategoryKey(), session));
						}catch (Exception ex) {
							warnings.add(Messages.DeploymentFromXml_ObservationNotImported + ex.getMessage());
							continue;
						}
						
						wo.setObservationGroup(group);
						try {
							if (xmlo.getObserverId() != null) 
								wo.setObserver(findEmployee(xmlo.getObserverId(), session));
						}catch (Exception ex) {
							warnings.add(Messages.DeploymentFromXml_ObserverNotSet + ex.getMessage());
						}
						group.getObservations().add(wo);
						
						for (XmlAttachmentType xmlattachment : xmlo.getAttachments()) {
							String filename = xmlattachment.getFilename();
							ObservationAttachment attachment = new ObservationAttachment();
							
							Path fname = workingDir.resolve(DeploymentToXml.ATTCHMENT_DIR).resolve(filename);
							if (!Files.exists(fname)) {
								warnings.add(MessageFormat.format(Messages.DeploymentFromXml_AttachmentNotImported, fname));
								continue;
							}
							attachment.setObservation(wo);
							attachment.setFilename(filename);
							attachment.setCopyFromLocation(fname);
							SignatureType stype = SignatureTypeManager.INSTANCE.findType(xmlattachment.getSignatureTypeKey(), ca, session);
							if (stype != null) {
								attachment.setSignatureType(stype);
							}else {
								warnings.add(MessageFormat.format(Messages.DeploymentFromXml_SignatureTypeNotFound,xmlattachment.getSignatureTypeKey()));
							}
							wo.getAttachments().add(attachment);
						}
						
						for (XmlWaypointObservationAttribute xmla : xmlo.getAttributes()) {
							WaypointObservationAttribute woa = new WaypointObservationAttribute();
							try {
								woa.setAttribute(findAttribute(wo.getCategory(), xmla.getAttributeKey(), xmla.getAttributeType()));
								switch(woa.getAttribute().getType()) {
								case BOOLEAN:
								case NUMERIC:
									 woa.setNumberValue(xmla.getDoubleValue());
									 break;
								case DATE:
								case TEXT:
									if (xmla.getStringValue().size() == 1)
										woa.setStringValue(xmla.getStringValue().get(0));
									break;
								case LIST:
									if (xmla.getStringValue().size() == 1)
										woa.setAttributeListItem(findDmAttributeListItem(woa.getAttribute(), xmla.getStringValue().get(0)));
									break;
								case TREE:
									if (xmla.getStringValue() != null && xmla.getStringValue().size() == 1)
										woa.setAttributeTreeNode(findDmAttributeTreeNode(woa.getAttribute(), xmla.getStringValue().get(0), session));
									break;
								case MLIST:
									List<WaypointObservationAttributeList> items = new ArrayList<>();
									
									for (String x : xmla.getStringValue()) {
										try {
											AttributeListItem li = findDmAttributeListItem(woa.getAttribute(), x);
											if (li != null) {
												WaypointObservationAttributeList item = new WaypointObservationAttributeList();
												item.setAttributeLisItem(li);
												item.setObservationAttribute(woa);
												items.add(item);
											}
										}catch (Exception ex) {
											warnings.add(ex.getMessage());
										}
									}
									if (items.isEmpty()) throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_NoListItemsFound, woa.getAttribute().getName()));
									woa.setAttributeListItems(items);
									
								}
							}catch (Exception ex) {
								warnings.add(Messages.DeploymentFromXml_AttributeNotImported + ex.getMessage());
								continue;
							}
							wo.getAttributes().add(woa);
							woa.setObservation(wo);
						}
					}
					
					wp.getObservationGroups().add(group);
	
				}
				
				//merge with an existing waypoint
				//find another asset waypoint at the same station with 
				//the same time and link this asset to that waypoint; adding observations
				//if necessary
				LocalDateTime wpdt = SmartUtils.toLocalDateTime(xmlwp.getDateTime());
				List<Waypoint> test = QueryFactory.buildQuery(session, Waypoint.class, 
						new Object[] {"conservationArea", ca}, //$NON-NLS-1$
						new Object[] {"sourceId", AssetWaypointSource.KEY}, //$NON-NLS-1$
						new Object[] {"dateTime", wpdt}).list(); //$NON-NLS-1$
				Waypoint tomerge = null;
				for(Waypoint w : test) {
					List<AssetWaypoint> aws = QueryFactory.buildQuery(session, AssetWaypoint.class, new Object[] {"waypoint",w}).list();  //$NON-NLS-1$
					for (AssetWaypoint awtest : aws){
						 if (awtest.getAssetDeployment().getStationLocation().getStation().equals(deployment.getStationLocation().getStation())) {
							 tomerge = w;
							 break;
						 }
						
					}
				}
				if (tomerge != null) {
					mergeWaypoints(tomerge, wp);
					aw.setWaypoint(tomerge);
					
					for (WaypointAttachment awa : wp.getAttachments()) {
						awa.setWaypoint(tomerge);
						tomerge.getAttachments().add(awa);
					}
				}else {
					aw.setWaypoint(wp);
				}
			}
			
			
			sub.subTask(Messages.DeploymentFromXml_SavingSubTask);
			sub.setWorkRemaining(1);
			
			if (!warnings.isEmpty()) {
				final int[] warnr = new int[1];
				
				Display.getDefault().syncExec(()->{
					WarningDialog warn = new WarningDialog(shell, 
							Messages.DeploymentFromXml_WarnTitle, 
							Messages.DeploymentFromXml_WarnMessage, 
							warnings,
							new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
					warnr[0] = warn.open();
				});
				
				if (warnr[0] != 0) return null;
			}
			
			//save 
			session.beginTransaction();
			try {
				for (AssetWaypoint aw : deployment.getAssetWaypoints()) {
					session.saveOrUpdate(aw.getWaypoint());
				}
				session.flush();
				session.save(deployment);
				session.getTransaction().commit();
			}catch (Exception ex) {
				try {
					if (session.getTransaction().isActive()) session.getTransaction().rollback();
				}catch (Exception e) {
					AssetPlugIn.log(e.getMessage(), e);
				}
				AssetPlugIn.displayLog(Messages.DeploymentFromXml_SaveError + ex.getMessage(), ex);
				return null;
			}
			
			broker.post(AssetEvents.ASSETDEPLOYMENT_NEW, Collections.singleton(deployment));
			
			return deployment;
		}finally {
			try {
				SmartUtils.deleteDirectory(workingDir);
			}catch (Exception ex) {
				AssetPlugIn.log(ex.getMessage(), ex);
			}
		}
	}
	

	private void mergeWaypoints(Waypoint existing, Waypoint newwp) {
		
		//merge observation groups
		for (WaypointObservationGroup wo : newwp.getObservationGroups()) {
			
			//find an observation group with all the same observations
			boolean found = false;
			for (WaypointObservationGroup existingg : existing.getObservationGroups()) {
				boolean ok = true;
				for (WaypointObservation oo : existingg.getObservations()) {
					if (!hasMatchingObservation(oo, wo.getObservations())) {
						ok = false;
						break;
					}
				}
				if (ok) {
					found = true;
					break;
				}
				
			}
			
			if (!found) {
				//add this observation group to the existing waypoint
				existing.getObservationGroups().add(wo);
				wo.setWaypoint(existing);
			}
		}
	}
	
	private boolean hasMatchingObservation(WaypointObservation wo, Collection<WaypointObservation> others) {
		
		for (WaypointObservation o : others) {
			if (o.getCategory().equals(wo.getCategory())) {
				//lets see if all the attributes match as well
				if (o.getAttributes().size() != wo.getAttributes().size()) continue;
				
				boolean ok = true;
				for (WaypointObservationAttribute a : wo.getAttributes()) {
					WaypointObservationAttribute test = o.findAttribute(a.getAttribute());
					if (test == null) {
						ok = false;
						break;
					}
					
					switch(a.getAttribute().getType()) {
					case NUMERIC:
					case BOOLEAN: 
						ok = (a.getNumberValue() != null && a.getNumberValue().equals(test.getNumberValue())) || (a.getNumberValue() == null && test.getNumberValue() == null);
						break;
					case DATE:
					case TEXT:
						ok = (a.getStringValue() != null && a.getStringValue().equals(test.getStringValue())) || (a.getStringValue() == null && test.getStringValue() == null); 
						break;
					case LIST:
						ok = (a.getAttributeListItem() != null && a.getAttributeListItem().equals(test.getAttributeListItem())) || (a.getAttributeListItem() == null && test.getAttributeListItem() == null);
						break;
					case TREE:
						ok = (a.getAttributeTreeNode() != null && a.getAttributeTreeNode().equals(test.getAttributeTreeNode())) || (a.getAttributeTreeNode() == null && test.getAttributeTreeNode() == null);
						break;
					default:
						break;
					
					}
					if (!ok) break;
				}
				
				if (!ok) continue;
				
				return true;
			}
		}
		return false;
	}
	
	private XmlAssetDeployment readXmlFile(Path xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Unmarshaller unmarshaller = context.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<XmlAssetDeployment> o = (JAXBElement<XmlAssetDeployment>) unmarshaller.unmarshal(xmlFile.toFile());
		return o.getValue();
	}
	
	private Employee findEmployee(String id, Session session) throws Exception{
		
		Employee employee = QueryFactory.buildQuery(session, Employee.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"id", id})				 //$NON-NLS-1$
			.uniqueResult();
		
		if (employee == null) throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_EmployeeNotFound, id));
		return employee;
	}
	
	private Category findDmCategory(String hkey, Session session) throws Exception{
			
		Category category = QueryFactory.buildQuery(session, Category.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"hkey", hkey})				 //$NON-NLS-1$
			.uniqueResult();
		
		if (category == null) throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_CategoryNotFound, hkey));
		return category;
	}
	
	private Attribute findAttribute(Category category, String attributeKey, String attributeType) throws Exception{
		List<Attribute> all = new ArrayList<>();
		category.getAllAttribute(all, null);
		for (Attribute a : all) {
			if (a.getKeyId().equalsIgnoreCase(attributeKey)) {
				if (a.getType().name().equalsIgnoreCase(attributeType)) return a;
			}
		}
		throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_AttributeNotFound, attributeType, attributeKey, category.getName()));
	}
	
	private AssetAttribute findAttribute(String keyId, String type, Session session) throws Exception{
		AssetAttribute.AttributeType atype = null;
	
		try {
			atype = AssetAttribute.AttributeType.valueOf(type.toUpperCase());
		}catch (IllegalArgumentException  t) {
			throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_InvalidAssetType, type));
		}
		
		AssetAttribute attribute = QueryFactory.buildQuery(session, AssetAttribute.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", keyId}, //$NON-NLS-1$
				new Object[] {"type", atype})				 //$NON-NLS-1$
			.uniqueResult();
		
		if (attribute == null) {
			throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_AssetAttributeNotFound, type, keyId));
		}
		return attribute;
	}
	
	private AttributeTreeNode findDmAttributeTreeNode(Attribute attribute, String hkey, Session session) throws Exception {
		if (hkey == null) return null;
		AttributeTreeNode treenode = QueryFactory.buildQuery(session, AttributeTreeNode.class, 
				new Object[] {"attribute", attribute}, //$NON-NLS-1$
				new Object[] {"hkey", hkey}) //$NON-NLS-1$
			.uniqueResult();
		
		if (treenode == null)  throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_TreeNodeNotFound, hkey, attribute.getName()));
		return treenode;
	}
	
	private AttributeListItem findDmAttributeListItem(Attribute attribute, String keyId) throws Exception {
		if (keyId == null) return null;
		for (AttributeListItem li : attribute.getAttributeList()) {
			if (li.getKeyId().equalsIgnoreCase(keyId)) return li;
		}
		throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_ListItemNotFound, keyId, attribute.getName()));
	}
	
	private AssetAttributeListItem findAttributeListItem(AssetAttribute attribute, String keyId) throws Exception {
		for (AssetAttributeListItem li : attribute.getAttributeList()) {
			if (li.getKeyId().equalsIgnoreCase(keyId)) return li;
		}
		throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_AssetListItemNotFound, keyId, attribute.getName()));
	}
	
	
	private Asset findAsset(String id, String type, Session session) throws Exception {
		AssetType atype = QueryFactory.buildQuery(session, AssetType.class, 
							new Object[] {"conservationArea", ca}, //$NON-NLS-1$
							new Object[] {"keyId", type}) //$NON-NLS-1$
						.uniqueResult();
		if (atype == null) {
			throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_AssetTypeNotFound, type));
		}
		
		Asset asset = QueryFactory.buildQuery(session, Asset.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"assetType", atype}, //$NON-NLS-1$
				new Object[] {"id", id}).uniqueResult(); //$NON-NLS-1$
		
		if (asset == null) throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_AssetNotFound, type, id));
		
		return asset;
	}
	
	private AssetStationLocation findStationLocation(String stationId, String locationId, Session session) throws Exception{
		AssetStation station = QueryFactory.buildQuery(session, AssetStation.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"id", stationId}).uniqueResult(); //$NON-NLS-1$
		if (station == null) throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_StationNotFound, stationId));
		
		for(AssetStationLocation location : station.getLocations()) {
			if (location.getId().equals(locationId)) return location;
		}
		
		throw new Exception(MessageFormat.format(Messages.DeploymentFromXml_LocationNotFound, locationId, stationId))  ;
	}
}
