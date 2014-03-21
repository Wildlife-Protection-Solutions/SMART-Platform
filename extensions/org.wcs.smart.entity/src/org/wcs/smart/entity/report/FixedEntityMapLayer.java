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
package org.wcs.smart.entity.report;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IResolve;

import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.data.oda.smart.impl.table.SmartTableQuery;
import org.wcs.smart.entity.map.FixedEntityGeoResource;
import org.wcs.smart.entity.map.FixedEntityService;
import org.wcs.smart.entity.map.FixedEntityServiceExtension;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.birt.map.IBirtMapLayerManager;

/**
 * Map Layers for fixed entity types.
 * 
 * @author Emily
 *
 */
public class FixedEntityMapLayer implements IBirtMapLayerManager {

	public FixedEntityMapLayer() {
	}

	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)){
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (odaHandle.getExtensionID().equals(SmartTableQuery.SMART_DATASET_TYPE)) {
			if (odaHandle.getQueryText().startsWith(EntityTable.ENTITYKEY_PREFIX)){
				String key = odaHandle.getQueryText().split(":")[1]; //$NON-NLS-1$
				
				Session s = HibernateManager.openSession();
				try{
					Criteria c = s.createCriteria(EntityType.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("keyId", key)); //$NON-NLS-1$ //$NON-NLS-2$
					List<?> data = c.list();
					if (data.size() > 0){
						if (((EntityType)data.get(0)).getType()==EntityType.Type.FIXED){
							return true;
						}
					}
				}finally{
					s.close();
				}
			}
		}
		return false;
	}

	@Override
	public List<IGeoResource> createLayer(DataSetHandle handle,
			IReportContext context) throws Exception {
		if (!(handle instanceof OdaDataSetHandle)){
			return null;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle)handle;
		if (!odaHandle.getExtensionID().equals(SmartTableQuery.SMART_DATASET_TYPE)){
			return null;
		}
		
		String entityTypeKey =  odaHandle.getQueryText().split(":")[1]; //$NON-NLS-1$
		
		EntityType et = null;
		//session is managed by running report
		Session s = HibernateManager.openSession();
		Criteria c = s.createCriteria(EntityType.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", entityTypeKey)); //$NON-NLS-1$ 
		List<?> data = c.list();
		if (data.size() > 0){
			if (((EntityType)data.get(0)).getType()==EntityType.Type.FIXED){
				et = (EntityType)data.get(0);
			}
		}
		
		if (et == null){
			return null;
		}
		
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(FixedEntityServiceExtension.CAUUID_KEY, et.getConservationArea().getUuid());
		
		//find fixed entity service
		FixedEntityService service = null;
		List<IResolve> citems = CatalogPlugin.getDefault().getLocalCatalog().find(FixedEntityServiceExtension.createURL(et.getConservationArea()), null);
		for (IResolve i : citems){
			if (i instanceof FixedEntityService){
				service = (FixedEntityService) i;
			}
			
		}
		if (service == null){
			service = (FixedEntityService) CatalogPlugin.getDefault().getLocalCatalog().acquire(FixedEntityServiceExtension.createURL(et.getConservationArea()), null);
		}
		
		//find georesource
		List<IGeoResource> resources = new ArrayList<IGeoResource>();
		List<? extends IGeoResource> items = service.resources(null);
		for (IGeoResource i : items){
			if (((FixedEntityGeoResource)i).getEntityTypeKey().equals(et.getKeyId())){
				resources.add(i);		
			}
		}
		for (EntityAttribute ea : et.getAttributes()){
			ea.getName();
		}
		service.refresh(et, null);
		
		return resources;
	}

}
