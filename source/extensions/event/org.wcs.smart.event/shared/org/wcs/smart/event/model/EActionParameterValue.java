package org.wcs.smart.event.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "smart.e_action_parameter_value")
public class EActionParameterValue {
	
	public static final int PARAM_KEY_MAX_LENGTH = 128;
	
	private EActionParameterValuePk id = new EActionParameterValuePk();
	private String parameterValue;
	
	@EmbeddedId
	public EActionParameterValuePk getId(){
		return this.id;
	}
	public void setId(EActionParameterValuePk id){
		this.id = id;
	}
	
	/**
	 * the string representation of the parameter value
	 * @return
	 */
	@Column(name="parameter_value")
	public String getParameterValue() {
		return this.parameterValue;
	}
	
	public void setParameterValue(String parameterValue) {
		this.parameterValue = parameterValue;
	}
	/**
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o){
		if (o.getClass().equals(EActionParameterValue.class)) {
			return this.id.equals(((EActionParameterValue)o).id);
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
	 * Primary key object for category attribute association 
	 * 
	 */
	@Embeddable
	public static class EActionParameterValuePk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private EAction action;
		private String parameterKey;

		public EActionParameterValuePk(){
			
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="action_uuid")
		public EAction getAction() {
			return action;
		}

		public void setAction(EAction action) {
			this.action = action;
		}
		
		@Column(name="parameter_key")
		public String getParameterKey() {
			return parameterKey;
		}

		public void setParameterKey(String parameterKey) {
			this.parameterKey = parameterKey;
		}
		
		@Override
		public boolean equals(Object key) {
			if (this == key) return true;
			if (key == null) return false;
			if (!getClass().equals(key.getClass())) return false;
			
			EActionParameterValuePk p = (EActionParameterValuePk)key;
			return Objects.equals(action, p.action) && Objects.equals(parameterKey, p.parameterKey);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(action, parameterKey);
		  }
	}

}
