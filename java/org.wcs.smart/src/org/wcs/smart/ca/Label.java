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
import java.util.Arrays;

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
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;

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

	/**
	 * Loads the label for the given element from the database.
	 * Looks first for the current language, then the default language
	 * of the current conservation area.
	 * 
	 * @param elementuuid
	 * @return
	 */
	@Transient
	public static synchronized String getDescription(byte[] elementuuid) {
		if (elementuuid == null || SmartDB.getCurrentConservationArea() == null){
			return ""; //$NON-NLS-1$
		}
		
		Label.LabelItemPK id = new Label.LabelItemPK();
		UuidItem h = new UuidItem();
		h.setUuid(elementuuid);
		id.setElement(h);
		id.setLanguage(SmartDB.getCurrentLanguage());

		String description = ""; //$NON-NLS-1$
		Session s = HibernateManager.openSession();
		Label lbl = (Label) s.get(Label.class, id);
		if (lbl == null ) {
			// try for the default language
			id.setLanguage(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			lbl = (Label) s.get(Label.class, id);
			
			//search for any label in one of the current ca languages
			for(Language l : SmartDB.getCurrentConservationArea().getLanguages()){
				id.setLanguage(l);
				lbl = (Label)s.get(Label.class, id);
				if (lbl != null){
					break;
				}
			}
			
			if(SmartDB.isMultipleAnalysis()){
				final String elementuuidstr = SmartUtils.encodeHex(elementuuid);
				final String[] temp = new String[]{null};
				//for whatever reason when i did this as hibernate queries
				//i could not load entity tables in reports; I don't understand why
				//but it would try to load additional objects that weren't needed and this causes
				//all sorts of addition problems.
				s.doWork(new Work(){
					@Override
					public void execute(Connection c) throws SQLException {
						ResultSet rs = c.createStatement().executeQuery(
								"SELECT l.value from smart.i18n_label l join smart.language a on " + //$NON-NLS-1$
								"a.uuid = l.language_uuid where l.element_uuid = x'" + elementuuidstr +  //$NON-NLS-1$
								"' and a.code = '" + SmartDB.getConservationAreaConfiguration().getLanguage().getCode() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						try{
							if (rs.next()){
								temp[0] = rs.getString(1);
								return;
							}
						}finally{
							rs.close();
						}
						try{
							rs = c.createStatement().executeQuery(
									"SELECT l.value from smart.i18n_label l join smart.language a on " + //$NON-NLS-1$
									"a.uuid = l.language_uuid where l.element_uuid = x'" + elementuuidstr +  //$NON-NLS-1$
									"' and a.code = '" +  //$NON-NLS-1$
									SmartDB.getConservationAreaConfiguration().getLanguage().getCode().split("_")[0] + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							if (rs.next()){
								temp[0] = rs.getString(1);
								return;
							}
						}finally{
							rs.close();
						}
					}});
				if (temp[0] != null){
					description = temp[0];
				}
			}
		}
		if (lbl != null) {
			description = lbl.getValue();
		}
		return description;
	}
	
	/**
	 * Loads the label for the given element from the database.  Looks
	 * for the language in the conservation area than matches the code
	 * of the current language.  If not found uses the default
	 * language of the provided ca.
	 * 
	 * @param elementuuid the element uuid
	 * @param cauuid the uuid the element is associated with
	 * @return
	 */
	@Transient
	public static synchronized String getDescription(byte[] elementuuid, byte[] cauuid) {
		String description = ""; //$NON-NLS-1$
		if (elementuuid == null || cauuid == null){
			return description;
		}
		
		Label.LabelItemPK id = new Label.LabelItemPK();
		UuidItem h = new UuidItem();
		h.setUuid(elementuuid);
		id.setElement(h);
		
		for (ConservationArea c : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
			if (Arrays.equals(c.getUuid(), cauuid)){
				Language l = SmartUtils.findLanguageMatch(c.getLanguages());
				if (l != null){
					id.setLanguage(l);
				}else{
					id.setLanguage(c.getDefaultLanguage());
				}
				break;
			}
		}

		if (id.getLanguage() == null){
			return description;
		}
		
		Session s = HibernateManager.openSession();
		Label lbl = (Label) s.get(Label.class, id);
		if (lbl != null) {
			description = lbl.getValue();
		}
		return description;
	}

	@Transient
	public static byte[] setCode(byte[] elementuuid, String nl){
		return elementuuid;
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
	protected static class LabelItemPK implements Serializable {
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
			
			if (p.language == null || this.language == null ||
				p.element == null || this.element == null ){
				
				if (p.language == null && this.language == null && 
					p.element == null && this.element == null){
						return true;
				}
				return false;
			}
			
			return p.language.equals(this.language)
					&& Arrays.equals(p.element.getUuid(), this.element.getUuid());
		}
		@Override
		public int hashCode() {
		    int code = 0;
		    if (language!= null) {code += Arrays.hashCode(getLanguage().getUuid());}
		    if (element != null && element.getUuid() != null) {code += Arrays.hashCode(element.getUuid()); }
		    return code;
		  }
	}
}


