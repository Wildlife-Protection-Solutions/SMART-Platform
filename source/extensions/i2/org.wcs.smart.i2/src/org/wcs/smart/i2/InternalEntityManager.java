/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.i2.ui.views.EntityExportDialog;
import org.wcs.smart.util.E3Utils;

/**
 * Common code for managing entities 
 * 
 * @author Emily
 *
 */
public enum InternalEntityManager {
	
	INSTANCE;
	
	/**
	 * Delete entities  
	 * 
	 * @param shell
	 * @param pService
	 * @param eventBroker
	 * @param entitiesToDelete
	 */
	public void deleteEntities(Shell shell, EPartService pService, IEventBroker eventBroker, List<UUID> entitiesToDelete){
		if (entitiesToDelete.isEmpty()) return;
		if (!IntelSecurityManager.INSTANCE.canDeleteEntityAny()) {
			MessageDialog.openError(shell, 
					Messages.InternalEntityManager_PrivilegeTitle, 
					Messages.InternalEntityManager_PrivilegeMessage);
			return;
		}
		
		if (!MessageDialog.openQuestion(shell, Messages.EntitySearchResultTable_DeleteEntityTitle,
				MessageFormat.format(Messages.EntitySearchResultTable_DeleteEntityMsg, 
						entitiesToDelete.size()))) return; 
		
		
		//look for any dirty record editors and save them first
		List<RecordEditor> editors = new ArrayList<>();
		StringBuilder names = new StringBuilder();
		for(MPart p : pService.getParts()){
			Object x = E3Utils.getSourceObject(p);
			if ( x instanceof RecordEditor && ((RecordEditor)x).isDirty()){
				editors.add((RecordEditor)x);
				names.append(((RecordEditor)x).getPartName());
				names.append(", "); //$NON-NLS-1$
			}
		}
		if (!editors.isEmpty()){
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.EntitySearchResultTable_SaveRequiredMsg);
			sb.append("\n"); //$NON-NLS-1$
			sb.append(names.substring(0, names.length() - 2));
			
			if (!MessageDialog.openQuestion(shell, Messages.EntitySearchResultTable_DeleteEntitiesTitle2, sb.toString())){
				return;
			}
			for (RecordEditor p : editors){
				try{
					//context.get(EPartService.class).savePart(p, false); -> this doesn't work it still prompts the user; we do not want to prompt user
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().saveEditor(p, false);
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
				}
			}
		}
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.EntitySearchResultTable_DeleteTaskName, entitiesToDelete.size());
					List<IntelEntity> deletedItems = new ArrayList<>();
					try(Session s = HibernateManager.openSession()){
						s.beginTransaction();
						try{
							boolean dmModified = false;
							for (UUID entityUuid : entitiesToDelete){
								IntelEntity entity = s.get(IntelEntity.class, entityUuid);
								
								if (!IntelSecurityManager.INSTANCE.canDeleteEntity(entity.getProfile())) {
									MessageDialog.openError(shell, 
											Messages.InternalEntityManager_PrivilegeTitle, 
											MessageFormat.format(Messages.InternalEntityManager_InvalidPermissions, entity.getIdAttributeAsText()));
									continue;
								}
								dmModified = dmModified || EntityManager.INSTANCE.deleteEntity(entity, s);
								deletedItems.add(entity);
								monitor.worked(1);
							}
							if (dmModified) {
								DataModelManager.INSTANCE.updateLastModified(s);
							}
							s.getTransaction().commit();
						}catch (Exception ex){
							s.getTransaction().rollback();
							throw new InvocationTargetException(ex);
						}
					}
					try{
						eventBroker.send(IntelEvents.ENTITY_DELETE, deletedItems);
					}catch (Exception ex){
						//error with events;
						Intelligence2PlugIn.displayLog(Messages.EntitySearchResultTable_RefreshError + ex.getMessage(), ex);
					}
				}
			});
		}catch (Throwable ex){
			Intelligence2PlugIn.displayLog(Messages.EntitySearchResultTable_DeleteError + ex.getCause().getMessage(), ex);
			return;
		}
	}
	
	/**
	 * Export entities to pdf or other format for printing
	 * 
	 * @param shell
	 * @param entityUuids
	 */
	public void printEntities(Shell shell,List<UUID> entityUuids){
		List<IntelEntity> toOpen = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			for (UUID entityUuid : entityUuids) {
				IntelEntity temp = null;
				temp = session.get(IntelEntity.class, entityUuid);
				if (temp != null) {
					temp.getIdAttributeAsText();
					temp.getEntityType();
					toOpen.add(temp);
				}
			}
		}
		if (toOpen.isEmpty()) return;
		EntityExportDialog dialog = new EntityExportDialog(shell, toOpen);
		dialog.open();
	}
}
