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
package org.wcs.smart.entity.ui.typelist.editor;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.event.IEntityListener;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Editor for managing entity types and 
 * their entities.
 * 
 * @author Emily
 *
 */
public class EntityTypeEditor extends MultiPageEditorPart  {


	public static final String ID = "org.wcs.smart.entity.editor.entitytype"; //$NON-NLS-1$
	
	private EntityTypeConfigurationPage configPage;
	private EntityTypeEntitiesPage entityPage;
	private EntityType entityType;
	
	
	private IEntityListener listener = new IEntityListener() {
		
		@Override
		public void handleEvent(int eventType, Object source) {
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
							initEditor(new IEntityTypeEditorPage[]{entityPage, configPage}, true);
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
							initEditor(new IEntityTypeEditorPage[]{entityPage}, false);
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
	 * 
	 * @return <code>true</code> if current user
	 * can modify the entity type
	 */
	public boolean canEdit(){
		return true;
	}

	@Override
	protected void createPages() {
		try{
			entityPage = new EntityTypeEntitiesPage(this);
			int index = addPage(entityPage, getEditorInput());
			super.setPageText(index, Messages.EntityTypeEditor_EntitiesPageName);
			super.setPageImage(index, EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.ENTITY_TYPE_ICON));

			
			configPage = new EntityTypeConfigurationPage(this);
			index = addPage(configPage, getEditorInput());
			super.setPageText(index, Messages.EntityTypeEditor_ConfigurationPageName);
			super.setPageImage(index, EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.CONFIGURATION_ICON));
			
					
		}catch (Exception ex){
			EntityPlugIn.displayLog(Messages.EntityTypeEditor_ErrorOpeningEditor + ex.getMessage(), ex);
		}
		initEditor(new IEntityTypeEditorPage[]{entityPage, configPage}, true);
		
		EntityEventManager.getInstance().addListener(listener);
	}
	
	private void initEditor(IEntityTypeEditorPage[] partsToUpdate, boolean typeChanged){
		getEntityType();
		
		Session s = HibernateManager.openSession();
		s.saveOrUpdate(entityType);
		entityType.getDmAttribute().getName();
		try{
			for (int i = 0; i < partsToUpdate.length; i ++){
				partsToUpdate[i].updatePage(s, typeChanged);
			}
		}finally{
			s.close();
		}
		
		setPartName(entityType.getLabel());
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
	
	@Override
	public void setFocus() {
		configPage.setFocus();
	}
	
	/**
	 * loads the plan from the database populating all 
	 * lazy fields from the database.
	 * 
	 * Will always get a new object.
	 */
	public EntityType loadEntityType(){
		byte[] uuid = ((EntityTypeEditorInput) getEditorInput()).getUuid();
		
		EntityType et = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			et = (EntityType) session.load(EntityType.class, uuid);
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
				monitor.beginTask(MessageFormat.format(Messages.EntityTypeEditor_SaveProgress, new Object[]{et.getId()}), 0);
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					
					
					s.saveOrUpdate(et);
					s.getTransaction().commit();
				}catch (Exception ex){
					if (s.getTransaction().isActive()){
						s.getTransaction().rollback();
					}
					EntityPlugIn.log(Messages.EntityTypeEditor_EditingSavingType + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
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
	
	
}
