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
package org.wcs.smart.cybertracker.export.data;

import org.wcs.smart.cybertracker.model.elements.Elements.List.Items.Item;

/**
 * Class represents key/value pair for a particular record. Intended to use in json generation for object values.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CtDataKeyValueRecord {
	
	private Item keyItem;
	private Item valueItem;
	private String valueString;
	
	public CtDataKeyValueRecord() {
		super();
	}
	
	public CtDataKeyValueRecord(Item keyItem, Item valueItem) {
		super();
		this.keyItem = keyItem;
		this.valueItem = valueItem;
	}

	public CtDataKeyValueRecord(Item keyItem, String valueString) {
		super();
		this.keyItem = keyItem;
		this.valueString = valueString;
	}

	public Item getKeyItem() {
		return keyItem;
	}
	public void setKeyItem(Item keyItem) {
		this.keyItem = keyItem;
	}
	
	public Item getValueItem() {
		return valueItem;
	}
	public void setValueItem(Item valueItem) {
		this.valueItem = valueItem;
	}

	public String getValueString() {
		return valueString;
	}
	public void setValueString(String valueString) {
		this.valueString = valueString;
	}
}
