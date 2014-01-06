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
package org.wcs.smart.entity.fixed.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import net.refractions.udig.catalog.IGeoResource;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;

/**
 * Geotools data store for SMART area layers.
 * @author Emily
 * @since 1.0.0
 */
public class FixedEntityDataSource extends AbstractDataStore{

	private ConservationArea ca;
	private HashMap<String, SimpleFeatureType> schemas = new HashMap<String, SimpleFeatureType>();
	private HashMap<String, EntityType> cachedTypes = new HashMap<String, EntityType>();
	
	public FixedEntityDataSource(ConservationArea ca){
		this.ca = ca;
	}

	@Override
	public void dispose(){
		super.dispose();
	}

	public void refresh(EntityType eType){
		String key = SmartUtils.encodeHex(eType.getUuid());
		//remove schemas from cache
		schemas.remove(key);
		cachedTypes.put(key, eType);
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.store.ContentDataStore#createTypeNames()
	 */
	@Override
	public String[] getTypeNames()  {
		List<String> names = new ArrayList<String>();
		Session s = HibernateManager.openSession();
		try {
				Query q = s.createQuery("SELECT uuid FROM EntityType WHERE conservationArea = :ca and type = :type");
				q.setParameter("conservationArea", ca);
				q.setParameter("type", EntityType.Type.FIXED);
				List data = q.list();
				for (int i = 0; i < data.size(); i++){
					names.add(SmartUtils.encodeHex((byte[])data.get(i)));
				}
			
//			List<? extends IGeoResource> members = service.resources(null);
//			String[] x = new String[members.size()];
//			int i = 0;
//			for (IGeoResource r : members) {
//				x[i++] = SmartUtils.encodeHex(((FixedEntityGeoResource) r).entityUuid);
//			}
//			return x;
			return names.toArray(new String[names.size()]);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			s.close();
		}
		return null;
	}
	/* (non-Javadoc)
	 * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String)
	 */
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
		EntityType et = cachedTypes.get(typeName);
		if (et == null){
			System.out.println("i am null" + typeName);
			//TODO:
			//load entity type and cache it
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
				throw new IOException("Feature schema could not be generated." + "\n\n" + ex.getLocalizedMessage(), ex);
			}
			schemas.put(typeName, type);
		}
		return type;
	}


	
	private SimpleFeatureType createEntitySchema(final String entityTypeUuid) throws SchemaException{
		final StringBuilder sb = new StringBuilder();
		Job j = new Job("Build Entity Type Schema"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					EntityType entityType = (EntityType) s.load(EntityType.class, SmartUtils.decodeHex(entityTypeUuid));
					
					sb.append("fid:String");
					sb.append(",id:String");
					sb.append(",status:String");
					HashSet<String> names = new HashSet<String>();
					for (EntityAttribute ea: entityType.getAttributes()){
						sb.append(",");
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
						sb.append(":");
						if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN){
							sb.append("Integer");
						}else if (ea.getDmAttribute().getType() == AttributeType.TEXT ||
								ea.getDmAttribute().getType() == AttributeType.TREE ||
								ea.getDmAttribute().getType() == AttributeType.LIST){
							sb.append("String");
						}else if (ea.getDmAttribute().getType() == AttributeType.NUMERIC){
							sb.append("Double");
						}else if (ea.getDmAttribute().getType() == AttributeType.DATE){
							sb.append("Date");
						}
						
						
					}
					sb.append(",geom:Point:srid=4326"); //$NON-NLS-1$
					
				}catch (Exception ex){
					ex.printStackTrace();
					//TODO: deal with me
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		SimpleFeatureType type =  DataUtilities.createType(entityTypeUuid, sb.toString()); //$NON-NLS-1$
		return type;
	}
	

}
