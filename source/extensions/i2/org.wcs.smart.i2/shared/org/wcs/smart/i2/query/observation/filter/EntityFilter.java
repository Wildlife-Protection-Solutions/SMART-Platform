package org.wcs.smart.i2.query.observation.filter;

import java.util.UUID;

import org.wcs.smart.util.UuidUtils;

/**
 * Filter for specific entity 
 * @author Emily
 *
 */
public class EntityFilter implements IQueryFilter {

	public static EntityFilter create(String key){
		return new EntityFilter(UuidUtils.stringToUuid(key.split(":")[1]));
	}
	
	private UUID uuid;
	
	public EntityFilter(UUID uuid){
		this.uuid = uuid;
	}
	
	public UUID getEntityUuid(){
		return this.uuid;
	}
}
