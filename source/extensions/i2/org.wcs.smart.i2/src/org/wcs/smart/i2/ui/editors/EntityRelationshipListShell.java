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
import java.util.Collections;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.ui.SmartShellDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;

/**
 * Shell dialog for relating entities
 * 
 * @author Emily
 *
 */
public abstract class EntityRelationshipListShell extends SmartShellDialog {

	private IntelEntity srcEntity;
	private IntelRelationshipType type;
	private IntelEntity targetEntity;
	
	private TableViewer types;
	
	private List<Object> entityOptions = null;
	private IEclipseContext context;
	
	public EntityRelationshipListShell(Shell owner, IntelEntity srcEntity, List<IntelEntity> entityOptions, IEclipseContext context){
		super(owner);
		this.context = context;
		this.srcEntity = srcEntity;
		this.entityOptions = new ArrayList<>();
		if (entityOptions != null) entityOptions.forEach(o->this.entityOptions.add(o));
	}
	
	public EntityRelationshipListShell(Shell owner, IntelEntity srcEntity, IEclipseContext context){
		this(owner, srcEntity, null, context);
		entityOptions = Collections.singletonList(Messages.EntityRelationshipListShell_NoEntitiesFound);
		
		if (this.context != null) {
			MPart part = context.get(EPartService.class).findPart(EntitySearchView.ID);			
			if (part != null) {
				EntitySearchView view  = (EntitySearchView) E3Utils.getSourceObject(part);
				List<? extends Object> entities = view.getEntities();
				if (entities != null) {
					entityOptions = new ArrayList<>();
					entities.forEach(e->entityOptions.add(e));
				}
			}
			
		}
	}
	
	@Override
	public void createContents(Composite parent){
		parent.setLayout(new GridLayout(2, true));
		
		Composite wrapper = new Composite(parent, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableViewer entityListTable = new TableViewer(wrapper, SWT.BORDER);
		entityListTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityListTable.setContentProvider(ArrayContentProvider.getInstance());
		entityListTable.setLabelProvider(new EntityTypeLabelProvider(){
			@Override
			public Image getImage(Object element){
				if (element instanceof IntelEntity){
					return getImage(((IntelEntity) element).getEntityType());
				}
				return null;
			}
			@Override
			public String getText(Object element){
				if (element instanceof IntelEntity){
					return ((IntelEntity) element).getIdAttributeAsText();
				}
				return super.getText(element);
			}
		});
		TableColumn tc = new TableColumn(entityListTable.getTable(), SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		layout.setColumnData(tc, new ColumnWeightData(100));
		wrapper.setLayout(layout);
		
		entityListTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = ((IStructuredSelection)entityListTable.getSelection());
				if (!sel.isEmpty()){
					if (sel.getFirstElement() instanceof IntelEntity){
						type = null;
						targetEntity = (IntelEntity) sel.getFirstElement();
						types.setInput(new String[]{DialogConstants.LOADING_TEXT});
						(new RelationshipSearchJob(srcEntity.getProfile(), srcEntity.getEntityType(), targetEntity.getProfile(), targetEntity.getEntityType()) {
							@Override
							protected void afterLoad() {
								Display.getDefault().syncExec(()->{
									if (types.getControl().isDisposed()) return;
										if (rtypes.isEmpty()){
											types.setInput(new String[]{Messages.EntityRelationshipListShell_NoRelationshipsFound});
										}else{
											types.setInput(rtypes);
										}
										types.refresh();
									}
								);
							}
						}).schedule(0);
					}else {
						types.setInput(new Object[] {});
					}
					
				}
			}
		});
		entityListTable.setInput(entityOptions);
		
		wrapper = new Composite(parent, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		types = new TableViewer(wrapper, SWT.BORDER);
		types.setContentProvider(ArrayContentProvider.getInstance());
		types.setLabelProvider(new RelationshipTypeLabelProvider());
		
		tc = new TableColumn(types.getTable(), SWT.NONE);
		layout = new TableColumnLayout();
		layout.setColumnData(tc, new ColumnWeightData(100));
		wrapper.setLayout(layout);
		
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
	}
	
	/**
	 * Called once an entity and relationship type are selected
	 */
	protected abstract void doEvent();
	
	/*
	 * Updates the relationship type
	 */
	private void setRelationshipType(IntelRelationshipType type){
		this.type = type;
	}
	/**
	 * Gets the selected relationships type
	 * @return
	 */
	public IntelRelationshipType getRelationshipType(){
		return this.type;
	}
	
	/**
	 * Gets the target entity
	 * @return
	 */
	public IntelEntity getTargetEntity(){
		return this.targetEntity;
	}


}
