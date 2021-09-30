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
package org.wcs.smart.i2.query;

import java.util.Objects;

/**
 * Class for traking keys and names.  Used for summary query filter options.
 * @author Emily
 *
 */
public class ListItem {

	private String keyId;
	private String name;
	
	private String fullName;
	
	public ListItem(String keyId, String name) {
		this(keyId, name, name);
	}
	
	public ListItem(String keyId, String name, String fullName) {
		this.keyId = keyId;
		this.name = name;
		this.fullName = fullName;
	}
	
	public String getFullName() {
		return this.fullName;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getKeyId() {
		return this.keyId;
	}
	
	public boolean equals(Object other) {
		if (other == this) return true;
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		return Objects.equals(keyId, ((ListItem)other).keyId);
	}
	
	public int hashCode() {
		return Objects.hash(keyId);
	}
}
