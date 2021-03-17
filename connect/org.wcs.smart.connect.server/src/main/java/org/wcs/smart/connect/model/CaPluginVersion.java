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

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Represents the valid plugins for each conservation area and their
 * associated versions.
 * @author Emily
 *
 */
@Entity
@Table(name="connect.ca_plugin_version")
@AssociationOverrides({
	@AssociationOverride(name = "id.conservationAreaUuid", 
		joinColumns = @JoinColumn(name = "ca_uuid")),
	@AssociationOverride(name = "id.pluginId", 
		joinColumns = @JoinColumn(name = "attribute_uuid")) })
public class CaPluginVersion {

	private String version;
	private CaPluginVersionPk id = new CaPluginVersionPk();

	
	public CaPluginVersion(){
		
	}
	
	@EmbeddedId
	public CaPluginVersionPk getId(){
		return this.id;
	}
	public void setId(CaPluginVersionPk id){
		this.id = id;
	}
	
	public String getVersion(){
		return this.version;
	}
	
	public void setVersion(String version){
		this.version = version;
	}
	
	@Transient
	public UUID getConservationAreaUuid(){
		return this.id.getConservationAreaUuid();
	}
	public void setConservationAreaUuid(UUID uuid){
		this.id.setConservationAreaUuid(uuid);
	}
	
	@Transient
	public String getPluginId(){
		return this.id.getPluginId();
	}
	public void setPluginId(String pluginid){
		this.id.setPluginId(pluginid);
	}
	@Embeddable
	private static class CaPluginVersionPk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private UUID uuid;
		private String pluginid;
		
		public CaPluginVersionPk(){
		}
	
		@Column(name="ca_uuid")
		public UUID getConservationAreaUuid(){
			return this.uuid;
		}
		public void setConservationAreaUuid(UUID uuid){
			this.uuid = uuid;
		}
		
		@Column(name="plugin_id")
		public String getPluginId(){
			return this.pluginid;
		}
		public void setPluginId(String pluginid){
			this.pluginid = pluginid;
		}
		
		public int hashCode(){
			return  uuid.hashCode() * 31 + pluginid.hashCode();
		}
		
		public boolean equals(Object other){
			if (other instanceof CaPluginVersionPk){
				if (uuid == null || pluginid == null){
					return super.equals(other);
				}
				return uuid.equals(((CaPluginVersionPk)other).uuid) &&
						pluginid.equals(((CaPluginVersionPk)other).pluginid);
			}
			return false;
		}	
	}
}
