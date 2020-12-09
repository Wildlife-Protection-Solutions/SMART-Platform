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
package org.wcs.smart.i2.ui.views;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;

/**
 * Minimal record information for RecordsView
 * @author Emily
 *
 */
public class IntelRecordProxy implements IAdaptable{

	private UUID uuid;
	private String title;
	private IntelRecordSource source;
	private IntelProfile profile;
	private IntelRecord.Status status;
	
	private LocalDateTime date;
	
	public IntelRecordProxy(String title, UUID uuid, IntelRecordSource source,
			IntelProfile profile,
			IntelRecord.Status status, LocalDateTime date){
		this.title = title;
		this.uuid = uuid;
		this.source = source;
		this.status = status;
		this.date = date;
		this.profile = profile;
	}
		
	public LocalDateTime getDate() {
		return this.date;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public IntelRecord.Status getStatus(){
		return this.status;
	}
	
	public IntelRecordSource getRecordSource(){
		return this.source;
	}
	
	public IntelProfile getProfile() {
		return this.profile;
	}
	
	public UUID getUuid(){
		return this.uuid;
	}

	public IntelRecord asRecord() {
		IntelRecord r = new IntelRecord();
		r.setUuid(uuid);
		r.setTitle(title);
		r.setStatus(status);
		r.setRecordSource(source);
		r.setProfile(profile);
		return r;
	}
	
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		IntelRecordProxy ie = (IntelRecordProxy) other;
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

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(IntelRecord.class)) {
			IntelRecord r = new IntelRecord();
			r.setUuid(getUuid());
			r.setProfile(getProfile());
			return (T)r;
		}
		return null;
	}

}
