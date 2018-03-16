package org.wcs.smart.event.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

@Entity
@Table(name = "smart.e_action")
public class EAction extends UuidItem{
	
	public static final int MAX_ID_LENGTH = 128; 
			
	private ConservationArea ca;
	private String id;
	private String actionTypeKey;
	private List<EActionParameterValue> parameterValues;
	
	/**
	 * The conservation area 
	 * @return
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	/**
	 * The action identifier
	 * @return
	 */
	@Column(name="id")
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * The action type associated with this action
	 * 
	 * @return
	 */
	@Column(name="type_key")
	public String getActionTypeKey() {
		return this.actionTypeKey;
	}
	
	public void setActionTypeKey(String actionTypeKey) {
		this.actionTypeKey = actionTypeKey;
	}
	
	/**
	 * Sets the parameter values for the action type
	 * 
	 * @return
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.action", orphanRemoval=true, cascade={CascadeType.ALL})
	public List<EActionParameterValue> getParameters(){
		return this.parameterValues;
	}
	
	public void setParameters(List<EActionParameterValue> values) {
		this.parameterValues = values;
	}
}
