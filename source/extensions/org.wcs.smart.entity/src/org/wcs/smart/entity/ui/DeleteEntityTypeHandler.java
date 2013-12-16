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
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.typelist.editor.EntityTypeEditorInput;
import org.wcs.smart.hibernate.HibernateManager;

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
				
				monitor.beginTask("Delete Entity Types", toDelete.size());
				for (EntityTypeEditorInput type : toDelete){
					monitor.subTask(MessageFormat.format("Deleting Type {0}", new Object[]{type.getName()}));
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
			
			EntityType toDelete = (EntityType) session.load(EntityType.class, type.getUuid());
			if (toDelete != null){
				if (DeleteManager.canDelete(toDelete, session)){
					session.delete(toDelete);
					session.delete(toDelete.getDmAttribute());
				}
			}
			session.getTransaction().commit();
			
			EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_DELETED, toDelete);
		}catch (Exception ex){
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
			EntityPlugIn.displayLog(MessageFormat.format("Error deleting entity type {0}.", new Object[]{type.getName()}) + "\n\n" + ex.getMessage(), ex);
		}finally{
			session.close();
		}
		
		
	}
}
