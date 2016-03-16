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
package org.wcs.smart.entity.ccca;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Query;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IService;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.EntityTypeMerger;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.map.FixedEntityService;
import org.wcs.smart.entity.map.FixedEntityServiceExtension;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
/**
 * Cross conservation area entity type manager.  This is responsible
 * for managing the entity types for cross conservation area analysis.
 * 
 * @author Emily
 *
 */
public class EntityTypeCcaaManager {

	private final static Object LOCK = new Object();
	private static EntityTypeCcaaManager instance;
	
	private List<EntityType> mergedTypes;
	
	/*
	 * Updates UI components when ca configuration is changed
	 */
	private IDataModelListener dmChanged = new IDataModelListener() {
		@Override
		public void modified() {
			mergedTypes = null;

			QueryDataModelManager.getInstance().clearDataModel();
			
			//this will update the entity type list view part
			EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_MODIFIED, null);
			
			//clear Fixed Entity Service
			URL url = FixedEntityServiceExtension.createURL(SmartDB.getCurrentConservationArea());
			FixedEntityService entityService = (FixedEntityService) CatalogPlugin.getDefault().getLocalCatalog().getById(IService.class, new ID(url), null);
			if (entityService != null){
				CatalogPlugin.getDefault().getLocalCatalog().remove(entityService);
			}
			
		}
	};
	
	private EntityTypeCcaaManager(){
		DataModelManager.INSTANCE.addChangeListener(dmChanged);
	}
	
	/**
	 * 
	 * @return the manager instance
	 */
	public static EntityTypeCcaaManager getInstance(){
		if (instance == null){
			synchronized (LOCK) {
				if (instance == null){
					instance = new EntityTypeCcaaManager();
				}
			}
			
		}
		return instance;
	}
	
	/**
	 * 
	 * @return all entity types shared across the conservation areas
	 */
	public List<EntityType> getAllEntityTypes(){
		if (mergedTypes == null){
			synchronized (instance) {
				if (mergedTypes == null){
					mergedTypes = getEntityTypes();
				}
			}
			
		}
		return mergedTypes;
	}
	
	/**
	 * Get all entity types
	 * @return
	 */
	private List<EntityType> getEntityTypes() {
		
		final List<EntityType> newTypes = new ArrayList<EntityType>();
		
		//ensure the data model is loaded here; outside the progress monitor 
		//to prevent deadlocking
		QueryDataModelManager.getInstance().getDataModel();
		
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
				try {
					dialog.run(true, false, new IRunnableWithProgress() {
						
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {
							Session s = HibernateManager.openSession();
							try{
								List<EntityType> tts = (new EntityTypeMerger(Locale.getDefault())).mergeEntityTypes(
										SmartDB.getConservationAreaConfiguration().getConservationAreas().toArray(new ConservationArea[SmartDB.getConservationAreaConfiguration().getConservationAreas().size()]),
										SmartDB.getConservationAreaConfiguration().getMainConservationArea(),
										s, monitor);
								newTypes.addAll(tts);
							}finally{
								s.close();
								
							}
							
						}
					});
				} catch (Exception e) {
					EntityPlugIn.displayLog(Messages.EntityTypeMerger_ErrorMergingEntityTypes + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
				}
			}});
		
		
		return newTypes;
	}
	
	/**
	 * Find the given entity type based on the key
	 * 
	 * @param keyId
	 * @return
	 */
	public EntityType findType(String keyId){
		for (EntityType et : getAllEntityTypes()){
			if (et.getKeyId().equals(keyId)){
				return et;
			}
		}
		return null;
	}
	
	/**
	 * Loads all entities for the given entity type, for
	 * all conservation areas in current configuration
	 * 
	 * @param entityTypeKey 
	 * @param session
	 * @return
	 */
	public List<Entity> getEntities(String entityTypeKey, Session session){
		Query q = session.createQuery("FROM Entity e WHERE e.entityType.keyId = :keyId and e.entityType.conservationArea in (:ca)"); //$NON-NLS-1$
		q.setParameter("keyId", entityTypeKey); //$NON-NLS-1$
		q.setParameterList("ca", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Entity> allEntities = q.list();	
		return allEntities;
	}
}
