package org.wcs.smart.i2.ui.editors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;

public class EntityComparisonInput  implements IEditorInput{

	public List<UUID> entitiesToCompare;
	public IntelEntityType type;
	
	public EntityComparisonInput(IntelEntityType type){
		this.type = type;
		entitiesToCompare = new ArrayList<>();
	}
	
	public EntityComparisonInput(IntelEntityType type, List<IntelEntity> entities){
		this(type);
		for (IntelEntity e : entities){
			if (e.getEntityType().equals(type)) entitiesToCompare.add(e.getUuid());
		}
	}

	
	public IntelEntityType getType(){
		return this.type;
	}
	
	public List<UUID> getEntities(){
		return this.entitiesToCompare;
	}
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public void addEntity(IntelEntity entity){
		if (entity.getEntityType().equals(type) && !entitiesToCompare.contains(entity.getUuid())) entitiesToCompare.add(entity.getUuid());
	}
	public void removeEntity(IntelEntity entity){
		entitiesToCompare.remove(entity.getUuid());
	}
	
	@Override
	public String getName() {
		return MessageFormat.format("Compare Entities ({0}) : {1}", type.getName(), entitiesToCompare.size());
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return null;
	}

}
