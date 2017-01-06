package org.wcs.smart.i2.query;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.wcs.smart.util.UuidUtils;

public class EntityColumn extends AbstractQueryColumn{

	public EntityColumn(String entityName, UUID uuid) {
		super("Is: " + entityName, "entity:" + UuidUtils.uuidToString(uuid));
	}

	public EntityColumn(String entityType, String entityTypeKey) {
		super("Is: "  + entityType, "entitytype:" + entityTypeKey);
	}
	
	@Override
	public String getValue(IResultItem item, Locale l) {
		return null;
	}

	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		return Objects.equals(getKey(), ((AbstractQueryColumn)other).getKey());
	}
	
	public int hashCode(){
		return Objects.hash(getKey());
	}
}
