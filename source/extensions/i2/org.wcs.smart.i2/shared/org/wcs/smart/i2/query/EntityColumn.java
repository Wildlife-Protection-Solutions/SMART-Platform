package org.wcs.smart.i2.query;

import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;
import org.wcs.smart.i2.query.observation.filter.EntityFilter;
import org.wcs.smart.i2.query.observation.filter.EntityTypeFilter;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.util.UuidUtils;

public class EntityColumn extends AbstractQueryColumn{

	private UUID entityUuid;
	private String entityTypeKey;
	
	public EntityColumn(String entityName, UUID uuid) {
		super(entityName, "entity:" + UuidUtils.uuidToString(uuid));
		this.entityUuid = uuid;
	}

	public EntityColumn(String entityType, String entityTypeKey) {
		super(entityType, "entitytype:" + entityTypeKey);
		this.entityTypeKey = entityTypeKey;
	}
	
	@Override
	public String getValue(IResultItem item, Locale l) {
		if (item instanceof IntelObservationResultItem){
			if (entityUuid != null){
				for (Entry<IQueryFilter, Boolean> filterValue : ((IntelObservationResultItem) item).getFilterValues().entrySet()){
					if (filterValue.getKey() instanceof EntityFilter){
						if (((EntityFilter)filterValue.getKey()).getEntityUuid().equals(entityUuid)){
							return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(filterValue.getValue(), l);
						}
					}
				}
			}else if (entityTypeKey != null){
				for (Entry<IQueryFilter, Boolean> filterValue : ((IntelObservationResultItem) item).getFilterValues().entrySet()){
					if (filterValue.getKey() instanceof EntityTypeFilter){
						if (((EntityTypeFilter)filterValue.getKey()).getTypeKey().equals(entityTypeKey)){
							return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(filterValue.getValue(), l);
						}
					}
				}
			}
		}
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
