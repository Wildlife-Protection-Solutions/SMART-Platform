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
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Extensions of a simple list item that also tracks a description 
 * field.  The description field is a uuid that references
 * a label in the label table.
 * 
 * <p>
 * WARNING: Currently users are required to save the labels entities
 * manually, they are not automatically saved
 * when the entity is saved.
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public class SimpleListItemWithDescription extends SimpleListItem {
	
	private byte[] descuuid;
	
	private String description;

	private Set<DescriptionLabel> descriptions;

	public SimpleListItemWithDescription() {
		super();
		this.descriptions = null;
	}

	@Column(name = "desc_uuid")
	public byte[] getDescUuid() {
		return descuuid;
	}

	public void setDescUuid(byte[] uuid) {
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
	@Transient
//	// @Subselect(sql="select a.* from Label a, Station b where a.id.element = b.desccuuid")
//	// @OneToMany(targetEntity = Label.class, fetch = FetchType.LAZY,
//	// mappedBy="id.element", cascade={CascadeType.ALL}, orphanRemoval=true)
	public Set<DescriptionLabel> getDescriptions() {
		if (this.descriptions == null) {
			this.descriptions = new HashSet<DescriptionLabel>();
			Session sess = HibernateManager.openSession();
			sess.beginTransaction();
			Criteria c = sess.createCriteria(DescriptionLabel.class).add(Restrictions.eq("id.element", this.descuuid));
			this.descriptions.addAll(c.list());
			sess.getTransaction().commit();
			sess.close();
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
	 * Finds the description with the associated language
	 * 
	 * @param lang
	 * @return the description associated with the language; empty string if
	 *         description not found
	 */
	public String findDescription(Language lang) {
		DescriptionLabel x = findValue(getDescriptions(), lang);
		if (x == null) {
			return "";
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
	public void updateDescription(Language lang, String description) {
		DescriptionLabel lbl = findValue(getDescriptions(), lang);
		if (lbl == null) {
			// create a new label
			lbl = new DescriptionLabel();
			lbl.setElement(this.descuuid);
			lbl.setLanguage(lang);
			getDescriptions().add(lbl);
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
