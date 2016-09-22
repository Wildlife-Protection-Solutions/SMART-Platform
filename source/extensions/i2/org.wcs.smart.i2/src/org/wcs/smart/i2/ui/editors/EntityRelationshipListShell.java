package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.i2.ui.views.EntitySearchView.EntitySearchViewWrapper;
import org.wcs.smart.ui.properties.DialogConstants;

public class EntityRelationshipListShell {

	private IntelEntity srcEntity;
	private Shell shell;
	
	private Shell hiddenParent;
	
	private IntelRelationshipType type;
	private IntelEntity targetEntity;
	
	private TableViewer types;
	
	public EntityRelationshipListShell(Display owner, IntelEntity srcEntity){
		this.srcEntity = srcEntity;
		
		hiddenParent = new Shell(owner);
		
		shell = new Shell(hiddenParent, SWT.NO_TRIM );
		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				hiddenParent.dispose();
				
			}
		});
		
		shell.setLayout(createGridLayoutNoMargin(2));
		shell.addListener(SWT.Deactivate, new Listener(){
			@Override
			public void handleEvent(Event event) {
				getShell().close();
			}
			
		});
		
		TableViewer entityListTable = new TableViewer(shell, SWT.BORDER);
		entityListTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityListTable.setContentProvider(ArrayContentProvider.getInstance());
		entityListTable.setLabelProvider(new LabelProvider(){
			@Override
			public Image getImage(Object element){
				if (element instanceof IntelEntity){
					return EntityTypeLabelProvider.INSTANCE.getImage(((IntelEntity) element).getEntityType());
				}
				return super.getImage(element);
			}
			@Override
			public String getText(Object element){
				if (element instanceof IntelEntity){
					return ((IntelEntity) element).getIdAttributeAsText();
				}
				return super.getText(element);
			}
		});
		entityListTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = ((IStructuredSelection)entityListTable.getSelection());
				if (!sel.isEmpty()){
					if (sel.getFirstElement() instanceof IntelEntity){
						type = null;
						targetEntity = (IntelEntity) sel.getFirstElement();
						types.setInput(new String[]{DialogConstants.LOADING_TEXT});
						relationshipSearchJob.schedule(0);
					}
					
				}
			}
		});
		
		types = new TableViewer(shell, SWT.BORDER);
		types.setContentProvider(ArrayContentProvider.getInstance());
		types.setLabelProvider(RelationshipTypeLabelProvider.INSTANCE);
		types.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		types.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IntelRelationshipType selection = null;
				if (!types.getSelection().isEmpty()){
					Object x = ((IStructuredSelection)types.getSelection()).getFirstElement();
					if (x instanceof IntelRelationshipType){
						selection = (IntelRelationshipType) x;
					}
				}
				setRelationshipType(selection);
				shell.close();
			}
		});
		
		EntitySearchView view = ((EntitySearchViewWrapper) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(EntitySearchView.ID)).getComponent();
		if (view != null){
			entityListTable.setInput(view.getEntities());
		}
		
		shell.setSize(400, 200);
	}
	
	public Shell getShell(){
		return shell;
	}
	
	private GridLayout createGridLayoutNoMargin(int col){
		GridLayout gd = new GridLayout(col, true);
		gd.marginWidth = 0;
		gd.marginHeight = 0;
		return gd;
	}
	
	private void setRelationshipType(IntelRelationshipType type){
		this.type = type;
	}
	public IntelRelationshipType getRelationshipType(){
		return this.type;
	}
	
	public IntelEntity getTargetEntity(){
		return this.targetEntity;
	}
	
	
	private Job relationshipSearchJob = new Job("relationship search"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			final List<IntelRelationshipType> rtypes = new ArrayList<IntelRelationshipType>();
			try{
				rtypes.addAll(s.createCriteria(IntelRelationshipType.class)
				.add(Restrictions.or (
						Restrictions.and(Restrictions.eq("sourceEntityType", srcEntity.getEntityType()), 
								Restrictions.eq("targetEntityType", targetEntity.getEntityType())),
						Restrictions.and(Restrictions.eq("sourceEntityType", targetEntity.getEntityType()), 
								Restrictions.eq("targetEntityType", srcEntity.getEntityType())),
								Restrictions.isNull("sourceEntityType"),
								Restrictions.isNull("targetEntityType")
						))
						.list());
				for (IntelRelationshipType i : rtypes){
					i.getSourceEntityType();
					i.getTargetEntityType();
					if (i.getRelationshipGroup() != null){
						i.getRelationshipGroup().getName();
					}
				}
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					if (rtypes.isEmpty()){
						types.setInput(new String[]{"No relationship types found for given entity types"});
					}else{
						types.setInput(rtypes);
					}
					types.refresh();
				}
				
			});
			return Status.OK_STATUS;
		} 
		
	};

}
