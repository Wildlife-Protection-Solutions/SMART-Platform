package org.wcs.smart.entity;

import java.util.List;

import org.wcs.smart.entity.model.EntityType;

public class EntityTypeCcaaManager {

	private static EntityTypeCcaaManager instance;
	
	private List<EntityType> mergedTypes;
	
	private EntityTypeCcaaManager(){
		
	}
	
	public static EntityTypeCcaaManager getInstance(){
		if (instance == null){
			instance = new EntityTypeCcaaManager();
		}
		return instance;
	}
	
	public List<EntityType> getAllEntityTypes(){
		if (mergedTypes == null){
			synchronized (instance) {
				if (mergedTypes == null){
					mergedTypes = EntityTypeMerger.getEntityTypes();
				}
			}
			
		}
		return mergedTypes;
	}
	
	public EntityType findType(String keyId){
		for (EntityType et : getAllEntityTypes()){
			if (et.getKeyId().equals(keyId)){
				return et;
			}
		}
		return null;
	}
	
}
