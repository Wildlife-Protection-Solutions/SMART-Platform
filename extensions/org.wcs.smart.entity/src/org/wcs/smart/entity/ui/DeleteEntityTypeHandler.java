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

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.editor.EntityTypeEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
/**
 * Delete entity type handler
 * @author Emily
 *
 */
public class DeleteEntityTypeHandler {

	@Execute
	public void execute(final Shell activeShell, @Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object selection){
		
		if (selection == null || !(selection instanceof IStructuredSelection) || ((IStructuredSelection)selection).isEmpty()){
			return;
		}
		
		final List<EntityTypeEditorInput> toDelete = new ArrayList<EntityTypeEditorInput>();
		for (Iterator<?> iterator = ((IStructuredSelection)selection).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof EntityTypeEditorInput){
				toDelete.add((EntityTypeEditorInput)x);
			}
		}
		
		if (toDelete.size() == 0) return;
		
		//confirm delete
		StringBuilder sb = new StringBuilder();
		for (EntityTypeEditorInput in : toDelete){
			sb.append(in.getName());
			sb.append(", "); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		
		if (!MessageDialog.openConfirm(activeShell, Messages.DeleteEntityTypeHandler_ConfirmDeleteTitle, 
				MessageFormat.format(Messages.DeleteEntityTypeHandler_ConfirmDeleteMessage, new Object[]{sb.toString()}))){
			return; 
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try{
		pmd.run(true, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				
				monitor.beginTask(Messages.DeleteEntityTypeHandler_DeleteProgress, toDelete.size());
				for (EntityTypeEditorInput type : toDelete){
					monitor.subTask(MessageFormat.format(Messages.DeleteEntityTypeHandler_DeleteTypeProgress, new Object[]{type.getName()}));
					delete(activeShell, type);
					monitor.worked(1);
				}		
			}
		});
		}catch (Exception ex){
			EntityPlugIn.log(ex.getMessage(), ex);
		}
	}

	private void delete(Shell activeShell, EntityTypeEditorInput type){
		
		//check for observations whose value matches
		//the attribute associated with this entity
		
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			
			final EntityType entity = (EntityType) session.load(EntityType.class, type.getUuid());
			if (entity == null){
				session.getTransaction().rollback();
				return;
			}
			
			//-- check if the entity type can be removed
			String errorMessage = null;
			try{
				if (!DeleteManager.canDelete(entity, session)){
					errorMessage = MessageFormat.format(Messages.DeleteEntityTypeHandler_ConfirmDeleteError, new Object[]{entity.getName()});
				}
			}catch (Exception ex){
				EntityPlugIn.log(ex.getMessage(), ex);
				errorMessage = MessageFormat.format(Messages.DeleteEntityTypeHandler_ConfirmDeleteError + "\n\n" + ex.getMessage(), new Object[]{entity.getName()}); //$NON-NLS-1$ 
			}
			if (errorMessage != null){
				session.getTransaction().rollback();
				displayMessage(activeShell, errorMessage);
				return;
			}
				
			Attribute dmAttribute = entity.getDmAttribute();
			
			
			//validate we can delete the entity
			if (!DeleteManager.canDelete(entity, session)){
				//cannot delete so rollback and exit
				session.getTransaction().rollback();
				return;
			}
				
			//delete any attribute values associated with this entity attribute
			if (entity.getAttributes().size() > 0){
				Query q = session.createQuery("DELETE EntityAttributeValue WHERE id.entityAttribute IN (:toDelete)"); //$NON-NLS-1$
				q.setParameterList("toDelete", entity.getAttributes()); //$NON-NLS-1$
				q.executeUpdate();	
				
				for (Iterator<EntityAttribute> iterator = entity.getAttributes().iterator(); iterator.hasNext();) {
					final EntityAttribute type2 = (EntityAttribute) iterator.next();
					type2.getDmAttribute().getKeyId();	//fix a hibernate bug
					
					iterator.remove();	//remove attribute
			
					//ask the user if they wish to delete the attribute 
					//from the data if it is not used anymore
					boolean canDeleteAttribute = false;
					try{
						if (DeleteManager.canDelete(type2.getDmAttribute(), session)){
							canDeleteAttribute = true;
						}
					}catch (Exception ex){
						//something is using this attribute therefore
						//it cannot be deleted
					}
						
					if (canDeleteAttribute){
						final int[] ret = {-1};
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), 
										Messages.DeleteEntityTypeHandler_DialogTitle, null,
										MessageFormat.format(Messages.DeleteEntityTypeHandler_DeleteEntityAttributeMessage, new Object[]{ type2.getDmAttribute().getName() } ), 
										MessageDialog.CONFIRM, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
								ret[0] = dialog.open();
							}});
					
						if (ret[0] == 0){  //YES
							boolean deletel = DataModelManager.getInstance().validateDelete(type2.getDmAttribute(), new NullProgressMonitor(), session);
							if (deletel){
								DataModelManager.getInstance().fireDeleteListener(session, type2.getDmAttribute());
								session.delete(type2.getDmAttribute());
							}
						}
					}
				}
			}
			session.delete(entity);
			
			
			// see if user wants to delete the attribute associated with the 
			//entity type
			final String message = MessageFormat.format(Messages.DeleteEntityTypeHandler_DeleteAttributeConfirm,
					new Object[]{type.getName(), dmAttribute.getName()});
			final Boolean[] deleteAttribute = new Boolean[]{null};
			
			//ask the user if they also want to delete
			//the attribute
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					deleteAttribute[0] = null;
					MessageDialog confirm = new MessageDialog(Display.getDefault().getActiveShell(),
							Messages.DeleteEntityTypeHandler_ConfirmDialogTitle,
							null,
							message,
							MessageDialog.QUESTION,
							new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
					int id = confirm.open();
					if (id == 0){
						//yes
						deleteAttribute[0] = true;
					}else if (id == 1){
						//no
						deleteAttribute[0] = false;
					}
				}});
			if (deleteAttribute[0] == null){
				//cancel
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
							displayCannotDelete(activeShell, entity);
							return;
						}
					}
					
					session.delete(dmAttribute);
					DataModelManager.getInstance().fireDeleteListener(session, dmAttribute);
				}else{
					//we cannot delete so we want to rollback and not delete anything
					session.getTransaction().rollback();
					displayCannotDelete(activeShell, entity);
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
	
	private void displayCannotDelete(final Shell shell, final EntityType entity){
		shell.getDisplay().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openInformation(shell,
						Messages.DeleteEntityTypeHandler_DeleteDialogTitle, MessageFormat.format(Messages.DeleteEntityTypeHandler_TypeDeleteError, new Object[]{entity.getName()})); 
			}});
	}
	
	private void displayMessage(final Shell shell, final String message){
		shell.getDisplay().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openInformation(shell,
						Messages.DeleteEntityTypeHandler_DeleteDialogTitle, 
						message); 
			}});
	}
	
	public static class DeleteEntityTypeHandlerWrapper extends DIHandler<DeleteEntityTypeHandler>{
		public DeleteEntityTypeHandlerWrapper(){
			super(DeleteEntityTypeHandler.class);
		}
	}
}
