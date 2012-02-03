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
package org.wcs.smart.patrol.model;

import java.util.Arrays;
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
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.SimpleList;
import org.wcs.smart.patrol.PatrolHibernateManager;

/**
 * Team object.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.team")
public class Team extends SimpleList{

	public static final String NAME = "Team";
	public static final String DESCRIPTION = "Description";
	public static final String MANDATE = "Mandate";
	
	private PatrolMandate mandate;
	private boolean isActive;
	private Set<Label> descriptions;
	
	private ConservationArea ca;
	
	private String description;
	private byte[] descuuid;
	
	public Team(){}
	
	/**
	 * 
	 * @return team {@link PatrolMandate} if assigned, null otherwise
	 */
	@ManyToOne
	@JoinColumn(name="patrol_mandate_uuid", referencedColumnName="uuid")
	public PatrolMandate getMandate(){
		return this.mandate;
	}
	/**
	 * Sets the team patrol mandate.
	 * 
	 * @param mandate new mandate or null.
	 */
	public void setMandate(PatrolMandate mandate){
		this.mandate = mandate;
	}
	
	/**
	 * 
	 * @return <code>true</code> if team active, <code>false</code> otherwise
	 */
	@Column(name="is_active")
	public boolean getIsActive(){
		return this.isActive;
	}
	/**
	 * 
	 * @param isActive  <code>true</code> if team active, <code>false</code> otherwise
	 */
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}
	
	/**
	 * 
	 * @return conservation area associated with team
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}

	/**
	 * 
	 * @param ca
	 *            conservation area associated with team
	 */
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}

	/**
	 * Description of the team in the current 
	 * language.
	 * 
	 * @return team description
	 */
	@Type(type="org.wcs.smart.ca.LabelUserType")
	@Column(name="desc_uuid", insertable=false, updatable=false)
	public String getDescription() {
		return description;
	}
	/**
	 * Sets the local description. 
	 * <p>
	 * Should uss updateDescription to save
	 * changes to database.
	 * </p>
	 * @param description
	 */
	public void setDescription(String description){
		this.description = description;
	}
	/**
	 * 
	 * @return uuid for the description field
	 */
	@Column(name="desc_uuid")
	public byte[] getDescUuid() {
		return descuuid;
	}

	/**
	 * 
	 * @param uuid uuid for description field
	 */
	public void setDescUuid(byte[] uuid) {
		this.descuuid = uuid;
	}
	
	/**
	 * 
	 * @return loads all descriptions in all languages
	 * for the given team
	 */
	@Transient
	public Set<Label> getDescriptions(){
		if (this.descriptions == null){
			this.descriptions = new HashSet<Label>();
			Session sess = PatrolHibernateManager.openSession();
			sess.beginTransaction();
			Criteria r = sess.createCriteria(Label.class);
			r.add(Restrictions.eq("elementuuid", this.descuuid));
			this.descriptions.addAll( r.list() );
			sess.getTransaction().commit();
			sess.close();
		}
		return this.descriptions;
	}
	
	/**
	 * Sets the descriptions for the current team.
	 * @param descriptions
	 */
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
			lbl.setLanguageuuid(lang.getUuid());
			getDescriptions().add(lbl);
		}
		lbl.setValue(description);
		
	}
	private Label findValue(Set<Label> list, Language lang){
		for(Label lbl : list){
			if (Arrays.equals(lbl.getLanguageuuid(), lang.getUuid())){
				return lbl;
			}
		}
		return null;
	}
}
