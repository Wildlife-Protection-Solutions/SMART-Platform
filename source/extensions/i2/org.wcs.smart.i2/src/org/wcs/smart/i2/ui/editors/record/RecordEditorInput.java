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
package org.wcs.smart.i2.ui.editors.record;

import java.util.Date;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.i2.model.IntelRecord;

/**
 * Input for intel record editor
 * @author Emily
 *
 */
public class RecordEditorInput implements IEditorInput{

	private String name;
	private UUID uuid;
	private Date date;
	private IntelRecord record;
	
	public RecordEditorInput(String name, UUID uuid, Date date){
		this.name = name;
		this.uuid = uuid;
		this.date = date;
	}
	
	public RecordEditorInput(IntelRecord record){
		this.record = record;
		this.name = record.getTitle();
		this.date = record.getDateCreated();
	}
	
	public IntelRecord getRecord(){
		return this.record;
	}
	
	public Date getDateCreated(){
		return this.date;
	}
	public UUID getUuid(){
		if (record != null){
			return record.getUuid();
		}
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
		if (other != null && other instanceof RecordEditorInput){
			if (record != null) return record.equals(((RecordEditorInput)other).record);
			return uuid.equals(((RecordEditorInput)other).uuid);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return uuid.hashCode();
	}

}
