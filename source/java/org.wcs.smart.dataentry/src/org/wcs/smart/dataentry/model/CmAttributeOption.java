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
package org.wcs.smart.dataentry.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.dataentry.internal.Messages;

/**
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "smart.cm_attribute_option")
public class CmAttributeOption extends UuidItem {

	public static final String ID_IS_VISIBLE = "IS_VISIBLE"; //$NON-NLS-1$
	public static final String ID_DEFAULT_VALUE = "DEFAULT_VALUE"; //$NON-NLS-1$
	public static final String ID_MULTISELECT = "MULTISELECT"; //$NON-NLS-1$
	public static final String ID_FLATTEN_TREE = "FLATTEN_TREE"; //$NON-NLS-1$
	public static final String ID_NUMERIC = "NUMERIC"; //$NON-NLS-1$
	public static final String ID_CUSTOM_CONFIG = "CUSTOM_CONFIG"; //$NON-NLS-1$
	public static final String ID_ENTER_ONCES = "ENTER_ONCE"; //$NON-NLS-1$
	
	public static enum EnterOnceType {
		NONE(Messages.CmAttributeOption_EnterOnceType_NONE),
		START(Messages.CmAttributeOption_EnterOnceType_START),
		END(Messages.CmAttributeOption_EnterOnceType_END);

		private String guiName;
		private EnterOnceType(String guiName) {
			this.guiName = guiName;
		}
		public String getGuiName() {
			return guiName;
		}
	}
	
	private CmAttribute cmAttribute;
	private String optionId; //NOTE: we cannot map this as emun in case we want to support some external options
	private String stringValue;
	private Double doubleValue;
	private byte[] uuidValue;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="cm_attribute_uuid", referencedColumnName="uuid")
	public CmAttribute getCmAttribute() {
		return cmAttribute;
	}
	public void setCmAttribute(CmAttribute cmAttribute) {
		this.cmAttribute = cmAttribute;
	}
	
	@Column(name="option_id")
	public String getOptionId() {
		return optionId;
	}
	public void setOptionId(String optionId) {
		this.optionId = optionId;
	}
	
	@Column(name="string_value")
	public String getStringValue() {
		return stringValue;
	}
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}
	
	@Column(name="number_value")
	public Double getDoubleValue() {
		return doubleValue;
	}
	public void setDoubleValue(Double doubleValue) {
		this.doubleValue = doubleValue;
	}
	
	/**
	 * Date attribute types are stored
	 * as in the string field in the ISO8601 format
	 * of yyyy-mm-dd.  This is a transient
	 * function which converts the string value to 
	 * a date.
	 * 
	 * @return
	 */
	@Transient
	public Date getDateValue(){
		if (getStringValue() == null){
			return null;
		}
		return java.sql.Date.valueOf(getStringValue());
	}
	
	/**
	 * This calls setStringValue formating the
	 * date as required for SMART
	 * @return
	 */
	@Transient
	public void setDateValue(Date date){
		if (date == null){
			setStringValue(null);
			return;
		}
		java.sql.Date tmp = new java.sql.Date(date.getTime());
		setStringValue(tmp.toString());
	}

	@Transient
	public Boolean getBooleanValue() {
		if (doubleValue == null) {
			return null;
		} else {
			return doubleValue > 0.5;
		}
	}
	@Transient
	public void setBooleanValue(Boolean value) {
		if (value == null) {
			this.doubleValue = null;
		} else {
			this.doubleValue = value ? 1.0 : 0.0;
		}
	}
	
	@Column(name="uuid_value")
	public byte[] getUuidValue() {
		return uuidValue;
	}
	public void setUuidValue(byte[] uuidValue) {
		this.uuidValue = uuidValue;
	}
	
}
