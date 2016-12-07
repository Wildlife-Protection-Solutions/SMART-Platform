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

import java.util.UUID;

public class QueryProxy {

	private UUID uuid;
	private UUID caUuid;
	private String name;
	private String type;
	private String typeKey;
	private String conservationAreaName;
	private String id;
	private Boolean isShared;
	private Boolean isCcaa;
	
	public QueryProxy(UUID uuid, String name, String type, String caName, String id, 
			Boolean isShared, UUID caUuid, Boolean isCcaa, String typeKey){
		this.uuid = uuid;
		this.name = name;
		this.type = type;
		this.conservationAreaName = caName;
		this.id = id;
		this.typeKey = typeKey;
		this.setCaUuid(caUuid);
		setIsCcaa(isCcaa);
	}
	
	



	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getTypeKey() {
		return typeKey;
	}
	public void setTypeKey(String typeKey) {
		this.typeKey = typeKey;
	}
	public String getConservationArea() {
		return conservationAreaName;
	}
	public void setConservationArea(String conservationAreaName) {
		this.conservationAreaName = conservationAreaName;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public Boolean getIsShared() {
		return isShared;
	}
	public void setIsShared(Boolean isShared) {
		this.isShared = isShared;
	}


	public UUID getCaUuid() {
		return caUuid;
	}
	public void setCaUuid(UUID caUuid) {
		this.caUuid = caUuid;
	}
	
	public boolean getIsCcaa(){
		return this.isCcaa;
	}
	public void setIsCcaa(boolean isCcaa){
		this.isCcaa = isCcaa;
	}
}
