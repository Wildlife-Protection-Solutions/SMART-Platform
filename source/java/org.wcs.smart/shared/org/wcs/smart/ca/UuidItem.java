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

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

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
//@Cache(region="uuidCache", usage = CacheConcurrencyStrategy.READ_WRITE)
public class UuidItem implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private UUID uuid;
	
	/**
	 * Creates a new label class
	 */
	public UuidItem(UUID uuid){
		this.uuid = uuid;
	}

	
	/**
	 * Creates a new label class
	 */
	public UuidItem(){}

	/**
	 * 
	 * @return the uuid for the list element
	 */
	@Id
	@UuidGenerator(style = UuidGenerator.Style.RANDOM)
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (getUuid() == null) return false;
		//this is required for proxy classes
		//https://stackoverflow.com/questions/11013138/hibernate-equals-and-proxy
		if (!getClass().isInstance(other) && !other.getClass().isInstance(this) ) return false;		
		UuidItem s = (UuidItem)other;
		//must use getUuid for hibernate proxies 
		return (Objects.equals(getUuid(), s.getUuid()));
	}

	public int hashCode(){
		return Objects.hashCode(uuid);
	}
}
