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
package org.wcs.smart.connect.model;

import java.io.Serializable;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Key value pairs of options for connect server configuration.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.connect_server_option")
@AssociationOverrides({
	@AssociationOverride(name = "id.server", 
		joinColumns = @JoinColumn(name = "server_uuid")),
	@AssociationOverride(name = "id.option", 
		joinColumns = @JoinColumn(name = "option_key")) })
public class ConnectServerOption {
	
	/*
	 * server connection options
	 */
	public enum Option{
		MAX_RETRY_DOWNLOAD (10),
		MAX_RETRY_UPLOAD(100),
		RETY_WAIT_TIME(500),
		MAX_PROCESSING_WAIT_TIME(5 * 60 * 1000l),
		SYNC_AUTOMATICALLY(Boolean.FALSE),
		SYNC_MINUTE(20),
		SYNC_PROMPT_PASSWORD(Boolean.FALSE),
		SYNC_DOWNLOAD(Boolean.FALSE),
		SYNC_AUTO_UPLOAD(Boolean.FALSE),
		DOWNLOAD_ON_STARTUP(Boolean.FALSE),
		UPLOAD_ON_STARTUP(Boolean.TRUE),
		DOWNLOAD_ON_SHUTDOWN(Boolean.FALSE),
		UPLOAD_ON_SHUTDOWN(Boolean.TRUE),
		PACKAGE_PROMPT(Boolean.FALSE),
		PACKAGE_PROMPT_SIZE(20);
		
		Object defaultValue;
		
		private Option(Object defaultValue){
			this.defaultValue = defaultValue;
		}
		
		public String getDefaultValueAsString(){
			return this.defaultValue.toString();
		}
		
		public Boolean getDefaultValueAsBoolean(){
			if (defaultValue instanceof Boolean){
				return (Boolean) this.defaultValue;
			}
			return null;
		}
		public Integer getDefaultValueAsInt(){
			if (defaultValue instanceof Integer){
				return (Integer) this.defaultValue;
			}
			return null;
		}
	}
	
	private String value;
	private ConnectServerOptionPk id = new ConnectServerOptionPk();
	
	
	/**
	 * 
	 * @return primary key for association
	 */
	@EmbeddedId
	public ConnectServerOptionPk getId(){
		return id;
	}
	/**
	 * 
	 * @param id primary key for association
	 */
	public void setId(ConnectServerOptionPk id){
		this.id = id;
	}
	
	/**
	 * Option key value
	 * @return
	 */
	@Column(name="value")
	public String getValue(){
		return this.value;
	}
	
	public void setValue(String value){
		this.value = value;
	}
	
	
	/**
	 * 
	 * @return the connect server 
	 */
	@Transient 
	public ConnectServer getServer(){
		return id.getServer();
	}
	
	public void setServer(ConnectServer server){
		id.setServer(server);
	}
	
	/**
	 * @return the server option
	 */
	@Transient 
	public Option getOption(){
		return id.getOption();
	}
	
	public void setOption(Option option){
		id.setOption(option);
	}
	
	@Override
	public boolean equals(Object o){
		if (o instanceof ConnectServerOption){
			return this.id.equals(((ConnectServerOption)o).id);
		}
		return false;
	}
	
	/**
	 * @return
	 */
	@Override
	public int hashCode(){
		return id.hashCode();
	}
	
	/**
	 * Primary key object
	 * 
	 */
	@Embeddable
	private static class ConnectServerOptionPk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private ConnectServer server;
		private Option option;
		

		public ConnectServerOptionPk(){
			
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		//@JoinColumn(name="server_uuid")
		//@Column(name="server_uuid")
		public ConnectServer getServer() {
			return server;
		}

		public void setServer(ConnectServer server) {
			this.server = server;
		}
		
		@Column(name="option_key")
		@Enumerated(EnumType.STRING)
		public Option getOption() {
			return option;
		}

		public void setOption(Option option) {
			this.option = option;
		}
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof ConnectServerOptionPk)){
				return false;
			}
			ConnectServerOptionPk p = (ConnectServerOptionPk)key;
			
			if (p.server == null || this.server == null ||
				p.option == null || this.option == null ){
				
				if (p.server == null && this.server == null && 
					p.option == null && this.option == null){
						return true;
				}
				return false;
			}
			
			return p.server.equals(this.server) &&
					p.option.equals(this.option);
		}
		@Override
		public int hashCode() {
		    int code = 0;
		    if (server != null) {code += server.hashCode();}
		    code *= 31;
		    if (option != null) {code += option.hashCode(); }
		    return code;
		  }
	} 

}
