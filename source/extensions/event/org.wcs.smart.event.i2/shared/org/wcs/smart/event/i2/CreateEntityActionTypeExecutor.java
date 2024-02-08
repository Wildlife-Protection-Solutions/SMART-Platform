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
package org.wcs.smart.event.i2;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.event.i2.IProfileEventLabelProvider.EventMessages;
import org.wcs.smart.event.i2.entity.EntityMapping;
import org.wcs.smart.event.i2.entity.ParameterKeys;
//import org.wcs.smart.event.i2.entity.EntityMapping;
//import org.wcs.smart.event.i2.entity.EntityMapping.Type;
//import org.wcs.smart.event.i2.entity.EntityTypeParameter;
//import org.wcs.smart.event.i2.entity.MappingParameter;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.event.model.IActionTypeExecutor;
import org.wcs.smart.hibernate.QueryFactory;

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
public class CreateEntityActionTypeExecutor implements IActionTypeExecutor {

	public static final String KEY = "org.wcs.smart.profile.i2.newentity"; //$NON-NLS-1$

	public static final Logger LOGGER = Logger.getLogger(CreateEntityActionTypeExecutor.class.getName());
	
	@Override
	public String getKey() {
		return KEY;
	}

	private String getMessage(IProfileEventLabelProvider.EventMessages message, Locale l) {
		return SmartContext.INSTANCE.getClass(IProfileEventLabelProvider.class).getLabel(message, l);
	}
	
	@Override
	public Object performAction(EAction action, EFilter filter, 
			WaypointObservation data, Locale l,
			Employee currentEmployee, Session session) {
		
		EActionParameterValue entityTypeParameter = action.findParameter(ParameterKeys.ENTITYTYPE_PARAM_KEY);
		if (entityTypeParameter == null) {
			LOGGER.log(Level.WARNING, "No entity type parameter specified for create entity action."); //$NON-NLS-1$
			return null; //no entity type specified; cannot create 
		}
		String entityTypeKey = entityTypeParameter.getParameterValue();
		
		EActionParameterValue attributeMappings = action.findParameter(ParameterKeys.MAPPING_PARAM_KEY);
		IntelEntity newEntity = null;
		
		EActionParameterValue profileParam = action.findParameter(ParameterKeys.PROFILE_PARAM_KEY);
		if (profileParam == null || profileParam.getParameterValue().isEmpty()) {
			throw new RuntimeException( getMessage(EventMessages.CreateEntityActionType_ProfileParameterNotSet, l) );
		}
		
		ConservationArea ca = data.getWaypoint().getConservationArea();

		//find profile
		String keyid = profileParam.getParameterValue();
		IntelProfile ip = QueryFactory.buildQuery(session, IntelProfile.class, 
				new Object[] {"keyId", keyid}, //$NON-NLS-1$
				new Object[] {"conservationArea", data.getWaypoint().getConservationArea()}).uniqueResult(); //$NON-NLS-1$
		
		if (ip == null) throw new RuntimeException ( getMessage(EventMessages.CreateEntityActionType_ProfileNotFound, l) );
			
			
		//parse mappings
		List<EntityMapping> mappings = new ArrayList<>();
		if (attributeMappings != null && !attributeMappings.getParameterValue().isEmpty()) {
			mappings.addAll(EntityMapping.parse(attributeMappings.getParameterValue(), session, ca));
		}
		
		IntelEntityType entityType = QueryFactory
				.buildQuery(session, IntelEntityType.class, 
						new Object[] { "keyId", entityTypeKey }, //$NON-NLS-1$
						new Object[] { "conservationArea", data.getWaypoint().getConservationArea() }) //$NON-NLS-1$
				.uniqueResult();
		
		if (entityType == null) {
			LOGGER.log(Level.WARNING,
					MessageFormat.format(
							"No entity type with key {0} found for Conservation Area {1} for create entity action.", //$NON-NLS-1$
							entityTypeKey, ca.getId()));
			return null;
		}

		// ensure profile is valid for entity type
		boolean ok = false;
		for (IntelProfileEntityType ipe : entityType.getProfiles()) {
			if (ipe.getProfile().equals(ip)) {
				ok = true;
			}
		}
		if (!ok) {
			throw new RuntimeException(MessageFormat.format(					
					getMessage(EventMessages.CreateEntityActionType_InvalidProfile, l), 
					ip.getName(), entityType.getName()));
		}

		// create entity
		newEntity = new IntelEntity();
		newEntity.setProfile(ip);
		newEntity.setEntityType(entityType);
		newEntity.setComment(generateComment(action, filter, data, l));
		newEntity.setConservationArea(ca);

		// copy attachments for entity
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
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				continue;
			}

