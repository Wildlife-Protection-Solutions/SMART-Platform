/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Global fire watch pull request target
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "connect.gfw")
public class GlobalForestWatch extends ConnectUuidItem{

	private AlertType alertType;
	private LocalDateTime lastDataDate;
	private SmartUser creator;
	private int alertLevel;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="alert_uuid", referencedColumnName="uuid")
	public AlertType getAlertType() {
		return this.alertType;
	}
	
	public void setAlertType(AlertType alertType) {
		this.alertType = alertType;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="creator_uuid", referencedColumnName="uuid")
	public SmartUser getCreator() {
		return this.creator;
	}
	
	public void setCreator(SmartUser creator) {
		this.creator = creator;
	}
	
	@Column(name="last_data")
	public LocalDateTime getLastDataDate() {
		return this.lastDataDate;
	}
	
	public void setLastDataDate(LocalDateTime date) {
		this.lastDataDate = date;
	}
	
	@Column(name="level")
	public int getLevel() {
		return this.alertLevel;
	}
	
	public void setLevel(int alertLevel) {
		this.alertLevel = alertLevel;
	}
}
