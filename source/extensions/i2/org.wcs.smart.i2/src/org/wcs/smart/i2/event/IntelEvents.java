package org.wcs.smart.i2.event;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityType;

public class IntelEvents {

	public static final String ENTITY_ALL = "INTEL_ENTITY/*";
	public static final String ENTITY_NEW = "INTEL_ENTITY/NEW";
	public static final String ENTITY_DELETE = "INTEL_ENTITY/DELETE";
	public static final String ENTITY_MODIFIED = "INTEL_ENTITY/UPDATED";
	

	public static final String ENTITY_TYPE_ALL = "INTEL_ENTITY_TYPE/*";
	public static final String ENTITY_TYPE_NEW = "INTEL_ENTITY_TYPE/NEW";
	public static final String ENTITY_TYPE_DELETE = "INTEL_ENTITY_TYPE/DELETE";
	public static final String ENTITY_TYPE_MODIFIED = "INTEL_ENTITY_TYPE/UPDATED";
	
//	public static final String ENTITY_RELATIONSHIP_ALL = "INTEL_ENTITY_RELATIONSHIP/NEW";
//	public static final String ENTITY_RELATIONSHIP_NEW = "INTEL_ENTITY_RELATIONSHIP/NEW";
//	public static final String ENTITY_RELATIONSHIP_DELETED = "INTEL_ENTITY_RELATIONSHIP/DELETED";
//	public static final String ENTITY_RELATIONSHIP_MODIFIED = "INTEL_ENTITY_RELATIONSHIP/MODIFIED";
	
	public static void fireNewEntity(IntelEntity entity, IEventBroker broker){
		broker.send(ENTITY_NEW, entity);
	}
	
	public static void fireDeleteEntity(IntelEntity entity, IEventBroker broker){
		broker.send(ENTITY_DELETE, entity);
	}

	public static void fireModifiedEntity(IntelEntity entity, IEventBroker broker){
		broker.send(ENTITY_MODIFIED, entity);
	}
	
	public static void fireNewEntityType(IntelEntityType entity, IEventBroker broker){
		broker.send(ENTITY_TYPE_NEW, entity);
	}
	
	public static void fireModifiedEntityType(IntelEntityType entity, IEventBroker broker){
		broker.send(ENTITY_TYPE_MODIFIED, entity);
	}
	
	public static void fireDeleteEntityType(IntelEntityType entity, IEventBroker broker){
		broker.send(ENTITY_TYPE_DELETE, entity);
	}
	
//	public static void fireNewEntityRelationship(IntelEntityRelationship relationship, IEventBroker broker){
//		broker.send(ENTITY_RELATIONSHIP_NEW, relationship);
//	}
//	
//	public static void fireModifiedEntityRelationship(IntelEntityRelationship relationship, IEventBroker broker){
//		broker.send(ENTITY_RELATIONSHIP_MODIFIED, relationship);
//	}
//	
//	public static void fireDeleteEntityRelationship(IntelEntityRelationship relationship, IEventBroker broker){
//		broker.send(ENTITY_RELATIONSHIP_DELETED, relationship);
//	}
}
