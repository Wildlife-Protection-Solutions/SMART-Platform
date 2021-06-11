package org.wcs.smart.i2.migrate.entity;


import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.migrate.entity.EntityTypeItem.Type;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.ui.properties.DialogConstants;
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

public class EntityMigrationJob implements IRunnableWithProgress {

	private IntelAttribute idAttribute = null;
	private IntelAttribute positionAttribute = null;
	
	private List<String> warnings = new ArrayList<>();
	
	private HashMap<UUID, IntelAttribute> attributeMapping;
	private HashMap<UUID, IntelRelationshipType> attributeRelationshipMapping;
	private HashMap<UUID, IntelAttributeListItem> listMappings;
	
	private List<EntityTypeMappingRecord> mappings;
	private Entity6Database db;
	
	public EntityMigrationJob(Entity6Database db, List<EntityTypeMappingRecord> mappings) {
		this.mappings = mappings;
		this.db = db;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
	
		attributeMapping = new HashMap<>();
		attributeRelationshipMapping = new HashMap<>();
		listMappings = new HashMap<>();
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (EntityTypeMappingRecord record : mappings) {
					IntelEntityType etype = convertType(record.getEntitytype(), record.getProfile(), session);
					convertEntities(record, etype, session);
				}
				
				int[] index = new int[] {0};
				if (!warnings.isEmpty()) {
					Display.getDefault().syncExec(()->{
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), "Warning", "The following warnings were generated during the conversion process. Do you want to continue?", 
								warnings, new String[] {"Continue", IDialogConstants.CANCEL_LABEL}, 0);
						index[0] = wd.open();
					});
					
				}
				if (index[0] == 1) {
					//cancel
					throw new InterruptedException();
					
				}
				session.getTransaction().commit();
			}catch (InterruptedException ex) {
				session.getTransaction().rollback();
				throw ex;
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw new InvocationTargetException(ex, ex.getMessage());
			}
		}

		return;
	}
	
	private void convertEntities(EntityTypeMappingRecord record, IntelEntityType etype, Session session) throws SQLException {
		
		Collection<EntityItem> items = db.getEntities(record.getEntitytype().getUuid());
		
		HashMap<UUID, IntelEntity> newItems = new HashMap<>();
		List<Object[]> relationshipsToBuild = new ArrayList<>();
		
		for (EntityItem item : items) {
			
			IntelEntity entity = new IntelEntity();
			entity.setConservationArea(etype.getConservationArea());
			entity.setEntityType(etype);
			entity.setComment("Migrated from SMART 6 Entity Module");
			
			UUID dmListItem = item.getDmUuid();
			for (AttributeListItem li : etype.getDmAttribute().getAttributeList()) {
				if (li.getUuid().equals(dmListItem)) {
					entity.setDmAttributeListItem(li);
					break;
				}
			}
			
			if (entity.getDmAttributeListItem() == null) {
				warnings.add("No dm item found for entity");
			}
			
			entity.setProfile(record.getProfile());
			entity.setAttributes(new ArrayList<>());
			session.save(entity);
			
			newItems.put(item.getUuid(), entity);
			
			//configure id attribute
			IntelAttribute idAttribute = etype.getIdAttribute();
			IntelEntityAttributeValue idvalue = new IntelEntityAttributeValue();
			idvalue.setEntity(entity);
			idvalue.setAttribute(idAttribute);
			idvalue.setStringValue(item.getId());
			entity.getAttributes().add(idvalue);
			
			if (record.getEntitytype().getType() == Type.FIXED && item.getX() != null && item.getY() != null) {
				//position attribute
				IntelEntityAttributeValue pvalue = new IntelEntityAttributeValue();
				pvalue.setEntity(entity);
				pvalue.setAttribute(positionAttribute);
				pvalue.setNumberValue(item.getX());
				pvalue.setNumberValue2(item.getY());
				entity.getAttributes().add(pvalue);
			}
			
			//convert attributes
			for (EntityItemAttribute attribute : item.getAttributes()) {
				if (attributeRelationshipMapping.containsKey( attribute.getAttributeUuid() )) {
					//TODO: this is a relationship
					
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
						warnings.add("Cannot find matching entity");
					}else {
						relationshipsToBuild.add(new Object[] {rtype, item.getUuid(), match.getUuid()});
					}
				}
				IntelAttribute ia = attributeMapping.get(attribute.getAttributeUuid());
				if (ia == null) {
					//TODO:
					warnings.add("Attribute not imported for entity; could not be mapped to profile attribute");
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
						//TODO: see if we can find the list value
						IntelAttributeListItem li = listMappings.get(attribute.getUuidValue());
						if (li != null) {
							ea.setAttributeListItem(li);
						}else {
							warnings.add(MessageFormat.format("No list item could be found to map to for entity {1} / attribute {0}.  Value will not be imported", entity.getIdAttributeAsText(), ia.getName()));
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
		
		for(Object[] r : relationshipsToBuild) {
			IntelRelationshipType type = (IntelRelationshipType) r[0];
			UUID one = (UUID) r[1];
			UUID two = (UUID) r[2];
			
			IntelEntity i1 = newItems.get(one);
			IntelEntity i2 = newItems.get(two);
			if (i1 == null || i2 == null) {
				warnings.add("Cannot create relationship");
			}else {
				IntelEntityRelationship relation = new IntelEntityRelationship();
				relation.setRelationshipType(type);
				relation.setSource(Source.ENTITY);
				relation.setSourceEntity(i1);
				relation.setTargetEntity(i2);
				session.save(relation);
			}
		}
	}
	
	private IntelEntityType convertType(EntityTypeItem item, IntelProfile targetProfile, Session session) {
		
		List<IntelEntityType> existingTypes = QueryFactory.buildQuery(session, IntelEntityType.class, new Object[] {"conservationArea", item.getConservationArea()}).list();
		
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
		
		//TODO:
		entityType.setProfiles(new HashSet<>());
		IntelProfileEntityType pe = new IntelProfileEntityType();
		pe.setEntityType(entityType);
		pe.setProfile(targetProfile);
		entityType.getProfiles().add(pe);
		
		
		
		Attribute entityDmAttribute = null;
		if (item.getDmUuid() != null) {
			entityDmAttribute = findAttribute(session, item.getDmUuid(), ca);
			if (entityDmAttribute == null) {
				//TODO: throw an exception or a warning
				warnings.add(MessageFormat.format("The entity type {0} is linked to data model attribute that no longer exists in the Conservation Area. This entity type will not be mapped to any data model attribute.", item.getKeyId()));
			}
			entityType.setDmAttribute(entityDmAttribute);
		}
		entityType.setActiveFilter(null);
		
		IntelAttribute idAttribute = findIdAttribute(session,ca);
		IntelEntityTypeAttribute eIdAttribute = new IntelEntityTypeAttribute();
		eIdAttribute.setAttribute(idAttribute);
		eIdAttribute.setDuplicateCheck(true);
		eIdAttribute.setEntityType(entityType);
		entityType.getAttributes().add(eIdAttribute);
		entityType.setIdAttribute(idAttribute);
		
		session.save(entityType);
		
		IntelEntityTypeAttributeGroup primaryGroup = new IntelEntityTypeAttributeGroup();
		primaryGroup.setEntityType(entityType);
		primaryGroup.setOrder(1);
		primaryGroup.setName("Primary");
		primaryGroup.updateName(SmartDB.getCurrentLanguage(), primaryGroup.getName());
		primaryGroup.updateName(ca.getDefaultLanguage(), primaryGroup.getName());
		session.save(primaryGroup);
		
		eIdAttribute.setAttributeGroup(primaryGroup);

		//attribute
		List<EntityTypeAttributeItem> sorted = new ArrayList<>(item.getAttributes());
		sorted.sort((a,b)->Integer.compare(a.getOrder(),b.getOrder()));
		
		
		List<IntelRelationshipType> allrTypes = QueryFactory.buildQuery(session, IntelRelationshipType.class, 
				new Object[] {"conservationArea", ca}).list();
		
		for (EntityTypeAttributeItem attributeItem : sorted) {
			
			Attribute dmAttribute = findAttribute(session, attributeItem.getDmAttribute(), ca);
			
			if (dmAttribute == null) {
				//TODO: throw an exception or warning
				warnings.add(MessageFormat.format("The entity type {0} has the attribute {1} associated with it that no longer exists in the Conservation Area. The values associated with this attribute will not be imported", item.getKeyId(), attributeItem.getKeyId() ));
			}else if (dmAttribute.getType() == Attribute.AttributeType.TREE ||
					dmAttribute.getType() == Attribute.AttributeType.MLIST) {
					
				//cannot convert tree/mlist not supported
				warnings.add(MessageFormat.format("The entity type {0} attribute {1} is of type tree or multi-list. These attribute types are not supported in the profile module. The values associated with this attribute will not be imported", item.getKeyId(), attributeItem.getKeyId() ));
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
		
		if (item.getType() == Type.FIXED) {
			//add a position attribute
			IntelAttribute position = findPositionAttribute(session, ca);
			IntelEntityTypeAttribute eposition = new IntelEntityTypeAttribute();
			eposition.setEntityType(entityType);
			eposition.setAttribute(position);
			eposition.setAttributeGroup(primaryGroup);
			entityType.getAttributes().add(eposition);
					
		}
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
				new Object[] {"conservationArea", ca},
				new Object[] {"keyId", attribute.getKeyId()}).uniqueResult();
		
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
						warnings.add(MessageFormat.format("The profile list attribute {0} doesn't have a value for key {1} which is required for importing entities. A list list item will be created and added to this attribute.", ia.getName(), ali.getKeyId()));
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
				
				//TODO: validate list
				return ia;
			}
		}
		
		
		if (dmAttribute.getType() == Attribute.AttributeType.MLIST || dmAttribute.getType() == Attribute.AttributeType.TREE) {
			throw new IllegalStateException();
		}
		
		//create a new intel attribute
		List<IntelAttribute> all = QueryFactory.buildQuery(session, IntelAttribute.class, 
				new Object[] {"conservationArea", ca}).list();
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
	
	private IntelAttribute findIdAttribute(Session session, ConservationArea ca) {
		if (idAttribute != null) return idAttribute;
		
		IntelAttribute ia = QueryFactory.buildQuery(session, IntelAttribute.class, 
				new Object[] {"conservationArea", ca},
				new Object[] {"keyId", "id"}).uniqueResult();
		
		if (ia != null && ia.getType().equals(IntelAttribute.AttributeType.TEXT)) {
			idAttribute = ia;
			return idAttribute;
		}
		
		String newKey = "id";
		if (ia != null) {
			//wrong type with id attribute
			List<IntelAttribute> allAttributes = QueryFactory.buildQuery(session, IntelAttribute.class, 
					new Object[] {"conservationArea", ca}).list();
			newKey = DataModelManager.INSTANCE.generateKey("id", allAttributes);
		}
		
		
		//create new attribute
		ia = new IntelAttribute();
		ia.setConservationArea(ca);
		ia.setKeyId(newKey);
		ia.setType(AttributeType.TEXT);
		ia.updateName(ca.getDefaultLanguage(), "ID");
		ia.updateName(SmartDB.getCurrentLanguage(), "ID");
		
		session.save(ia);
		idAttribute = ia;
		
		return idAttribute;
	}
	
	private IntelAttribute findPositionAttribute(Session session, ConservationArea ca) {
		if (positionAttribute != null) return positionAttribute;
		
		IntelAttribute ia = QueryFactory.buildQuery(session, IntelAttribute.class, 
				new Object[] {"conservationArea", ca},
				new Object[] {"keyId", "position"}).uniqueResult();
		
		if (ia != null && ia.getType().equals(IntelAttribute.AttributeType.POSITION)) {
			positionAttribute = ia;
			return positionAttribute;
		}
		
		String newKey = "position";
		if (ia != null) {
			//wrong type with id attribute
			List<IntelAttribute> allAttributes = QueryFactory.buildQuery(session, IntelAttribute.class, 
					new Object[] {"conservationArea", ca}).list();
			newKey = DataModelManager.INSTANCE.generateKey("position", allAttributes);
		}
		
		
		//create new attribute
		ia = new IntelAttribute();
		ia.setConservationArea(ca);
		ia.setKeyId(newKey);
		ia.setType(AttributeType.POSITION);
		ia.updateName(ca.getDefaultLanguage(), "Position");
		ia.updateName(SmartDB.getCurrentLanguage(), "Position");
		
		session.save(ia);
		positionAttribute = ia;
		
		return positionAttribute;
	}



	
}
