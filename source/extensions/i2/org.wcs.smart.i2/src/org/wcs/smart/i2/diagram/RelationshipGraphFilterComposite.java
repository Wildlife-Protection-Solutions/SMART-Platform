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
package org.wcs.smart.i2.diagram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.ui.CheckBoxDropDown;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Composite that contains relationship graph filtering components.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphFilterComposite extends Composite {
	
	private static final List<Integer> GRAPH_DEPTH_OPTIONS = Arrays.asList(1 ,2, 3, 4);
	
	private ComboViewer cmbDepth;
	private CheckBoxDropDown cmbEntityTypes;
	private CheckBoxDropDown cmbRelationTypes;
	
	private RelationshipGraphFilterData filterData;
	
	private List<IRelationshipGraphFilterChangeListener> listeners = new ArrayList<>();
	
	private LoadEntityTypeJob entityTypeJob = new LoadEntityTypeJob(true) {
		@Override
		protected void processData(List<IntelEntityType> types) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					cmbEntityTypes.setInput(types);
					cmbEntityTypes.setValue(types);
					filterData.setEntityTypes(types);
					fireFilterChanged(filterData);
				}		
			});
		}
	};
	
	private LoadRelationshipTypeJob relationshipTypeJob = new LoadRelationshipTypeJob(true) {
		@Override
		protected void processData(List<IntelRelationshipType> types) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (cmbRelationTypes.isDisposed()) return;
					cmbRelationTypes.setInput(types);
					cmbRelationTypes.setValue(types);
					filterData.setRelationshipTypes(types);
					fireFilterChanged(filterData);
				}		
			});
		}
	};

	private EventHandler entityTypesHandler = new EventHandler() {
		@Override
		public void handleEvent(Event event) {
			entityTypeJob.cancel();
			entityTypeJob.schedule();
		}
	};

	private EventHandler relationshipTypesHandler = new EventHandler() {
		@Override
		public void handleEvent(Event event) {
			relationshipTypeJob.cancel();
			relationshipTypeJob.schedule();
		}
	};
	
	public RelationshipGraphFilterComposite(Composite parent) {
		super(parent, SWT.NONE);
		filterData = new RelationshipGraphFilterData();
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = layout.marginHeight = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createContent(this);
	}

	private void createContent(Composite parent) {
		Group grp = new Group(parent, SWT.NONE );
		grp.setLayout(new GridLayout(2, false));
		grp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		grp.setText(Messages.RelationshipGraphFilterComposite_Filter);
		
		Label lblDepth = new Label(grp, SWT.NONE);
		lblDepth.setText(Messages.RelationshipGraphFilterComposite_Depth);

		cmbDepth = new ComboViewer(grp, SWT.READ_ONLY | SWT.BORDER);
		cmbDepth.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		cmbDepth.getControl().setBackground(cmbDepth.getControl().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbDepth.setContentProvider(ArrayContentProvider.getInstance());
		cmbDepth.setInput(GRAPH_DEPTH_OPTIONS);
		cmbDepth.setSelection(new StructuredSelection(filterData.getDepth()));
		cmbDepth.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) cmbDepth.getSelection();
				filterData.setDepth((Integer)selection.getFirstElement());
				fireFilterChanged(filterData);
			}
		});
		
		Label lblEntityTypes = new Label(grp, SWT.NONE);
		lblEntityTypes.setText(Messages.RelationshipGraphFilterComposite_EntityTypes);

		cmbEntityTypes = new CheckBoxDropDown(grp);
		cmbEntityTypes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbEntityTypes.setLabelProvider(new EntityTypeLabelProvider());
		cmbEntityTypes.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityTypes.setInput(Arrays.asList(DialogConstants.LOADING_TEXT));
		cmbEntityTypes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				@SuppressWarnings("unchecked")
				List<IntelEntityType> types = (List<IntelEntityType>) cmbEntityTypes.getCheckObjects();
				filterData.setEntityTypes(types);
				fireFilterChanged(filterData);
			}
		});
		
		Label lblRelationTypes = new Label(grp, SWT.NONE);
		lblRelationTypes.setText(Messages.RelationshipGraphFilterComposite_RelationshipTypes);

		cmbRelationTypes = new CheckBoxDropDown(grp);
		cmbRelationTypes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbRelationTypes.setLabelProvider(new RelationshipTypeLabelProvider());
		cmbRelationTypes.setContentProvider(ArrayContentProvider.getInstance());
		cmbRelationTypes.setInput(Arrays.asList(DialogConstants.LOADING_TEXT));
		cmbRelationTypes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				@SuppressWarnings("unchecked")
				List<IntelRelationshipType> types = (List<IntelRelationshipType>) cmbRelationTypes.getCheckObjects();
				filterData.setRelationshipTypes(types);
				fireFilterChanged(filterData);
			}
		});

		IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		eventBroker.subscribe(IntelEvents.ACTIVE_PROFILES, entityTypesHandler);
		eventBroker.subscribe(IntelEvents.ACTIVE_PROFILES, relationshipTypesHandler);
		eventBroker.subscribe(IntelEvents.ENTITY_TYPE_ALL, entityTypesHandler);
		eventBroker.subscribe(IntelEvents.RELATION_TYPE_ALL, relationshipTypesHandler);
		
		entityTypeJob.cancel();
		entityTypeJob.schedule();

		relationshipTypeJob.cancel();
		relationshipTypeJob.schedule();
	}

	void setFilterDepth(int depth) {
		filterData.setDepth(depth);
		cmbDepth.setSelection(new StructuredSelection(depth));
	}
	
	@Override
	public void dispose() {
		super.dispose();
		IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		eventBroker.unsubscribe(entityTypesHandler);
		eventBroker.unsubscribe(relationshipTypesHandler);
	}
	
	public RelationshipGraphFilterData getFilterData() {
		return filterData;
	}

	public void addFilterChangeListener(IRelationshipGraphFilterChangeListener listener) {
		listeners.add(listener);
	}
	
	private void fireFilterChanged(RelationshipGraphFilterData data) {
		for (IRelationshipGraphFilterChangeListener l : listeners) {
			l.filterChanged(data);
		}
	}
	
}
