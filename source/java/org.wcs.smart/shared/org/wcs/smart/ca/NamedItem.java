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

import java.text.Collator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.wcs.smart.util.I18nUtil;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;

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
@MappedSuperclass
public class NamedItem extends UuidItem implements Comparable<NamedItem>{

	private static final long serialVersionUID = 1L;
	
	private Set<Label> names ;
	private String name;
	
	/**
	 * Creates a new simple list
	 */
	public NamedItem(){}

	/**
	 * 
	 * @return the names associated with the list element
	 */
	@OneToMany(targetEntity = Label.class, fetch = FetchType.EAGER,
			mappedBy="id.element", cascade={CascadeType.ALL}, orphanRemoval=true)
	@BatchSize(size=10)
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
	@Transient
	public String getName() {
		return this.name;
	}
	
	@PostLoad
	public void loadName() {
		
		Object ltemp = I18nUtil.getLocale();
		if (ltemp instanceof UUID) {
			UUID lang = (UUID)ltemp;		
			
			for (Label l : getNames()) {
				if (l.getLanguage().getUuid().equals(lang)) {
					this.name = l.getValue();
					return;
				}else if (l.getLanguage().isDefault()) {
					this.name = l.getValue();
				}
			}
		}else if (ltemp instanceof Locale) {
			this.name = findName(getNames(), (Locale) ltemp);
		}
		
		if (this.name == null) {
			for (Label l : getNames()) {
				this.name = l.getValue();
				if (l.getLanguage().isDefault()) {
					return;
				}			
			}
		}

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
	@Transient
	public void setName(String name) {
		this.name = name;
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
	@Transient
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
	@Transient
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
	@Transient
	public void updateName(Language lang, String newName){
		if (lang == null) return;
		if (getNames() == null){
			names = new HashSet<>();
		}
		
		Label found = null;
		for (Iterator<Label> iterator = getNames().iterator(); iterator.hasNext();) {
			Label type = iterator.next();
			if (type.getLanguage().equals(lang)){
				found = type;
				break;
			}
		}
		
		if (found != null) {
			if (newName == null) {
				getNames().remove(found);
			}else {
				//only update if they are different; this will
				//ensure we don't have extra things to sync that aren't really required
				if (!found.getValue().equals(newName)) found.setValue(newName);
			}		
			return;
		}
		
		//create a new label
		if (newName == null) return;
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
	
	@Override
	public int compareTo(NamedItem o) {
		String s1 = getName();
		String s2 = o.getName();
		if (s1 != null && s2 != null) return Collator.getInstance().compare(getName().toLowerCase(), o.getName().toLowerCase());
		if (s1 == null && s2 == null) return 0;
		if (s1 == null && s2 != null) return -1;
		return 1;
	}
	
	@Transient
	public static String findName(Collection<Label> names, Locale searchLocale) {
		Locale lc = (Locale) searchLocale;
		String c1 = lc.getLanguage();
		String c2 = lc.getLanguage()  + "_" + lc.getCountry();  //$NON-NLS-1$
		String name = null;
		for (Label l : names) {
			if (l.getLanguage().getCode().equalsIgnoreCase(c2)) {
				return l.getValue();
			}else if (l.getLanguage().getCode().equalsIgnoreCase(c1)) {
				name = l.getValue();
			}else if (name == null && l.getLanguage().isDefault()) {
				name = l.getValue();
			}
		}
		return name;
	}
}
