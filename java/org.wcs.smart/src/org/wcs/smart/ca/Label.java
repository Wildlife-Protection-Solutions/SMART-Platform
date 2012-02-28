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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.wcs.smart.hibernate.HibernateManager;

@Entity 
@Table (name="smart.i18n_label")
@IdClass(Label.LabelItemPK.class)
@BatchSize(size=100)
//TODO
//@Cacheable
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Label  {

	private byte[] elementuuid;
	private byte[] languageuuid;
	private String value;
	
	
	private static GetDescriptionRunnable runnable = new GetDescriptionRunnable();
	
	
	@Transient
	public static synchronized String getDescription(byte[] elementuuid, String nl){
		//TODO: review this
		runnable.nl = nl;
		runnable.elementuuid = elementuuid;
		runnable.description =null;
		Thread x = new Thread(runnable);
		x.start();
		
		try {
			x.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return runnable.description;

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
	
	@Id
	@Column(name="language_uuid")
	public byte[] getLanguageuuid() {
		return languageuuid;
	}

	public void setLanguageuuid(byte[] languageuuid) {
		this.languageuuid = languageuuid;
	}
	
	@Id
	@Column(name="element_uuid")
	public byte[] getElementuuid() {
		return elementuuid;
	}

	public void setElementuuid(byte[] elementuuid) {
		this.elementuuid = elementuuid;
	}

	
	@Embeddable
	private static class LabelItemPK implements Serializable {
		private byte[] languageuuid;
		private byte[] elementuuid;

		public LabelItemPK(){
			
		}
		public LabelItemPK(byte[] language, byte[] element){
			this.languageuuid = language;
			this.elementuuid = element;
		}
		
		public byte[] getLanguageuuid() {
			return languageuuid;
		}

		public void setLanguageuuid(byte[] languageuuid) {
			this.languageuuid = languageuuid;
		}
		
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
			
			if (p.languageuuid == null || this.languageuuid == null ||
				p.elementuuid == null || this.elementuuid == null ){
				
				if (p.languageuuid == null && this.languageuuid == null && 
					p.elementuuid == null && this.elementuuid == null){
						return true;
				}
				return false;
			}
			
			return Arrays.equals(p.languageuuid, this.languageuuid)
					&& Arrays.equals(p.elementuuid, this.elementuuid);
		}
	}
	
	@Override
	public int hashCode() {
	    int code = 0;
	    if (languageuuid!=null) {code += Arrays.hashCode(languageuuid);}
	    if (elementuuid!=null) {code += Arrays.hashCode(elementuuid); }
	    return code;
	  }
	
	
}


class GetDescriptionRunnable implements Runnable{
		public String nl;
		public byte[] elementuuid;
		public String description;
		
		@Override
		public void run() {
			String query = "SELECT l.value from Label as l, Language as g WHERE l.languageuuid = g.uuid and "
					+ "g.code = :nl and l.elementuuid = :elementuuid";
			//HibernateManager.getCurrentSession().beginTransaction();
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try {
				Query q = s.createQuery(query);
				q.setString("nl", nl).setBinary("elementuuid", elementuuid);
				List ret = q.list();
				// HibernateManager.getCurrentSession().getTransaction().commit();

				if (ret.size() == 0) {
					// lets lookup default language
					// TODO "= 'true'" is a derby hack to make booleans work
					query = "SELECT l.value from Label as l, Language as g WHERE l.languageuuid = g.uuid and "
							+ "g.default = 'true' and l.elementuuid = :elementuuid";
					q = s.createQuery(query);
					q.setBinary("elementuuid", elementuuid);
					ret = q.list();
					if (ret.size() == 0) {
						description = null;
						return;
					} else {
						description = (String)ret.get(0);
						return;
					}
				} else {
					description = (String)ret.get(0);
					return;
				}
			} finally {
				s.getTransaction().rollback();
				s.close();
			}
			
		}
	}


