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

import java.util.UUID;

/**
 * class to represent a SMART6 entity item attribute
 * 
 * @author Emily
 *
 */
public class EntityItemAttribute {

	private UUID attributeUuid;
	private Double doubleValue;
	private String stringValue;
	private UUID uuidValue;
	
	public UUID getAttributeUuid() {
		return attributeUuid;
	}
	public void setAttributeUuid(UUID attributeUuid) {
		this.attributeUuid = attributeUuid;
	}
	public Double getDoubleValue() {
		return doubleValue;
	}
	public void setDoubleValue(Double doubleValue) {
		this.doubleValue = doubleValue;
	}
	public String getStringValue() {
		return stringValue;
	}
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}
	public UUID getUuidValue() {
		return uuidValue;
	}
	public void setUuidValue(UUID uuidValue) {
		this.uuidValue = uuidValue;
	}

	
}
