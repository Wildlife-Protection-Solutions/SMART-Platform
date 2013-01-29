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
package org.wcs.smart.intelligence.model;

import java.io.Serializable;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.patrol.model.Patrol;

/**
 * Mapping between patrols and intelligences to track intelligences that motivated certain patrol
 * 
 * @author elitvin
 * @since 1.0.0
 */
@Entity
@Table(name="smart.patrol_intelligence")
@AssociationOverrides({
	@AssociationOverride(name = "id.intelligence", 
		joinColumns = @JoinColumn(name = "intelligence_uuid")),
	@AssociationOverride(name = "id.patrol", 
		joinColumns = @JoinColumn(name = "patrol_uuid")) })
public class PatrolIntelligence {
	
	private PatrolIntelligencePk id = new PatrolIntelligencePk();

	@EmbeddedId
	public PatrolIntelligencePk getId() {
		return this.id;
	}
	public void setId(PatrolIntelligencePk id) {
		this.id = id;
	}

	@Transient
	public Patrol getPatrol() {
		return id.patrol;
	}
	public void setPatrol(Patrol patrol) {
		id.patrol = patrol;
	}
	
	@Transient
	public Intelligence getIntelligence() {
		return id.intelligence;
	}
	public void setIntelligence(Intelligence intelligence) {
		id.intelligence = intelligence;
	}
	
	/**
	 * Primary key for {@link PatrolIntelligence}
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	@Embeddable
	protected static class PatrolIntelligencePk implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private Patrol patrol;
		private Intelligence intelligence;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="patrol_uuid", referencedColumnName="uuid")
		public Patrol getPatrol() {
			return patrol;
		}
		public void setPatrol(Patrol patrol) {
			this.patrol = patrol;
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="intelligence_uuid", referencedColumnName="uuid")
		public Intelligence getIntelligence() {
			return intelligence;
		}
		public void setIntelligence(Intelligence intelligence) {
			this.intelligence = intelligence;
		}
		
	}
	

}
