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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
public class FixedEntityDataSource extends AbstractDataStore{

	private HashMap<String, SimpleFeatureType> schemas = new HashMap<String, SimpleFeatureType>();
	private HashMap<String, EntityType> cachedTypes = new HashMap<String, EntityType>();
	
	public FixedEntityDataSource(){
	}

	@Override
	public void dispose(){
		super.dispose();
	}

	/**
	 * Updates the schema for a given entity type
	 * @param eType
	 */
	public void refresh(EntityType eType){
		String key = eType.getKeyId();
		//remove schemas from cache
		schemas.remove(key);
		cachedTypes.put(key, eType);
	}

	/**
	 * Returns one type name for each fixed entity type 
	 * for the given conservation area.
	 * 
	 * @see org.geotools.data.store.ContentDataStore#createTypeNames()
	 */
	@Override
	public String[] getTypeNames()  {
		List<String> names = new ArrayList<String>();
		try {
			for (EntityType et : EntityHibernateManager.getInstance().getActiveEntityTypes()){
				if (et.getType() == EntityType.Type.FIXED){
					names.add(et.getKeyId());
				}
			}
//				Query q = s.createQuery("SELECT keyId FROM EntityType WHERE conservationArea = :ca and type = :type"); //$NON-NLS-1$
//				q.setParameter("conservationArea", ca); //$NON-NLS-1$
//				q.setParameter("type", EntityType.Type.FIXED); //$NON-NLS-1$
//				List<?> data = q.list();
//				for (int i = 0; i < data.size(); i++){
//					names.add(SmartUtils.encodeHex((byte[])data.get(i)));
//				}
			return names.toArray(new String[names.size()]);
		} catch (Exception e) {
			EntityPlugIn.log("Could not determine data source type names for fixed entity types.", e); //$NON-NLS-1$
		}
		return null;
	}
	
	/**
	 * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String)
	 */
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
		EntityType et = cachedTypes.get(typeName);
		if (et == null){
			throw new IOException(Messages.FixedEntityDataSource_NoEntityTypeFound);
			//TODO: consider using a session to load the entity type and cache it
		}
		return new FixedEntityDataSourceFeatureReader(et, getSchema(typeName));
	}

	
	/**
	 * @see org.geotools.data.AbstractDataStore#getSchema(java.lang.String)
	 */
	@Override
	public SimpleFeatureType getSchema(String typeName) throws IOException {
		SimpleFeatureType type = schemas.get(typeName);
		if (type == null){
			try {
				type = createEntitySchema(typeName);
			}catch(SchemaException ex){
				throw new IOException(Messages.FixedEntityDataSource_FeatureSchemaNotGenerated + "\n\n" + ex.getLocalizedMessage(), ex);  //$NON-NLS-1$
			}
			schemas.put(typeName, type);
		}
		return type;
	}


	
	private SimpleFeatureType createEntitySchema(final String entityTypeKey) throws SchemaException{
		final StringBuilder sb = new StringBuilder();
		Job j = new Job(Messages.FixedEntityDataSource_BuildSchemaJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
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
				}finally{
					s.close();
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
