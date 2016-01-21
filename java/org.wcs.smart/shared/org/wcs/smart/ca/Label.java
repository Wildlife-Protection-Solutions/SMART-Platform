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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.wcs.smart.util.I18nUtil;
import org.wcs.smart.util.UuidUtils;

/**
 * 
 * Represents a internationalized label.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity 
@Table (name="smart.i18n_label")

@AssociationOverrides({
	@AssociationOverride(name = "id.language", 
		joinColumns = @JoinColumn(name = "language_uuid")),
	@AssociationOverride(name = "id.element", 
		joinColumns = @JoinColumn(name = "element_uuid")) })

@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Label  {

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

		
		@ManyToOne(fetch = FetchType.LAZY)
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
			if (! (key instanceof LabelItemPK)){
				return false;
			}
			LabelItemPK p = (LabelItemPK)key;
			
			if (p.getLanguage() == null || this.getLanguage() == null ||
				p.getElement() == null || this.getElement() == null ){
				
				if (p.getLanguage() == null && this.getLanguage() == null && 
					p.getElement() == null && this.getElement() == null){
						return true;
				}
				return false;
			}
			
			return p.getLanguage().equals(this.getLanguage())
					&& p.getElement().getUuid().equals(this.getElement().getUuid());
					//Arrays.equals(p.getElement().getUuid(), this.getElement().getUuid());
		}
		
		@Override
		public int hashCode() {
		    int code = 0;
//		    if (getLanguage() != null) {code += Arrays.hashCode(getLanguage().getUuid());}
//		    if (getElement() != null && getElement().getUuid() != null) {code += Arrays.hashCode(getElement().getUuid()); }
		    if (getLanguage() != null) {code += getLanguage().getUuid().hashCode();}
		    if (getElement() != null && getElement().getUuid() != null) {code += getElement().getUuid().hashCode(); }
		    return code;
		  }
	}
	
	private static synchronized String searchAll(UUID element, Session session){
		Locale lang = (Locale) I18nUtil.getLocale();
		if (lang == null) return ""; //$NON-NLS-1$
		
		List<?> options = session.createCriteria(Label.class)
				.add(Restrictions.eq("id.element.uuid",element)).list(); //$NON-NLS-1$
		if (options.size() == 1){
			return ((Label)options.get(0)).getValue();
		}
		
		String langmatch = null;
		String defaultvalue = null;
		for (Object obj : options){
			Label l = (Label)obj;
			String code = l.getLanguage().getCode();
			Locale test = null;
			if (code.contains("_")){ //$NON-NLS-1$
				String[] bits = code.split("_"); //$NON-NLS-1$
				test = new Locale(bits[0], bits[1]);
			}else{
				test = new Locale(code);
			}
			if (test.equals(lang)){
				return l.getValue();
			}else if (test.getLanguage().equals(lang.getLanguage())){
				langmatch = l.getValue();
			}
			if (l.getLanguage().isDefault()){
				defaultvalue = l.getValue();
			}
		}
		if (langmatch != null) return langmatch;
		if (defaultvalue != null) return defaultvalue;
		return null;
	}
	@Transient
	public static synchronized String getDescription(
			UUID elementuuid,
			Session session) {
		Object ltemp = I18nUtil.getLocale();
		if (ltemp instanceof Locale){
			return searchAll(elementuuid, session);
		}
		UUID lang = (UUID)ltemp;
		UUID ca = I18nUtil.getCa();
		if (lang == null || ca == null){
			return searchAll(elementuuid, session);
		}
	
		if (elementuuid == null || ca == null){
			return ""; //$NON-NLS-1$
		}
		
		Label.LabelItemPK id = new Label.LabelItemPK();
		UuidItem h = new UuidItem();
		h.setUuid(elementuuid);
		id.setElement(h);
		
		Language ltmp = (Language) session.get(Language.class, lang);
		id.setLanguage(ltmp);

		String description = ""; //$NON-NLS-1$
		Label lbl = (Label) session.get(Label.class, id);
		if (lbl == null ) {
			// try for the default language
			ConservationArea localca = (ConservationArea) session.load(ConservationArea.class, ca);
			id.setLanguage(localca.getDefaultLanguage());
			lbl = (Label) session.get(Label.class, id);
			
			//search for any label in one of the current ca languages
			for(Language l : localca.getLanguages()){
				id.setLanguage(l);
				lbl = (Label)session.get(Label.class, id);
				if (lbl != null){
					break;
				}
			}
			
//			final String elementuuidstr = SmartUtils.encodeHex(elementuuid);
			final String elementuuidstr = UuidUtils.uuidToString(elementuuid);
			final String[] temp = new String[]{null};
			Language llang = (Language) session.load(Language.class, lang);
			final String searchCode = llang.getCode();
			//for whatever reason when i did this as hibernate queries
			//i could not load entity tables in reports; I don't understand why
			//but it would try to load additional objects that weren't needed and this causes
			//all sorts of addition problems.
			//TODO: the x'elementuuid' in the following statements is not going to work in postgresql
			session.doWork(new Work(){
				@Override
				public void execute(Connection c) throws SQLException {
					
					//try to find with the same language/country code
					String query = "SELECT l.value from smart.i18n_label l join smart.language a on " + //$NON-NLS-1$
							"a.uuid = l.language_uuid where l.element_uuid = x'" + elementuuidstr +  //$NON-NLS-1$
							"' and a.code = '" + searchCode + "'"; //$NON-NLS-1$ //$NON-NLS-2$
					try(ResultSet rs = c.createStatement().executeQuery(query)){
						if (rs.next()){
							temp[0] = rs.getString(1);
							return;
						}
					}
					//try to find with the same language code
					if (searchCode.contains("_")) { //$NON-NLS-1$
						query = "SELECT l.value from smart.i18n_label l join smart.language a on " + //$NON-NLS-1$
								"a.uuid = l.language_uuid where l.element_uuid = x'" + elementuuidstr +  //$NON-NLS-1$
								"' and a.code = '" +  //$NON-NLS-1$
								searchCode.split("_")[0] + "'"; //$NON-NLS-1$ //$NON-NLS-2$
						try(ResultSet rs = c.createStatement().executeQuery(query)){
							if (rs.next()){
								temp[0] = rs.getString(1);
								return;
							}
						}
					}
					//try to find any forcing default to be first
					query = "SELECT l.value from smart.i18n_label l join smart.language a on " + //$NON-NLS-1$
							"a.uuid = l.language_uuid where l.element_uuid = x'" + elementuuidstr +  //$NON-NLS-1$
							"' order by a.ISDEFAULT desc"; //$NON-NLS-1$ 
					try(ResultSet rs = c.createStatement().executeQuery(query)){
						if (rs.next()){
							temp[0] = rs.getString(1);
							return;
						}
					}
				}});
			if (temp[0] != null){
				description = temp[0];
			}
		}
		if (lbl != null) {
			description = lbl.getValue();
		}
		return description;
	}
}


