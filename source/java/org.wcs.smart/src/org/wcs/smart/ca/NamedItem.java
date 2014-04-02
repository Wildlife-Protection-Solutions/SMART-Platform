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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.Type;

/**
 * Super class for items with names.  
 * Items with names must have a uuid field.  That uuid
 * field is used to store names in the i18n_label field.  Multiple
 * names can exist for the different languages (although only
 * a single name per language).
 * 
 * @author Emily
 *
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public class NamedItem extends UuidItem {

	
	private Set<Label> names;
	private String name;

	/**
	 * Creates a new simple list
	 */
	public NamedItem(){}

	
	/**
	 * 
	 * @return the names associated with the list element
	 */
	@OneToMany(targetEntity = Label.class, fetch = FetchType.LAZY, mappedBy="id.element", cascade={CascadeType.ALL}, orphanRemoval=true)
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
	 * Do not use to set the name.<br>
	 * This will only set the name for
	 * the current object; it will not be persisted to the database 
	 * for future objects.  To persist name changes use 
	 * the updateName(Language, String)
	 * function.
	 *  
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
	 * 
	 * @return the label associated with the default language
	 */
	@Transient
	public String getDefaultName(){
		for (Label l : getNames()){
			if (l.getLanguage().isDefault()){
				return l.getValue();
			}
		}
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * Finds the name in a given language.
	 * 
	 * @param lang
	 * @return the name for the language or an empty string
	 */
	public String findName(Language lang){
		String x = findNameNull(lang);
		if (x != null){
			return x;
		}
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * Finds the name in a given language or returns null if not found
	 * 
	 * @param lang
	 * @return the name for the language or null if not found
	 */
	public String findNameNull(Language lang){
		for (Iterator<Label> iterator = getNames().iterator(); iterator.hasNext();) {
			Label type = iterator.next();
			if (type.getLanguage().equals(lang)){
				return type.getValue();
			}
		}
		return null;
	}
	/**
	 * Updates the name in of the given language.
	 * Will create a new name if name not found for current language.
	 * 
	 * @param lang
	 * @param newName
	 */
	public void updateName(Language lang, String newName){
		if (lang == null) return;
		if (getNames() == null){
			names = new HashSet<Label>();
		}
		for (Iterator<Label> iterator = getNames().iterator(); iterator.hasNext();) {
			Label type = iterator.next();
			if (type.getLanguage().equals(lang)){
				type.setValue(newName);
				return;
			}
		}
		//create a new label
		getNames().add(createLabel(lang, newName));		
	}
	
	
	private Label createLabel(Language lang, String newName){
		//create a new label
		Label lbl = new Label();
		lbl.setElement(this);
		lbl.setLanguage(lang);
		lbl.setValue(newName);
		return lbl;
	}
	
}
