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
package org.wcs.smart.plan.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.ca.ISmartPoint;

/**
 * Point for {@link SpatialPlanTarget}
 * 
 * @author elitvin
 * @since 1.0.0
 */
@Entity
@Table(name = "smart.plan_target_point")
public class SpatialPlanTargetPoint implements ISmartPoint {

	private UUID uuid;
    private SpatialPlanTarget planTarget;
	private double x;
	private double y;

	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	@ManyToOne
	@JoinColumn(name="plan_target_uuid", referencedColumnName="uuid")
	public SpatialPlanTarget getPlanTarget() {
		return planTarget;
	}
	public void setPlanTarget(SpatialPlanTarget planTarget) {
		this.planTarget = planTarget;
	}
	
	@Override
	@Column(name="x")
	public double getX() {
		return x;
	}
	@Override
	public void setX(double x) {
		this.x = x;
	}

	@Override
	@Column(name="y")
	public double getY() {
		return y;
	}
	@Override
	public void setY(double y) {
		this.y = y;
	}
	
	/**
	 * Clones the target point copying the x, y, and plan target values.
	 * Uuid is not copied.
	 */
	public SpatialPlanTargetPoint clone(){
		SpatialPlanTargetPoint clone = new SpatialPlanTargetPoint();
		clone.setX(getX());
		clone.setY(getY());
		clone.setPlanTarget(getPlanTarget());
		return clone;
	}

}
