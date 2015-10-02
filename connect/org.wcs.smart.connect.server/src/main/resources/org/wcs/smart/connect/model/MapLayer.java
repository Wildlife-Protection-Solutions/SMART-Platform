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
