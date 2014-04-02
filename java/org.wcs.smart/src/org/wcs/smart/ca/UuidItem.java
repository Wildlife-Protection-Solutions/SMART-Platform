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
package org.wcs.smart.ca;

import java.util.Arrays;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.GenericGenerator;

/**
 * This represents entities with simple uuid field.
 * 
 * <p>
 * Entities which have associated name labels should
 * extend this class or the NamedItem class.
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public class UuidItem {
	
	private byte[] uuid;
	
	/**
	 * Creates a new label class
	 */
	public  UuidItem(byte[] uuid){
		this.uuid = uuid;
	}

	
	/**
	 * Creates a new label class
	 */
	public  UuidItem(){}

	/**
	 * 
	 * @return the uuid for the list element
	 */
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public byte[] getUuid() {
		return uuid;
	}

	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}
	
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof UuidItem){
			UuidItem s = (UuidItem)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return this == s;
			}else if (s.getUuid() != null && this.getUuid() != null){
				return Arrays.equals(s.getUuid(), this.getUuid());
			}
		}
		return false;
	}
	
	
	public int hashCode(){
		if (uuid != null){
			return Arrays.hashCode(uuid);
		}
		return super.hashCode();
	}
}
