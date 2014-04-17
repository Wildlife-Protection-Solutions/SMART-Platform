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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.refractions.udig.project.ui.ApplicationGIS;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.EntityCsvExporter;
import org.wcs.smart.entity.EntityCsvImporter;
import org.wcs.smart.entity.EntityPermissionManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.ExportEntityDialog;
import org.wcs.smart.entity.ui.importwizard.ImportEntitiesWizard;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Editor page for managing entities.
 * 
 * @author Emily
 *
 */
public class EntitiesPage extends EditorPart implements IEntityTypeEditorPage {
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private Form form;

	private EntityListTable entityTable;
	private EntityTypeEditor parentEditor;
	
	private EntityInfoPanelComposite entityInfoPanel;
	
	private Button btnEdit = null;
	private Button btnDelete = null;
	private Button btnExport = null;
			
	/**
	 * Creates a new plan editor page
	 * @param editor
	 */
	public EntitiesPage(EntityTypeEditor editor){
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
		
		final SashForm sashForm = new SashForm(form.getBody(), SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		final Section entityList = toolkit.createSection(sashForm, Section.TITLE_BAR | Section.EXPANDED );
		entityList.setLayout(new GridLayout(1, false));
		entityList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityList.setText(Messages.EntityTypeEntitiesPage_EntityListSectionTitle);
		
		Composite main = toolkit.createComposite(entityList);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityList.setClient(main);

		// --- attribute table list
		entityTable = new EntityListTable(main);
		entityTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)entityTable.getLayoutData()).heightHint = 200;
		entityTable.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editEntity();
			}
		});
		

		int buttonSize = 80;
		if (EntityPermissionManager.canCreateEditDeleteEntities()){
			Composite buttonTableComp = toolkit.createComposite(main);
			buttonTableComp.setLayout(new GridLayout(6, false));
			buttonTableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
		
			Button btnAdd = toolkit.createButton(buttonTableComp, DialogConstants.ADD_BUTTON_TEXT, SWT.PUSH);
			btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)btnAdd.getLayoutData()).widthHint = buttonSize;
			btnAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					addEntity();
				}
			});
		
			btnEdit = toolkit.createButton(buttonTableComp, DialogConstants.EDIT_BUTTON_TEXT, SWT.PUSH);
			btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)btnEdit.getLayoutData()).widthHint = buttonSize;
			btnEdit.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					editEntity();
				}
			});
		
			btnDelete = toolkit.createButton(buttonTableComp, DialogConstants.DELETE_BUTTON_TEXT, SWT.PUSH);
			btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)btnDelete.getLayoutData()).widthHint = buttonSize;
			btnDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteEntity();
				}
			});
			btnEdit.setEnabled(false);
			btnDelete.setEnabled(false);
			
			Label l = toolkit.createLabel(buttonTableComp, ""); //$NON-NLS-1$
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						
			Button btnImport = toolkit.createButton(buttonTableComp, DialogConstants.IMPORT_BUTTON_TEXT, SWT.PUSH);
			btnImport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)btnImport.getLayoutData()).widthHint = buttonSize;
			btnImport.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					importEntities();
				}
			});

			btnExport = toolkit.createButton(buttonTableComp, DialogConstants.EXPORT_BUTTON_TEXT, SWT.PUSH);
			btnExport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)btnExport.getLayoutData()).widthHint = buttonSize;
			btnExport.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					exportEntities();
				}
			});
		}else if (SmartDB.isMultipleAnalysis()){
			Composite buttonTableComp = toolkit.createComposite(main);
			GridLayout gl = new GridLayout();
			gl.marginWidth = gl.marginHeight = 0;
			buttonTableComp.setLayout(gl);
			buttonTableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			btnExport = toolkit.createButton(buttonTableComp, DialogConstants.EXPORT_BUTTON_TEXT, SWT.PUSH);
			btnExport.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
			((GridData)btnExport.getLayoutData()).widthHint = buttonSize;
			btnExport.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					exportEntities();
				}
			});
		}
		
		final Section entityDetailsSection = toolkit.createSection(sashForm, Section.TITLE_BAR | Section.EXPANDED);
		entityDetailsSection.setLayout(new GridLayout(1, false));
		entityDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityDetailsSection.setText(Messages.EntityTypeEntitiesPage_EntityDetailsSectionTitle);
		
		Composite entityDetails = toolkit.createComposite(entityDetailsSection);
		entityDetailsSection.setClient(entityDetails);
		entityDetails.setLayout(new GridLayout());
		entityDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	
		entityInfoPanel = new EntityInfoPanelComposite(entityDetails);
		entityInfoPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		entityTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (entityTable.getSelection().isEmpty()){
					entityInfoPanel.setEntity(null);
				}else{
					Entity e = (Entity) ((IStructuredSelection)entityTable.getSelection()).getFirstElement();
					entityInfoPanel.setEntity(e);
				}
			}
		});
		
		
		if (btnEdit != null){
			entityTable.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					btnEdit.setEnabled(!entityTable.getSelection().isEmpty());
					btnDelete.setEnabled(!entityTable.getSelection().isEmpty());
				}
			});
		}
		
		sashForm.setWeights(new int[]{70,30});
	}
	
	private void importEntities(){
		EntityCsvImporter importer = new EntityCsvImporter(parentEditor.getEntityType());
		WizardDialog wd = new WizardDialog(getSite().getShell(), new ImportEntitiesWizard(importer));
		wd.open();
	}
	
	private void exportEntities(){
		EntityType et = parentEditor.getEntityType();
		EntityCsvExporter exporter = new EntityCsvExporter(et);
		ExportEntityDialog dialog = new ExportEntityDialog(getSite().getShell(), exporter);
		dialog.open();
	}
	/*
	 * Adds an entity
	 */
	private void addEntity(){
		NewEntityDialog dialog = new NewEntityDialog(getSite().getShell(), parentEditor.getEntityType(),null);
		if (dialog.open() == NewEntityDialog.OK){
			try{
				EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_ADDED, dialog.getEntity());
			}catch (Exception ex){
				EntityPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		ApplicationGIS.getToolManager().setCurrentEditor(parentEditor);
		entityTable.setSelection(new StructuredSelection(dialog.getEntity()), true);
	}
	
	/*
	 * Deletes entities
	 */
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
			message = MessageFormat.format(Messages.EntityTypeEntitiesPage_DeleteConfirmSingle,
					new Object[]{toDelete.get(0).getId(), parentEditor.getEntityType().getDmAttribute().getName()});
		}else{
			message = MessageFormat.format(Messages.EntityTypeEntitiesPage_DeleteConfirmMulti,
					new Object[]{toDelete.size(), toDelete.get(0).getEntityType().getDmAttribute().getName()});
		}
		if (!MessageDialog.openConfirm(getSite().getShell(), Messages.EntityTypeEntitiesPage_DeleteDialogTitle, message)){
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
			EntityPlugIn.displayLog(Messages.EntityTypeEntitiesPage_DeleteError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}
		
		try{
			DataModelManager.getInstance().fireChangeListeners();
		}catch(Exception ex){
			EntityPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}
	
	/*
	 * deletes a specific entity
	 */
	private void deleteEntity(final Entity e){
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		
		try{
			s.update(e);
			//-- check if the entity can be removed
			String errorMessage = null;
			try{
				if (!DeleteManager.canDelete(e, s)){
					errorMessage = MessageFormat.format(Messages.EntityTypeEntitiesPage_EntityDeleteError, new Object[]{e.getId()});
				}
			}catch (Exception ex){
				EntityPlugIn.log(ex.getMessage(), ex);
				errorMessage = MessageFormat.format(Messages.EntityTypeEntitiesPage_EntityDeleteError + "\n\n" + ex.getMessage(), new Object[]{e.getId()}); //$NON-NLS-1$
			}
			if (errorMessage != null){
				final String lerror = errorMessage;
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openInformation(getSite().getShell(), Messages.EntityTypeEntitiesPage_DeleteEntityDialogTitle,lerror);
					}});
				return;
			}
			s.flush();
			AttributeListItem itemToDelete = e.getAttributeListItem();
			s.update(itemToDelete.getAttribute());
			s.delete(e);
			s.flush();
			//at this point we should try to delete the attribute list item as well
			errorMessage = null;
			try{
				if (!DeleteManager.canDelete(itemToDelete, s)){
					errorMessage = MessageFormat.format(Messages.EntityTypeEntitiesPage_EntityCannotDelete, new Object[]{e.getId(), itemToDelete.getName()});
				}
			}catch (Exception ex){
				EntityPlugIn.log(ex.getMessage(), ex);
				errorMessage = MessageFormat.format(Messages.EntityTypeEntitiesPage_EntityCannotDelete + "\n\n" + ex.getMessage(), new Object[]{e.getId(), itemToDelete.getName()}); //$NON-NLS-1$
			}

			if (errorMessage != null){
				final String error = errorMessage;
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openInformation(getSite().getShell(), Messages.EntityTypeEntitiesPage_DeleteEntityDialogTitle,error);
					}});
				return;
			}
						
			//remove attribute list item
			itemToDelete.getAttribute().getAttributeList().remove(itemToDelete);
			itemToDelete.setAttribute(null);
			s.flush();
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			EntityPlugIn.displayLog(MessageFormat.format(Messages.EntityTypeEntitiesPage_DeleteEntityError + "\n\n" + ex.getMessage(), new Object[]{e.getId()}), ex); //$NON-NLS-1$
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
	
	/*
	 * edits the selected entity
	 */
	private void editEntity(){
		if (!EntityPermissionManager.canCreateEditDeleteEntities()){
			return;
		}
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
		ApplicationGIS.getToolManager().setCurrentEditor(parentEditor);
		entityTable.setSelection(new StructuredSelection(toUpdate), true);
	}
	
	
	
	/**
	 * Updates the entity table
	 */
	public void updatePage(Session currentSession, boolean typeModified) {
		EntityType type = this.parentEditor.getEntityType();
		form.setText(MessageFormat.format(Messages.EntitiesPage_PageName, new Object[]{getEditorInput().getName()}));
		
		entityTable.setInput(null);
		if (typeModified) {
			entityTable.setEntityType(type);
			entityInfoPanel.setEntityType(type);
		}else{
			entityInfoPanel.initEntityFields();
		}
		entityTable.setInput(parentEditor.getEntities(currentSession));
	}
	
	@Override
	public void setFocus() {
		entityTable.setFocus();
	}

}