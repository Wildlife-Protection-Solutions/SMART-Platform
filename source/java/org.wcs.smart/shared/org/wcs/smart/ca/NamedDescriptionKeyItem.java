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
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;

/**
 * Named key item with a description
 * @author Emily
 *
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public class NamedDescriptionKeyItem extends NamedKeyItem {

	private UUID descuuid;
	
	private String description;

	private Set<DescriptionLabel> descriptions;

	public NamedDescriptionKeyItem() {
		super();
		this.descriptions = null;
	}

	@Column(name = "desc_uuid")
	public UUID getDescUuid() {
		return descuuid;
	}

	public void setDescUuid(UUID uuid) {
		this.descuuid = uuid;
	}

	@Type(type = "org.wcs.smart.ca.LabelUserType")
	@Column(name = "desc_uuid", insertable = false, updatable = false)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

//	// TODO: There must be a better way to do this
//	// This fails;
//	 @OneToMany(fetch = FetchType.LAZY)
//	 @JoinColumn(name="element_uuid", referencedColumnName="desc_uuid")
	@SuppressWarnings("unchecked")
	@Transient
	/**
	 * If not previously loaded, this runs a database
	 * query using the current active session and
	 * associated transaction 
	 * @return
	 */
	public Set<DescriptionLabel> getDescriptions(Session session) {
		if (this.descriptions == null) {
			this.descriptions = new HashSet<DescriptionLabel>();
			Criteria c = session.createCriteria(DescriptionLabel.class).add(Restrictions.eq("id.element", descuuid)); //$NON-NLS-1$
			descriptions.addAll(c.list());
		}
		return this.descriptions;
	}

	public void setDescriptions(Set<DescriptionLabel> descriptions) {
		if (descriptions == null) {
			this.descriptions = new HashSet<DescriptionLabel>();
		} else {
			this.descriptions = descriptions;
		}
	}

	/**
	 * Finds the description with the associated language.  Returns null if
	 * no description found.
	 * 
	 * @param lang
	 * @return the description associated with the language; null if string not found
	 */
	public String findDescriptionNull(Session session, Language lang) {
		DescriptionLabel x = findValue(getDescriptions(session), lang);
		if (x == null) {
			return null;
		} else {
			return x.getValue();
		}
	}

	/**
	 * Updates the description for the current language. Will create a new label
	 * if label is not found.
	 * 
	 * @param lang
	 *            lanaguage
	 * @param description
	 *            new description
	 */
	public void updateDescription(Session session, Language lang, String description) {
		DescriptionLabel lbl = findValue(getDescriptions(session), lang);
		if (lbl == null) {
			// create a new label
			lbl = new DescriptionLabel();
			lbl.setElement(this.descuuid);
			lbl.setLanguage(lang);
			getDescriptions(session).add(lbl);
		}
		lbl.setValue(description);

	}

	private DescriptionLabel findValue(Set<DescriptionLabel> list, Language lang) {
		if(list == null){
			return null;
		}
		for (DescriptionLabel lbl : list) {
			if (lbl.getLanguage().equals(lang)) {
				return lbl;
			}
		}
		return null;
	}

}
