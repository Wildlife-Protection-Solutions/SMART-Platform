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
package org.wcs.smart.observation.query.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Incident type to support queries
 * @since 8.1.0
 */
public class QueryIncidentType {

	private String name;
	private String key;
	private Set<UUID> uuids;
	private boolean isActive;
	
	
	public QueryIncidentType(String name, String key, UUID uuid, boolean isActive) {
		this.name = name;
		this.key = key;
		this.uuids = new HashSet<>();
		this.uuids.add(uuid);
		this.isActive = isActive;
	}
	
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	public boolean isActive() {
		return this.isActive;
	}
	public String getName() {
		return this.name;
	}
	public String getKey() {
		return this.key;
	}
	public Set<UUID> getUuids(){
		return this.uuids;
	}
	
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (getKey() == null) return false;	
		if (getClass() != other.getClass()) return false;
		QueryIncidentType s = (QueryIncidentType)other;
		return (Objects.equals(getKey(), s.getKey()));
	}

	public int hashCode(){
		return Objects.hashCode(this.key);
	}
}
