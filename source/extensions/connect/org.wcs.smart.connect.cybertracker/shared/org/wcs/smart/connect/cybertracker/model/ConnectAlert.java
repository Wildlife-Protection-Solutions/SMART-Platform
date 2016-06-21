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
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Smart connect alert for configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
@Entity
@Table(name="smart.connect_alert")
public class ConnectAlert extends UuidItem {

	private ConfigurableModel model; 
	private UuidItem alertItem;
	private CmAttribute attrubute; 
	private String type;
	private Integer level;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="cm_uuid", referencedColumnName="uuid")
	public ConfigurableModel getModel() {
        return model;
    }
    public void setModel(ConfigurableModel model) {
        this.model = model;
    }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="alert_item_uuid", referencedColumnName="uuid")
    public UuidItem getAlertItem() {
		return alertItem;
	}
    public void setAlertItem(UuidItem alertItem) {
		this.alertItem = alertItem;
	}
    
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="cm_attribute_uuid", referencedColumnName="uuid")
    public CmAttribute getAttrubute() {
		return attrubute;
	}
    public void setAttrubute(CmAttribute attrubute) {
		this.attrubute = attrubute;
	}
    
	@Column(name="Type")
	public String getTypeInternal(){
		return type;
	}
	
	public void setTypeInternal(String type){
		this.type = type;
	}
	
	@Transient
    public UUID getType() {
		if (type == null) return null;
		return UUID.fromString(getTypeInternal());
	}
	
	@Transient
    public void setType(UUID type) {
		if (type == null){
			this.type = null;
		}else{
			this.type = type.toString();
		}
	}
    
	@Column(name="level")
    public Integer getLevel() {
		return level;
	}
    public void setLevel(Integer level) {
		this.level = level;
	}
}
