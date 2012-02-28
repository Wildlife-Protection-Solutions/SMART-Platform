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
package org.wcs.smart.ca.datamodel;

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
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;

/**
 * A Conservation Area data model object.  This represents
 * an object with a uuid, set of names, and a key;
 * 
 * @author Emily
 * @since 1.0.0
 */

@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public class DmObject {
	
	/**
	 * Maximum length of the key identifier
	 */
	public static final int MAX_KEY_LENGTH = 128;
	
	protected byte[] uuid;		//unique id
	private Set<Label> names;	//names
	private String name;		//default name		
	private String keyid;		//key
	
	protected DmObject(){
		
	}
	
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

	/**
	 * 
	 * @param uuid the unique identifier
	 */
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
	
	/**
	 * 
	 * @return the object key
	 */
	@Column(name="keyid")
	public String getKeyId() {
		return keyid;
	}

	/**
	 * 
	 * <p>Key should be validated using DataModel.validateKey
	 * </p>
	 * @param keyid the object key
	 */
	public void setKeyId(String keyid) {
		this.keyid = keyid;
	}
	
	/**
	 * Copys the key, name, and labels from the old object into the 
	 * current object.
	 * 
	 * The conservation area objects are used to determine 
	 * which languages are equivalent across the conservation areas.
	 * 
	 * @param oldObject  Old object to copy values from 
	 * @param newCa the conservation area associated with the new object
	 * @param oldCa the conservation area associated with the old object
	 */
	protected void copyValues(DmObject oldObject, ConservationArea newCa, ConservationArea oldCa, String defaultLang){
		this.keyid = oldObject.keyid;
		this.name = oldObject.name;
		
		this.setNames(new HashSet<Label>());
		
			
			
		for (Language ll : oldCa.getLanguages()){
			Language newLang = null;
			for (Language lang : newCa.getLanguages()){
				if (lang.getCode().equals(ll.getCode())){
					newLang = lang;
					break;
				}
			}
			if (newLang != null){
				String value = oldObject.findName(ll);
				Label lblClone = new Label();
				lblClone.setValue(value);
				lblClone.setLanguageuuid(newLang.getUuid());
				this.getNames().add(lblClone);
			}
					
			if (defaultLang != null && defaultLang.equals(ll.getCode())){
				//this label needs to be copy to default lang
				String value = oldObject.findName(ll);
				Label lblClone = new Label();
				lblClone.setValue(value);
				lblClone.setLanguageuuid(newCa.getDefaultLanguage().getUuid());
				this.getNames().add(lblClone);
					
			}
			
			
		}
	}
}
