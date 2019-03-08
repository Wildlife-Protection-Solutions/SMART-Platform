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
package org.wcs.smart.entity.map;

import java.io.IOException;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Data source for fixed entity type locations.  Displays
 * the locations of all the entities.
 * <p>Data source supports all 
 * fixed entity type in database for a given conservation area.  Each
 * fixed entity type represents a different type.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class FixedEntityFeatureSource extends ContentFeatureSource{

	private EntityType et;
	
	public FixedEntityFeatureSource(ContentEntry entry, EntityType et) {
		super(entry, Query.ALL);
		this.et = et;
	}

	

	@Override
	protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
		return null;
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		return -1;
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		return new FixedEntityDataSourceFeatureReader(et, getSchema());
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try {
			return createEntitySchema(entry.getTypeName());
		}catch(SchemaException ex){
			throw new IOException(Messages.FixedEntityDataSource_FeatureSchemaNotGenerated + "\n\n" + ex.getLocalizedMessage(), ex);  //$NON-NLS-1$
		}
	}
	
	private SimpleFeatureType createEntitySchema(final String entityTypeKey) throws SchemaException{
		final StringBuilder sb = new StringBuilder();
		Job j = new Job(Messages.FixedEntityDataSource_BuildSchemaJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try(Session s = HibernateManager.openSession()){
					EntityType entityType = EntityHibernateManager.getInstance().getEntityType(entityTypeKey, s);
					
					sb.append("fid:String"); //$NON-NLS-1$
					sb.append(",id:String"); //$NON-NLS-1$
					sb.append(",status:String"); //$NON-NLS-1$
					HashSet<String> names = new HashSet<String>();
					for (EntityAttribute ea: entityType.getAttributes()){
						sb.append(","); //$NON-NLS-1$
						String name = ea.getName();
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
						if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN){
							sb.append(QueryColumn.ColumnType.INTEGER.geotoolsType); 
						}else if (ea.getDmAttribute().getType() == AttributeType.TEXT ||
								ea.getDmAttribute().getType() == AttributeType.TREE ||
								ea.getDmAttribute().getType() == AttributeType.LIST){
							sb.append(QueryColumn.ColumnType.STRING.geotoolsType); 
						}else if (ea.getDmAttribute().getType() == AttributeType.NUMERIC){
							sb.append(QueryColumn.ColumnType.NUMBER.geotoolsType); 
						}else if (ea.getDmAttribute().getType() == AttributeType.DATE){
							sb.append(QueryColumn.ColumnType.DATE.geotoolsType); 
						}
					}
					sb.append(",geom:Point:srid=4326"); //$NON-NLS-1$
					
				}catch (Exception ex){
					EntityPlugIn.log("Error creating feature type for entity type key: " + entityTypeKey, ex); //$NON-NLS-1$
				}
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			EntityPlugIn.log("Error creating feature type for entity type key: " + entityTypeKey, e); //$NON-NLS-1$
		}
		
		SimpleFeatureType type =  DataUtilities.createType(entityTypeKey, sb.toString());
		return type;
	}
}