			IntelAttachment ie = new IntelAttachment();
			ie.setConservationArea(ca);
			ie.setCopyFromLocation(aa.getAttachmentFile());
			ie.setFilename(aa.getFilename());
			ie.setDateCreated(LocalDateTime.now());
			ie.setCreatedBy(currentEmployee);

			IntelEntityAttachment iea = new IntelEntityAttachment();
			iea.setAttachment(ie);
			iea.setEntity(newEntity);
			newEntity.getEntityAttachments().add(iea);
		}

		if (!newEntity.getEntityAttachments().isEmpty()) {
			newEntity.setPrimaryAttachment(newEntity.getEntityAttachments().get(0).getAttachment());
		}
		// map entity attributes using mappings specified
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
				LOGGER.log(Level.WARNING, MessageFormat.format(
						getMessage(EventMessages.CreateEntityActionType_AttributeNotFound, l),
						iattribute.getKeyId(), newEntity.getEntityType().getKeyId()));
				continue;
			}

			// check if attribute already mapped
			boolean isDuplicate = false;
			for (IntelEntityAttributeValue v : newEntity.getAttributes()) {
				if (v.getAttribute().equals(iattribute)) {
					isDuplicate = true;
					break;
				}
			}
			if (isDuplicate) {
				// already mapped; skipped this
				continue;
			}

			IntelEntityAttributeValue avalue = new IntelEntityAttributeValue();
			avalue.setAttribute(iattribute);
			avalue.setEntity(newEntity);
			boolean add = true;
			if (em.getType() == EntityMapping.Type.POSITION && iattribute.getType() == AttributeType.POSITION) {
				// map to position
				avalue.setNumberValue(data.getWaypoint().getX());
				avalue.setNumberValue2(data.getWaypoint().getY());
			} else if (em.getType() == EntityMapping.Type.FIXED) {
				// map to fixed value
				switch (iattribute.getType()) {
				case BOOLEAN:
					avalue.setNumberValue(em.getFixedBooleanValue() ? 1.0 : 0.0);
					break;
				case DATE:
					avalue.setDateValue(em.getFixedDateValue());
					break;
				case EMPLOYEE:
					if (em.getFixedEmployee() != null) {
						avalue.setEmployee(em.getFixedEmployee());
					} else {
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
			} else if (em.getType() == EntityMapping.Type.DM) {
				// map to observation value
				add = false;
				if (data.getAttributes() != null) {
					for (WaypointObservationAttribute wo : data.getAttributes()) {
						if (!wo.getAttribute().equals(em.getDataModelAttribute()))
							continue;

						switch (iattribute.getType()) {
						case BOOLEAN:
							if (wo.getNumberValue() == null)
								break;
							add = true;
							avalue.setNumberValue(wo.getNumberValue());
							break;
						case DATE:
							if (wo.getDateValue() == null)
								break;
							add = true;
							avalue.setDateValue(wo.getDateValue());
							break;
						case EMPLOYEE:
							// not supported (no employee data model attributes)
							add = false;
							break;
						case LIST:
							String dmKey = null;
							if (wo.getAttribute().getType() == Attribute.AttributeType.LIST) {
								dmKey = wo.getAttributeListItem().getKeyId();
							} else if (wo.getAttribute().getType() == Attribute.AttributeType.TREE) {
								dmKey = wo.getAttributeTreeNode().getHkey();
							}
							if (dmKey == null)
								break;

							String iKey = null;
							for (Entry<String, String> listmappings : em.getListItemMappings().entrySet()) {
								if (listmappings.getValue().equalsIgnoreCase(dmKey)) {
									iKey = listmappings.getKey();
									break;
								}
							}
							if (iKey == null)
								break;

							IntelAttributeListItem li = null;
							for (IntelAttributeListItem ii : iattribute.getAttributeList()) {
								if (ii.getKeyId().equalsIgnoreCase(iKey)) {
									li = ii;
									break;
								}
							}
							if (li == null)
								break;
							add = true;
							avalue.setAttributeListItem(li);
							break;
						case NUMERIC:
							if (wo.getNumberValue() == null)
								break;
							add = true;
							avalue.setNumberValue(wo.getNumberValue());
							break;
						case POSITION:
							// not supported (no position data model attributes)
							add = false;
							break;
						case TEXT:
							if (wo.getStringValue() == null)
								break;
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

		// ensure it has an id even if it is system generated
		IntelEntityAttributeValue idvalue = newEntity.findAttributeValue(newEntity.getEntityType().getIdAttribute());
		if (idvalue == null) {
			IntelAttribute idAttribute = newEntity.getEntityType().getIdAttribute();
			idvalue = new IntelEntityAttributeValue();
			idvalue.setAttribute(idAttribute);
			idvalue.setEntity(newEntity);
			switch (idAttribute.getType()) {
			case BOOLEAN:
				idvalue.setNumberValue(1.0);
				break;
			case DATE:
				idvalue.setDateValue(LocalDate.now());
				break;
			case EMPLOYEE:
				idvalue.setEmployee(currentEmployee);
				break;
			case LIST:
				idvalue.setAttributeListItem(idAttribute.getAttributeList().get(0));
				break;
			case NUMERIC:
				Long cnt = QueryFactory.buildCountQuery(session, IntelEntity.class,
						new Object[] { "conservationArea", ca }, //$NON-NLS-1$
						new Object[] { "entityType", newEntity.getEntityType() }); //$NON-NLS-1$
				idvalue.setNumberValue(Double.valueOf(cnt + 1));
				break;
			case POSITION:
				break;
			case TEXT:
				cnt = QueryFactory.buildCountQuery(session, IntelEntity.class, new Object[] { "conservationArea", ca }, //$NON-NLS-1$
						new Object[] { "entityType", newEntity.getEntityType() }); //$NON-NLS-1$

				StringBuilder sb = new StringBuilder();
				sb.append(newEntity.getEntityType().getName());
				sb.append(" - "); //$NON-NLS-1$
				sb.append(cnt + 1);
				idvalue.setStringValue(sb.toString());
				break;
			}
			newEntity.getAttributes().add(idvalue);
		}

		if (newEntity.getEntityType().getIdAttribute().getType() == IntelAttribute.AttributeType.TEXT) {
			// if it's a text attribute make sure it is unique. We do not ensure uniqueness
			// for
			// other attribute types
			String baseName = newEntity.getIdAttributeAsText();

			int loop = 0;
			String id = baseName;
			while (loop < 5000) {
				id = baseName + (loop > 0 ? " " + loop : ""); //$NON-NLS-1$ //$NON-NLS-2$

				Long cnt = session.createQuery(
						"SELECT count(*) FROM IntelEntityAttributeValue where stringValue = :it and id.attribute = :attribute and id.entity.entityType = :type and id.entity.conservationArea = :ca", //$NON-NLS-1$
						Long.class).setParameter("attribute", newEntity.getEntityType().getIdAttribute()) //$NON-NLS-1$
						.setParameter("it", id) //$NON-NLS-1$
						.setParameter("type", newEntity.getEntityType()) //$NON-NLS-1$
						.setParameter("ca", newEntity.getConservationArea()).uniqueResult(); //$NON-NLS-1$
				if (cnt == 0)
					break;
				loop++;
			}
			for (IntelEntityAttributeValue v : newEntity.getAttributes()) {
				if (v.getAttribute().equals(newEntity.getEntityType().getIdAttribute())) {
					v.setStringValue(id);
					break;
				}
			}
		}

		try {
			session.beginTransaction();
			session.persist(newEntity);
			for (IntelEntityAttachment a : newEntity.getEntityAttachments()) {
				session.persist(a);
			}
			session.getTransaction().commit();
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			return null;
		}
		
		return newEntity;
	}
	
	private String generateComment(EAction action, EFilter filter, WaypointObservation data, Locale l) {
		StringBuilder sb = new StringBuilder();
		sb.append(MessageFormat.format( getMessage(EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg1, l), action.getId(), filter.getId()));
		sb.append("\n\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(getMessage(EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg2, l), data.getWaypoint().getSourceId()));
		sb.append("\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(getMessage(EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg3, l), (DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")).format(data.getWaypoint().getDateTime()))); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(getMessage(EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg4, l), data.getWaypoint().getComment()));
		sb.append("\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(getMessage(EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg5, l), data.getCategory().getName()));
		sb.append("\n"); //$NON-NLS-1$
		for (WaypointObservationAttribute a : data.getAttributes()) {
			sb.append(MessageFormat.format("{0}: {1}", a.getAttribute().getName(), a.getAttributeValueAsString(Locale.getDefault()))); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
		}
		return sb.toString();
	}

}
