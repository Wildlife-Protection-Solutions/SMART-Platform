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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.util.I18nUtil;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

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
@MappedSuperclass
public class NamedDescriptionItem extends NamedItem {
	
	private static final long serialVersionUID = 1L;
	
	private UUID descuuid;
	private Set<DescriptionLabel> descriptions;
	
	private String description;

	public NamedDescriptionItem() {
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

	@Transient
	public String getDescription() {
		return description;
	}
	@Transient
	public void setDescription(String description) {
		this.description = description;
	}

	// TODO: There must be a better way to do this
	// This fails;
	//@OneToMany(fetch = FetchType.LAZY)
	//@JoinColumn(name = "element_uuid", referencedColumnName = "desc_uuid")
	/**
	 * If not previously loaded, this runs a database query using the current active
	 * session and associated transaction
	 * 
	 * @return
	 */
	@Transient
	public Set<DescriptionLabel> getDescriptions(Session session) {

		if (this.descriptions == null) {
			this.descriptions = new HashSet<DescriptionLabel>();

			descriptions.addAll(
					session.createQuery("FROM DescriptionLabel WHERE id.element = :uuid", DescriptionLabel.class) //$NON-NLS-1$
							.setParameter("uuid", descuuid) //$NON-NLS-1$
							.list());
			computeDescription();
		}
		return this.descriptions;
	}

	public void setDescriptions(Set<DescriptionLabel> descriptions) {
		if (descriptions == null) {
			this.descriptions = new HashSet<DescriptionLabel>();
		} else {
			this.descriptions = descriptions;
		}
		computeDescription();
	}

	
	@Transient
	private void computeDescription() {
		Object ltemp = I18nUtil.getLocale();
		if (ltemp instanceof UUID) {
			UUID lang = (UUID)ltemp;		
			
			for (DescriptionLabel l : this.descriptions) {
				if (l.getLanguage().getUuid().equals(lang)) {
					this.description = l.getValue();
					return;
				}else if (l.getLanguage().isDefault()) {
					this.description = l.getValue();
				}
			}
		}else if (ltemp instanceof Locale) {
			Locale lc = (Locale) ltemp;
			String c1 = lc.getLanguage();
			String c2 = lc.getLanguage()  + "_" + lc.getCountry();  //$NON-NLS-1$
			for (DescriptionLabel l :this.descriptions) {
				if (l.getLanguage().getCode().equalsIgnoreCase(c2)) {
					this.description = l.getValue();
					return;
				}else if (l.getLanguage().getCode().equalsIgnoreCase(c1)) {
					this.description = l.getValue();
				}else if (this.description == null && l.getLanguage().isDefault()) {
					this.description = l.getValue();
				}
			}
			
		}
		
		if (this.description == null) {
			for (DescriptionLabel l :this.descriptions) {
				this.description = l.getValue();
				if (l.getLanguage().isDefault()) {
					return;
				}			
			}
		}
	}
	/**
	 * Finds the description with the associated language.  Returns null if
	 * no description found.
	 * 
	 * @param lang
	 * @return the description associated with the language; null if string not found
	 */
	@Transient
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
	@Transient
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
