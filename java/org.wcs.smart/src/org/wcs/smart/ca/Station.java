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
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;

@Entity
@Table(name = "smart.station")
public class Station extends SimpleList {// implements Serializable{

	@Transient
	public static final String ID = "Id";
	@Transient
	public static final String NAME = "Name";
	@Transient
	public static final String DESCRIPTION = "Description";
	
	private byte[] descuuid;
	
	private ConservationArea ca;
	
	public boolean isActive;
	
	private String description;
	
	private Set<Label> descriptions;
	
	public Station(){
		super();
		this.descriptions = null;
	}

	@Column(name="desc_uuid")
	public byte[] getDescUuid() {
		return descuuid;
	}

	public void setDescUuid(byte[] uuid) {
		this.descuuid = uuid;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}

	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name="is_active")
	public boolean getIsActive(){
		return this.isActive;
	}
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}

	
	@Type(type="org.wcs.smart.ca.LabelUserType")
	@Column(name="desc_uuid", insertable=false, updatable=false)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description){
		this.description = description;
	}
	
	
	//TODO:  There must be a better way to do this
	//This fails;
	//@OneToMany(fetch = FetchType.LAZY)
	//@JoinColumn(insertable = false, updatable = false, name="element_uuid", referencedColumnName="desc_uuid")
	@Transient
	public Set<Label> getDescriptions(){
		if (this.descriptions == null){
			this.descriptions = new HashSet<Label>();
			Session sess = HibernateManager.openSession();
			sess.beginTransaction();
			Criteria r = sess.createCriteria(Label.class);
			r.add(Restrictions.eq("id.elementuuid", this.descuuid));
			this.descriptions.addAll( r.list() );
			sess.getTransaction().commit();
			sess.close();
		}
		return this.descriptions;
	}
	
	public void setDescriptions(Set<Label> descriptions){
		if (descriptions == null){
			this.descriptions = new HashSet<Label>();
		}else{
			this.descriptions = descriptions;
		}
	}
	
	
	/**
	 * Finds the description with the associated language
	 * @param lang
	 * @return the description associated with the language; empty
	 * string if description not found
	 */
	public String findDescription(Language lang){
		Label x = findValue(getDescriptions(), lang);
		if (x == null){
			return "";
		}else{
			return x.getValue();
		}
	}
	/**
	 * Updates the description for the current language.
	 * Will create a new label if label is not found.
	 * 
	 * @param lang lanaguage 
	 * @param description new description
	 */
	public void updateDescription(Language lang, String description){
		Label lbl = findValue(getDescriptions(), lang);
		if (lbl == null){
			lbl = new Label();
			lbl.setElementuuid(getUuid());
			lbl.setLanguage(lang);
			getDescriptions().add(lbl);
		}
		lbl.setValue(description);
		
	}
	
	private Label findValue(Set<Label> list, Language lang){
		for(Label lbl : list){
			if (lbl.getLanguage().equals(lang)){
				return lbl;
			}
		}
		return null;
	}
	

}
