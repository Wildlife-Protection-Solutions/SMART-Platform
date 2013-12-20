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
package org.wcs.smart.entity.ui;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.typelist.editor.EntityTypeEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
/**
 * Delete entity type handler
 * @author Emily
 *
 */
public class DeleteEntityTypeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		final IStructuredSelection lastSelection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		if (lastSelection.size() == 0){
			return null;	//nothing to delete
		}
		
		final List<EntityTypeEditorInput> toDelete = new ArrayList<EntityTypeEditorInput>();
		for (Iterator<?> iterator = lastSelection.iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof EntityTypeEditorInput){
				toDelete.add((EntityTypeEditorInput)x);
			}
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(HandlerUtil.getActiveShell(event));
		try{
		pmd.run(true, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				
				monitor.beginTask(Messages.DeleteEntityTypeHandler_DeleteProgress, toDelete.size());
				for (EntityTypeEditorInput type : toDelete){
					monitor.subTask(MessageFormat.format(Messages.DeleteEntityTypeHandler_DeleteTypeProgress, new Object[]{type.getName()}));
					delete(type);
					monitor.worked(1);
				}		
			}
		});
		}catch (Exception ex){
			EntityPlugIn.log(ex.getMessage(), ex);
		}
		
		return null;
	}

	private void delete(EntityTypeEditorInput type){
		
		//check for observations whose value matches
		//the attribute associated with this entity
		
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			
			final EntityType entity = (EntityType) session.load(EntityType.class, type.getUuid());
			
			if (entity == null){
				return;
			}
				
			Attribute dmAttribute = entity.getDmAttribute();
			
			final String message = MessageFormat.format(Messages.DeleteEntityTypeHandler_DeleteAttributeConfirm,
					new Object[]{type.getName(), dmAttribute.getName()});
			final boolean[] deleteAttribute = new boolean[]{true};
			
			//ask the user if they also want to delete
			//the attribute
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), 
							Messages.DeleteEntityTypeHandler_ConfirmDialogTitle, message)){
						deleteAttribute[0] = false;
					}
				}});

			
			//validate we can delete the entity and delete it
			if (DeleteManager.canDelete(entity, session)){
				session.delete(entity);
			}else{
				//cannot delete so rollback and exist
				session.getTransaction().rollback();
				return;
			}
			
			//validate we can delete the attribute
			if (deleteAttribute[0]){
				if ( DeleteManager.canDelete(dmAttribute, session) ){
					//we need to find all category/attribute relationships
					//and delete these before we delete the attribute
					Criteria query = session.createCriteria(CategoryAttribute.class);
					query.add(Restrictions.eq("id.attribute", dmAttribute)); //$NON-NLS-1$
					@SuppressWarnings("unchecked")
					List<CategoryAttribute> items = query.list();
					for (CategoryAttribute ca : items){
						if (DeleteManager.canDelete(ca, session)){
							session.delete(ca);
							DataModelManager.getInstance().fireDeleteListener(session, ca);
						}else{
							//we cannot delete so rollback and exit
							session.getTransaction().rollback();
							displayCannotDelete(entity);
							return;
						}
					}
					
					session.delete(dmAttribute);
					DataModelManager.getInstance().fireDeleteListener(session, dmAttribute);
				}else{
					//we cannot delete so we want to rollback and not delete anything
					session.getTransaction().rollback();
					displayCannotDelete(entity);
					return;
				}
			}
			session.getTransaction().commit();
			
			//fire data model change listeners as we have edited the data model
			if (deleteAttribute[0]){
				try{
					DataModelManager.getInstance().fireChangeListeners();
				}catch (Exception ex){
					EntityPlugIn.displayLog(ex.getMessage(), ex);
				}
			}
			
			//fire entity delete event
			try{
				EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_DELETED, entity);
			}catch (Exception ex){
				EntityPlugIn.displayLog(ex.getMessage(), ex);
			}
		}catch (Exception ex){
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
			EntityPlugIn.displayLog(MessageFormat.format(Messages.DeleteEntityTypeHandler_DeleteError, new Object[]{type.getName()}) + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}finally{
			session.close();
		}
		
		
	}
	
	private void displayCannotDelete(final EntityType entity){
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(),
						Messages.DeleteEntityTypeHandler_DeleteDialogTitle, MessageFormat.format("The entity type {0} was not deleted.", new Object[]{entity.getName()})); //$NON-NLS-1$
			}});
	}
}
