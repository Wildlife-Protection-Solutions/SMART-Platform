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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

/**
 * Super class for simple lists.  A simple list
 * contains a uuid and a link to a list of 
 * names that represent the list name in various
 * languages.
 * 
 * @author Emily
 *
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public class SimpleList {

	private byte[] uuid;
	private Set<Label> names;
	private String name;

	/**
	 * Creates a new simple list
	 */
	public SimpleList(){}

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
	
	/**
	 * 
	 * @return the names associated with the list element
	 */
	@OneToMany(targetEntity = Label.class, fetch = FetchType.LAZY)
	@JoinColumn(updatable = false, insertable = false, name="element_uuid", referencedColumnName="uuid")
	public Set<Label> getNames() {
		if (names == null){
			names = new HashSet<Label>();
		}
		return names;
	}

	public void setNames(Set<Label> names) {
		this.names = names;
	}

	/**
	 * 
	 * @return the names associated with the list element in the
	 * language the platform is running in.
	 */
	@Type(type="org.wcs.smart.ca.LabelUserType")
	@Column(name="uuid", insertable=false, updatable=false)
	public String getName() {
		return name;
	}
	/**
	 * Do not use to set the name.
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Evicts all names from the given session
	 * @param session
	 */
	public void evitNames(Session session){
		for(Label name: names){
			if (name.getElementuuid() != null){
				session.evict(name);
			}
		}
	}
	
	/**
	 * Finds the name in a given language.
	 * Will return empty string if name not found.
	 * 
	 * @param lang
	 * @return
	 */
	public String findName(Language lang){
		for (Iterator<Label> iterator = getNames().iterator(); iterator.hasNext();) {
			Label type = iterator.next();
			if ( Arrays.equals(type.getLanguageuuid(), lang.getUuid())){
				return type.getValue();
			}
		}
		return "";
	}
	/**
	 * Updates the name in of the given language.
	 * Will create a new name if name not found for current language.
	 * 
	 * @param lang
	 * @param newName
	 */
	public void updateName(Language lang, String newName){
		for (Iterator<Label> iterator = getNames().iterator(); iterator.hasNext();) {
			Label type = iterator.next();
			if ( Arrays.equals(type.getLanguageuuid(), lang.getUuid())){
				type.setValue(newName);
				return;
			}
		}
		//create a new label
		Label lbl = new Label( );
		lbl.setElementuuid(getUuid());
		lbl.setLanguageuuid(lang.getUuid());
		lbl.setValue(newName);
		getNames().add(lbl);
		
	}
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof SimpleList){
			SimpleList s = (SimpleList)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return s.hashCode() == hashCode();
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
