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

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.wcs.smart.util.I18nUtil;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * 
 * Represents a internationalized label.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity 
@Table (name="i18n_label", schema="smart")

@AssociationOverrides({
	@AssociationOverride(name = "id.language", 
		joinColumns = @JoinColumn(name = "language_uuid")),
	@AssociationOverride(name = "id.element", 
		joinColumns = @JoinColumn(name = "element_uuid")) })

@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Label implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int MAX_LENGTH = 1024;
	
	private LabelItemPK id;
	private String value;
	
	public Label(){
		id = new LabelItemPK();
	}

	public String getValue(){
		return value;
	}
	public void setValue(String label){
		this.value = label;
	}
	
	@EmbeddedId
	public LabelItemPK getId(){
		return this.id;
	}
	public void setId(LabelItemPK id){
		this.id = id;
	}
	
	@Transient
	public Language getLanguage() {
		return id.getLanguage();
	}

	public void setLanguage(Language language) {
		id.setLanguage(language);
	}
	
	@Transient
	public UuidItem getElementuuid() {
		return id.element;
	}

	
	public void setElement(UuidItem elementuuid) {
		id.setElement(elementuuid);
	}

	
	@Embeddable
	public static class LabelItemPK implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Language language;
		private UuidItem element;

		
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name="language_uuid", referencedColumnName="uuid")
		public Language getLanguage() {
			return language;
		}

		public void setLanguage(Language language) {
			this.language = language;
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="element_uuid", referencedColumnName="uuid")
		public UuidItem getElement() {
			return element;
		}

		public void setElement(UuidItem element) {
			this.element = element;
		}
		
		@Override
		public boolean equals(Object key) {
			if (this == key) return true;
			if (key == null) return false;
			if (key.getClass() != getClass()) return false;
			return Objects.equals(getLanguage(), ((LabelItemPK)key).getLanguage()) 
					&& Objects.equals(getElement(), ((LabelItemPK)key).getElement());
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(getLanguage(), getElement());
		  }
	}
	
//	public static synchronized String searchAll(Locale lang, UUID element, Session session){
//		if (lang == null) return ""; //$NON-NLS-1$
//		List<Label> labels = session.createQuery("FROM Label WHERE id.element.uuid = :element", Label.class)
//			.setParameter("element", element).list();
//		
////		Query<Tuple> query = session.createQuery("SELECT id.language.code, value, id.language.default FROM Label WHERE id.element.uuid = :element", Tuple.class); //$NON-NLS-1$
////		query.setParameter("element", element); //$NON-NLS-1$
////		List<Tuple> options = query.list();
////			
//		if (labels.size() == 1){
//			return labels.get(0).getValue();
//		}
//			
//		return "test";
////		String langmatch = null;
////		String defaultvalue = null;
////		for (Tuple obj : options){
////			String label = (String)obj.get(1);
////			String code = (String)obj.get(0);
////			Locale test = null;
////			if (code.contains("_")){ //$NON-NLS-1$
////				String[] bits = code.split("_"); //$NON-NLS-1$
////				test = new Locale(bits[0], bits[1]);
////			}else{
////				test = new Locale(code);
////			}
////			if (test.equals(lang)){
////				return label;
////			}else if (test.getLanguage().equals(lang.getLanguage())){
////				langmatch = label;
////			}
////			if ((Boolean)obj.get(2)){
////				defaultvalue = label;
////			}
////		}
////		if (langmatch != null) return langmatch;
////		if (defaultvalue != null) return defaultvalue;
////		return null;
//	}
//	
//	@Transient
//	//public static synchronized String getDescription(
//	public static String getDescription(
//			NamedItem item,
//			Session session) {
//		
//		Object ltemp = I18nUtil.getLocale();
//		if (ltemp instanceof UUID) {
//			UUID lang = (UUID)ltemp;		
//			UUID ca = I18nUtil.getCa();
//			
//			if (lang != null && ca != null) {
//				Label.LabelItemPK id = new Label.LabelItemPK();
//				id.setElement(new UuidItem(item.getUuid()));
//				Language ltmp = (Language) session.getReference(Language.class, lang);
//				id.setLanguage(ltmp);
//
//				Label lbl = (Label) session.get(Label.class, id);
//				if (lbl != null) return lbl.getValue();
//				
//				// try for the default language
//				ConservationArea localca = (ConservationArea) session.get(ConservationArea.class, ca);
//				id.setLanguage(localca.getDefaultLanguage());
//				lbl = (Label) session.get(Label.class, id);
//				if (lbl != null) return lbl.getValue();
//			}
//		}
//		
//		//TODO: if ltemp is a locale try to match that
//		item = session.getReference(item);
//		for (Label l : item.getNames()) {
//			if(l.getLanguage().isDefault()) return l.getValue();
//		}
//		
//		return "ERROR";
//	
//	}
//	
	@Transient
	public static String findLabel(UUID elementuuid, Session session) {
		if (elementuuid == null) return "";
		
		Object ltemp = I18nUtil.getLocale();
		
		if (ltemp instanceof UUID) {
			UUID lang = (UUID)ltemp;		
			UUID ca = I18nUtil.getCa();
			if (lang != null || ca != null){
				Label.LabelItemPK id = new Label.LabelItemPK();
				id.setElement(new UuidItem(elementuuid));
				Language ltmp = (Language) session.getReference(Language.class, lang);
				id.setLanguage(ltmp);

				Label lbl = (Label) session.get(Label.class, id);
				if (lbl != null) return lbl.getValue();
				
				// try for the default language
				ConservationArea localca = (ConservationArea) session.get(ConservationArea.class, ca);
				id.setLanguage(localca.getDefaultLanguage());
				lbl = (Label) session.get(Label.class, id);
				if (lbl != null) return lbl.getValue();
			}
			
		}
		Locale l = Locale.getDefault();
		if (ltemp instanceof Locale){
			l = (Locale) ltemp;
		}
		
		List<Label> labels = session.createQuery("FROM Label WHERE id.element.uuid = :element", Label.class) //$NON-NLS-1$
				.setParameter("element", elementuuid).list(); //$NON-NLS-1$
		return NamedItem.findName(labels, l);		
		
	}
}


