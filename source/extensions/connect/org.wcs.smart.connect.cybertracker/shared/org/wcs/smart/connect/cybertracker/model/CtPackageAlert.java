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
package org.wcs.smart.connect.cybertracker.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.util.UuidUtils;

/**
 * Smart connect alert for configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
//@Entity
//@Table(name="smart.connect_alert")
public class CtPackageAlert extends UuidItem {

	/*
	 * Values for connect alert level
	 */
	public static enum Level{
		ONE(1),
		TWO(2),
		THREE(3),
		FOUR(4),
		FIVE(5);
		
		public int value;
		
		private Level( int value){
			this.value = value;
		}
	}
	
	private ICtPackage ctpackage; 
	
	//this is cmnode
	private CmNode cmNode;
	private CmAttribute attrubute;
	private UuidItem attributeItem;
	
	private UUID type;
	private Integer level;

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name="cm_uuid", referencedColumnName="uuid")
	public ICtPackage getPackage() {
        return ctpackage;
    }
    public void setPackage(ICtPackage ctpackage) {
        this.ctpackage = ctpackage;
    }

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name="alert_item_uuid", referencedColumnName="uuid")
    public CmNode getCmNode() {
		return cmNode;
	}
    public void setCmNode(CmNode cmNode) {
		this.cmNode = cmNode;
	}
    
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name="cm_attribute_uuid", referencedColumnName="uuid")
    public CmAttribute getCmAttrubute() {
		return attrubute;
	}
    public void setCmAttrubute(CmAttribute attrubute) {
		this.attrubute = attrubute;
	}
    
    public UuidItem getCmAttributeItem() {
  		return attributeItem;
  	}
      public void setCmAttributeItem(UuidItem attributeItem) {
  		this.attributeItem = attributeItem;
  	}
    public UUID getType() {
		return type;
	}
	
    public void setType(UUID type) {
		this.type = type;
	}
    
//	@Column(name="level")
    public Integer getLevel() {
		return level;
	}
    public void setLevel(Integer level) {
		this.level = level;
	}
}
