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
package org.wcs.smart.i2.ui.editors.record;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.search.IntelEntitySearchResult;
import org.wcs.smart.i2.search.IntelSearchResult;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.SmartShellDialog;
import org.wcs.smart.i2.ui.dialogs.NewEntityDialog;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog shell for listing entities.  First list displays all entities in most recent search,
 * second list displays entity types and third list displays all entities of the given type.
 * 
 * @author Emily
 *
 */
public class EntityListShell extends SmartShellDialog {
	
	private static final String ALL_ENTITIES = "All Entities >";
	private static final String NEW_ENTITY = "New Entity...";

	private TableViewer tblSearchEntityList;
	private TableViewer tblEntityTypeList;
	private TableViewer tblEntityList;
	
	private IntelEntity selectedEntity;
	private RecordEditor editor;
	
	private IntelEntityType lastSelectedType;
	
	@Inject
	private IEclipseContext context;
	
	public EntityListShell(Shell ownerShell, RecordEditor editor){
		super(ownerShell);
		this.editor = editor;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void createContents(Composite parent){
		Composite owner = new Composite(parent, SWT.NONE);
		owner.setLayout(createGridLayoutNoMargin(1));
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblSearchEntityList = new TableViewer(owner, SWT.BORDER);
		tblSearchEntityList.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblSearchEntityList.setContentProvider(ArrayContentProvider.getInstance());
		tblSearchEntityList.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof IntelEntity) return ((IntelEntity) element).getIdAttributeAsText();
				return super.getText(element);
			}
			
			public Image getImage(Object element){
				if (element == ALL_ENTITIES) return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY);
				if (element == NEW_ENTITY) return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY_NEW);
				return null;
			}
		});
		
		tblSearchEntityList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selection = ((StructuredSelection)tblSearchEntityList.getSelection()).getFirstElement();
				if (selection instanceof IntelEntity){
					selectedEntity = (IntelEntity) selection;
					close();
				}else if (selection == ALL_ENTITIES){
					if (tblEntityTypeList == null){
						createEntityListTable(owner);
						((GridLayout)owner.getLayout()).numColumns = 2;
						owner.layout();
					}
					tblEntityTypeList.getTable().setVisible(true);
					tblEntityTypeList.setInput(new String[]{DialogConstants.LOADING_TEXT});
					loadEntityTypesJob.schedule();
				}else if (selection == NEW_ENTITY){
					NewEntityDialog dialog = new NewEntityDialog(editor.getSite().getShell());
					ContextInjectionFactory.inject(dialog, context);
					if (dialog.open() == NewEntityDialog.OK){
						editor.linkEntity(dialog.getNewEntity());
					}
				}
			}
		});
		
		
		List<IntelEntitySearchResult> entities = ((IntelSearchResult) editor.getContext().get(EntitySearchView.ENTITY_SEARCH_RESULTS_KEY)).getResults();
		List<Object> allItems = new ArrayList<Object>();
		allItems.add(ALL_ENTITIES);
		allItems.add(NEW_ENTITY);
		if (entities != null){
			entities.forEach(a -> allItems.add(a.getEntity()));
		}
		
		tblSearchEntityList.setInput(allItems);	
	}
	
	
	private void createEntityListTable(Composite parent){
		tblEntityTypeList = new TableViewer(parent, SWT.BORDER);
		tblEntityTypeList.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblEntityTypeList.setContentProvider(ArrayContentProvider.getInstance());
		tblEntityTypeList.setLabelProvider(new EntityTypeLabelProvider());
		tblEntityTypeList.getTable().setVisible(false);
		tblEntityTypeList.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selection = ((StructuredSelection)tblEntityTypeList.getSelection()).getFirstElement();
				if (selection instanceof IntelEntityType){
					if (tblEntityList == null){
						
						tblEntityList = new TableViewer(parent, SWT.BORDER);
						tblEntityList.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
						tblEntityList.setContentProvider(ArrayContentProvider.getInstance());
						tblEntityList.setLabelProvider(new LabelProvider(){
							@Override
							public String getText(Object element){
								if (element instanceof IntelEntity) return ((IntelEntity) element).getIdAttributeAsText();
								return super.getText(element);
							}
						});
						tblEntityList.addSelectionChangedListener(new ISelectionChangedListener() {
							@Override
							public void selectionChanged(SelectionChangedEvent event) {
								Object selection = ((StructuredSelection)tblEntityList.getSelection()).getFirstElement();
								if (selection instanceof IntelEntity){
									selectedEntity = (IntelEntity) selection;
									close();
								}
							}
						});
						
						shell.setSize(600, 200);		
						((GridLayout)parent.getLayout()).numColumns = 3;
						parent.layout(true);
					}
					
					lastSelectedType = (IntelEntityType) selection;
					tblEntityList.setInput(new String[]{DialogConstants.LOADING_TEXT});
					loadEntitiesJob.schedule();
				}
			}
		});
	}
	
	private GridLayout createGridLayoutNoMargin(int col){
		GridLayout gd = new GridLayout(col, true);
		gd.marginWidth = 0;
		gd.marginHeight = 0;
		return gd;
	}
	
	public IntelEntity getTargetEntity(){
		return this.selectedEntity;
	}
	
	private Job loadEntityTypesJob = new Job("load entity types"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelEntityType> types = new ArrayList<IntelEntityType>();
			Session s = HibernateManager.openSession();
			try{
				types.addAll(EntityTypeManager.INSTANCE.getEntityTypes(s, SmartDB.getCurrentConservationArea()));
			}finally{
				s.close();
			}
			Display.getDefault().syncExec(() -> {if (!tblEntityTypeList.getTable().isDisposed()){tblEntityTypeList.setInput(types);}});
			return Status.OK_STATUS;
		}
		
	};
	
	private Job loadEntitiesJob = new Job("load entities"){

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelEntity> entities = new ArrayList<IntelEntity>();
			if (lastSelectedType != null){
				Session s = HibernateManager.openSession();
				try{
					entities.addAll(s.createCriteria(IntelEntity.class)
					.add(Restrictions.eq("entityType", lastSelectedType))
					.list());
					for (IntelEntity e : entities){
						e.getIdAttributeAsText();
						try {
							e.getPrimaryAttachment().computeFileLocation(s);
						} catch (Exception ex) {
							Intelligence2PlugIn.log(ex.getMessage(), ex);
						}
					}
				}finally{
					s.close();
				}
			}
			Display.getDefault().syncExec(() -> {if (!tblEntityList.getTable().isDisposed()){tblEntityList.setInput(entities);}});
			return Status.OK_STATUS;
		}
	};

}
