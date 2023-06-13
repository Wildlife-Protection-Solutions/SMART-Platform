/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.event.i2.entity;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.i2.ProfileParameter;
import org.wcs.smart.event.i2.entity.EntityMapping.Type;
import org.wcs.smart.event.i2.internal.Messages;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.event.model.IActionParameter;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileEntityType;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * Action that creates a new profile entity.
 * 
 * @author Emily
 *
 */
public class CreateEntityActionType implements IActionType {

	public static final String KEY = "org.wcs.smart.profile.i2.newentity"; //$NON-NLS-1$

	private List<IActionParameter> parameters;
	
	public CreateEntityActionType() {
		parameters = new ArrayList<>();
		parameters.add(ProfileParameter.INSTANCE);
		parameters.add(EntityTypeParameter.INSTANCE);
		parameters.add(MappingParameter.INSTANCE);
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName(Locale l) {
		return Messages.CreateEntityActionType_ActionName;
	}

	@Override
	public String getDescription(Locale l) {
		return Messages.CreateEntityActionType_ActionDescription;
	}

	@Override
	public List<IActionParameter> getActionParameters() {
		return parameters;
	}

	@Override
	public void performAction(EAction action, EFilter filter, WaypointObservation data, Locale l) {
		EActionParameterValue entityTypeParameter = action.findParameter(EntityTypeParameter.INSTANCE.getKey());
		if (entityTypeParameter == null) {
			EventPlugIn.log("No entity type parameter specified for create entity action.", null); //$NON-NLS-1$
			return; //no entity type specified; cannot create 
		}
		String entityTypeKey = entityTypeParameter.getParameterValue();
		
		EActionParameterValue attributeMappings = action.findParameter(MappingParameter.INSTANCE.getKey());
		IntelEntity newEntity = null;
		
		EActionParameterValue profileParam = action.findParameter(ProfileParameter.INSTANCE.getKey());
		if (profileParam == null || profileParam.getParameterValue().isEmpty()) {
			throw new RuntimeException(Messages.CreateEntityActionType_ProfileParameterNotSet);
		}
		
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor(false))) {
			//WaypointObservation temp = session.get(WaypointObservation.class, data.getUuid());
			//if (temp != null) data = temp;
			ConservationArea ca = data.getWaypoint().getConservationArea();

			//find profile
			String keyid = profileParam.getParameterValue();
			IntelProfile ip = QueryFactory.buildQuery(session, IntelProfile.class, 
					new Object[] {"keyId", keyid}, //$NON-NLS-1$
					new Object[] {"conservationArea", data.getWaypoint().getConservationArea()}).uniqueResult(); //$NON-NLS-1$
			
			if (ip == null) throw new RuntimeException(Messages.CreateEntityActionType_ProfileNotFound);
			
			
			//parse mappings
			List<EntityMapping> mappings = new ArrayList<>();
			if (attributeMappings != null && !attributeMappings.getParameterValue().isEmpty()) {
				mappings.addAll(EntityMapping.parse(attributeMappings.getParameterValue(), session, ca));
			}
			
			IntelEntityType entityType = QueryFactory.buildQuery(session, IntelEntityType.class, 
					new Object[] {"keyId", entityTypeKey}, //$NON-NLS-1$
					new Object[] {"conservationArea", data.getWaypoint().getConservationArea()}).uniqueResult(); //$NON-NLS-1$
			if (entityType == null) {
				EventPlugIn.log(MessageFormat.format("No entity type with key {0} found for Conservation Area {1} for create entity action.", entityTypeKey, ca.getId()), null); //$NON-NLS-1$
				return;
			}
			
			//ensure profile is valid for entity type
			boolean ok = false;
			for (IntelProfileEntityType ipe : entityType.getProfiles()) {
				if (ipe.getProfile().equals(ip)) {
					ok = true;
				}
			}
			if (!ok) {
				throw new RuntimeException(MessageFormat.format(Messages.CreateEntityActionType_InvalidProfile, ip.getName(), entityType.getName()));
			}
			
			//create entity
			newEntity = new IntelEntity();
			newEntity.setProfile(ip);
			newEntity.setEntityType(entityType);
			newEntity.setComment(generateComment(action,filter,data));
			newEntity.setConservationArea(ca);
			
			//copy attachments for entity
			newEntity.setEntityAttachments(new ArrayList<>());
			List<ISmartAttachment> attachmentsToCopy = new ArrayList<>();
			if (data.getAttachments() != null) {
				attachmentsToCopy.addAll(data.getAttachments());
			}
			if (data.getWaypoint() != null && data.getWaypoint().getAttachments() != null) {
				attachmentsToCopy.addAll(data.getWaypoint().getAttachments());
			}
			for (ISmartAttachment aa : attachmentsToCopy) {
				try {
					aa.computeFileLocation(session);
				} catch (Exception ex) {
					EventPlugIn.log(ex.getMessage(), ex);
					continue;
				}
				
				IntelAttachment ie = new IntelAttachment();
				ie.setConservationArea(ca);
				ie.setCopyFromLocation(aa.getAttachmentFile());
				ie.setFilename(aa.getFilename());
				ie.setDateCreated(LocalDateTime.now());
				ie.setCreatedBy(SmartDB.getCurrentEmployee());
				
				IntelEntityAttachment iea = new IntelEntityAttachment();
				iea.setAttachment(ie);
				iea.setEntity(newEntity);
				newEntity.getEntityAttachments().add(iea);
			}
			
			if (!newEntity.getEntityAttachments().isEmpty()) {
				newEntity.setPrimaryAttachment(newEntity.getEntityAttachments().get(0).getAttachment());
			}
			//map entity attributes using mappings specified 
			newEntity.setAttributes(new ArrayList<>());
			for (EntityMapping em : mappings) {
				IntelAttribute iattribute = em.getEntityAttribute();
				boolean found = false;
				for (IntelEntityTypeAttribute aa : entityType.getAttributes()) {
					if (aa.getAttribute().equals(iattribute)) {
						found = true;
						break;
					}
				}
				if (!found) {
					EventPlugIn.log(MessageFormat.format(Messages.CreateEntityActionType_AttributeNotFound, iattribute.getKeyId(), newEntity.getEntityType().getKeyId()), null);
					continue;
				}
				
				//check if attribute already mapped
				boolean isDuplicate = false;
				for (IntelEntityAttributeValue v : newEntity.getAttributes()) {
					if (v.getAttribute().equals(iattribute)) {
						isDuplicate = true;
						break;
					}
				}
				if (isDuplicate) {
					//already mapped; skipped this
					continue;
				}
				
				IntelEntityAttributeValue avalue = new IntelEntityAttributeValue();
				avalue.setAttribute(iattribute);
				avalue.setEntity(newEntity);
				boolean add = true;
				if (em.getType() == Type.POSITION && iattribute.getType() == AttributeType.POSITION) {
					//map to position
					avalue.setNumberValue(data.getWaypoint().getX());
					avalue.setNumberValue2(data.getWaypoint().getY());
				}else if (em.getType() == Type.FIXED) {
					//map to fixed value
					switch(iattribute.getType()) {
					case BOOLEAN:
						avalue.setNumberValue(em.getFixedBooleanValue() ? 1.0 : 0.0);
						break;
					case DATE:
						avalue.setDateValue(em.getFixedDateValue());
						break;
					case EMPLOYEE:
						if (em.getFixedEmployee() != null) {
							avalue.setEmployee(em.getFixedEmployee());
						}else {
							add = false;
							break;
						}
						break;
					case LIST:
						avalue.setAttributeListItem(em.getIntelListItem());
						break;
					case NUMERIC:
						avalue.setNumberValue(em.getFixedDouble1Value());
						break;
					case POSITION:
						avalue.setNumberValue(em.getFixedDouble1Value());
						avalue.setNumberValue2(em.getFixedDouble2Value());
						break;
					case TEXT:
						avalue.setStringValue(em.getFixedStringValue());
						break;
					}
				}else if (em.getType() == Type.DM) {
					//map to observation value
					add = false;
					if (data.getAttributes() != null) {
						for (WaypointObservationAttribute wo : data.getAttributes()) {
							if (!wo.getAttribute().equals(em.getDataModelAttribute())) continue;
						
							switch(iattribute.getType()) {
							case BOOLEAN:
								if (wo.getNumberValue() == null) break;
								add = true;
								avalue.setNumberValue(wo.getNumberValue());
								break;
							case DATE:
								if (wo.getDateValue() == null) break;
								add = true;
								avalue.setDateValue(wo.getDateValue());
								break;
							case EMPLOYEE:
								//not supported (no employee data model attributes)
								add = false;
								break;
							case LIST:
								String dmKey = null;
								if (wo.getAttribute().getType() == Attribute.AttributeType.LIST) {
									dmKey = wo.getAttributeListItem().getKeyId();
								}else if (wo.getAttribute().getType() == Attribute.AttributeType.TREE) {
									dmKey = wo.getAttributeTreeNode().getHkey();
								}
								if (dmKey == null) break;
								
								String iKey = null;
								for (Entry<String,String> listmappings : em.getListItemMappings().entrySet()) {
									if (listmappings.getValue().equalsIgnoreCase(dmKey)) {
										iKey = listmappings.getKey();
										break;
									}
								}
								if (iKey == null) break;
								
								IntelAttributeListItem li = null;
								for (IntelAttributeListItem ii : iattribute.getAttributeList()) {
									if (ii.getKeyId().equalsIgnoreCase(iKey)) {
										li = ii;
										break;
									}
								}
								if (li == null) break;
								add = true;
								avalue.setAttributeListItem(li);
								break;
							case NUMERIC:
								if (wo.getNumberValue() == null) break;
								add = true;
								avalue.setNumberValue(wo.getNumberValue());
								break;
							case POSITION:
								//not supported (no position data model attributes)
								add = false;
								break;
							case TEXT:
								if (wo.getStringValue() == null) break;
								add = true;
								avalue.setStringValue(wo.getStringValue());
								break;
							}
							break;
							
						}
					}
				}
				if (add) {
					newEntity.getAttributes().add(avalue);
				}
				
			}
			
			//ensure it has an id even if it is system generated
			IntelEntityAttributeValue idvalue = newEntity.findAttributeValue(newEntity.getEntityType().getIdAttribute());
			if (idvalue == null) {
				IntelAttribute idAttribute = newEntity.getEntityType().getIdAttribute();
				idvalue = new IntelEntityAttributeValue();
				idvalue.setAttribute(idAttribute);
				idvalue.setEntity(newEntity);
				switch(idAttribute.getType()) {
				case BOOLEAN:
					idvalue.setNumberValue(1.0);
					break;
				case DATE:
					idvalue.setDateValue(LocalDate.now());
					break;
				case EMPLOYEE:
					idvalue.setEmployee(SmartDB.getCurrentEmployee());
					break;
				case LIST:
					idvalue.setAttributeListItem(idAttribute.getAttributeList().get(0));
					break;
				case NUMERIC:
					Long cnt = QueryFactory.buildCountQuery(session, IntelEntity.class, 
							new Object[] {"conservationArea", ca}, //$NON-NLS-1$
							new Object[] {"entityType", newEntity.getEntityType()}); //$NON-NLS-1$
					idvalue.setNumberValue(Double.valueOf(cnt+1));
					break;
				case POSITION:
					break;
				case TEXT:
					cnt = QueryFactory.buildCountQuery(session, IntelEntity.class, 
							new Object[] {"conservationArea", ca}, //$NON-NLS-1$
							new Object[] {"entityType", newEntity.getEntityType()}); //$NON-NLS-1$
					
					StringBuilder sb = new StringBuilder();
					sb.append(newEntity.getEntityType().getName());
					sb.append(" - "); //$NON-NLS-1$
					sb.append(cnt+1);
					idvalue.setStringValue(sb.toString());
					break;
				}
				newEntity.getAttributes().add(idvalue);
			}
			
			
			if (newEntity.getEntityType().getIdAttribute().getType() == IntelAttribute.AttributeType.TEXT) {
				//if it's a text attribute make sure it is unique.  We do not ensure uniqueness for
				//other attribute types
				String baseName = newEntity.getIdAttributeAsText();
								
				int loop = 0;
				String id = baseName;
				while(loop < 5000) {
					id = baseName + (loop > 0 ? " " + loop : ""); //$NON-NLS-1$ //$NON-NLS-2$
					
					Long cnt = session.createQuery("SELECT count(*) FROM IntelEntityAttributeValue where stringValue = :it and id.attribute = :attribute and id.entity.entityType = :type and id.entity.conservationArea = :ca", Long.class) //$NON-NLS-1$
						.setParameter("attribute", newEntity.getEntityType().getIdAttribute()) //$NON-NLS-1$
						.setParameter("it", id) //$NON-NLS-1$
						.setParameter("type", newEntity.getEntityType()) //$NON-NLS-1$
						.setParameter("ca", newEntity.getConservationArea()).uniqueResult(); //$NON-NLS-1$
					if (cnt == 0) break;
					loop++;
				}
				for (IntelEntityAttributeValue v : newEntity.getAttributes() ) {
					if (v.getAttribute().equals(newEntity.getEntityType().getIdAttribute())) {
						v.setStringValue(id);
						break;
					}
				}
			}
			
			try{
				session.beginTransaction();
				for (IntelEntityAttachment a : newEntity.getEntityAttachments()) {
					HibernateManager.saveOrMerge(session, a.getAttachment());
				}
				HibernateManager.saveOrMerge(session, newEntity);
				session.getTransaction().commit();
			}catch (Exception ex) {
				EventPlugIn.log(ex.getMessage(), ex);
				return;
			}
		}
		
