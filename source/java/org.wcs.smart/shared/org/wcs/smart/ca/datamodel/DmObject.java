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

import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;

/**
 * A Conservation Area data model object.  This represents
 * an object with a uuid, set of names, and a key;
 * 
 * @author Emily
 * @since 1.0.0
 */

@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public class DmObject extends NamedKeyItem{
	
	
	/**
	 * Maximum length of the name identifier
	 */
	public static final int MAX_NAME_LENGTH = 1024;

	
	protected DmObject(){
		
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
	 */
	protected void copyValues(DmObject oldObject, ConservationArea newCa, String defaultLang){
		setKeyId(oldObject.getKeyId());
		setName(oldObject.getName());
		
		
		this.setNames(new HashSet<Label>());
		
		if (oldObject.getNames() != null){
			for (Label l : oldObject.getNames()){
				if (l.getLanguage().getCode().equals(defaultLang)){
					this.updateName(newCa.getDefaultLanguage(), l.getValue());
				}
				for (Language lang : newCa.getLanguages()){
					if (l.getLanguage().isSame(lang) ){
						this.updateName(lang, l.getValue());
						break;
					}
				}
			}
		}
	}
}
