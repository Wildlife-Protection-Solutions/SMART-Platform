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
package org.wcs.smart.entity.ui.editor;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.entity.EntityPermissionManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.ccca.EntityTypeCcaaManager;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.event.IEntityListener;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.EntitySightingQuery;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Editor for managing entity types and 
 * their entities.
 * 
 * @author Emily
 *
 */
public class EntityTypeEditor extends MultiPageEditorPart implements MapPart, IAdaptable{


	public static final String ID = "org.wcs.smart.entity.editor.entitytype"; //$NON-NLS-1$
	
	private EntityTypeConfigurationPage configPage;
	private EntitiesPage entityPage;
	private SightingMapPage sightingsMapPage;
	private SightingPage sightingsPage;
	
	private EntityType entityType;
	private List<Entity> cachedEntities = null;	//for ccaa analysis
	
	private IEntityListener listener = new IEntityListener() {
		
		@Override
		public void handleEvent(final int eventType, Object source) {
			boolean isThisEditor = false;
			
			if (source instanceof EntityType){
				if (((EntityType)source).equals(entityType)){
					isThisEditor = true;
				}
			}else if (source instanceof EntityTypeEditorInput){
				if (Arrays.equals(((EntityTypeEditorInput) source).getUuid(), entityType.getUuid())){
					isThisEditor = true;
				}
			}else if (source instanceof Entity){
				if (((Entity)source).getEntityType().equals(entityType)){
					isThisEditor = true;
				}
			}
			
			if (eventType == EntityEventManager.ENTITY_TYPE_MODIFIED){
				if (isThisEditor){
					//reload
					entityType = null;
					
					getSite().getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							initEditor(new IEntityTypeEditorPage[]{entityPage, configPage, sightingsPage, sightingsMapPage}, true);
						}});
					
				}
			}else if (eventType == EntityEventManager.ENTITY_TYPE_DELETED){
				if (isThisEditor){
					//close this editor
					getSite().getShell().getDisplay().asyncExec(new Runnable(){
						@Override
						public void run() {
							getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(EntityTypeEditor.this, false);					
						}
					});
				}
			}else if (eventType == EntityEventManager.ENTITY_ADDED || 
					eventType == EntityEventManager.ENTITY_MODIFIED ||
					eventType == EntityEventManager.ENTITY_DELETED){
				
				if (isThisEditor){
					entityType = null;
					getSite().getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							initEditor(new IEntityTypeEditorPage[]{entityPage,sightingsMapPage, sightingsPage}, false);
						}});
					
				}
			}
		}
	};
	
	/**
	 * 
	 * @return the current entity type associated with the editor
	 */
	public synchronized EntityType getEntityType(){
		if (entityType == null){
			entityType = loadEntityType();
		}
		return entityType;
	}

	@Override
	public void dispose(){
		super.dispose();
		
		EntityEventManager.getInstance().removeListener(listener);
	}
	
	/**
	 * The last query from the sightings page 
	 * @return
	 */
	public EntitySightingQuery getCurrentQuery(){
		if (sightingsPage == null){
			return null;
		}
		return sightingsPage.getCurrentQuery();
	}
	
	
	public SightingMapPage getMapPage(){
		return this.sightingsMapPage;
	}

	@Override
	protected void createPages() {
		try{
			entityPage = new EntitiesPage(this);
			int index = addPage(entityPage, getEditorInput());
			super.setPageText(index, Messages.EntityTypeEditor_EntitiesPageName);
			super.setPageImage(index, EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.ENTITY_TYPE_ICON));

			
			configPage = new EntityTypeConfigurationPage(this);
			index = addPage(configPage, getEditorInput());
			super.setPageText(index, Messages.EntityTypeEditor_ConfigurationPageName);
			super.setPageImage(index, EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.CONFIGURATION_ICON));
			
			if (EntityPermissionManager.canViewSightings()){
				sightingsPage = new SightingPage(this);
				index = addPage(sightingsPage, getEditorInput());
				super.setPageText(index, Messages.EntityTypeEditor_SightingsPageName);
				super.setPageImage(index, EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.SIGHTINGS_ICON));
			}
			
			sightingsMapPage = new SightingMapPage(this);
			index = addPage(sightingsMapPage, getEditorInput());
			super.setPageText(index, Messages.EntityTypeEditor_MapPageName);
			super.setPageImage(index, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
					
		}catch (Exception ex){
			EntityPlugIn.displayLog(Messages.EntityTypeEditor_ErrorOpeningEditor + ex.getMessage(), ex);
		}
		initEditor(new IEntityTypeEditorPage[]{entityPage, configPage, sightingsPage, sightingsMapPage}, true);
		
		EntityEventManager.getInstance().addListener(listener);
	}
	
	
	private void initEditor(final IEntityTypeEditorPage[] partsToUpdate, final  boolean typeChanged){
		
	
		Job loadEntity = new Job("load entity"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				getEntityType();
				
				final Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					if (entityType.getUuid() != null){
						s.saveOrUpdate(entityType);
					}
					entityType.getNames().size();
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							for (int i = 0; i < partsToUpdate.length; i ++){
								if (partsToUpdate[i] != null){
									partsToUpdate[i].updatePage(s, typeChanged);
								}
							}
							setPartName(entityType.getLabel());							
						}});

				}finally{
					s.getTransaction().rollback();
					s.close();
				}
				return Status.OK_STATUS;
			}};
		loadEntity.setSystem(true);
		loadEntity.schedule();

		
		setTitleImage(EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.ENTITY_TYPE_ICON));
		
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		
		return false;
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof EntityTypeEditorInput)) {
			throw new IllegalArgumentException("Invalid editor input."); //$NON-NLS-1$
		}
		setSite(site);
		setInput(input);
	}

	
	@Override
	public boolean isDirty() {
		return false;
	}
	
	
	/**
	 * 
	 * @param currentSession
	 * @return list of entities associated with current type
	 */
	public List<Entity> getEntities(Session currentSession){
		EntityType type = getEntityType();
		if (SmartDB.isMultipleAnalysis()){
			if (cachedEntities == null){
				cachedEntities = EntityTypeCcaaManager.getInstance().getEntities(type.getKeyId(), currentSession);
			}
			return cachedEntities;
		}else{
			return type.getEntities();
		}
	}
	
	/**
	 * loads the plan from the database populating all 
	 * lazy fields from the database.
	 * 
	 * Will always get a new object.
	 */
	private EntityType loadEntityType(){
		if (SmartDB.isMultipleAnalysis()){
			return EntityTypeCcaaManager.getInstance().findType(((EntityTypeEditorInput) getEditorInput()).getKeyId());
		}
		byte[] uuid = ((EntityTypeEditorInput) getEditorInput()).getUuid();
		EntityType et = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			et = (EntityType) session.load(EntityType.class, uuid);
			et.getDmAttribute().getName();
			et.getName();
			
			//ensure attributes are lazily loaded
			if (et.getAttributes() != null){
				for (EntityAttribute att : et.getAttributes()){
					att.getName().length();
				}
			}
		}finally{
			session.getTransaction().rollback();
			session.close();
		}
		
		return et;
	}
	
	/**
	 * Saves the entity type to the database
	 * @param dmChanged true if a change has been made to the
	 * data model; false if not changed 
	 */
	public void saveEntityType(){
		final EntityType et = entityType;
		
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getSite().getShell());
		try{
		dialog.run(true, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				monitor.beginTask(MessageFormat.format(Messages.EntityTypeEditor_SaveProgress, new Object[]{et.getName()}), 0);
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					s.saveOrUpdate(et);
					s.getTransaction().commit();
				}catch (Exception ex){
					if (s.getTransaction().isActive()){
						s.getTransaction().rollback();
					}
					EntityPlugIn.displayLog(Messages.EntityTypeEditor_EditingSavingType + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
					return;
				}finally{
					s.close();
				}
				
				//fire associated event changes
				try{
					EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_MODIFIED, et);
				}catch(Exception ex){
					EntityPlugIn.displayLog(ex.getMessage(), ex);
				}
				
			}
		});	
		}catch (Exception ex){
			EntityPlugIn.log(Messages.EntityTypeEditor_EditingSavingType + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			return;
		}
	}

	@Override
	public Map getMap() {
		return sightingsMapPage.getMap();
	}

	@Override
	public void openContextMenu() {
		sightingsMapPage.openContextMenu();
		
	}

	@Override
	public void setFont(Control textArea) {
		sightingsMapPage.setFont(textArea);
		}

	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		sightingsMapPage.setSelectionProvider(selectionProvider);		
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return sightingsMapPage.getStatusLineManager();
	}
	
	
}
