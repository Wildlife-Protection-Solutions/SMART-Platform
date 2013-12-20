package org.wcs.smart.entity.ui.typelist.editor;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

public class EntityTypeEntitiesPage extends EditorPart implements IEntityTypeEditorPage {
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private Form form;

	private EntityListTable entityTable;
	private EntityTypeEditor parentEditor;
	
	private EntityInfoPanelComposite entityInfoPanel;
	
		
	/**
	 * Creates a new plan editor page
	 * @param editor
	 */
	public EntityTypeEntitiesPage(EntityTypeEditor editor){
		this.parentEditor = editor;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit.setBorderStyle(SWT.BORDER);
		form = toolkit.createForm(parent);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		

		GridLayout glayout = new GridLayout();
		glayout.verticalSpacing = 0;
		glayout.marginHeight = 0;
		form.getBody().setLayout(glayout);
		
		final Section entityList = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED );
		entityList.setLayout(new GridLayout(1, false));
		entityList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityList.setText("Entity List");
		
		Composite main = toolkit.createComposite(entityList);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityList.setClient(main);
		
		// --- attribute table list
		entityTable = new EntityListTable(main);
		entityTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityTable.getViewer().addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editEntity();
			}
		});
		
		
		Composite buttonTableComp = toolkit.createComposite(main);
		buttonTableComp.setLayout(new GridLayout(4, false));
		
		Button btnAdd = toolkit.createButton(buttonTableComp, DialogConstants.ADD_BUTTON_TEXT, SWT.PUSH);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)btnAdd.getLayoutData()).widthHint = 100;
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addEntity();
			}
		});
		
		final Button btnEdit = toolkit.createButton(buttonTableComp, DialogConstants.EDIT_BUTTON_TEXT, SWT.PUSH);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)btnEdit.getLayoutData()).widthHint = 100;
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editEntity();
			}
		});
		
		final Button btnDelete = toolkit.createButton(buttonTableComp, DialogConstants.DELETE_BUTTON_TEXT, SWT.PUSH);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)btnDelete.getLayoutData()).widthHint = 100;
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteEntity();
			}
		});

		
		final Section entityDetailsSection = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		entityDetailsSection.setLayout(new GridLayout(1, false));
		entityDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityDetailsSection.setText("Entity Details");
		entityDetailsSection.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				if (entityDetailsSection.isExpanded()){
					entityDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));			
				}else{
					entityDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}
				entityDetailsSection.getParent().layout(true, true);
			}
		});
		Composite entityDetails = toolkit.createComposite(entityDetailsSection);
		entityDetailsSection.setClient(entityDetails);
		entityDetails.setLayout(new GridLayout());
		entityDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	
		entityInfoPanel = new EntityInfoPanelComposite(entityDetails);
		entityInfoPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		entityTable.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Entity e = (Entity) ((IStructuredSelection)entityTable.getSelection()).getFirstElement();
				entityInfoPanel.setEntity(e);
			}
		});
		
		btnEdit.setEnabled(false);
		btnDelete.setEnabled(false);
		entityTable.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnEdit.setEnabled(!entityTable.getSelection().isEmpty());
				btnDelete.setEnabled(!entityTable.getSelection().isEmpty());
			}
		});
	}
	
	
	private void addEntity(){
		NewEntityDialog dialog = new NewEntityDialog(getSite().getShell(), parentEditor.getEntityType(),null);
		if (dialog.open() == NewEntityDialog.OK){
			try{
				EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_ADDED, dialog.getEntity());
			}catch (Exception ex){
				EntityPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
	}
	
	
	private void deleteEntity(){
		if (entityTable.getSelection().isEmpty()){
			return;
		}
		
		final List<Entity> toDelete = new ArrayList<Entity>();
		for (Iterator<?> iterator = ((StructuredSelection)entityTable.getSelection()).iterator(); iterator.hasNext();) {
			Entity entity = (Entity) iterator.next();
			toDelete.add(entity);
		}
		
		String message = null;
		if (toDelete.size() == 1){
			message = MessageFormat.format("Are you sure you want to delete the entity {0}? The associated list item from the data model attribute {1} will also be deleted. This action cannot be undone.",
					new Object[]{toDelete.get(0).getId(), parentEditor.getEntityType().getDmAttribute().getName()});
		}else{
			message = MessageFormat.format("Are you sure you want to delete the {0,number,integer} selected entities? The associated list items from the data model attribute {1} will also be deleted. This action cannot be undone.",
					new Object[]{toDelete.size(), toDelete.get(0).getEntityType().getDmAttribute().getName()});
		}
		if (!MessageDialog.openConfirm(getSite().getShell(), "Delete", message)){
			return;
		}
		
		ProgressMonitorDialog deleteDialog = new ProgressMonitorDialog(getSite().getShell());
		try{
		deleteDialog.run(true, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				for (final Entity e : toDelete){
					deleteEntity(e);
				}
			}
		});
		}catch (Exception ex){
			EntityPlugIn.displayLog("Error deleting entities.  Please close and re-open the editor. " + "\n\n" + ex.getMessage(), ex);
		}
		
		try{
			DataModelManager.getInstance().fireChangeListeners();
		}catch(Exception ex){
			EntityPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}
	
	private void deleteEntity(final Entity e){
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		
		try{
			s.update(e);
			//-- check if the entity can be removed
			String errorMessage = null;
			try{
				if (!DeleteManager.canDelete(e, s)){
					errorMessage = MessageFormat.format("The entity {0} cannot be removed.", new Object[]{e.getId()});
				}
			}catch (Exception ex){
				EntityPlugIn.log(ex.getMessage(), ex);
				errorMessage = MessageFormat.format("The entity {0} cannot be removed." + "\n\n" + ex.getMessage(), new Object[]{e.getId()});
			}
			if (errorMessage != null){
				final String lerror = errorMessage;
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openInformation(getSite().getShell(), "Delete Entity",lerror);
					}});
				return;
			}
			AttributeListItem itemToDelete = e.getAttributeListItem();
			s.update(itemToDelete.getAttribute());
			s.delete(e);
			
			//at this point we should try to delete the attribute list item as well
			errorMessage = null;
			try{
				if (!DeleteManager.canDelete(itemToDelete, s)){
					errorMessage = MessageFormat.format("The entity {0} cannot be removed because the attribute list item {1} cannot be removed.", new Object[]{e.getId(), itemToDelete.getName()});
				}
			}catch (Exception ex){
				EntityPlugIn.log(ex.getMessage(), ex);
				errorMessage = MessageFormat.format("The entity {0} cannot be removed because the attribute list item {1} cannot be removed." + "\n\n" + ex.getMessage(), new Object[]{e.getId(), itemToDelete.getName()});
			}

			if (errorMessage != null){
				final String error = errorMessage;
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openInformation(getSite().getShell(), "Delete Entity",error);
					}});
				return;
			}
						
			//remove attribute list item
			
			itemToDelete.getAttribute().getAttributeList().remove(itemToDelete);
			itemToDelete.setAttribute(null);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			EntityPlugIn.displayLog(MessageFormat.format("Error deleting the entity {0}.  " + "\n\n" + ex.getMessage(), new Object[]{e.getId()}), ex);
		}finally{
			if (s.getTransaction().isActive()){
				s.getTransaction().rollback();
			}
			s.close();
		}
		try{
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_DELETED, e);
				}
			});
		}catch(Exception ex){
			EntityPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}
	
	private void editEntity(){
		if (entityTable.getSelection().isEmpty()){
			return;
		}
		Entity toUpdate = (Entity) ((IStructuredSelection)entityTable.getSelection()).getFirstElement();
		NewEntityDialog dialog = new NewEntityDialog(getSite().getShell(), parentEditor.getEntityType(), toUpdate);
		if (dialog.open() == NewEntityDialog.OK){
			try{
				EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_MODIFIED, toUpdate);
			}catch (Exception ex){
				EntityPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
	}
	
	
	
	/**
	 * Updates the table structure widgets with the value from the plan.
	 */
	public void updatePage(Session currentSession, boolean typeModified) {
		EntityType type = this.parentEditor.getEntityType();
		form.setText(getEditorInput().getName());

		if (typeModified) {
			entityTable.setEntityType(type);
			entityInfoPanel.setEntityType(type);
		}else{
			entityInfoPanel.initEntityFields();
		}
		entityTable.getViewer().setInput(type.getEntities());
	}
	
	@Override
	public void setFocus() {
		entityTable.getViewer().getTable().setFocus();
	}

}