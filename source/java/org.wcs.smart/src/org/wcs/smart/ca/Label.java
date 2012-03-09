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
import java.util.Arrays;
import java.util.List;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

@Entity 
@Table (name="smart.i18n_label")

@AssociationOverrides({
	@AssociationOverride(name = "id.language", 
		joinColumns = @JoinColumn(name = "language_uuid")),
	@AssociationOverride(name = "id.elementuuid", 
		joinColumns = @JoinColumn(name = "element_uuid")) })

//TODO
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Label  {

	private LabelItemPK id;
	private String value;
	
	
//	private static GetDescriptionRunnable runnable = new GetDescriptionRunnable();
	
	
	@Transient
	public static synchronized String getDescription(byte[] elementuuid) {
		Session s = HibernateManager.openSession();

		Label.LabelItemPK id = new Label.LabelItemPK();
		id.setElementuuid(elementuuid);
		id.setLanguage(SmartDB.getCurrentLanguage());
		String description = "";

		Label lbl = (Label) s.get(Label.class, id);
		if (lbl == null) {
			// try for the default language
			id.setLanguage(SmartDB.getCurrentConservationArea()
					.getDefaultLanguage());
			lbl = (Label) s.get(Label.class, id);
		}
		if (lbl != null) {
			description = lbl.getValue();
		}
		return description;

	}
	
	public Label(){
		id = new LabelItemPK();
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
	public byte[] getElementuuid() {
		return id.elementuuid;
	}

	
	public void setElementuuid(byte[] elementuuid) {
		id.setElementuuid(elementuuid);
	}

	
	@Embeddable
	protected static class LabelItemPK implements Serializable {
		private Language language;
		private byte[] elementuuid;

		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="language_uuid", referencedColumnName="uuid")
		public Language getLanguage() {
			return language;
		}

		public void setLanguage(Language language) {
			this.language = language;
		}
		
		@Column(name="element_uuid")
		public byte[] getElementuuid() {
			return elementuuid;
		}

		public void setElementuuid(byte[] elementuuid) {
			this.elementuuid = elementuuid;
		}
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof LabelItemPK)){
				return false;
			}
			LabelItemPK p = (LabelItemPK)key;
			
			if (p.language == null || this.language == null ||
				p.elementuuid == null || this.elementuuid == null ){
				
				if (p.language == null && this.language == null && 
					p.elementuuid == null && this.elementuuid == null){
						return true;
				}
				return false;
			}
			
			return p.language.equals(this.language)
					&& Arrays.equals(p.elementuuid, this.elementuuid);
		}
		@Override
		public int hashCode() {
		    int code = 0;
		    if (language!=null) {code += Arrays.hashCode(getLanguage().getUuid());}
		    if (elementuuid!=null) {code += Arrays.hashCode(elementuuid); }
		    return code;
		  }
	}
	
	
	
	
}


class GetDescriptionRunnable implements Runnable{

		public byte[] elementuuid;
		public String description;
		
		@Override
		public void run() {
			
			Session s = HibernateManager.openSession();
//			s.beginTransaction();
			try {
				Label.LabelItemPK id = new Label.LabelItemPK();
				id.setElementuuid(elementuuid);
				id.setLanguage(SmartDB.getCurrentLanguage());
				
				Label lbl = (Label) s.load(Label.class, id);
				description = lbl.getValue();
				Label lbl2 = (Label) s.load(Label.class, id);
				description = lbl2.getValue();
				
				
				return;
			}catch (Exception ex){
				ex.printStackTrace();
			} finally {
//				s.getTransaction().rollback();
				//s.close();
			}
			
//			String query = "SELECT l.value from Label as l, Language as g WHERE l.languageuuid = g.uuid and "
//					+ "g.code = :nl and l.elementuuid = :elementuuid";
//			//HibernateManager.getCurrentSession().beginTransaction();
//			Session s = HibernateManager.openSession();
//			s.beginTransaction();
//			try {
//				Query q = s.createQuery(query);
//				q.setString("nl", nl).setBinary("elementuuid", elementuuid);
//				List ret = q.list();
//				// HibernateManager.getCurrentSession().getTransaction().commit();
//
//				if (ret.size() == 0) {
//					// lets lookup default language
//					// TODO "= 'true'" is a derby hack to make booleans work
//					query = "SELECT l.value from Label as l, Language as g WHERE l.languageuuid = g.uuid and "
//							+ "g.default = 'true' and l.elementuuid = :elementuuid";
//					q = s.createQuery(query);
//					q.setBinary("elementuuid", elementuuid);
//					ret = q.list();
//					if (ret.size() == 0) {
//						description = null;
//						return;
//					} else {
//						description = (String)ret.get(0);
//						return;
//					}
//				} else {
//					description = (String)ret.get(0);
//					return;
//				}
//			} finally {
//				s.getTransaction().rollback();
//				s.close();
//			}
			
		}
	}


