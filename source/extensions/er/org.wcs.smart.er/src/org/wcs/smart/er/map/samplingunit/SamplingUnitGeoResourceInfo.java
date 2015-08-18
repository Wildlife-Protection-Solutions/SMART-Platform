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
package org.wcs.smart.er.map.samplingunit;

import java.io.IOException;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Georesource Information for a sampling unit georesource 
 * @author Emily
 * @since 1.0.0
 */
public class SamplingUnitGeoResourceInfo extends IGeoResourceInfo {

	
	public SamplingUnitGeoResourceInfo( SamplingUnitGeoResource resource, IProgressMonitor monitor){
		String title = Messages.SamplingUnitGeoResourceInfo_SuLayerNamePrefix + " - " + GeometryType.valueOf(resource.getDataType()).getGuiName(Locale.getDefault());  //$NON-NLS-1$
		try {
			String sdName = ((SamplingUnitService)resource.service(monitor)).getCachedName();
			title += " [" + sdName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IOException e) {
			EcologicalRecordsPlugIn.log(e.getMessage(), e);
		}
		super.title = title;
		computeBounds(resource, monitor);
	}
	
	/**
	 * Recomputes the bounds for this resource info.
	 * 
	 * @param resource resource source
	 */
	public void computeBounds(final SamplingUnitGeoResource resource, final IProgressMonitor monitor){
		Job boundsJob = new Job("Computing bounds for sampling units"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					@SuppressWarnings("unchecked")
					FeatureSource<SimpleFeatureType, SimpleFeature> fs = resource.resolve(FeatureSource.class, monitor);
					final ReferencedEnvelope env = new ReferencedEnvelope(fs.getSchema().getCoordinateReferenceSystem());
					SamplingUnitGeoResourceInfo.this.bounds = env;
					fs.getFeatures().accepts(new FeatureVisitor() {
						@Override
						public void visit(Feature f) {
							BoundingBox bb = f.getBounds();
							env.expandToInclude(new Envelope(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY()));
						}
					}, null);
					
				} catch (Exception e) {
					EcologicalRecordsPlugIn.log(e.getMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
		boundsJob.setSystem(true);
		boundsJob.schedule();
		try {
			boundsJob.join();
		} catch (InterruptedException e) {
			EcologicalRecordsPlugIn.log(e.getMessage(), e);
		}
		

	}
}
