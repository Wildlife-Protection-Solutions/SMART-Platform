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
package org.wcs.smart.connect.model;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

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
public class ConnectUuidItem {
	
	private UUID uuid;
	
	/**
	 * Creates a new label class
	 */
	public ConnectUuidItem(UUID uuid){
		this.uuid = uuid;
	}

	
	/**
	 * Creates a new label class
	 */
	public  ConnectUuidItem(){}

	/**
	 * 
	 * @return the uuid for the list element
	 */
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	@Type(type = "pg-uuid")
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof ConnectUuidItem){
			ConnectUuidItem s = (ConnectUuidItem)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return this == s;
			}else if (s.getUuid() != null && this.getUuid() != null){
				return s.getUuid().compareTo(this.getUuid()) == 0;
			}
		}
		return false;
	}
	
	
	public int hashCode(){
		if (getUuid() != null){
			return getUuid().hashCode();
		}
		return super.hashCode();
	}
}
