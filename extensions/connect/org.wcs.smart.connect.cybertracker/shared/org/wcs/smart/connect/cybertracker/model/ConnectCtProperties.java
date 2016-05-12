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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Smart connect alert for configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
@Entity
@Table(name="smart.connect_ct_properties")
public class ConnectCtProperties extends UuidItem {
	
	public static final int PING_FREQUENCY_MIN_VALUE = 0;
	public static final int PING_FREQUENCY_MAX_VALUE = 999999999;
	public static final int PING_FREQUENCY_DEFAULT_VALUE = 0;

	private ConfigurableModel model;
	private Integer pingFrequency;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="cm_uuid", referencedColumnName="uuid")
	public ConfigurableModel getModel() {
		return model;
	}
	public void setModel(ConfigurableModel model) {
		this.model = model;
	}
	
	@Column(name="ping_frequency")
	public Integer getPingFrequency() {
		return pingFrequency;
	}
	public void setPingFrequency(Integer pingFrequency) {
		this.pingFrequency = pingFrequency;
	}
	
}
