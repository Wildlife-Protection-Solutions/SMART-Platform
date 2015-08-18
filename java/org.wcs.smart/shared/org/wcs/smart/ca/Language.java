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

import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.util.I18nUtil;

/**
 * Supported lanaguage.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.language")
public class Language extends UuidItem{

	
	private String code;
	private ConservationArea ca;
	private boolean isDefault;
	
	@Transient
	private Locale locale;

	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	
	@ManyToOne
	public ConservationArea getCa() {
		return ca;
	}
	public void setCa(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name="isdefault")
	public boolean isDefault() {
		return isDefault;
	}
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
		
	/**
	 * Two languages are considered the same
	 * if they have the same code;
	 * @param l
	 * @return
	 */
	public boolean isSame(Language l){
		return getCode().trim().equals(l.getCode().trim());
	}
	
	/**
	 * Returns a common string to display the
	 * language in the GUI.  Combines the
	 * locale display name with the code.
	 * 
	 * @return name and code
	 * 
	 */
	@Transient
	public String getLabel(){
		Locale l = getLocale();
		if (l != null){
			return l.getDisplayName() + " [" + getCode().trim() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return getCode();
	}
	
	@Transient
	private Locale getLocale(){
		if (locale == null){
			locale = I18nUtil.stringToLocale(getCode());
		}
		return locale;
	}
	
	/**
	 * 
	 * @return the locale display name of unique
	 * code if no locale name found
	 */
	@Transient
	public String getDisplayName(){
		if (getLocale() != null){
			return getLocale().getDisplayName();
		}
		return getCode();
	}
	
	
}
