/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.entity;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.migrate.MigratePlugin;
import org.wcs.smart.i2.migrate.entity.EntityItem.Status;
import org.wcs.smart.i2.migrate.entity.EntityTypeItem.Type;
import org.wcs.smart.i2.migrate.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityRelationship.Source;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;

/**
 * Job for migrating smart6 entities to smart 7 profile entities
 * 
 * @author Emily
 *
 */
public class EntityMigrationJob implements IRunnableWithProgress {

	//link from cauuid to attribute
	private HashMap<UUID, IntelAttribute> idAttribute = null;
	private HashMap<UUID, IntelAttribute> positionAttribute = null;
	private HashMap<UUID, IntelAttribute> statusAttribute = null;
	
	private List<String> warnings = new ArrayList<>();
	
	private HashMap<UUID, IntelAttribute> attributeMapping;
	private HashMap<UUID, IntelRelationshipType> attributeRelationshipMapping;
	private HashMap<UUID, IntelAttributeListItem> listMappings;
	private Map<ConservationArea, Employee> userMapping;
	
	private List<EntityTypeMappingRecord> mappings;
	private IEntityDatabase db;
	
	private List<IntelEntityType> newTypes;
	private int totalEntities = 0;
	
	private String taskName = Messages.EntityMigrationJob_TaskName;
	private String entityComment = Messages.EntityMigrationJob_EntityMigrationComment;
	private String warningListItemAdded = Messages.EntityMigrationJob_WarningListItemAdded;
	
	public EntityMigrationJob(IEntityDatabase db, List<EntityTypeMappingRecord> mappings, Map<ConservationArea, Employee> userMapping) {
		this.mappings = mappings;
		this.db = db;
		this.userMapping = userMapping;
	}

	public int getSavedEntities() {
		return totalEntities;
	}
	
	public List<IntelEntityType> getSavedTypes(){
		return newTypes;
	}
	
	public void setLabels(String taskName, String entityComment, String warningListItem) {
		this.taskName = taskName;
		this.entityComment = entityComment;
		this.warningListItemAdded = warningListItem;
	}
	
	
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
	
		monitor.beginTask(taskName, mappings.size()*2 + 2);
		idAttribute = new HashMap<>();
		positionAttribute = new HashMap<>();
		statusAttribute = new HashMap<>();
		
		attributeMapping = new HashMap<>();
		attributeRelationshipMapping = new HashMap<>();
		listMappings = new HashMap<>();
		newTypes = new ArrayList<>();
		totalEntities = 0;
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (EntityTypeMappingRecord record : mappings) {
					monitor.subTask(MessageFormat.format(Messages.EntityMigrationJob_SubTask, record.getEntitytype().getKeyId()));
					IntelEntityType etype = convertType(record.getEntitytype(), record.getProfile(), session);
					totalEntities += convertEntities(record, etype, session);
					newTypes.add(etype);
					session.flush();
					monitor.worked(1);
				}
				
