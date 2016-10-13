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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.ui.SmartShellDialog;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.i2.ui.views.EntitySearchView.EntitySearchViewWrapper;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Shell dialog for relating to entities
 * 
 * @author Emily
 *
 */
public abstract class EntityRelationshipListShell extends SmartShellDialog {

	private IntelEntity srcEntity;
	private IntelRelationshipType type;
	private IntelEntity targetEntity;
	
	private TableViewer types;
		
	public EntityRelationshipListShell(Shell owner, IntelEntity srcEntity){
		super(owner);
		this.srcEntity = srcEntity;
	}
	
	@Override
	public void createContents(Composite parent){
		parent.setLayout(new GridLayout(2, true));
		TableViewer entityListTable = new TableViewer(parent, SWT.BORDER);
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
		
		types = new TableViewer(parent, SWT.BORDER);
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
				doEvent();
			}
		});
		
		EntitySearchView view = ((EntitySearchViewWrapper) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(EntitySearchView.ID)).getComponent();
		if (view != null){
			entityListTable.setInput(view.getEntities());
		}
	}
	
	protected abstract void doEvent();
	
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

		@SuppressWarnings("unchecked")
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
					i.getAttributes().size();
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