		if (newEntity != null) {
			IEventBroker eventBroker = EclipseContextFactory.getServiceContext(EventPlugIn.getDefault().getBundle().getBundleContext()).get(IEventBroker.class);
			eventBroker.send(IntelEvents.ENTITY_NEW, Collections.singletonList(newEntity));
		}
	}
	
	private String generateComment(EAction action, EFilter filter, WaypointObservation data) {
		StringBuilder sb = new StringBuilder();
		sb.append(MessageFormat.format( Messages.AdvIntelLabelProvider_CreateActionTypeMsg1, action.getId(), filter.getId()));
		sb.append("\n\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(Messages.AdvIntelLabelProvider_CreateActionTypeMsg2, data.getWaypoint().getSourceId()));
		sb.append("\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(Messages.AdvIntelLabelProvider_CreateActionTypeMsg3, (DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")).format(data.getWaypoint().getDateTime()))); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(Messages.AdvIntelLabelProvider_CreateActionTypeMsg4, data.getWaypoint().getComment()));
		sb.append("\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(Messages.AdvIntelLabelProvider_CreateActionTypeMsg5, data.getCategory().getName()));
		sb.append("\n"); //$NON-NLS-1$
		for (WaypointObservationAttribute a : data.getAttributes()) {
			sb.append(MessageFormat.format("{0}: {1}", a.getAttribute().getName(), a.getAttributeValueAsString(Locale.getDefault()))); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
		}
		return sb.toString();
	}

}
