/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.entity.ui.typelist.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;

/**
 * Editor input for entity types.
 * 
 * @author Emily
 *
 */
public class EntityTypeEditorInput implements IEditorInput {

	private byte[] uuid;
	private String id;
	private String name;
	
	/**
	 * Creates a new input
	 * 
	 * @param entityType uuid
	 * @param id the id
	 * @param name the name
	 */
	public EntityTypeEditorInput(byte[] uuid, String id, String name){
		this.uuid = uuid;
		this.id = id;
		this.name = name;
	}
	
	/**
	 * 
	 * @return the uuid
	 * of the entity type represented
	 */
	public byte[] getUuid(){
		return this.uuid;
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
		return EntityPlugIn.getDefault().getImageRegistry().getDescriptor(EntityPlugIn.ENTITY_TYPE_ICON);
	}

	@Override
	public String getName() {
		return name + " [" + id + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return Messages.EntityTypeEditorInput_EntityTypeEditorTooltip;
	}

}
