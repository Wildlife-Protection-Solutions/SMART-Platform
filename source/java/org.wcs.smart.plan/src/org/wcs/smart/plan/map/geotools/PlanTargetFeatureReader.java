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
package org.wcs.smart.plan.map.geotools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.PlanTargetStatus;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTargetPoint;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * SMART spaital plan target feature reader.
 * @author Emily
 * @since 1.0.0
 */
public class PlanTargetFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private Iterator<SpatialPlanTargetPoint> fIterator;
	
	private static GeometryFactory gf = new GeometryFactory();
	
	/**
	 * Creates a new feature reader
	 * @param plan plan
	 * @param subPlans if subplan targets only
	 * @param ftype feature type
	 */
	public PlanTargetFeatureReader(Plan plan, boolean subPlans,
			SimpleFeatureType ftype) {
		
		this.ftype = ftype;
		if (plan == null){
			fIterator = null;
			return;
		}
		List<SpatialPlanTargetPoint> pnt = new ArrayList<SpatialPlanTargetPoint>();
		if (!subPlans){
			for (PlanTarget pt : plan.getTargets()){
				if (pt instanceof SpatialPlanTarget){
					SpatialPlanTarget spt = (SpatialPlanTarget)pt;
					pnt.addAll(spt.getPoints());
				}
			}
		}else{
			List<Plan> plansToProcess = new ArrayList<Plan>();
			plansToProcess.addAll(plan.getChildren());
			while(plansToProcess.size() > 0){
				Plan kid = plansToProcess.remove(0);
				for (PlanTarget pt : kid.getTargets()){
					if (pt instanceof SpatialPlanTarget){
						SpatialPlanTarget spt = (SpatialPlanTarget)pt;
						pnt.addAll(spt.getPoints());
					}
				}
				plansToProcess.addAll(kid.getChildren());
			}
		}
	
		fIterator = pnt.iterator();
		
	}
	

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		fIterator = null;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#getFeatureType()
	 */
	@Override
	public SimpleFeatureType getFeatureType() {
		return ftype;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#hasNext()
	 */
	@Override
	public boolean hasNext() throws IOException {
		if (fIterator == null) return false;
		return fIterator.hasNext();
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {
		return getPointAsFeature(this.fIterator.next());
	}
	
	// builds a simple feature
	private SimpleFeature getPointAsFeature(SpatialPlanTargetPoint point){
		//
		Object data[] = new Object[7];
		data[0] = gf.createPoint(new Coordinate(point.getX(), point.getY()));
		data[1] = ftype.getName() + "." + UuidUtils.uuidToString(point.getUuid()); //$NON-NLS-1$
		data[2] = point.getPlanTarget().getName();
		data[3] = point.getPlanTarget().getDescription();
		if (point.getPlanTarget().getCurrentStatus() != null){
			data[4] = point.getPlanTarget().getCurrentStatus().getDisplayString();
			data[5] = point.getPlanTarget().getCurrentStatus().getStatus().key;
		}else{
			data[4] = PlanTargetStatus.Status.UNKNOWN.guiName;
			data[5] = PlanTargetStatus.Status.UNKNOWN.key;
		}
		data[6] = point.getPlanTarget().getPlan().getId();
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
	
}