				int[] index = new int[] {0};
				if (!warnings.isEmpty()) {
					Display.getDefault().syncExec(()->{
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), Messages.EntityMigrationJob_WarningTitle, 
								Messages.EntityMigrationJob_WarningMsg, 
								warnings, new String[] {Messages.EntityMigrationJob_ContinueButton, IDialogConstants.CANCEL_LABEL}, 0);
						index[0] = wd.open();
					});
					
				}
				if (index[0] == 1) {
					//cancel
					throw new InterruptedException();
					
				}
				
				session.flush();
				
				monitor.worked(1);
				session.getTransaction().commit();
			}catch (InterruptedException ex) {
				session.getTransaction().rollback();
				throw ex;
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw new InvocationTargetException(ex, ex.getMessage());
			}
		}

		//create BIRT templates
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (IntelEntityType type : newTypes) {
					
					monitor.subTask(MessageFormat.format(Messages.EntityMigrationJob_templatesubtask, type.getName() ) );
					try {
						type = session.get(IntelEntityType.class, type.getUuid());
						IntelReportManager.INSTANCE.generateTemplate(type);
						monitor.worked(1);
					}catch (Exception ex) {
						MigratePlugin.log(ex.getMessage(), ex);
					}
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				MigratePlugin.log(ex.getMessage(), ex);
			}
		}
		monitor.worked(1);
		return;
	}
	
	private int convertEntities(EntityTypeMappingRecord record, IntelEntityType etype, Session session) throws SQLException {
		
		Collection<EntityItem> items = db.getEntities(record.getEntitytype().getUuid());
		
		HashMap<UUID, IntelEntity> newItems = new HashMap<>();
		List<Object[]> relationshipsToBuild = new ArrayList<>();
		
		int cnt = 0;
		boolean dmModified = false;
		
		for (EntityItem item : items) {
			
			IntelEntity entity = new IntelEntity();
			entity.setConservationArea(etype.getConservationArea());
			entity.setEntityType(etype);
			entity.setComment(entityComment);
			entity.setCreatedBy(userMapping.get(etype.getConservationArea()));
			entity.setLastModifiedBy(userMapping.get(etype.getConservationArea()));
			entity.setDateCreated(LocalDateTime.now());
			entity.setDateModified(LocalDateTime.now());
			UUID dmListItem = item.getDmUuid();
			if (etype.getDmAttribute() != null) {
				for (AttributeListItem li : etype.getDmAttribute().getAttributeList()) {
					if (li.getUuid() != null && li.getUuid().equals(dmListItem)) {
						entity.setDmAttributeListItem(li);
						break;
					}
				}
				
				if (entity.getDmAttributeListItem() == null) {
					String key = item.getId().toLowerCase();
					key = DataModelManager.INSTANCE.generateKey(key, etype.getDmAttribute().getAttributeList());
					
					AttributeListItem li = new AttributeListItem();
					li.setKeyId(key);
					li.setListOrder(etype.getDmAttribute().getAttributeList().size()+1);
					li.updateName(etype.getConservationArea().getDefaultLanguage(), item.getId());
					li.updateName(SmartDB.getCurrentLanguage(), item.getId());
					li.setAttribute(etype.getDmAttribute());
					li.setIsActive(item.getStatus() == null || item.getStatus() == Status.ACTIVE);
					etype.getDmAttribute().getAttributeList().add(li);
					
					entity.setDmAttributeListItem(li);
					dmModified = true;
					warnings.add(MessageFormat.format(Messages.EntityMigrationJob_ErrorListItemNotFound, item.getId(), etype.getName(), etype.getDmAttribute().getName() ));
					
					session.flush();
				}
			}
			
			
			
			entity.setProfile(record.getProfile());
			entity.setAttributes(new ArrayList<>());
			session.save(entity);
			cnt++;
			newItems.put(item.getUuid(), entity);
			
			//configure id attribute
			IntelAttribute idAttribute = etype.getIdAttribute();
			IntelEntityAttributeValue idvalue = new IntelEntityAttributeValue();
			idvalue.setEntity(entity);
			idvalue.setAttribute(idAttribute);
			idvalue.setStringValue(item.getId());
			entity.getAttributes().add(idvalue);
			
			IntelAttribute caStatusAttribute = statusAttribute.get(entity.getConservationArea().getUuid());
			
			if (item.getStatus() != null && statusAttribute != null) {
				IntelEntityAttributeValue statusValue = new IntelEntityAttributeValue();
				statusValue.setEntity(entity);
				statusValue.setAttribute(caStatusAttribute);
				for (IntelAttributeListItem i : caStatusAttribute.getAttributeList()) {
					if (item.getStatus() == Status.ACTIVE && i.getKeyId().equalsIgnoreCase(Status.ACTIVE.name().toLowerCase())) {
						statusValue.setAttributeListItem(i);
						break;
					}else if (item.getStatus() == Status.INACTIVE && i.getKeyId().equalsIgnoreCase(Status.INACTIVE.name().toLowerCase())) {
						statusValue.setAttributeListItem(i);
						break;
					}
				}
				entity.getAttributes().add(statusValue);
			} 
			
			if (record.getEntitytype().getType() == Type.FIXED && item.getX() != null && item.getY() != null) {
				//position attribute
				IntelAttribute caPositionAttribute = positionAttribute.get(entity.getConservationArea().getUuid());

				IntelEntityAttributeValue pvalue = new IntelEntityAttributeValue();
				pvalue.setEntity(entity);
				pvalue.setAttribute(caPositionAttribute);
				pvalue.setNumberValue(item.getX());
				pvalue.setNumberValue2(item.getY());
				entity.getAttributes().add(pvalue);
			}
			
			//convert attributes
			for (EntityItemAttribute attribute : item.getAttributes()) {
				if (attributeRelationshipMapping.containsKey( attribute.getAttributeUuid() )) {
					
					IntelRelationshipType rtype = attributeRelationshipMapping.get( attribute.getAttributeUuid() );
					//this maps to another
					EntityItem match = null;
					for (EntityItem t : items) {
						if (t.getDmUuid().equals(attribute.getUuidValue())) {
							match = t;
							break;
						}
					}
					if (match == null) {
						warnings.add(MessageFormat.format(Messages.EntityMigrationJob_ErrorMatchingEntityNotFound, item.getId(), rtype.getName()));
					}else {
						relationshipsToBuild.add(new Object[] {rtype, item.getUuid(), match.getUuid()});
					}
				}else {
					IntelAttribute ia = attributeMapping.get(attribute.getAttributeUuid());
					if (ia == null) {
						//warning already exists for entity type
						//warnings.add(MessageFormat.format("Entity Conversion: Attribute {0} not imported for entity type {1}", smart6attributes.get(attribute.getAttributeUuid()), entity.getEntityType().getName()));
					}else {
						IntelEntityAttributeValue ea = new IntelEntityAttributeValue();
						ea.setAttribute(ia);
						ea.setEntity(entity);
						boolean add = true;
						switch(ia.getType()) {
						case BOOLEAN:
							ea.setNumberValue(attribute.getDoubleValue());
							break;
						case DATE:
							ea.setStringValue(attribute.getStringValue());
							break;
						case EMPLOYEE:
							//not supported
							break;
						case LIST:
							IntelAttributeListItem li = listMappings.get(attribute.getUuidValue());
							if (li != null) {
								ea.setAttributeListItem(li);
							}else {
								warnings.add(MessageFormat.format(Messages.EntityMigrationJob_ErrorListItemNotFound2, entity.getIdAttributeAsText(), ia.getName()));
								add = false;
							}
							break;
						case NUMERIC:
							ea.setNumberValue(attribute.getDoubleValue());
							break;
						case POSITION:
							//not supported
							break;
						case TEXT:
							ea.setStringValue(attribute.getStringValue());
							break;
						default:
							break;
						
						}
						if (add) {
							entity.getAttributes().add(ea);
						}		
					}
				}
			}
		}
		
		for(Object[] r : relationshipsToBuild) {
			IntelRelationshipType type = (IntelRelationshipType) r[0];
			UUID one = (UUID) r[1];
			UUID two = (UUID) r[2];
			
			IntelEntity i1 = newItems.get(one);
			IntelEntity i2 = newItems.get(two);
			if (i1 == null || i2 == null) {
				warnings.add(Messages.EntityMigrationJob_ErrorRelatinshipNotFound);
			}else {
				IntelEntityRelationship relation = new IntelEntityRelationship();
				relation.setRelationshipType(type);
				relation.setSource(Source.ENTITY);
				relation.setSourceEntity(i1);
				relation.setTargetEntity(i2);
				session.save(relation);
			}
		}
		
		if (dmModified) DataModelManager.INSTANCE.updateLastModified(session);
		return cnt;
	}
	
	private IntelEntityType convertType(EntityTypeItem item, IntelProfile targetProfile, Session session) {
		
		List<IntelEntityType> existingTypes = QueryFactory.buildQuery(session, IntelEntityType.class, 
				new Object[] {"conservationArea", item.getConservationArea()}).list(); //$NON-NLS-1$
		
		IntelEntityType entityType = new IntelEntityType();
		
		ConservationArea ca = session.get(ConservationArea.class, item.getConservationArea().getUuid());
		
		entityType.setAttributes(new ArrayList<>());
		entityType.setConservationArea(ca);
		
		entityType.setIcon(null);
		
		String keyid = DataModelManager.INSTANCE.generateKey(item.getKeyId(), existingTypes);
		entityType.setKeyId(keyid);
		entityType.setNames(new HashSet<>());
		for (Entry<UUID,String> name : item.getNames().entrySet()) {
			Language l = session.get(Language.class, name.getKey());
			if (l != null && l.getCa().equals(ca)) {
				entityType.updateName(l, name.getValue());
				if (l.isDefault()) entityType.setName(name.getValue());
			}
		}
		if (entityType.getNames().isEmpty()) {
			if (item.getNames().isEmpty()) {
				entityType.setName(keyid);
				entityType.updateName(ca.getDefaultLanguage(), keyid);
			}
		}
		if (entityType.findNameNull(ca.getDefaultLanguage()) == null) {
			entityType.updateName(ca.getDefaultLanguage(), keyid);
		}
		
		entityType.setProfiles(new HashSet<>());
		IntelProfileEntityType pe = new IntelProfileEntityType();
		pe.setEntityType(entityType);
		pe.setProfile(targetProfile);
		entityType.getProfiles().add(pe);
		
		
		Attribute entityDmAttribute = null;
		if (item.getDmUuid() != null) {
			entityDmAttribute = findAttribute(session, item.getDmUuid(), ca);
			if (entityDmAttribute == null) {
				warnings.add(MessageFormat.format(Messages.EntityMigrationJob_ErrorDataModelAttributeNotFound, item.getKeyId()));
			}
			entityType.setDmAttribute(entityDmAttribute);
		}
		entityType.setActiveFilter(null);
		
		//id attribute
		IntelAttribute idAttribute = findIdAttribute(session,ca);
		IntelEntityTypeAttribute eIdAttribute = new IntelEntityTypeAttribute();
		eIdAttribute.setAttribute(idAttribute);
		eIdAttribute.setDuplicateCheck(true);
		eIdAttribute.setEntityType(entityType);
		entityType.getAttributes().add(eIdAttribute);
		entityType.setIdAttribute(idAttribute);
		
		//status attributes
		IntelAttribute status = findStatusAttribute(session, ca);
		IntelEntityTypeAttribute sattribute = new IntelEntityTypeAttribute();
		sattribute.setEntityType(entityType);
		sattribute.setAttribute(status);
		entityType.getAttributes().add(sattribute);
		
		
		if (entityDmAttribute != null && status != null) {
			entityType.setActiveFilter("a:" + IntelAttribute.AttributeType.LIST.key + ":" + status.getKeyId() + " " + Operator.EQUALS.asSmartValue() + " " + Status.ACTIVE.name().toLowerCase()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		
		session.save(entityType);
		session.flush();
		
		IntelEntityTypeAttributeGroup primaryGroup = new IntelEntityTypeAttributeGroup();
		primaryGroup.setEntityType(entityType);
		primaryGroup.setOrder(1);
		primaryGroup.setName(Messages.EntityMigrationJob_PrimaryAttributeGroupName);
		primaryGroup.updateName(SmartDB.getCurrentLanguage(), primaryGroup.getName());
		primaryGroup.updateName(ca.getDefaultLanguage(), primaryGroup.getName());
		session.save(primaryGroup);
		session.flush();
		
		eIdAttribute.setAttributeGroup(primaryGroup);
		sattribute.setAttributeGroup(primaryGroup);
		
		//attribute
		List<EntityTypeAttributeItem> sorted = new ArrayList<>(item.getAttributes());
		sorted.sort((a,b)->Integer.compare(a.getOrder(),b.getOrder()));
		
		
		List<IntelRelationshipType> allrTypes = QueryFactory.buildQuery(session, IntelRelationshipType.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		
		for (EntityTypeAttributeItem attributeItem : sorted) {
			
			Attribute dmAttribute = findAttribute(session, attributeItem.getDmAttribute(), ca);
			
			if (dmAttribute == null) {
				warnings.add(MessageFormat.format(Messages.EntityMigrationJob_ErrorAttributeNotFound, item.getKeyId(), attributeItem.getKeyId() ));
			}else if (dmAttribute.getType() == Attribute.AttributeType.TREE ||
					dmAttribute.getType() == Attribute.AttributeType.MLIST) {
					
				//cannot convert tree/mlist not supported
				warnings.add(MessageFormat.format(Messages.EntityMigrationJob_ErrorTreeMultiListAttributesNotSupported, item.getKeyId(), attributeItem.getKeyId() ));
			}else {
				if (dmAttribute.equals(entityDmAttribute)) {
					
					//need to make a relationship type for this link
					IntelRelationshipType rType = new IntelRelationshipType();
					rType.setSourceEntityType(entityType);
					rType.setTargetEntityType(entityType);
					rType.setKeyId(DataModelManager.INSTANCE.generateKey(attributeItem.getKeyId(), allrTypes));
					rType.setName(keyid);
					
					for (Label l : entityDmAttribute.getNames()) {
						rType.updateName(l.getLanguage(),l.getValue());
					}
					
					for (Entry<UUID,String> name : attributeItem.getNames().entrySet()) {
						Language l = session.get(Language.class, name.getKey());
						if (l != null && l.getCa().equals(ca)) {
							rType.updateName(l, name.getValue());
						}
					}
					if (rType.getNames().isEmpty()) {
						rType.updateName(ca.getDefaultLanguage(), keyid);
					}
					if (rType.findNameNull(ca.getDefaultLanguage()) == null) {
						rType.updateName(ca.getDefaultLanguage(), keyid);
					}
					
					rType.setSourceProfile(targetProfile);
					rType.setTargetProfile(targetProfile);
					rType.setConservationArea(ca);
					
					session.save(rType);
					
					attributeRelationshipMapping.put(attributeItem.getUuid(), rType);
				}else {
					IntelAttribute ia = searchForAttribute(session, attributeItem, dmAttribute, ca);

					if (ia != null) {
						boolean canadd = true;
						for (IntelEntityTypeAttribute current : entityType.getAttributes()) {
							if (current.getAttribute().equals(ia)) {
								warnings.add(MessageFormat.format(Messages.EntityMigrationJob_DuplicateAttributes, ia.getName(), ia.getKeyId()));
								canadd = false;
								break;
							}
						}
						
						if (canadd) {
							attributeMapping.put(attributeItem.getUuid(), ia);
							
							IntelEntityTypeAttribute iea = new IntelEntityTypeAttribute();
							iea.setAttribute(ia);
							iea.setDuplicateCheck(false);
							iea.setEntityType(entityType);
							if (attributeItem.isPrimary()) {
								iea.setAttributeGroup(primaryGroup);
							}
							iea.setOrder(attributeItem.getOrder());
							
							entityType.getAttributes().add(iea);
						}
					}
				}
			}

		}
		session.flush();
		if (item.getType() == Type.FIXED) {
			//add a position attribute
			IntelAttribute position = findPositionAttribute(session, ca);
			IntelEntityTypeAttribute eposition = new IntelEntityTypeAttribute();
			eposition.setEntityType(entityType);
			eposition.setAttribute(position);
			eposition.setAttributeGroup(primaryGroup);
			entityType.getAttributes().add(eposition);
					
		}
		session.flush();
		//re-sort attribute
		int order = 1;
		for (IntelEntityTypeAttribute ia : entityType.getAttributes()) {
			ia.setOrder(order++);
		}
		session.save(entityType);
		return entityType;	
	}
	
	private IntelAttribute searchForAttribute(Session session, EntityTypeAttributeItem attribute, Attribute dmAttribute, ConservationArea ca) {

		IntelAttribute ia = QueryFactory.buildQuery(session, IntelAttribute.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", attribute.getKeyId()}).uniqueResult(); //$NON-NLS-1$
		
		if (ia != null) {
			
			if (ia.getType() == IntelAttribute.AttributeType.BOOLEAN && dmAttribute.getType() == Attribute.AttributeType.BOOLEAN ||
					ia.getType() == IntelAttribute.AttributeType.NUMERIC && dmAttribute.getType() == Attribute.AttributeType.NUMERIC ||
					ia.getType() == IntelAttribute.AttributeType.DATE && dmAttribute.getType() == Attribute.AttributeType.DATE ||
					ia.getType() == IntelAttribute.AttributeType.TEXT && dmAttribute.getType() == Attribute.AttributeType.TEXT) {
				return ia;
			}
			if (ia.getType() == IntelAttribute.AttributeType.LIST && dmAttribute.getType() == Attribute.AttributeType.LIST) {
				
				//map all items from the dm list to ath intel list
				HashMap<String, IntelAttributeListItem> lmapping = new HashMap<>();
				ia.getAttributeList().forEach(e->lmapping.put(e.getKeyId(), e));
				
				for (AttributeListItem ali : dmAttribute.getAttributeList()) {
					IntelAttributeListItem ili = lmapping.get(ali.getKeyId());
					if (ili == null) {
						warnings.add(MessageFormat.format(warningListItemAdded, ia.getName(), ali.getKeyId()));
						//create a new list item for this key
						
						IntelAttributeListItem iali = new IntelAttributeListItem();
						iali.setAttribute(ia);
						ia.getAttributeList().add(iali);
						iali.setKeyId(ali.getKeyId());
						iali.setOrder(ali.getListOrder());
						for (Label l : ali.getNames()) {
							iali.updateName(l.getLanguage(),l.getValue());
						}
						listMappings.put(ali.getUuid(), iali);
						
					}else {
						listMappings.put(ali.getUuid(),ili);
					}
				}
				return ia;
			}
		}
		
		
		if (dmAttribute.getType() == Attribute.AttributeType.MLIST || dmAttribute.getType() == Attribute.AttributeType.TREE) {
			throw new IllegalStateException();
		}
		
		//create a new intel attribute
		List<IntelAttribute> all = QueryFactory.buildQuery(session, IntelAttribute.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		String keyId = DataModelManager.INSTANCE.generateKey(attribute.getKeyId(), all);
		
		ia = new IntelAttribute();
		ia.setConservationArea(ca);
		ia.setKeyId(keyId);
		
		for (Label l : dmAttribute.getNames()) {
			ia.updateName(l.getLanguage(),l.getValue());
		}
		for (Entry<UUID, String> aname : attribute.getNames().entrySet()) {
			Language l = session.get(Language.class, aname.getKey());
			if (l != null && l.getCa().equals(ca)) {
				ia.updateName(l, aname.getValue());
			}
		}
		
		if (dmAttribute.getType() == Attribute.AttributeType.BOOLEAN) ia.setType(IntelAttribute.AttributeType.BOOLEAN);
		else if (dmAttribute.getType() == Attribute.AttributeType.DATE) ia.setType(IntelAttribute.AttributeType.DATE);
		else if (dmAttribute.getType() == Attribute.AttributeType.LIST) ia.setType(IntelAttribute.AttributeType.LIST);
		else if (dmAttribute.getType() == Attribute.AttributeType.NUMERIC) ia.setType(IntelAttribute.AttributeType.NUMERIC);
		else if (dmAttribute.getType() == Attribute.AttributeType.TEXT) ia.setType(IntelAttribute.AttributeType.TEXT);
	
		if (ia.getType() == AttributeType.LIST) {
			ia.setAttributeList(new ArrayList<>());
			for (AttributeListItem ali : dmAttribute.getAttributeList()) {
				IntelAttributeListItem iali = new IntelAttributeListItem();
				iali.setAttribute(ia);
				ia.getAttributeList().add(iali);
				iali.setKeyId(ali.getKeyId());
				iali.setOrder(ali.getListOrder());
				for (Label l : ali.getNames()) {
					iali.updateName(l.getLanguage(),l.getValue());
				}
				listMappings.put(ali.getUuid(), iali);
			}
		}
		session.save(ia);
		return ia;
	}
	
	
	private Attribute findAttribute(Session session, UUID dmUuid, ConservationArea ca) {
		Attribute a = session.get(Attribute.class, dmUuid);
		if (a == null) return null;
		if (!a.getConservationArea().equals(ca)) return null;
		return a;
	}
	
	/**
	 * Find.create id attribute for entity type
	 * @param session
	 * @param ca
	 * @return
	 */
	private IntelAttribute findIdAttribute(Session session, ConservationArea ca) {
		if (idAttribute.get(ca.getUuid()) != null) return idAttribute.get(ca.getUuid());
		
		IntelAttribute ia = QueryFactory.buildQuery(session, IntelAttribute.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", "id"}).uniqueResult(); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (ia != null && ia.getType().equals(IntelAttribute.AttributeType.TEXT)) {
			idAttribute.put(ca.getUuid(), ia);
			return ia;
		}
		
		String newKey = "id"; //$NON-NLS-1$
		if (ia != null) {
			//wrong type with id attribute
			List<IntelAttribute> allAttributes = QueryFactory.buildQuery(session, IntelAttribute.class, 
					new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
			newKey = DataModelManager.INSTANCE.generateKey("id", allAttributes); //$NON-NLS-1$
		}
		
		
		//create new attribute
		ia = new IntelAttribute();
		ia.setConservationArea(ca);
		ia.setKeyId(newKey);
		ia.setType(AttributeType.TEXT);
		ia.updateName(ca.getDefaultLanguage(), Messages.EntityMigrationJob_IDName);
		ia.updateName(SmartDB.getCurrentLanguage(), Messages.EntityMigrationJob_IDName);
		ia.setName(Messages.EntityMigrationJob_IDName);
		
		session.save(ia);
		
		idAttribute.put(ca.getUuid(), ia);
		return ia;
	}
	
	/**
	 * Find/create position attribute for entity types
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	private IntelAttribute findPositionAttribute(Session session, ConservationArea ca) {
		if (positionAttribute.get(ca.getUuid()) != null) return positionAttribute.get(ca.getUuid());

		
		IntelAttribute ia = QueryFactory.buildQuery(session, IntelAttribute.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", "position"}).uniqueResult();  //$NON-NLS-1$//$NON-NLS-2$
		
		if (ia != null && ia.getType().equals(IntelAttribute.AttributeType.POSITION)) {
			positionAttribute.put(ca.getUuid(), ia);
			return ia;
		}
		
		String newKey = "position"; //$NON-NLS-1$
		if (ia != null) {
			//wrong type with id attribute
			List<IntelAttribute> allAttributes = QueryFactory.buildQuery(session, IntelAttribute.class, 
					new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
			newKey = DataModelManager.INSTANCE.generateKey("position", allAttributes); //$NON-NLS-1$
		}
		
		
		//create new attribute
		ia = new IntelAttribute();
		ia.setConservationArea(ca);
		ia.setKeyId(newKey);
		ia.setType(AttributeType.POSITION);
		ia.updateName(ca.getDefaultLanguage(), Messages.EntityMigrationJob_PositionName);
		ia.updateName(SmartDB.getCurrentLanguage(), Messages.EntityMigrationJob_PositionName);
		ia.setName(Messages.EntityMigrationJob_PositionName);
		
		session.save(ia);
		positionAttribute.put(ca.getUuid(), ia);
		return ia;
	}

	/**
	 * Find/create status attribute 
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	private IntelAttribute findStatusAttribute(Session session, ConservationArea ca) {
		if (statusAttribute.get(ca.getUuid()) != null) return statusAttribute.get(ca.getUuid());
		
		IntelAttribute ia = QueryFactory.buildQuery(session, IntelAttribute.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", "status"}).uniqueResult(); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (ia != null && ia.getType().equals(IntelAttribute.AttributeType.LIST)) {
			
			//look for active/inactive keys
			boolean hasActive = false;
			boolean hasInactive = false;
			for (IntelAttributeListItem li : ia.getAttributeList()) {
				if (li.getKeyId().equalsIgnoreCase(Status.ACTIVE.name())) hasActive = true;
				if (li.getKeyId().equalsIgnoreCase(Status.INACTIVE.name())) hasInactive = true;
			}
			
			if (hasActive && hasInactive) {
				statusAttribute.put(ca.getUuid(), ia);
				return ia;
			}
		}
		
		String newKey = "status"; //$NON-NLS-1$
		if (ia != null) {
			//wrong type with id attribute
			List<IntelAttribute> allAttributes = QueryFactory.buildQuery(session, IntelAttribute.class, 
					new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
			newKey = DataModelManager.INSTANCE.generateKey(newKey, allAttributes);
		}
		
		
		//create new attribute
		ia = new IntelAttribute();
		ia.setConservationArea(ca);
		ia.setKeyId(newKey);
		ia.setType(AttributeType.LIST);
		ia.updateName(ca.getDefaultLanguage(), Messages.EntityMigrationJob_StatusName);
		ia.updateName(SmartDB.getCurrentLanguage(), Messages.EntityMigrationJob_StatusName);
		ia.setName(Messages.EntityMigrationJob_StatusName);
		ia.setAttributeList(new ArrayList<>());
		
		IntelAttributeListItem active = new IntelAttributeListItem();
		active.setKeyId(Status.ACTIVE.name().toLowerCase());
		active.updateName(ca.getDefaultLanguage(), Messages.EntityMigrationJob_ActiveName);
		active.updateName(SmartDB.getCurrentLanguage(), Messages.EntityMigrationJob_ActiveName);
		active.setName(Messages.EntityMigrationJob_ActiveName);
		active.setAttribute(ia);
		ia.getAttributeList().add(active);
		
		IntelAttributeListItem inactive = new IntelAttributeListItem();
		inactive.setKeyId(Status.INACTIVE.name().toLowerCase());
		inactive.updateName(ca.getDefaultLanguage(), Messages.EntityMigrationJob_InactiveName);
		inactive.updateName(SmartDB.getCurrentLanguage(), Messages.EntityMigrationJob_InactiveName);
		inactive.setName(Messages.EntityMigrationJob_InactiveName);
		inactive.setAttribute(ia);
		ia.getAttributeList().add(inactive);
		
		session.save(ia);
		statusAttribute.put(ca.getUuid(), ia);
		return ia;
	}

	
}
