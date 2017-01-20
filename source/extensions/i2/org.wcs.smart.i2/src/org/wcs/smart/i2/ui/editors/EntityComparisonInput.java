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

/**
 * Input for comparing entities.
 * 
 * @author Emily
 *
 */
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
	
	@SuppressWarnings("rawtypes")
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
		return MessageFormat.format("Compare {0} ({1})", type.getName(), entitiesToCompare.size());
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
