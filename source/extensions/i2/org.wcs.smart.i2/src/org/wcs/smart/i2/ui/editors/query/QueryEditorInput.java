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
package org.wcs.smart.i2.ui.editors.query;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;

/**
 * Editor input for query editor.
 * 
 * @author Emily
 *
 */
public class QueryEditorInput implements IEditorInput{

	private String name;
	private UUID uuid;
	
	private boolean isNew;
	
	public QueryEditorInput(String name, UUID uuid){
		this.name = name;
		this.uuid = uuid;
		if (this.uuid == null){
			isNew = true;
			//generate temporary random uuid
			this.uuid = UUID.randomUUID();
		}
	}
	
	public QueryEditorInput(IntelRecordObservationQuery record){
		this(record.getName(), record.getUuid());
	}
	
	public void setUuid(UUID uuid){
		this.isNew = false;
		this.uuid = uuid;
	}
	
	public boolean isNew(){
		return this.isNew;
	}
	public UUID getUuid(){
		return this.uuid;
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

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return null;
	}
	
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		QueryEditorInput ie = (QueryEditorInput) other;
		if (getUuid() != null){
			return Objects.equals(getUuid(), ie.getUuid());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		if (uuid != null) return uuid.hashCode();
		return super.hashCode();
	}

}