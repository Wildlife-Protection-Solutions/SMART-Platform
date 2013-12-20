package org.wcs.smart.entity.ui.typelist.editor;

import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.event.IEntityListener;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;

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
					initEditor(new IEntityTypeEditorPage[]{entityPage, configPage}, true);
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
					initEditor(new IEntityTypeEditorPage[]{entityPage}, false);
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
			super.setPageText(index, "Entities");
			super.setPageImage(index, EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.ENTITY_TYPE_ICON));

			
			configPage = new EntityTypeConfigurationPage(this);
			index = addPage(configPage, getEditorInput());
			super.setPageText(index, "Configuration");
			super.setPageImage(index, EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.CONFIGURATION_ICON));
			
					
		}catch (Exception ex){
			EntityPlugIn.displayLog("Error opening entity type editor. " + ex.getMessage(), ex);
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
		
		Session session = HibernateManager.openSession();
		//load parent plan so don't have lazy loading issues later.
		session.beginTransaction();
		EntityType t = (EntityType) session.load(EntityType.class, uuid);
//		if (t.getAttributes() != null){
//			for (EntityAttribute a : t.getAttributes()){
//				a.getDmAttribute().getNames().size();
//				a.getNames().size();
//			}
//		}
//		t.getDmAttribute().getNames().size();
		session.getTransaction().rollback();
		session.close();
		return t;
	}
	
	
	public void saveEntityType(){
		EntityType et = entityType;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			
			for (EntityAttribute a : et.getAttributes()){
				if (a.getDmAttribute().getUuid() == null){
					//new attribute save it and fire required events
					s.save(a.getDmAttribute());
					
					DataModelManager.getInstance().fireAddListener(s, a.getDmAttribute());
				}
			}
			s.saveOrUpdate(et);
			s.getTransaction().commit();
		}catch (Exception ex){
			if (s.getTransaction().isActive()){
				s.getTransaction().rollback();
			}
			EntityPlugIn.log("Error saving entity type modifications.  Please close and re-open the editor." + "\n\n" + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		
		try{
			DataModelManager.getInstance().fireChangeListeners();
		}catch(Exception ex){
			EntityPlugIn.displayLog(ex.getMessage(), ex);
		}
		try{
			EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_MODIFIED, et);
		}catch(Exception ex){
			EntityPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}
	
}
