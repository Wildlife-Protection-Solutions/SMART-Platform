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
import java.text.MessageFormat;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Data source for sampling units.  This will find
 * all sampling units associated with a given survey design. 
 * It contains two layers, one for plots and one for transects.
 * 
 * 
 * @author Emily
 *
 */
public class SamplingUnitDataSource extends AbstractDataStore{

	private SurveyDesign sd;
	
	private SimpleFeatureType tType;
	private SimpleFeatureType pType;
	private SimpleFeatureType rType;
	
	/**
	 * Creates a new data source for the given survey design.
	 * @param sd
	 */
	public SamplingUnitDataSource(SurveyDesign sd){
		this.sd = sd;
	}
	
	/**
	 * 
	 * @return the design associated with the survey
	 */
	public SurveyDesign getDesign(){
		return this.sd;
	}
	/**
	 * Updates the survey design; should be called whenever a modification
	 * is made.
	 * 
	 * @param sd new survey design
	 */
	public void update(SurveyDesign sd){
		this.sd = sd;
		this.tType = null;
		this.pType = null;
		this.rType = null;
	}
	
	@Override
	public String[] getTypeNames() throws IOException {
		return new String[]{
				SamplingUnitType.PLOT.name(), 
				SamplingUnitType.TRANSECT.name(),
				SamplingUnitType.RECON.name()};
	}

	@Override
	public SimpleFeatureType getSchema(String typeName) throws IOException {
		try{
			if (typeName.equals(SamplingUnitType.TRANSECT.name())){
				if (tType == null){
					tType = createType(typeName);
				}
				return tType;
			}else if (typeName.equals(SamplingUnitType.PLOT.name())){
				if (pType == null){
					pType = createType(typeName);
				}
				return pType;
			}else if (typeName.equals(SamplingUnitType.RECON.name())){
				if (rType == null){
					rType = createType(typeName);
				}
				return rType;
			}
		}catch (SchemaException ex){
			throw new IOException(ex);
		}
		throw new IOException(MessageFormat.format(Messages.SamplingUnitDataSource_TypeNotSupported, new Object[]{typeName}));
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(
			String typeName) throws IOException {
		return new SamplingUnitFeatureReader(sd, getSchema(typeName));
	}

	/**
	 * Creates the survey feature type.
	 */
	private SimpleFeatureType createType(String typeName) throws SchemaException{
		
		SimpleFeatureType type =  DataUtilities.createType("smart." + typeName, //$NON-NLS-1$ 
					getFeatureSchemaDef(typeName)); 
		return type;
	}
	

	private String getFeatureSchemaDef(final String typeName){
	
		
		final StringBuilder sb = new StringBuilder();
		
		if (typeName.equals(SamplingUnitType.RECON.name())){
			sb.append("fid:String,"); //$NON-NLS-1$
			sb.append("id:String,"); //$NON-NLS-1$
			sb.append("mission_id:String,"); //$NON-NLS-1$
			sb.append("survey_id:String,");  //$NON-NLS-1$
			sb.append("date:Date,"); //$NON-NLS-1$
			sb.append("geom:LineString:srid=4326"); //$NON-NLS-1$
			return sb.toString();
		}
		
		Job j = new Job("build feature schema job"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					SurveyDesign lDesign = (SurveyDesign) s.load(SurveyDesign.class, sd.getUuid());
					
					sb.append("fid:String,id:String"); //$NON-NLS-1$
					HashSet<String> names = new HashSet<String>();
					for (SurveyDesignSamplingUnitAttribute sua : lDesign.getSamplingUnitAttributes()){
					
						sb.append(","); //$NON-NLS-1$
						String name = sua.getSamplingUnitAttribute().getName();
						name = name.replaceAll(" ", "_");  //$NON-NLS-1$//$NON-NLS-2$
						name = name.replaceAll("[^\\p{L}\\p{Nd}_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
					
						String tempname = name;
						int cnt = 1;
						while(names.contains(tempname)){
							tempname = name + "_" + cnt; //$NON-NLS-1$
							cnt++;
						}
					
						sb.append(tempname);
						sb.append(":"); //$NON-NLS-1$
						if (sua.getSamplingUnitAttribute().getType() == AttributeType.NUMERIC){
							sb.append("Double"); //$NON-NLS-1$
						}else if (sua.getSamplingUnitAttribute().getType() == AttributeType.TEXT){
							sb.append("String"); //$NON-NLS-1$
						}else{
							//this is not supported by we can try string
							sb.append("String"); //$NON-NLS-1$
						}
					}
					sb.append(",geom:"); //$NON-NLS-1$
					if (typeName.equals(SamplingUnitType.PLOT.name())){
						sb.append("Point:srid=4326"); //$NON-NLS-1$
					}else if (typeName.equals(SamplingUnitType.TRANSECT.name())){
						sb.append("LineString:srid=4326"); //$NON-NLS-1$
					}
				}finally{
					s.close();
				}
				return Status.OK_STATUS;
			}
			
		};
		
		j.setSystem(true);
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			EcologicalRecordsPlugIn.log("Error creating feature type for survey design sampling unit: " + sd.getKeyId() + "_" + typeName, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return sb.toString();
	}
	

}
