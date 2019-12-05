/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

@Entity
@Table(name="smart.paws_service")
public class PawsService extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	private ConservationArea ca;
	private String apikey;
	private String heatmapapi;
	private String taskapi;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name="api_key")
	public String getApiKey() {
		return this.apikey;
	}
	
	public void setApiKey(String apikey) {
		this.apikey = apikey;
	}
	
	@Column(name="heatmap_api")
	public String getHeatmapApi() {
		return this.heatmapapi;
	}
	
	public void setHeatmapApi(String heatmapapi) {
		this.heatmapapi = heatmapapi;
	}
	
	@Column(name="task_api")
	public String getTaskApi() {
		return this.taskapi;
	}
	
	public void setTaskApi(String taskapi) {
		this.taskapi = taskapi;
	}
	
	@Transient
	public boolean isConfigured() {
		if (taskapi == null || heatmapapi == null || apikey== null || taskapi.isEmpty() || heatmapapi.isBlank() || apikey.isBlank() ) return false;
		return true;
	}
}
