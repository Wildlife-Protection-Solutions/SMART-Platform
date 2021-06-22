/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.entity;

import java.util.HashMap;
import java.util.UUID;

/**
 * smart6 entit attribute
 * @author Emily
 *
 */
public class EntityTypeAttributeItem {

	private UUID uuid;
	
	private UUID dmAttribute;
	private int order;
	private boolean isPrimary;
	private String keyId;
	
	private HashMap<UUID, String> names;

	
	public EntityTypeAttributeItem() {
		names = new HashMap<>();

	}
	
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public UUID getUuid() {
		return this.uuid;
	}
	public void addName(UUID languageUuid, String value) {
		names.put(languageUuid, value);
	}
	
	public HashMap<UUID, String> getNames(){
		return this.names;
	}
	
	
	public UUID getDmAttribute() {
		return dmAttribute;
	}
	public void setDmAttribute(UUID dmAttribute) {
		this.dmAttribute = dmAttribute;
	}
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	public boolean isPrimary() {
		return isPrimary;
	}
	public void setPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}
	public String getKeyId() {
		return keyId;
	}
	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}
	
}
