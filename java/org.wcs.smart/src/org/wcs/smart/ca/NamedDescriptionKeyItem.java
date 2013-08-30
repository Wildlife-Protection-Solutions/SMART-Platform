package org.wcs.smart.ca;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Transient;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;

public class NamedDescriptionKeyItem extends NamedKeyItem {

	private byte[] descuuid;

	private String description;

	private Set<DescriptionLabel> descriptions;

	public NamedDescriptionKeyItem() {
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

	// // TODO: There must be a better way to do this
	// // This fails;
	// @OneToMany(fetch = FetchType.LAZY)
	// @JoinColumn(name="element_uuid", referencedColumnName="desc_uuid")
	@SuppressWarnings("unchecked")
	@Transient
	public Set<DescriptionLabel> getDescriptions() {
		if (this.descriptions == null) {
			this.descriptions = new HashSet<DescriptionLabel>();
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			Criteria c = session.createCriteria(DescriptionLabel.class).add(
					Restrictions.eq("id.element", this.descuuid)); //$NON-NLS-1$
			this.descriptions.addAll(c.list());
			session.getTransaction().commit();
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
	 * Finds the description with the associated language. Returns null if no
	 * description found.
	 * 
	 * @param lang
	 * @return the description associated with the language; null if string not
	 *         found
	 */
	public String findDescriptionNull(Language lang) {
		DescriptionLabel x = findValue(getDescriptions(), lang);
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
		if (list == null) {
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
