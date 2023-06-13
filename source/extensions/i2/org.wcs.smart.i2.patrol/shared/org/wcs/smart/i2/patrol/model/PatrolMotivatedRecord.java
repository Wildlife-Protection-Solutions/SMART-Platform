package org.wcs.smart.i2.patrol.model;

import java.io.Serializable;
import java.util.Objects;

import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.patrol.model.Patrol;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "i_patrol_record_motivation", schema="smart")
public class PatrolMotivatedRecord {
	
	private PatrolMotivatedRecordPk id = new PatrolMotivatedRecordPk();	
	
	public PatrolMotivatedRecord(){
		
	}
	
	@EmbeddedId
	public PatrolMotivatedRecordPk getId(){
		return this.id;
	}
	public void setId(PatrolMotivatedRecordPk id){
		this.id = id;
	}
	
	@Override
	public boolean equals(Object o){
		if (o instanceof PatrolMotivatedRecord){
			return this.id.equals(((PatrolMotivatedRecord)o).id);
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
	public static class PatrolMotivatedRecordPk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private Patrol patrol;
		private IntelRecord record;
		

		public PatrolMotivatedRecordPk(){
			
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="i_record_uuid")
		public IntelRecord getIntelRecord() {
			return record;
		}

		public void setIntelRecord(IntelRecord record) {
			this.record = record;
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="patrol_uuid")
		public Patrol getPatrol() {
			return patrol;
		}

		public void setPatrol(Patrol patrol) {
			this.patrol = patrol;
		}
		
		@Override
		public boolean equals(Object key) {
			if (!key.getClass().equals(PatrolMotivatedRecordPk.class)) return false;
			
			PatrolMotivatedRecordPk other = (PatrolMotivatedRecordPk)key;
			return Objects.equals(this.patrol, other.patrol) &&
					Objects.equals(this.record, other.record);
		}
		
		@Override
		public int hashCode() {
		    return Objects.hash(this.patrol, this.record);
		  }
	}
	
}
