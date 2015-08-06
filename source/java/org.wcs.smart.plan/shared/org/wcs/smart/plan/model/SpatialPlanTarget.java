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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.wcs.smart.SmartContext;
import org.wcs.smart.plan.IPlanLabelProvider;


/**
 * Represents a SpatialPlanTarget object
 * 
 * @author elitvin
 * @since 1.0.0
 */
@Entity
@DiscriminatorValue("SPATIAL")
public class SpatialPlanTarget extends PlanTarget {

	public static final String SUMMARY_KEY = "spatialsummary"; //$NON-NLS-1$
	
	private String description;
	private List<SpatialPlanTargetPoint> points;
	private Integer distanceForCompletion;
	
	
	@Column(name = "description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@OneToMany(fetch = FetchType.EAGER, mappedBy="planTarget", orphanRemoval=true, cascade={CascadeType.ALL})
//	@LazyCollection(LazyCollectionOption.EXTRA) //to be able to know collection size
	public List<SpatialPlanTargetPoint> getPoints() {
		if (points == null) {
			points = new ArrayList<SpatialPlanTargetPoint>();
		}
		return points;
	}

	public void setPoints(List<SpatialPlanTargetPoint> points) {
		this.points = points;
	}

	/** 
	 * 
	 * @return the distance in meters a patrol must come within to consider this target achieved.
	 */
	@Column(name = "success_distance")
	public int getDistanceForCompletion() {
		return distanceForCompletion;
	}

	public void setDistanceForCompletion(Integer distanceForCompletion) {
		this.distanceForCompletion = distanceForCompletion;
	}


	@Override
	public PlanTarget clone() {
		SpatialPlanTarget spt = new SpatialPlanTarget();
		super.clone(spt);
		ArrayList<SpatialPlanTargetPoint> targets = new ArrayList<SpatialPlanTargetPoint>();
		for (SpatialPlanTargetPoint pnt: getPoints()){
			SpatialPlanTargetPoint copy = pnt.clone();
			copy.setPlanTarget(spt);
			targets.add(copy);
		}
		spt.setPoints(targets);
		spt.setDescription(getDescription());
		spt.setDistanceForCompletion(getDistanceForCompletion());
		return spt;
	}

	@Override
	public String getSummary(Locale l) {
		return MessageFormat.format(
				SmartContext.INSTANCE.getClass(IPlanLabelProvider.class).getLabel(SUMMARY_KEY, l),
				getName(), getPoints().size());
	}

}