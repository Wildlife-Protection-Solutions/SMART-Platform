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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "connect.map_layers")
public class MapLayer extends ConnectUuidItem{
		
	private Integer layerType;
	private String token; 
	private String mapboxId;
	private String wmsLayerList;
	private String layerName;
	private boolean active;
	private int layerOrder;
	
	@Column(name="layer_type")
	public Integer getLayerType() {
		return layerType;
	}
	public void setLayerType(Integer layerType) {
		this.layerType = layerType;
	}
	
	@Column(name="token")
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	
	@Column(name="mapboxid")
	public String getMapboxId() {
		return mapboxId;
	}
	public void setMapboxId(String mapboxId) {
		this.mapboxId = mapboxId;
	}
	
	@Column(name="wms_layer_list")
	public String getWmsLayerList() {
		return wmsLayerList;
	}
	public void setWmsLayerList(String wmsLayerList) {
		this.wmsLayerList = wmsLayerList;
	}
	
	@Column(name="layer_name")
	public String getLayerName() {
		return layerName;
	}
	public void setLayerName(String layerName) {
		this.layerName = layerName;
	}
	
	@Column(name="active")
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	
	@Column(name="layer_order")
	public int getLayerOrder() {
		return layerOrder;
	}
	public void setLayerOrder(int layerOrder) {
		this.layerOrder = layerOrder;
	}	
}
