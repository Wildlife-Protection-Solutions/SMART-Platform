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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * A object class which represents the i18n_label table.
 * <p>
 * This is similar to the Label object but does not
 * user a has_label object as the primary key.  Instead
 * it references the element_uuid automatically.
 *  </p>
 *  <p>
 *  This is used for description fields and can be used
 *  for other strings that need to be internationalized using
 *  a field other than the entity uuid.
 *  
 *  </p>
 * @author Emily
 * @since 1.0.0
 */

@Entity 
@Table (name="smart.i18n_label")

public class DescriptionLabel {

	private DescLabelItemPK id;
	private String value;

	
	public DescriptionLabel(){
		id = new DescLabelItemPK();
	}
	
	
	public String getValue(){
		return value;
	}
	public void setValue(String label){
		this.value = label;
	}
	
	@EmbeddedId
	public DescLabelItemPK getId(){
		return this.id;
	}
	public void setId(DescLabelItemPK id){
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
		return id.element;
	}

	
	public void setElement(byte[] elementuuid) {
		id.setElement(elementuuid);
	}

	
	@Embeddable
	protected static class DescLabelItemPK implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Language language;
		private byte[] element;

		
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name="language_uuid", referencedColumnName="uuid")
		public Language getLanguage() {
			return language;
		}

		public void setLanguage(Language language) {
			this.language = language;
		}
		
		@Column(name="element_uuid")
		public byte[] getElement() {
			return element;
		}

		public void setElement(byte[] element) {
			this.element = element;
		}
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof DescLabelItemPK)){
				return false;
			}
			DescLabelItemPK p = (DescLabelItemPK)key;
			
			if (p.getLanguage() == null || this.getLanguage() == null ||
				p.getElement() == null || this.getElement() == null ){
				
				if (p.getLanguage() == null && this.getLanguage() == null && 
					p.getElement() == null && this.getElement() == null){
						return true;
				}
				return false;
			}
			
			return p.getLanguage().equals(this.getLanguage())
					&& Arrays.equals(p.getElement(), this.getElement());
		}
		@Override
		public int hashCode() {
		    int code = 0;
		    if (getLanguage() != null) {code += Arrays.hashCode(getLanguage().getUuid());}
		    if (getElement() != null) {code += Arrays.hashCode(getElement()); }
		    return code;
		  }
	}
}