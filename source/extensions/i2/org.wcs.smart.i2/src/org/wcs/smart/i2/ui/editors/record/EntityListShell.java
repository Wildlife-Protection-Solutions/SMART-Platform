package org.wcs.smart.i2.ui.editors.record;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.ui.properties.DialogConstants;

public class EntityListShell implements Listener {
	
	private static final String ALL_ENTITIES = "All Entities >";
	
	private Shell shell;
	private Shell hiddenParent;
	
	private TableViewer tblSearchEntityList;
	private TableViewer tblEntityTypeList;
	private TableViewer tblEntityList;
	
	private IntelEntity selectedEntity;
	private RecordEditor editor;
	
	private IntelEntityType lastSelectedType;
	
	
	public EntityListShell(Display ownerDisplay, RecordEditor editor){
		
		hiddenParent = new Shell(ownerDisplay);
		
		shell = new Shell(hiddenParent, SWT.NO_TRIM );
		shell.setLayout(createGridLayoutNoMargin(1));
		
		shell.addListener(SWT.Dispose, this);
		shell.addListener(SWT.Deactivate, this);

		Composite owner = new Composite(shell, SWT.NONE);
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
		});
		
		tblSearchEntityList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selection = ((StructuredSelection)tblSearchEntityList.getSelection()).getFirstElement();
				if (selection instanceof IntelEntity){
					selectedEntity = (IntelEntity) selection;
					shell.close();
				}else if (selection == ALL_ENTITIES){
					if (tblEntityTypeList == null){
						createEntityListTable(owner);
						((GridLayout)owner.getLayout()).numColumns = 2;
						owner.layout();
					}
					tblEntityTypeList.getTable().setVisible(true);
					tblEntityTypeList.setInput(new String[]{DialogConstants.LOADING_TEXT});
					loadEntityTypesJob.schedule();
				}
			}
		});
		
		
		List<IntelEntity> entities = (List<IntelEntity>) editor.getContext().get(EntitySearchView.ENTITY_SEARCH_RESULTS_KEY);
		List<Object> allItems = new ArrayList<Object>();
		allItems.add(ALL_ENTITIES);
		allItems.addAll(entities);
		
		tblSearchEntityList.setInput(allItems);
		
		shell.setSize(400, 200);		
	}
	
	public Shell getShell(){
		return shell;
	}
	
	private void createEntityListTable(Composite parent){
		tblEntityTypeList = new TableViewer(parent, SWT.BORDER);
		tblEntityTypeList.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblEntityTypeList.setContentProvider(ArrayContentProvider.getInstance());
		tblEntityTypeList.setLabelProvider(EntityTypeLabelProvider.INSTANCE);
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
									shell.close();
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

	@Override
	public void handleEvent(Event event) {
		if (event.type == SWT.Dispose){
			hiddenParent.dispose();
			return;
		}
		if (event.type == SWT.Deactivate){
			getShell().close();
			return;
		}
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
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
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
