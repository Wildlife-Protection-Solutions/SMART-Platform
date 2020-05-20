/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query;

import java.util.Objects;
import java.util.UUID;

public class QueryProxy {

	private final UUID uuid;
	private final String name;
	private final String type;
	private final String conservationAreaName;
	private final String id;
	private final Boolean isShared;
	private final UUID caUuid;
	private final UUID folderUuid;
	private final String typeKey;
	private final String iconName;
	private final Boolean isCcaa;
	
	public QueryProxy(UUID uuid, String name, String type, String caName, String id, 
			Boolean isShared, UUID caUuid, UUID folderUuid, Boolean isCcaa, String typeKey, String iconName){
		this.uuid = uuid;
		this.name = name;
		this.type = type;
		this.conservationAreaName = caName;
		this.id = id;
		this.isShared = isShared;
		this.caUuid = caUuid;
		this.folderUuid = folderUuid;
		this.isCcaa = isCcaa;
		this.typeKey = typeKey;
		this.iconName = iconName;
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getTypeKey() {
		return typeKey;
	}

	public String getConservationArea() {
		return conservationAreaName;
	}

	public String getId() {
		return id;
	}
	
	public Boolean getIsShared() {
		return isShared;
	}

	public UUID getCaUuid() {
		return caUuid;
	}

	public UUID getFolderUuid() {
		return folderUuid;
	}

	public boolean getIsCcaa(){
		return this.isCcaa;
	}

	public String getIconName() {
		return iconName;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other.getClass() != getClass()) return false;
		return Objects.equals(uuid, ((QueryProxy)other).uuid);
	}
	
	public int hashCode() {
		return this.uuid.hashCode();
	}
}
