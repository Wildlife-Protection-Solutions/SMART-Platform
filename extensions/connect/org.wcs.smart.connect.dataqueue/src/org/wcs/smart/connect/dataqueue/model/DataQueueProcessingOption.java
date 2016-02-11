package org.wcs.smart.connect.dataqueue.model;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name ="smart.connect_data_queue_option")
public class DataQueueProcessingOption {

	private String value;
	private DataQueueProcessingOptionPk id = new DataQueueProcessingOptionPk();
	
	
	/**
	 * 
	 * @return primary key for association
	 */
	@EmbeddedId
	public DataQueueProcessingOptionPk getId(){
		return id;
	}
	/**
	 * 
	 * @param id primary key for association
	 */
	public void setId(DataQueueProcessingOptionPk id){
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
	public UUID getConservationArea(){
		return id.getConservationArea();
	}
	
	public void setConservationArea(UUID server){
		id.setConservationArea(server);
	}
	
	/**
	 * @return the server option
	 */
	@Transient 
	public String getOptionKey(){
		return id.getOptionKey();
	}
	
	public void setOptionKey(String optionKey){
		id.setOptionKey(optionKey);
	}
	
	@Override
	public boolean equals(Object o){
		if (o instanceof DataQueueProcessingOption){
			return this.id.equals(((DataQueueProcessingOption)o).id);
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
	public static class DataQueueProcessingOptionPk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private UUID caUuid;
		private String optionKey;
		

		public DataQueueProcessingOptionPk(){
			
		}
		
		@Column(name="ca_uuid")
		public UUID getConservationArea() {
			return caUuid;
		}

		public void setConservationArea(UUID caUuid) {
			this.caUuid = caUuid;
		}
		
		@Column(name="keyid")
		public String getOptionKey() {
			return optionKey;
		}

		public void setOptionKey(String optionKey) {
			this.optionKey = optionKey;
		}
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof DataQueueProcessingOptionPk)){
				return false;
			}
			DataQueueProcessingOptionPk p = (DataQueueProcessingOptionPk)key;
			
			if (p.caUuid == null || this.caUuid == null ||
				p.optionKey == null || this.optionKey == null ){
				
				if (p.caUuid == null && this.caUuid == null && 
					p.optionKey == null && this.optionKey == null){
						return true;
				}
				return false;
			}
			
			return p.caUuid.equals(this.caUuid) &&
					p.optionKey.equals(this.optionKey);
		}
		@Override
		public int hashCode() {
		    int code = 0;
		    if (caUuid != null) {code += caUuid.hashCode();}
		    code *= 31;
		    if (optionKey != null) {code += optionKey.hashCode(); }
		    return code;
		  }
	} 

}
