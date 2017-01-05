package org.wcs.smart.i2.query.observation.filter;


/**
 * Filter for a specific entity type 
 * @author Emily
 *
 */
public class EntityTypeFilter implements IQueryFilter {

	public static EntityTypeFilter create(String key){
		return new EntityTypeFilter(key.split(":")[1]);
	}
	
	public String typeKey;
	
	public EntityTypeFilter(String typeKey){
		this.typeKey = typeKey;
	}
	
	public String getTypeKey(){
		return this.typeKey;
	}

}
