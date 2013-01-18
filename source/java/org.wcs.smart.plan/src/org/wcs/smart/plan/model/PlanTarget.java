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
package org.wcs.smart.plan.model;


import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

/**
 * Represents a patrol object
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@Table(name="smart.plan_target")
public class PlanTarget{

	private byte[] uuid;
	private byte[] plan_uuid;
	

	public String getDescription() {
		return description;
	}
	
	@Transient 
	public String getSummary() {
		return type + " " + op + " " + value;
	}

	public void setDescription(String description) {
		this.description = description;
	}



	private Plan plan;
	private String name;
	private String description;
	private double value;
	private String op;
	private String type;
	private tarCategory cat;
	
	public enum tarCategory {
		ALPHANUMERIC, SPATIAL, ADMIN; 
	}
	
	
	public PlanTarget(){
		
	}

	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public byte[] getUuid() {
		return uuid;
	}

	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="plan_uuid", referencedColumnName="uuid")
	public Plan getPlan() {
		return plan;
	}
	
	public void setPlan(Plan plan) {
		this.plan = plan;
	}
	
	
	@Override
	public int hashCode(){
		if (uuid != null){
			return Arrays.hashCode(uuid);
		}else{
			return super.hashCode();
		}
	}
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof PlanTarget){
			PlanTarget s = (PlanTarget)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return s.hashCode() == hashCode();
			}else if (s.getUuid() != null && this.getUuid() != null){
				return Arrays.equals(s.getUuid(), this.getUuid());
			}
		}
		return false;
	}

	@Column(name = "name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(name = "value")
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
		
	}
		
	@Column(name = "op")
	public String getOp() {
		return this.op;
	}
	public void setOp(String op) {
		this.op = op;
	}	
	
	@Column(name = "type")
	public String getType() {
		return this.type;
	}
	public void setType(String type) {
		this.type = type;
	}

	
	@Column(name = "category")
	public tarCategory getCat() {
		return cat;
	}

	public void setCat(tarCategory cat) {
		this.cat = cat;
	}
}
